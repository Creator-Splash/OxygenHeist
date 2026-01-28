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

public class ReefHarpoonGun implements Listener {

    private final Plugin plugin;
    
    private final Map<String, Integer> itemAmmo = new HashMap<>();
    
    private final Map<UUID, Long> reloadStart = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Set<UUID> shootingBurst = new HashSet<>();
    private final Map<UUID, Long> burstCooldown = new HashMap<>();
    private static final Map<UUID, String> lastGunUsed = new HashMap<>();
    
    private static final int MAX_DISTANCE = 12;
    private static final long BURST_COOLDOWN = 500;

    private static class GunData {
        Material material;
        int base, aim;
        int[] reloadFrames;
        int maxAmmo;
        double damage;
        long reloadTime;
        double recoil;
        String name;
        double aimPrecisionBonus;

        GunData(Material material, int base, int aim, int[] reloadFrames, int maxAmmo,
                double damage, long reloadTime, double recoil, String name, double aimPrecisionBonus) {
            this.material = material;
            this.base = base;
            this.aim = aim;
            this.reloadFrames = reloadFrames;
            this.maxAmmo = maxAmmo;
            this.damage = damage;
            this.reloadTime = reloadTime;
            this.recoil = recoil;
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
            2001,
            2002,
            new int[]{2003, 2004, 2005, 2006, 2007},
            10,
            2.0,
            3500,
            0,
            "Spikes Shooter",
            0.5
    );

    public ReefHarpoonGun(Plugin plugin) {
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

                    if (!reloading.contains(uuid) && !shootingBurst.contains(uuid)) {
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

        if (burstCooldown.containsKey(uuid)) {
            long timeSinceLastBurst = System.currentTimeMillis() - burstCooldown.get(uuid);
            if (timeSinceLastBurst < BURST_COOLDOWN) {
                return;
            }
        }

        String itemId = ensureItemId(item);
        int currentAmmo = getAmmo(itemId);

        if (currentAmmo < 3) {
            player.sendActionBar(ChatColor.RED + "No ammo! Reloading...");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
            forceReload(player);
            return;
        }

        boolean isAiming = getCMD(item) == gun.aim;
        activateGunBurst(player, item, isAiming);

        player.setMetadata("spike_recent", new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("spike_recent", plugin);
            }
        }.runTaskLater(plugin, 100L);
    }

    private void activateGunBurst(Player player, ItemStack item, boolean isAiming) {
        UUID uuid = player.getUniqueId();

        shootingBurst.add(uuid);
        burstCooldown.put(uuid, System.currentTimeMillis());

        if (gun.recoil > 0) {
            applyRecoil(player, gun.recoil);
        }

        String itemId = getItemId(item);

        new BukkitRunnable() {
            int shots = 0;

            @Override
            public void run() {
                if (shots >= 3) {
                    shootingBurst.remove(uuid);
                    
                    int finalAmmo = getAmmo(itemId);
                    if (finalAmmo <= 0) {
                        forceReload(player);
                    }
                    
                    cancel();
                    return;
                }

                double spreadMultiplier = player.isSneaking() ? gun.aimPrecisionBonus : 1.0;
                fireSpike(player, spreadMultiplier);
                shots++;
                
                int currentAmmo = getAmmo(itemId);
                setAmmo(itemId, currentAmmo - 1);
                
                player.sendActionBar(ChatColor.YELLOW + "Ammo: " + ChatColor.WHITE + (currentAmmo - 1) + "/" + gun.maxAmmo);
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void fireSpike(Player player, double spreadMultiplier) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 0.6f, 1.2f);

        Snowball snowball = player.launchProjectile(Snowball.class);
        
        Vector velocity = snowball.getVelocity();
        velocity.multiply(1.2);
        snowball.setVelocity(velocity);
        
        ItemStack snowballItem = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = snowballItem.getItemMeta();
        meta.setCustomModelData(2000);
        meta.setDisplayName(ChatColor.YELLOW + "Spike Projectile");
        snowballItem.setItemMeta(meta);
        snowball.setItem(snowballItem);
        
        if (spreadMultiplier > 0.5) {
            Vector vel = snowball.getVelocity();
            Random random = new Random();
            double spread = 0.05 * spreadMultiplier;
            vel.add(new Vector(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread
            ));
            snowball.setVelocity(vel);
        }
        
        snowball.setMetadata("spike", new FixedMetadataValue(plugin, true));
        snowball.setMetadata("origin", new FixedMetadataValue(plugin, player.getLocation()));
        snowball.setMetadata("damage", new FixedMetadataValue(plugin, gun.damage));
        snowball.setMetadata("shooter", new FixedMetadataValue(plugin, player.getUniqueId()));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!proj.hasMetadata("spike")) return;

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

            shooterPlayer.setMetadata("spike_damage", new FixedMetadataValue(plugin, true));

            target.damage(damage, shooterPlayer);

            shooterPlayer.removeMetadata("spike_damage", plugin);

            int additionalDuration = 100;
            int newDuration = additionalDuration;
            PotionEffect currentPoison = target.getPotionEffect(PotionEffectType.POISON);
            if (currentPoison != null) {
                newDuration += currentPoison.getDuration();
                target.removePotionEffect(PotionEffectType.POISON);
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, newDuration, 0, false, false, true));

            if (target instanceof Player) {
                lastGunUsed.put(target.getUniqueId(), gun.name);
            }
        }

        proj.remove();
    }

    private void applyRecoil(Player player, double recoilStrength) {
        Vector dir = player.getLocation().getDirection().clone().multiply(-recoilStrength);
        player.setVelocity(player.getVelocity().add(new Vector(dir.getX(), recoilStrength / 2, dir.getZ())));
    }

    private void forceReload(Player player) {
        UUID uuid = player.getUniqueId();
        shootingBurst.remove(uuid);

        reloading.add(uuid);
        reloadStart.put(uuid, System.currentTimeMillis());
        reloadSlot.put(uuid, player.getInventory().getHeldItemSlot());

        ItemStack item = player.getInventory().getItemInMainHand();
        setCMD(item, gun.reloadFrames[0]);
        player.sendActionBar(ChatColor.RED + "Reloading...");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
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

        if (e.getDamager() instanceof Player player && player.hasMetadata("spike_damage")) {
            return; // Permitir el daño del proyectil
        }

        if (e.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (gun.isGun(item) && e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                e.setCancelled(true);
                
                UUID uuid = player.getUniqueId();
                
                if (!reloading.contains(uuid) && !shootingBurst.contains(uuid)) {
                    if (burstCooldown.containsKey(uuid)) {
                        long timeSinceLastBurst = System.currentTimeMillis() - burstCooldown.get(uuid);
                        if (timeSinceLastBurst < BURST_COOLDOWN) {
                            return;
                        }
                    }
                    
                    String itemId = ensureItemId(item);
                    int currentAmmo = getAmmo(itemId);
                    
                    if (currentAmmo >= 3) {
                        boolean isAiming = getCMD(item) == gun.aim;
                        activateGunBurst(player, item, isAiming);
                        
                        player.setMetadata("spike_recent", new FixedMetadataValue(plugin, true));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.removeMetadata("spike_recent", plugin);
                            }
                        }.runTaskLater(plugin, 100L);
                    } else {
                        player.sendActionBar(ChatColor.RED + "No ammo! Reloading...");
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
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
            if (player.hasMetadata("spike_recent")) {
                event.setCancelled(true);
                player.removeMetadata("spike_recent", plugin);
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
                    victim.getName() + " was shredded by " +
                            killer.getName() + " using " + ChatColor.YELLOW + gunName
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
    
    private String getItemId(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "gun_id");
        
        if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        }
        
        return null;
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
            bar.append(i < filled ? ChatColor.RED + "█" : ChatColor.GRAY + "░");
        }
        return bar.toString();
    }
}
