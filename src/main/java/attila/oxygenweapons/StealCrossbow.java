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

public class StealCrossbow implements Listener {

    private final Plugin plugin;
    
    private final Map<String, Integer> itemAmmo = new HashMap<>();
    private final Map<UUID, Long> reloadStart = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Map<UUID, Long> shotCooldown = new HashMap<>();
    private static final Map<UUID, String> lastGunUsed = new HashMap<>();
    
    private static final int MAX_DISTANCE = 25;
    private static final long SHOT_COOLDOWN = 2000;

    private static class GunData {
        Material material;
        int loaded, unloaded, aim;
        int[] reloadFrames;
        int maxAmmo;
        double damage;
        long reloadTime;
        String name;
        double aimPrecisionBonus;

        GunData(Material material, int loaded, int unloaded, int aim, int[] reloadFrames, int maxAmmo,
                double damage, long reloadTime, String name, double aimPrecisionBonus) {
            this.material = material;
            this.loaded = loaded;
            this.unloaded = unloaded;
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
            if (cmd == loaded || cmd == unloaded || cmd == aim) return true;
            for (int r : reloadFrames) if (cmd == r) return true;
            return false;
        }
    }

    private final GunData gun = new GunData(
            Material.GOLDEN_HORSE_ARMOR,
            7001,      // loaded
            7003,      // unloaded (estado inicial)
            7002,      // aim
            new int[]{7003}, // frames de recarga
            1,         // maxAmmo (1 bala)
            3.0,
            3000,
            "Reclaimer Crossbow",
            0.2
    );

    public StealCrossbow(Plugin plugin) {
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
                            setCMD(item, gun.loaded);
                            reloading.remove(uuid);
                            reloadStart.remove(uuid);
                            reloadSlot.remove(uuid);
                            player.sendActionBar(ChatColor.GREEN + "Crossbow Loaded!");
                            player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1f, 1f);
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
                        int ammo = getAmmo(itemId);
                        setCMD(item, ammo > 0 ? gun.loaded : gun.unloaded);
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

        String itemId = ensureItemId(item);
        int ammo = getAmmo(itemId);

        if (event.isSneaking() && (getCMD(item) == gun.loaded || getCMD(item) == gun.unloaded)) {
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
            player.sendActionBar(ChatColor.RED + "No ammo! Hold right-click to reload.");
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
            return;
        }

        boolean isAiming = getCMD(item) == gun.aim;
        fireBolt(player, item, itemId, isAiming);

        shotCooldown.put(uuid, System.currentTimeMillis());

        setAmmo(itemId, currentAmmo - 1);
        setCMD(item, gun.unloaded);
        player.sendActionBar(ChatColor.YELLOW + "Ammo: " + ChatColor.WHITE + "0/" + gun.maxAmmo);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;

        UUID uuid = player.getUniqueId();
        if (reloading.contains(uuid)) return;

        String itemId = ensureItemId(item);
        int currentAmmo = getAmmo(itemId);

        if (currentAmmo >= gun.maxAmmo) {
            player.sendActionBar(ChatColor.YELLOW + "Crossbow already loaded!");
            return;
        }

        startReload(player);
    }

    private void fireBolt(Player player, ItemStack item, String itemId, boolean isAiming) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.2f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 0.9f);

        Arrow arrow = player.launchProjectile(Arrow.class);
        
        arrow.setDamage(gun.damage);
        arrow.setCritical(false);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        
        Vector velocity = arrow.getVelocity();
        velocity.multiply(2.0); 
        arrow.setVelocity(velocity);
        
        double spreadMultiplier = isAiming ? gun.aimPrecisionBonus : 1.0;
        if (spreadMultiplier > 0.2) {
            Vector vel = arrow.getVelocity();
            Random random = new Random();
            double spread = 0.02 * spreadMultiplier;
            vel.add(new Vector(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread
            ));
            arrow.setVelocity(vel);
        }
        
        arrow.setMetadata("stealcrossbow", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("origin", new FixedMetadataValue(plugin, player.getLocation()));
        arrow.setMetadata("shooter", new FixedMetadataValue(plugin, player.getUniqueId()));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!proj.hasMetadata("stealcrossbow")) return;

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

            if (target instanceof Player targetPlayer) {
                ItemStack targetItem = targetPlayer.getInventory().getItemInMainHand();
                if (targetItem != null && targetItem.getType() == Material.GOLDEN_HORSE_ARMOR) {

                    ItemStack stolenItem = targetItem.clone();
                    shooterPlayer.getInventory().addItem(stolenItem);

                    targetPlayer.getInventory().setItemInMainHand(null);
                    
                    targetPlayer.sendMessage(ChatColor.RED + "Your weapon was stolen by " + shooterPlayer.getName() + "!");
                    shooterPlayer.sendMessage(ChatColor.GREEN + "You stole " + targetPlayer.getName() + "'s weapon!");
                    
                    targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
                    shooterPlayer.getWorld().playSound(shooterPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
                    
                    targetPlayer.getWorld().spawnParticle(Particle.WITCH, targetPlayer.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                }
                
                lastGunUsed.put(targetPlayer.getUniqueId(), gun.name);
            }
        }

        proj.remove();
    }

    private void startReload(Player player) {
        UUID uuid = player.getUniqueId();

        reloading.add(uuid);
        reloadStart.put(uuid, System.currentTimeMillis());
        reloadSlot.put(uuid, player.getInventory().getHeldItemSlot());

        ItemStack item = player.getInventory().getItemInMainHand();
        setCMD(item, gun.reloadFrames[0]);
        player.sendActionBar(ChatColor.RED + "Reloading...");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, 1f, 1f);
    }

    private void cancelReload(Player player) {
        UUID uuid = player.getUniqueId();
        reloading.remove(uuid);
        reloadStart.remove(uuid);
        reloadSlot.remove(uuid);
        player.sendActionBar(ChatColor.RED + "Reload canceled!");
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (gun.isGun(item)) {
            String itemId = ensureItemId(item);
            int ammo = getAmmo(itemId);
            setCMD(item, ammo > 0 ? gun.loaded : gun.unloaded);
        }
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
        if (e.getDamager() instanceof Arrow && e.getDamager().hasMetadata("stealcrossbow")) {
            return;
        }
        
        if (e.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (gun.isGun(item)) {
                e.setCancelled(true);
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
                    victim.getName() + " was shot by " +
                            killer.getName() + " using " + ChatColor.GOLD + gunName
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
        if (itemId == null) return 0;
        return itemAmmo.getOrDefault(itemId, 0);
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
