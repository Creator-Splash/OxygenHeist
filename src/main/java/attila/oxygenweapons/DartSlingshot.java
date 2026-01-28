package attila.oxygenweapons;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class DartSlingshot implements Listener {

    private final Plugin plugin;
    
    private final Map<String, Integer> itemAmmo = new HashMap<>();
    private final Map<UUID, Long> reloadStart = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Map<UUID, Long> shotCooldown = new HashMap<>();
    private static final Map<UUID, String> lastGunUsed = new HashMap<>();
    
    private static final int MAX_DISTANCE = 15;
    private static final long SHOT_COOLDOWN = 500;

    private static class GunData {
        Material material;
        int base, aim;
        int[] reloadFrames;
        int maxAmmo;
        double damage;
        long reloadTime;
        String name;
        double aimPrecisionBonus;

        GunData(Material material, int base, int aim, int[] reloadFrames, int maxAmmo,
                double damage, long reloadTime, String name, double aimPrecisionBonus) {
            this.material = material;
            this.base = base;
            this.aim = aim;
            this.reloadFrames = reloadFrames;
            this.maxAmmo = maxAmmo;
            this.damage = damage;
            this.reloadTime = reloadTime;
            this.name = name;
            this.aimPrecisionBonus = aimPrecisionBonus;
        }

        boolean isGun(ItemStack item) {
            if (item == null || item.getType() != material || !item.hasItemMeta()) return false;
            int cmd = item.getItemMeta().getCustomModelData();
            if (cmd == base || cmd == aim) return true;
            for (int r : reloadFrames) if (cmd == r) return true;
            return false;
        }
    }

    private final GunData gun = new GunData(
            Material.GOLDEN_HORSE_ARMOR,
            6001,
            6002,
            new int[]{6003},
            3,
            2.0,
            2500,
            "Dart Slingshot",
            0.7
    );

    public DartSlingshot(Plugin plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    ItemStack item = player.getInventory().getItemInMainHand();

                    if (!gun.isGun(item)) continue;

                    String itemId = ensureItemId(item);
                    int currentAmmo = getAmmo(itemId);

                    if (!reloading.contains(uuid)) {
                        player.sendActionBar(ChatColor.YELLOW + "Ammo: " + ChatColor.WHITE + currentAmmo + "/" + gun.maxAmmo);
                    }

                    if (reloading.contains(uuid)) {
                        if (player.getInventory().getHeldItemSlot() != reloadSlot.get(uuid)) {
                            cancelReload(player);
                            continue;
                        }

                        long elapsed = System.currentTimeMillis() - reloadStart.get(uuid);

                        if (elapsed >= gun.reloadTime) {
                            setAmmo(itemId, gun.maxAmmo);
                            setCMD(item, gun.base);
                            reloading.remove(uuid);
                            reloadStart.remove(uuid);
                            reloadSlot.remove(uuid);
                            player.sendActionBar(ChatColor.GREEN + "Weapon Reloaded!");
                        } else {
                            double progress = (double) elapsed / gun.reloadTime;
                            player.sendActionBar(progressBar(progress));

                            long phase = Math.max(1, gun.reloadTime / gun.reloadFrames.length);
                            int index = (int) (elapsed / phase);
                            if (index >= gun.reloadFrames.length) index = gun.reloadFrames.length - 1;
                            setCMD(item, gun.reloadFrames[index]);
                        }
                    }

                    if (getCMD(item) == gun.aim && !player.isSneaking()) {
                        setCMD(item, gun.base);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;

        if (event.isSneaking() && getCMD(item) == gun.base) {
            setCMD(item, gun.aim);
        }
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        if (reloading.contains(uuid)) return;

        if (shotCooldown.containsKey(uuid)) {
            long timeSinceLastShot = System.currentTimeMillis() - shotCooldown.get(uuid);
            if (timeSinceLastShot < SHOT_COOLDOWN) {
                return;
            }
        }

        String itemId = ensureItemId(item);
        int currentAmmo = getAmmo(itemId);

        if (currentAmmo < 1) {
            player.sendActionBar(ChatColor.RED + "No ammo! Reloading...");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.5f, 1.2f);
            forceReload(player);
            return;
        }

        boolean isAiming = getCMD(item) == gun.aim;
        fireDart(player, item, itemId, isAiming);

        shotCooldown.put(uuid, System.currentTimeMillis());

        setAmmo(itemId, currentAmmo - 1);

        if (currentAmmo - 1 <= 0) {
            forceReload(player);
        }

        player.setMetadata("dart_recent", new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("dart_recent", plugin);
            }
        }.runTaskLater(plugin, 100L);
    }

    private void fireDart(Player player, ItemStack item, String itemId, boolean isAiming) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.3f, 1.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BAMBOO_HIT, 0.4f, 1.5f);

        Snowball snowball = player.launchProjectile(Snowball.class);
        
        Vector velocity = snowball.getVelocity();
        velocity.multiply(2.5);
        snowball.setVelocity(velocity);
        
        ItemStack dartItem = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = dartItem.getItemMeta();
        meta.setCustomModelData(6000);
        meta.setDisplayName(ChatColor.DARK_GREEN + "Poison Dart");
        dartItem.setItemMeta(meta);
        snowball.setItem(dartItem);
        
        double spreadMultiplier = isAiming ? gun.aimPrecisionBonus : 1.0;
        if (spreadMultiplier > 0.7) {
            Vector vel = snowball.getVelocity();
            Random random = new Random();
            double spread = 0.015 * spreadMultiplier;
            vel.add(new Vector(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread
            ));
            snowball.setVelocity(vel);
        }
        
        snowball.setMetadata("dart", new FixedMetadataValue(plugin, true));
        snowball.setMetadata("origin", new FixedMetadataValue(plugin, player.getLocation()));
        snowball.setMetadata("damage", new FixedMetadataValue(plugin, gun.damage));
        snowball.setMetadata("shooter", new FixedMetadataValue(plugin, player.getUniqueId()));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!proj.hasMetadata("dart")) return;

        if (proj.hasMetadata("origin")) {
            Location origin = (Location) proj.getMetadata("origin").get(0).value();
            if (origin.distance(proj.getLocation()) > MAX_DISTANCE) {
                proj.remove();
                return;
            }
        }

        UUID shooterUUID = null;
        if (proj.hasMetadata("shooter")) {
            shooterUUID = (UUID) proj.getMetadata("shooter").get(0).value();
        }
        
        Player shooterPlayer = shooterUUID != null ? Bukkit.getPlayer(shooterUUID) : null;
        if (shooterPlayer == null) {
            proj.remove();
            return;
        }

        if (event.getHitEntity() instanceof LivingEntity target) {
            if (target.equals(shooterPlayer)) {
                proj.remove();
                return;
            }

            double damage = gun.damage;
            if (proj.hasMetadata("damage")) {
                damage = (double) proj.getMetadata("damage").get(0).value();
            }

            shooterPlayer.setMetadata("dart_damage", new FixedMetadataValue(plugin, true));

            target.damage(damage, shooterPlayer);

            shooterPlayer.removeMetadata("dart_damage", plugin);

            Location hitLoc = target.getLocation().add(0, 1, 0);
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, hitLoc, 5, 0.2, 0.3, 0.2, 0);

            int poisonDuration = 80;
            int newDuration = poisonDuration;
            PotionEffect currentPoison = target.getPotionEffect(PotionEffectType.POISON);
            if (currentPoison != null) {
                newDuration += currentPoison.getDuration();
                target.removePotionEffect(PotionEffectType.POISON);
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, newDuration, 0, false, false, true));

            target.getWorld().spawnParticle(Particle.ITEM_SLIME, hitLoc, 10, 0.3, 0.4, 0.3, 0.05);

            if (target instanceof Player) {
                lastGunUsed.put(target.getUniqueId(), gun.name);
            }
        }

        proj.remove();
    }

    private void forceReload(Player player) {
        UUID uuid = player.getUniqueId();

        reloading.add(uuid);
        reloadStart.put(uuid, System.currentTimeMillis());
        reloadSlot.put(uuid, player.getInventory().getHeldItemSlot());

        ItemStack item = player.getInventory().getItemInMainHand();
        setCMD(item, gun.reloadFrames[0]);
        player.sendActionBar(ChatColor.RED + "Reloading...");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.6f, 1.2f);
    }

    private void cancelReload(Player player) {
        UUID uuid = player.getUniqueId();
        reloading.remove(uuid);
        reloadStart.remove(uuid);
        reloadSlot.remove(uuid);
        player.sendActionBar(ChatColor.RED + "Reload canceled!");
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        if (reloading.contains(e.getPlayer().getUniqueId())) {
            cancelReload(e.getPlayer());
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (reloading.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack dropped = e.getItemDrop().getItemStack();
        
        if (reloading.contains(e.getPlayer().getUniqueId())) {
            if (gun.isGun(dropped)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your weapon while reloading!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (gun.isGun(item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent e) {

        if (e.getDamager() instanceof Player player && player.hasMetadata("dart_damage")) {
            return; // Permitir el daño del proyectil
        }

        if (e.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (gun.isGun(item) && e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                e.setCancelled(true);
                
                UUID uuid = player.getUniqueId();
                
                if (!reloading.contains(uuid)) {
                    if (shotCooldown.containsKey(uuid)) {
                        long timeSinceLastShot = System.currentTimeMillis() - shotCooldown.get(uuid);
                        if (timeSinceLastShot < SHOT_COOLDOWN) {
                            return;
                        }
                    }
                    
                    String itemId = ensureItemId(item);
                    int currentAmmo = getAmmo(itemId);
                    
                    if (currentAmmo >= 1) {
                        boolean isAiming = getCMD(item) == gun.aim;
                        fireDart(player, item, itemId, isAiming);
                        
                        shotCooldown.put(uuid, System.currentTimeMillis());
                        setAmmo(itemId, currentAmmo - 1);
                        
                        if (currentAmmo - 1 <= 0) {
                            forceReload(player);
                        }
                        
                        player.setMetadata("dart_recent", new FixedMetadataValue(plugin, true));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.removeMetadata("dart_recent", plugin);
                            }
                        }.runTaskLater(plugin, 100L);
                    } else {
                        player.sendActionBar(ChatColor.RED + "No ammo! Reloading...");
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.5f, 1.2f);
                        forceReload(player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPearl(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Player player = event.getPlayer();
            if (player.hasMetadata("dart_recent")) {
                event.setCancelled(true);
                player.removeMetadata("dart_recent", plugin);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && lastGunUsed.containsKey(victim.getUniqueId())) {
            String gunName = lastGunUsed.get(victim.getUniqueId());
            event.setDeathMessage(
                    victim.getName() + " was poisoned by " +
                            killer.getName() + " using " + ChatColor.DARK_GREEN + gunName
            );
        }

        lastGunUsed.remove(victim.getUniqueId());
    }
    
    private String ensureItemId(ItemStack item) {
        if (!item.hasItemMeta()) return UUID.randomUUID().toString();
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "gun_id");
        
        if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        }
        
        String itemId = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, itemId);
        item.setItemMeta(meta);
        
        return itemId;
    }
    
    private int getAmmo(String itemId) {
        if (itemId == null) return gun.maxAmmo;
        return itemAmmo.getOrDefault(itemId, gun.maxAmmo);
    }

    private void setAmmo(String itemId, int ammo) {
        if (itemId == null) return;
        itemAmmo.put(itemId, Math.max(0, Math.min(ammo, gun.maxAmmo)));
    }

    private int getCMD(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()
                ? item.getItemMeta().getCustomModelData()
                : -1;
    }

    private void setCMD(ItemStack item, int cmd) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
    }

    private String progressBar(double progress) {
        int filled = (int) Math.round(20 * progress);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            bar.append(i < filled ? ChatColor.GREEN + "█" : ChatColor.GRAY + "░");
        }
        return bar.toString();
    }
}
