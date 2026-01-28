package attila.oxygenweapons;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import org.bukkit.Particle.DustOptions;

import java.util.*;

public class VenomSpitter implements Listener {

    private final Plugin plugin;

    private final Map<String, Integer> itemAmmo = new HashMap<>();

    private final Map<UUID, Long> reloadStart = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Set<UUID> shooting = new HashSet<>();
    private final Map<UUID, Set<UUID>> hitEntities = new HashMap<>(); 

    private static class GunData {
        Material material;
        int base, aim;
        int[] reloadFrames;
        int maxAmmo;
        long reloadTime;

        GunData(Material material, int base, int aim, int[] reloadFrames, int maxAmmo, long reloadTime) {
            this.material = material;
            this.base = base;
            this.aim = aim;
            this.reloadFrames = reloadFrames;
            this.maxAmmo = maxAmmo;
            this.reloadTime = reloadTime;
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

    private final GunData gun = new GunData(
            Material.GOLDEN_HORSE_ARMOR,
            3001,
            3002,
            new int[]{3003},
            25,
            5000
    );

    public VenomSpitter(Plugin plugin) {
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
                    
                    if (!reloading.contains(uuid) && !shooting.contains(uuid)) {
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
                    if (shooting.contains(uuid)) {
                        int ammo = getAmmo(itemId);

                        if (ammo <= 0) {
                            shooting.remove(uuid);
                            hitEntities.remove(uuid);
                            startAutoReload(player, item);
                        } else {
                            shoot(player);
                            setAmmo(itemId, ammo - 1);
                            player.sendActionBar(ChatColor.YELLOW + "Ammo: " + ChatColor.WHITE + (ammo - 1) + "/" + gun.maxAmmo);
                        }
                    }

                    if (gun.isApuntado(item) && !player.isSneaking()) {
                        setCMD(item, gun.base);
                        shooting.remove(uuid);
                        hitEntities.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }


    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;

        if (e.isSneaking() && getCMD(item) == gun.base) {
            setCMD(item, gun.aim);
        } else {
            shooting.remove(p.getUniqueId());
            hitEntities.remove(p.getUniqueId());
        }
    }

    @EventHandler
    public void onShoot(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;
        if (reloading.contains(p.getUniqueId())) return;

        e.setCancelled(true);

        String itemId = ensureItemId(item);
        int ammo = getAmmo(itemId);
        
        if (ammo <= 0) {
            p.sendActionBar(ChatColor.RED + "Reloading...");
            return;
        }

        shooting.add(p.getUniqueId());
        hitEntities.putIfAbsent(p.getUniqueId(), new HashSet<>());
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        UUID uuid = p.getUniqueId();

        if (!gun.isGun(item)) return;
        if (reloading.contains(uuid)) return;

        String itemId = ensureItemId(item);
        int ammo = getAmmo(itemId);
        
        if (ammo >= gun.maxAmmo) {
            return;
        }

        startReload(p, item);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        if (reloading.contains(e.getPlayer().getUniqueId())) {
            cancelReload(e.getPlayer());
        }
        hitEntities.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (reloading.contains(e.getPlayer().getUniqueId())) {
            cancelReload(e.getPlayer());
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

        if (e.getDamager() instanceof Player player && player.hasMetadata("venom_damage")) {
            return; // Permitir el daño del rayo
        }

        if (e.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (gun.isGun(item) && e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                e.setCancelled(true);
                
                UUID uuid = player.getUniqueId();
                
                if (!reloading.contains(uuid)) {
                    String itemId = ensureItemId(item);
                    int ammo = getAmmo(itemId);
                    
                    if (ammo > 0) {
                        shooting.add(uuid);
                        hitEntities.putIfAbsent(uuid, new HashSet<>());
                    } else {
                        player.sendActionBar(ChatColor.RED + "No ammo!");
                    }
                }
            }
        }
    }


    private void shoot(Player p) {
        Location start = p.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        World w = p.getWorld();
        UUID shooterUUID = p.getUniqueId();
        
        Set<UUID> hitThisTick = new HashSet<>();

        for (double i = 1; i <= 7; i += 0.5) {
            Location loc = start.clone().add(dir.clone().multiply(i));
            w.spawnParticle(Particle.DUST, loc, 4, 0, 0, 0, 0, new DustOptions(Color.GREEN, 1.2f));
            w.spawnParticle(Particle.DUST, loc, 1, new DustOptions(Color.GREEN, 1.2f));

            for (Entity e : w.getNearbyEntities(loc, 1.5, 2, 1.5)) {
                if (e instanceof LivingEntity le && !le.equals(p)) {
                    UUID targetUUID = le.getUniqueId();
                    
                    if (!hitThisTick.contains(targetUUID)) {
                        hitThisTick.add(targetUUID);

                        p.setMetadata("venom_damage", new FixedMetadataValue(plugin, true));

                        le.damage(0.5, p);

                        p.removeMetadata("venom_damage", plugin);
                        
                        int additionalDuration = 20;
                        int newDuration = additionalDuration;
                        PotionEffect currentPoison = le.getPotionEffect(PotionEffectType.POISON);
                        if (currentPoison != null) {
                            newDuration += currentPoison.getDuration();
                            le.removePotionEffect(PotionEffectType.POISON);
                        }
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, newDuration, 0, false, false, true));
                        
                    }
                }
            }
        }

        w.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
    }

    private void startAutoReload(Player p, ItemStack item) {
        UUID uuid = p.getUniqueId();
        
        reloading.add(uuid);
        reloadStart.put(uuid, System.currentTimeMillis());
        reloadSlot.put(uuid, p.getInventory().getHeldItemSlot());

        setCMD(item, gun.reloadFrames[0]);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
        p.sendActionBar(ChatColor.RED + "Reloading...");
    }

    private void startReload(Player p, ItemStack item) {
        UUID uuid = p.getUniqueId();
        
        reloading.add(uuid);
        reloadStart.put(uuid, System.currentTimeMillis());
        reloadSlot.put(uuid, p.getInventory().getHeldItemSlot());

        setCMD(item, gun.reloadFrames[0]);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
    }

    private void cancelReload(Player p) {
        UUID uuid = p.getUniqueId();
        reloading.remove(uuid);
        reloadStart.remove(uuid);
        reloadSlot.remove(uuid);
        p.sendActionBar(ChatColor.RED + "Reload canceled!");
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

    private String progressBar(double p) {
        int bars = (int) (20 * p);
        return ChatColor.RED + "█".repeat(bars) + ChatColor.GRAY + "░".repeat(20 - bars);
    }
}
