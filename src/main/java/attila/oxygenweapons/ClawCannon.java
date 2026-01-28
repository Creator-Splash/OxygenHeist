package attila.oxygenweapons;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ClawCannon implements Listener {

    private final Plugin plugin;
    
    private final Map<UUID, Long> launchCooldown = new HashMap<>();
    private final Set<UUID> inFlight = new HashSet<>();
    
    private static final long LAUNCH_COOLDOWN = 3000;
    private static final int CUSTOM_MODEL_DATA = 8001;
    private static final double LAUNCH_SPEED = 2.5;
    private static final double LAUNCH_Y = 0.8;
    private static final double EXPLOSION_RADIUS = 4.0;
    private static final double EXPLOSION_DAMAGE = 8.0;
    private static final double MELEE_KNOCKBACK = 2.0;
    private static final double MELEE_KNOCKBACK_Y = 0.5;

    public ClawCannon(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isClawCannon(item)) return;

        UUID uuid = player.getUniqueId();
        
        if (launchCooldown.containsKey(uuid)) {
            long timeSinceLaunch = System.currentTimeMillis() - launchCooldown.get(uuid);
            if (timeSinceLaunch < LAUNCH_COOLDOWN) {
                long remaining = (LAUNCH_COOLDOWN - timeSinceLaunch) / 1000 + 1;
                player.sendActionBar(ChatColor.RED + "Rocket cooldown: " + remaining + "s");
                return;
            }
        }

        launchPlayer(player);
        launchCooldown.put(uuid, System.currentTimeMillis());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        
        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (!isClawCannon(item)) return;
        if (attacker.hasMetadata("rocket_damage")) return;
        
        Vector direction = victim.getLocation().toVector()
            .subtract(attacker.getLocation().toVector())
            .normalize();
        
        Vector knockback = direction.multiply(MELEE_KNOCKBACK);
        knockback.setY(MELEE_KNOCKBACK_Y);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                victim.setVelocity(victim.getVelocity().add(knockback));
            }
        }.runTaskLater(plugin, 1L);
        
        victim.getWorld().spawnParticle(Particle.CRIT, 
            victim.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.5f);
        
        attacker.sendActionBar(ChatColor.GOLD + "CRITICAL HIT!");
    }

    private void launchPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Vector direction = player.getLocation().getDirection();
        
        direction.setY(0);
        direction.normalize();
        
        Vector velocity = direction.multiply(LAUNCH_SPEED);
        velocity.setY(LAUNCH_Y);
        
        player.setVelocity(velocity);
        inFlight.add(uuid);
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.8f);
        
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3, 0.3, 0.3, 0.3, 0.1);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
        
        player.sendActionBar(ChatColor.GOLD + "LAUNCHED!");
        
        player.setMetadata("rocket_flying", new FixedMetadataValue(plugin, System.currentTimeMillis()));
        
        new BukkitRunnable() {
            int ticks = 0;
            Location lastLoc = player.getLocation().clone();
            
            @Override
            public void run() {
                if (!player.isOnline() || !inFlight.contains(uuid) || ticks > 60) {
                    cancel();
                    return;
                }
                
                Location currentLoc = player.getLocation();
                
                player.getWorld().spawnParticle(Particle.FLAME, 
                    currentLoc.clone().add(0, 0.5, 0), 8, 0.2, 0.2, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.SMOKE, 
                    currentLoc.clone().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.FIREWORK, 
                    currentLoc.clone().add(0, 0.5, 0), 3, 0.15, 0.15, 0.15, 0.01);
                
                if (player.isOnGround() || player.getLocation().getY() < lastLoc.getY() - 0.5) {
                    if (ticks > 5) {
                        explodeOnLanding(player);
                        inFlight.remove(uuid);
                        player.removeMetadata("rocket_flying", plugin);
                        cancel();
                    }
                }
                
                lastLoc = currentLoc.clone();
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void explodeOnLanding(Player player) {
        Location landLoc = player.getLocation();
        World world = player.getWorld();
        UUID shooterUUID = player.getUniqueId();
        
        world.spawnParticle(Particle.EXPLOSION_EMITTER, landLoc, 2);
        world.spawnParticle(Particle.FLAME, landLoc, 60, 2.5, 0.5, 2.5, 0.15);
        world.spawnParticle(Particle.SMOKE, landLoc, 40, 2, 1, 2, 0.1);
        world.spawnParticle(Particle.LAVA, landLoc, 25, 2, 0.5, 2, 0.1);
        world.spawnParticle(Particle.CLOUD, landLoc, 30, 1.5, 0.5, 1.5, 0.1);
        
        world.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.7f);
        world.playSound(landLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.9f);
        world.playSound(landLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);

        for (Entity entity : world.getNearbyEntities(landLoc, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            if (entity instanceof LivingEntity target) {
                if (target.getUniqueId().equals(shooterUUID)) continue;
                
                double distance = target.getLocation().distance(landLoc);
                double damageMultiplier = 1.0 - (distance / EXPLOSION_RADIUS);
                double finalDamage = EXPLOSION_DAMAGE * damageMultiplier;
                
                if (finalDamage > 0.5) {
                    player.setMetadata("rocket_damage", new FixedMetadataValue(plugin, true));
                    target.damage(finalDamage, player);
                    player.removeMetadata("rocket_damage", plugin);
                    
                    Vector knockback = target.getLocation().toVector()
                        .subtract(landLoc.toVector())
                        .normalize()
                        .multiply(1.8 * damageMultiplier);
                    knockback.setY(0.6 * damageMultiplier);
                    
                    target.setVelocity(target.getVelocity().add(knockback));
                    
                    target.getWorld().spawnParticle(Particle.CRIT, 
                        target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
                }
            }
        }
        
        new BukkitRunnable() {
            double radius = 0;
            @Override
            public void run() {
                if (radius > EXPLOSION_RADIUS) {
                    cancel();
                    return;
                }
                
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = landLoc.clone().add(x, 0.1, z);
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                }
                
                radius += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isClawCannon(item)) {
            e.setCancelled(true);
        }
    }

    private boolean isClawCannon(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_HORSE_ARMOR || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().hasCustomModelData() && 
               item.getItemMeta().getCustomModelData() == CUSTOM_MODEL_DATA;
    }
}

