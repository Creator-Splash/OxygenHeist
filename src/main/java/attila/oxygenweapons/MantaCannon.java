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

public class MantaCannon implements Listener {

    private final Plugin plugin;

    private final Map<String, Integer> itemAmmo = new HashMap<>();
    private final Map<UUID, Long> reloadStart = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Map<UUID, Long> lastShot = new HashMap<>();

    private static final long SHOT_COOLDOWN = 3000;
    private static final double CONE_ANGLE = 60.0;
    private static final double MAX_RANGE = 8.0;
    private static final double DAMAGE = 12.0;

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
    }

    private final GunData gun = new GunData(
            Material.GOLDEN_HORSE_ARMOR,
            5001,
            5002,
            new int[]{5003, 5004},
            4,
            6000
    );

    public MantaCannon(Plugin plugin) {
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

                            player.sendActionBar(ChatColor.GREEN + "Manta Cannon Charged!");
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
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
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;

        if (e.isSneaking() && getCMD(item) == gun.base) {
            setCMD(item, gun.aim);
        }
    }

    @EventHandler
    public void onShoot(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;

        e.setCancelled(true);
        
        UUID uuid = p.getUniqueId();
        if (reloading.contains(uuid)) return;

        if (lastShot.containsKey(uuid)) {
            long timeSince = System.currentTimeMillis() - lastShot.get(uuid);
            if (timeSince < SHOT_COOLDOWN) {
                return;
            }
        }

        String itemId = ensureItemId(item);
        int ammo = getAmmo(itemId);
        
        if (ammo <= 0) {
            p.sendActionBar(ChatColor.RED + "No charge! Reloading...");
            p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
            startReload(p, item);
            return;
        }

        fireElectricBlast(p);
        setAmmo(itemId, ammo - 1);
        lastShot.put(uuid, System.currentTimeMillis());

        if (ammo - 1 <= 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() && gun.isGun(p.getInventory().getItemInMainHand())) {
                        ItemStack currentItem = p.getInventory().getItemInMainHand();
                        startReload(p, currentItem);
                    }
                }
            }.runTaskLater(plugin, 20L);
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

        if (e.getDamager() instanceof Player player && player.hasMetadata("manta_damage")) {
            return; // Permitir el daño del cañón
        }

        if (e.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (gun.isGun(item) && e.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                e.setCancelled(true);
                
                UUID uuid = player.getUniqueId();
                
                if (!reloading.contains(uuid)) {
                    if (lastShot.containsKey(uuid)) {
                        long timeSince = System.currentTimeMillis() - lastShot.get(uuid);
                        if (timeSince < SHOT_COOLDOWN) {
                            return;
                        }
                    }
                    
                    String itemId = ensureItemId(item);
                    int ammo = getAmmo(itemId);
                    
                    if (ammo > 0) {
                        fireElectricBlast(player);
                        setAmmo(itemId, ammo - 1);
                        lastShot.put(uuid, System.currentTimeMillis());
                        
                        if (ammo - 1 <= 0) {
                            Player p = player;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (p.isOnline() && gun.isGun(p.getInventory().getItemInMainHand())) {
                                        ItemStack currentItem = p.getInventory().getItemInMainHand();
                                        startReload(p, currentItem);
                                    }
                                }
                            }.runTaskLater(plugin, 20L);
                        }
                    } else {
                        player.sendActionBar(ChatColor.RED + "No charge! Reloading...");
                        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1f);
                        startReload(player, item);
                    }
                }
            }
        }
    }

    private void fireElectricBlast(Player p) {
        Location eyeLoc = p.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        World world = p.getWorld();
        
        Set<UUID> hitEntities = new HashSet<>();

        world.playSound(eyeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.2f);
        world.playSound(eyeLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
        world.playSound(eyeLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        
        Location impactPoint = null;
        double travelDistance = 0;
        boolean shouldReverseCone = false;
        
        for (double dist = 0.5; dist <= MAX_RANGE; dist += 0.5) {
            Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(dist));
            Material blockType = checkLoc.getBlock().getType();
            
            if (blockType.isSolid()) {
                impactPoint = checkLoc.subtract(direction.clone().multiply(0.5));
                travelDistance = dist - 0.5;
                
                if (blockType.name().contains("CONCRETE") || blockType.name().contains("MUD")) {
                    shouldReverseCone = true;
                } else {
                    Location behindBlock = checkLoc.clone().add(direction.clone().multiply(0.5));
                    if (behindBlock.getBlock().getType().isSolid()) {
                        shouldReverseCone = true;
                    }
                }
                break;
            }
        }
        
        if (impactPoint == null) {
            impactPoint = eyeLoc.clone().add(direction.clone().multiply(MAX_RANGE));
            travelDistance = MAX_RANGE;
            shouldReverseCone = false;
        }
        
        final Location finalImpact = impactPoint;
        final double finalDistance = travelDistance;
        final Vector coneDirection = shouldReverseCone ? direction.clone().multiply(-1) : direction.clone();
        final Plugin pluginRef = this.plugin; // Guardar referencia al plugin
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 12) {
                    cancel();
                    return;
                }
                
                if (ticks <= 4) {
                    double progress = (ticks / 4.0) * finalDistance;
                    
                    for (double dist = 0; dist <= progress; dist += 0.4) {
                        Location beamLoc = eyeLoc.clone().add(direction.clone().multiply(dist));
                        
                        world.spawnParticle(Particle.ELECTRIC_SPARK, beamLoc, 2, 0.05, 0.05, 0.05, 0.01);
                        world.spawnParticle(Particle.DUST, beamLoc, 1, 0.02, 0.02, 0.02, 0,
                            new DustOptions(Color.AQUA, 1.8f));
                        
                        if (Math.random() < 0.3) {
                            world.spawnParticle(Particle.DUST, beamLoc, 1, 0.02, 0.02, 0.02, 0,
                                new DustOptions(Color.WHITE, 2.2f));
                        }
                    }
                }
                
                if (ticks >= 5) {
                    int conePhase = ticks - 5;
                    double coneProgress = conePhase / 6.0;
                    
                    for (int ray = 0; ray < 16; ray++) {
                        double angleOffset = (ray / 16.0) * 360.0;
                        
                        for (double coneDist = 0.5; coneDist <= 6.0 * coneProgress; coneDist += 0.4) {
                            double coneRadius = Math.tan(Math.toRadians(CONE_ANGLE / 2.0)) * coneDist;
                            
                            double offsetX = Math.cos(Math.toRadians(angleOffset)) * coneRadius * (0.5 + Math.random() * 0.5);
                            double offsetY = Math.sin(Math.toRadians(angleOffset)) * coneRadius * (0.5 + Math.random() * 0.5);
                            
                            Vector offset = getPerpendicularVector(coneDirection)
                                .multiply(offsetX)
                                .add(getPerpendicularVector(coneDirection).crossProduct(coneDirection).multiply(offsetY));
                            
                            Location coneLoc = finalImpact.clone().add(coneDirection.clone().multiply(coneDist)).add(offset);
                            
                            if (Math.random() < 0.5) {
                                world.spawnParticle(Particle.ELECTRIC_SPARK, coneLoc, 1, 0.15, 0.15, 0.15, 0.03);
                            }
                            if (Math.random() < 0.4) {
                                world.spawnParticle(Particle.DUST, coneLoc, 1, 0.08, 0.08, 0.08, 0,
                                    new DustOptions(Color.AQUA, 1.5f));
                            }
                            if (Math.random() < 0.25) {
                                world.spawnParticle(Particle.DUST, coneLoc, 1, 0.08, 0.08, 0.08, 0,
                                    new DustOptions(Color.WHITE, 2.0f));
                            }
                            
                            for (Entity entity : world.getNearbyEntities(coneLoc, 1.8, 1.8, 1.8)) {
                                if (entity instanceof LivingEntity le && !le.equals(p) && !hitEntities.contains(le.getUniqueId())) {
                                    hitEntities.add(le.getUniqueId());

                                    p.setMetadata("manta_damage", new FixedMetadataValue(pluginRef, true));

                                    le.damage(DAMAGE, p);

                                    p.removeMetadata("manta_damage", pluginRef);
                                    
                                    Location hitLoc = le.getLocation().add(0, 1, 0);
                                    world.spawnParticle(Particle.DAMAGE_INDICATOR, hitLoc, 8, 0.3, 0.4, 0.3, 0);
                                    
                                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, true));
                                    
                                    world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 20, 0.3, 0.5, 0.3, 0.1);
                                    world.spawnParticle(Particle.FLASH, hitLoc, 1);
                                    world.playSound(hitLoc, Sound.ENTITY_GENERIC_HURT, 1f, 1f);
                                    world.playSound(hitLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.5f);
                                }
                            }
                        }
                    }
                    
                    if (conePhase == 0) {
                        world.spawnParticle(Particle.FLASH, finalImpact, 2);
                        world.spawnParticle(Particle.ELECTRIC_SPARK, finalImpact, 40, 0.5, 0.5, 0.5, 0.2);
                        world.playSound(finalImpact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.3f);
                    }
                }
                
                if (ticks % 2 == 0 && ticks <= 4) {
                    for (int i = 0; i < 360; i += 30) {
                        double radians = Math.toRadians(i);
                        Vector ringOffset = new Vector(Math.cos(radians), 0, Math.sin(radians)).multiply(0.5);
                        Location ringLoc = eyeLoc.clone().add(direction.clone().multiply(0.5)).add(ringOffset);
                        world.spawnParticle(Particle.DUST, ringLoc, 1, 0, 0, 0, 0,
                            new DustOptions(Color.fromRGB(100, 200, 255), 1.2f));
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private Vector getPerpendicularVector(Vector v) {
        Vector perp;
        if (Math.abs(v.getX()) > 0.1) {
            perp = new Vector(-v.getY(), v.getX(), 0);
        } else {
            perp = new Vector(0, -v.getZ(), v.getY());
        }
        return perp.normalize();
    }

    private void startReload(Player p, ItemStack item) {
        UUID uuid = p.getUniqueId();
        
        reloading.add(uuid);
        reloadStart.put(uuid, System.currentTimeMillis());
        reloadSlot.put(uuid, p.getInventory().getHeldItemSlot());

        setCMD(item, gun.reloadFrames[0]);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
        p.sendActionBar(ChatColor.AQUA + "Charging Manta Cannon...");
    }

    private void cancelReload(Player p) {
        UUID uuid = p.getUniqueId();
        reloading.remove(uuid);
        reloadStart.remove(uuid);
        reloadSlot.remove(uuid);
        p.sendActionBar(ChatColor.RED + "Charge interrupted!");
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

    private String progressBar(double p) {
        int bars = (int) (20 * p);
        return ChatColor.AQUA + "█".repeat(bars) + ChatColor.GRAY + "░".repeat(20 - bars);
    }
}
