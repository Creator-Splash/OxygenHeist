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
import org.bukkit.projectiles.ProjectileSource;

public class SpikeShooter implements Listener {

    private final Plugin plugin;
    
    private final Map<String, Integer> itemAmmo = new HashMap<>();
    
    private final Map<UUID, Long> recargaStartTime = new HashMap<>();
    private final Set<UUID> inCooldown = new HashSet<>();
    private static final Map<UUID, String> lastGunUsed = new HashMap<>();

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

        GunData(Material material, int base, int aim, int[] reloadFrames, int maxAmmo, double damage, long reloadTime, double recoil, String name, double aimPrecisionBonus) {
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

        boolean isApuntado(ItemStack item) {
            if (item == null || item.getType() != material || !item.hasItemMeta()) return false;
            return item.getItemMeta().getCustomModelData() == aim;
        }

        boolean isReloadFrame(ItemStack item) {
            if (item == null || item.getType() != material || !item.hasItemMeta()) return false;
            int cmd = item.getItemMeta().getCustomModelData();
            for (int r : reloadFrames) if (cmd == r) return true;
            return false;
        }
    }

    private final GunData harpoon = new GunData(
            Material.GOLDEN_HORSE_ARMOR,
            1001,
            1002,
            new int[]{1003},
            1,
            10.0,
            5000,
            0.5,
            "Reef Harpoon Gun",
            0.3  
    );

    private final List<GunData> guns = Collections.singletonList(harpoon);

    public SpikeShooter(Plugin plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    GunData gun = getGun(item);
                    if (gun == null) continue;

                    UUID uuid = player.getUniqueId();
                    String itemId = ensureItemId(item);
                    int currentAmmo = getAmmo(itemId);

                    if (!inCooldown.contains(uuid)) {
                        player.sendActionBar(ChatColor.YELLOW + "Ammo: " + ChatColor.WHITE + currentAmmo + "/" + gun.maxAmmo);
                    }

                    if (inCooldown.contains(uuid) && recargaStartTime.containsKey(uuid)) {
                        long elapsed = System.currentTimeMillis() - recargaStartTime.get(uuid);

                        if (elapsed >= gun.reloadTime) {
                            setAmmo(itemId, gun.maxAmmo);
                            setCMD(item, gun.base);
                            recargaStartTime.remove(uuid);
                            inCooldown.remove(uuid);
                            player.sendActionBar(ChatColor.GREEN + "Weapon Reloaded!");
                        } else {
                            double progress = (double) elapsed / gun.reloadTime;
                            player.sendActionBar(getProgressBar(progress, 20, ChatColor.RED, ChatColor.GRAY));

                            long phase = Math.max(1, gun.reloadTime / gun.reloadFrames.length);
                            int index = (int) (elapsed / phase);
                            if (index >= gun.reloadFrames.length) index = gun.reloadFrames.length - 1;
                            setCMD(item, gun.reloadFrames[index]);
                        }
                    }

                    if (gun.isApuntado(item) && !player.isSneaking()) {
                        setCMD(item, gun.base);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 2L);
    }

    @EventHandler
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        GunData gun = getGun(item);
        if (gun == null) return;

        if (event.isSneaking() && getCMD(item) == gun.base) {
            setCMD(item, gun.aim);
        }
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        GunData gun = getGun(item);
        if (gun == null) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        if (inCooldown.contains(uuid)) {
            return;
        }

        String itemId = ensureItemId(item);
        int currentAmmo = getAmmo(itemId);

        if (currentAmmo <= 0) {
            player.sendActionBar(ChatColor.RED + "No ammo! Reloading...");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
            return;
        }

        boolean isAiming = gun.isApuntado(item);

        if (activateGun(player, item, gun, isAiming)) {
            setAmmo(itemId, 0);
            
            recargaStartTime.put(uuid, System.currentTimeMillis());
            inCooldown.add(uuid);
            player.setMetadata("harpoon_recent", new FixedMetadataValue(plugin, true));
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("harpoon_recent", plugin);
                }
            }.runTaskLater(plugin, 80L);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        GunData gun = getGun(item);
        if (gun == null) return;

        UUID uuid = player.getUniqueId();

        if (!inCooldown.contains(uuid) && gun.isReloadFrame(item)) {
            startReload(player, item, gun);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        cancelReload(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        cancelReload(event.getPlayer().getUniqueId());
    }

    private void cancelReload(UUID uuid) {
        recargaStartTime.remove(uuid);
        inCooldown.remove(uuid);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped == null || !dropped.hasItemMeta()) return;

        GunData gun = getGun(dropped);
        if (gun == null) return;

        int cmd = getCMD(dropped);
        if (cmd != gun.base && cmd != gun.aim) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your weapon while reloading!");
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        GunData gun = getGun(item);
        if (gun != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            GunData gun = getGun(item);
            if (gun != null) {
                e.setCancelled(true);
                
                UUID uuid = player.getUniqueId();
                
                if (!inCooldown.contains(uuid)) {
                    String itemId = ensureItemId(item);
                    int currentAmmo = getAmmo(itemId);
                    
                    if (currentAmmo > 0) {
                        boolean isAiming = gun.isApuntado(item);
                        
                        if (activateGun(player, item, gun, isAiming)) {
                            setAmmo(itemId, 0);
                            
                            recargaStartTime.put(uuid, System.currentTimeMillis());
                            inCooldown.add(uuid);
                            player.setMetadata("harpoon_recent", new FixedMetadataValue(plugin, true));
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.removeMetadata("harpoon_recent", plugin);
                                }
                            }.runTaskLater(plugin, 80L);
                        }
                    } else {
                        player.sendActionBar(ChatColor.RED + "No ammo! Reloading...");
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
                    }
                }
            }
        }
    }

    private boolean activateGun(Player player, ItemStack gunItem, GunData gun, boolean isAiming) {
        applyRecoil(player, gun.recoil);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.2f, 1f);

        Trident trident = player.launchProjectile(Trident.class);
        
        Vector velocity = trident.getVelocity();
        velocity.multiply(0.8); 
        trident.setVelocity(velocity);
        
        trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        
        double spreadMultiplier = isAiming ? gun.aimPrecisionBonus : 1.0;
        
        if (spreadMultiplier > 0.3) {
            Vector vel = trident.getVelocity();
            Random random = new Random();
            double spread = 0.08 * spreadMultiplier;
            vel.add(new Vector(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread
            ));
            trident.setVelocity(vel);
        }
        
        trident.setGravity(true);
        
        final Vector initialVelocity = trident.getVelocity().clone();
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!trident.isValid() || trident.isDead() || ticks > 100) {
                    cancel();
                    return;
                }
                
                Location loc = trident.getLocation();
                Material blockType = loc.getBlock().getType();
                boolean inWater = blockType == Material.WATER || blockType == Material.BUBBLE_COLUMN;
                
                if (inWater) {
                    Vector currentVel = trident.getVelocity();
                    currentVel.multiply(0.7); 
                    trident.setVelocity(currentVel);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
        
        trident.setMetadata("harpoon", new FixedMetadataValue(plugin, true));
        trident.setDamage(gun.damage);

        player.setMetadata("harpoon_recent", new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("harpoon_recent", plugin);
            }
        }.runTaskLater(plugin, 100L);

        return true;
    }

    private void applyRecoil(Player player, double recoilStrength) {
        Vector dir = player.getLocation().getDirection().clone().multiply(-recoilStrength);
        player.setVelocity(player.getVelocity().add(new Vector(dir.getX(), recoilStrength / 2, dir.getZ())));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (proj == null) return;
        if (!proj.hasMetadata("harpoon")) return;

        ProjectileSource shooter = proj.getShooter();
        if (!(shooter instanceof Player)) return;
        Player shooterPlayer = (Player) shooter;

        if (event.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getHitEntity();
            if (target.equals(shooterPlayer)) return;

            target.damage(harpoon.damage, shooterPlayer);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, false, false, true));

            if (target instanceof Player) {
                lastGunUsed.put(target.getUniqueId(), harpoon.name);
            }
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Player player = event.getPlayer();
            if (player.hasMetadata("harpoon_recent")) {
                event.setCancelled(true);
                player.removeMetadata("harpoon_recent", plugin);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && lastGunUsed.containsKey(victim.getUniqueId())) {
            String gunName = lastGunUsed.get(victim.getUniqueId());
            event.setDeathMessage(victim.getName()
                    + " he was shot by "
                    + killer.getName()
                    + " using his " + ChatColor.AQUA + gunName + ChatColor.RESET);
        }
        lastGunUsed.remove(victim.getUniqueId());
    }

    private GunData getGun(ItemStack item) {
        for (GunData gun : guns) {
            if (gun.isGun(item)) return gun;
        }
        return null;
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
        if (itemId == null) return harpoon.maxAmmo;
        return itemAmmo.getOrDefault(itemId, harpoon.maxAmmo);
    }

    private void setAmmo(String itemId, int ammo) {
        if (itemId == null) return;
        itemAmmo.put(itemId, Math.max(0, Math.min(ammo, harpoon.maxAmmo)));
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

    private String getProgressBar(double progress, int length, ChatColor filledColor, ChatColor emptyColor) {
        int filled = (int) Math.round(length * progress);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) {
            bar.append(i < filled ? filledColor + "█" : emptyColor + "░");
        }
        return bar.toString();
    }

    private void startReload(Player player, ItemStack item, GunData gun) {
        UUID uuid = player.getUniqueId();
        recargaStartTime.put(uuid, System.currentTimeMillis());
        inCooldown.add(uuid);
        setCMD(item, gun.reloadFrames[0]);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.1f);
    }
}
