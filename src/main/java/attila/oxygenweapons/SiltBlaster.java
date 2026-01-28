package attila.oxygenweapons;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SiltBlaster implements Listener {

    private final Plugin plugin;
    
    private final Map<UUID, Long> useCooldown = new HashMap<>();
    private final Map<UUID, ItemStack> temporaryRemoved = new HashMap<>();
    private final Set<UUID> invertedControls = new HashSet<>();
    
    private static final long USE_COOLDOWN = 2000;
    private static final int CUSTOM_MODEL_DATA = 9001;
    private static final double CLOUD_RADIUS = 3.0;
    private static final int EFFECT_DURATION = 40;
    private static final int ITEM_REMOVAL_TICKS = 100;

    public SiltBlaster(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isSiltBlaster(item)) return;

        UUID uuid = player.getUniqueId();
        
        if (useCooldown.containsKey(uuid)) {
            long timeSinceUse = System.currentTimeMillis() - useCooldown.get(uuid);
            if (timeSinceUse < USE_COOLDOWN) {
                long remaining = (USE_COOLDOWN - timeSinceUse) / 1000 + 1;
                player.sendActionBar(ChatColor.YELLOW + "Silt Blaster cooldown: " + remaining + "s");
                return;
            }
        }

        activateSiltBlaster(player, item);
        useCooldown.put(uuid, System.currentTimeMillis());
    }

    private void activateSiltBlaster(Player player, ItemStack item) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        UUID playerUUID = player.getUniqueId();
        
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.5f, 0.6f);
        world.playSound(loc, Sound.BLOCK_SAND_BREAK, 2.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.2f);
        
        world.spawnParticle(Particle.EXPLOSION, loc, 1);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 50, 1.5, 1.5, 1.5, 0.05);
        
        player.getInventory().setItemInMainHand(null);
        temporaryRemoved.put(playerUUID, item.clone());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && temporaryRemoved.containsKey(playerUUID)) {
                    player.getInventory().setItemInMainHand(temporaryRemoved.get(playerUUID));
                    temporaryRemoved.remove(playerUUID);
                }
            }
        }.runTaskLater(plugin, ITEM_REMOVAL_TICKS);

        createSiltCloud(loc, world);
        
        applyEffectsToNearbyPlayers(loc, world, playerUUID);
    }

    private void createSiltCloud(Location center, World world) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= EFFECT_DURATION) {
                    cancel();
                    return;
                }
                
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * CLOUD_RADIUS;
                    double height = Math.random() * 2.5;
                    
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    Location particleLoc = center.clone().add(x, height, z);
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    world.spawnParticle(Particle.CLOUD, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void applyEffectsToNearbyPlayers(Location center, World world, UUID casterUUID) {
        for (Entity entity : world.getNearbyEntities(center, CLOUD_RADIUS, CLOUD_RADIUS, CLOUD_RADIUS)) {
            if (entity instanceof Player target) {
                if (target.getUniqueId().equals(casterUUID)) continue;
                
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, EFFECT_DURATION, 1, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, EFFECT_DURATION, 2, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, EFFECT_DURATION, 1, false, true));
                
                invertedControls.add(target.getUniqueId());
                target.setMetadata("inverted_controls", new FixedMetadataValue(plugin, true));
                
                target.sendTitle(ChatColor.DARK_GRAY + "DISORIENTED!", 
                    ChatColor.GRAY + "Controls inverted!", 10, 20, 10);
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        invertedControls.remove(target.getUniqueId());
                        target.removeMetadata("inverted_controls", plugin);
                    }
                }.runTaskLater(plugin, EFFECT_DURATION);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!invertedControls.contains(player.getUniqueId())) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null || from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;
        
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        
        Location newTo = from.clone();
        newTo.setX(from.getX() - deltaX);
        newTo.setZ(from.getZ() - deltaZ);
        newTo.setY(to.getY());
        newTo.setYaw(to.getYaw());
        newTo.setPitch(to.getPitch());
        
        event.setTo(newTo);
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isSiltBlaster(item)) {
            e.setCancelled(true);
        }
    }

    private boolean isSiltBlaster(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_HORSE_ARMOR || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().hasCustomModelData() && 
               item.getItemMeta().getCustomModelData() == CUSTOM_MODEL_DATA;
    }
}