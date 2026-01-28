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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class NeedleRifle implements Listener {

    private final Plugin plugin;
    
    private final Map<String, Integer> itemAmmo = new HashMap<>();    
    private final Map<UUID, Long> reloadStart = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Map<UUID, Long> shotCooldown = new HashMap<>();
    private static final Map<UUID, String> lastGunUsed = new HashMap<>();
    
    private static final int MAX_DISTANCE = 10;
    private static final long SHOT_COOLDOWN = 2000;
    private static final double CLOSE_RANGE_DISTANCE = 3.0;
    private static final double CLOSE_RANGE_BONUS = 3.0;
    private static final double LONG_RANGE_DISTANCE = 6.0;
    private static final double LONG_RANGE_NERF = 3.0;

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
            4001,
            4002,
            new int[]{4003},
            8,
            7.0,
            4000,
            0,
            "Needle Rifle",
            0.3
    );

    public NeedleRifle(Plugin plugin) {
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
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
            forceReload(player);
            return;
        }

        boolean isAiming = getCMD(item) == gun.aim;
        fireNeedle(player, item, itemId, isAiming);

        shotCooldown.put(uuid, System.currentTimeMillis());

        setAmmo(itemId, currentAmmo - 1);
        player.sendActionBar(ChatColor.YELLOW + "Ammo: " + ChatColor.WHITE + (currentAmmo - 1) + "/" + gun.maxAmmo);

        if (currentAmmo - 1 <= 0) {
            forceReload(player);
        }

        player.setMetadata("needle_recent", new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("needle_recent", plugin);
            }
        }.runTaskLater(plugin, 100L);
    }

    private void fireNeedle(Player player, ItemStack item, String itemId, boolean isAiming) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.2f, 0.5f);

        Snowball snowball = player.launchProjectile(Snowball.class);
        
        Vector velocity = snowball.getVelocity();
        velocity.multiply(1.5); 
        snowball.setVelocity(velocity);
        
        ItemStack snowballItem = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = snowballItem.getItemMeta();
        meta.setCustomModelData(4000);
        meta.setDisplayName(ChatColor.GRAY + "Needle Shot");
        snowballItem.setItemMeta(meta);
        snowball.setItem(snowballItem);
        
        double spreadMultiplier = isAiming ? gun.aimPrecisionBonus : 1.0;
        if (spreadMultiplier > 0.3) {
            Vector vel = snowball.getVelocity();
            Random random = new Random();
            double spread = 0.03 * spreadMultiplier;
            vel.add(new Vector(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread
            ));
            snowball.setVelocity(vel);
        }
        
        final Vector initialVelocity = snowball.getVelocity().clone();
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!snowball.isValid() || snowball.isDead() || ticks > 100) {
                    cancel();
                    return;
                }
                
                Location loc = snowball.getLocation();
                Material blockType = loc.getBlock().getType();
                boolean inWater = blockType == Material.WATER || blockType == Material.BUBBLE_COLUMN;
                
                if (inWater) {
                    Vector currentVel = snowball.getVelocity();
                    currentVel.setY(currentVel.getY() + 0.04); 
                    
                    double speedRatio = initialVelocity.length() / currentVel.length();
                    if (speedRatio > 1.0) {
                        currentVel.multiply(speedRatio * 0.9); 
                    }
                    
                    snowball.setVelocity(currentVel);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
        
        snowball.setMetadata("needle", new FixedMetadataValue(plugin, true));
        snowball.setMetadata("origin", new FixedMetadataValue(plugin, player.getLocation()));
        snowball.setMetadata("damage", new FixedMetadataValue(plugin, gun.damage));
        snowball.setMetadata("shooter", new FixedMetadataValue(plugin, player.getUniqueId()));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!proj.hasMetadata("needle")) return;

        Location origin = null;
        if (proj.hasMetadata("origin")) {
            origin = (Location) proj.getMetadata("origin").get(0).value();
            double distance = origin.distance(proj.getLocation());
            
            if (distance > MAX_DISTANCE) {
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

            double finalDamage = gun.damage;
            if (proj.hasMetadata("damage")) {
                finalDamage = (double) proj.getMetadata("damage").get(0).value();
            }
            
            if (origin != null) {
                double distance = origin.distance(target.getLocation());
                
                if (distance <= CLOSE_RANGE_DISTANCE) {
                    finalDamage += CLOSE_RANGE_BONUS;
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
                }
                else if (distance > LONG_RANGE_DISTANCE) {
                    finalDamage -= LONG_RANGE_NERF;
                }
            }

            shooterPlayer.setMetadata("needle_damage", new FixedMetadataValue(plugin, true));

            target.damage(finalDamage, shooterPlayer);

            shooterPlayer.removeMetadata("needle_damage", plugin);

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

        if (e.getDamager() instanceof Player player && player.hasMetadata("needle_damage")) {
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
                        fireNeedle(player, item, itemId, isAiming);
                        
                        shotCooldown.put(uuid, System.currentTimeMillis());
                        setAmmo(itemId, currentAmmo - 1);
                        player.sendActionBar(ChatColor.YELLOW + "Ammo: " + ChatColor.WHITE + (currentAmmo - 1) + "/" + gun.maxAmmo);
                        
                        if (currentAmmo - 1 <= 0) {
                            forceReload(player);
                        }
                        
                        player.setMetadata("needle_recent", new FixedMetadataValue(plugin, true));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.removeMetadata("needle_recent", plugin);
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
            if (player.hasMetadata("needle_recent")) {
                event.setCancelled(true);
                player.removeMetadata("needle_recent", plugin);
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
                    victim.getName() + " was sniped by " +
                            killer.getName() + " using " + ChatColor.GRAY + gunName
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
            bar.append(i < filled ? ChatColor.RED + "█" : ChatColor.GRAY + "░");
        }
        return bar.toString();
    }
}
