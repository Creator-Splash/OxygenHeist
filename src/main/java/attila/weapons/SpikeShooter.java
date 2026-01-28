package attila.weapons;

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
    private final Map<UUID, Long> recargaStartTime = new HashMap<>();
    private final Set<UUID> inCooldown = new HashSet<>();
    private static final Map<UUID, String> lastGunUsed = new HashMap<>();

    private final Set<UUID> shootingBurst = new HashSet<>();
    private final Map<UUID, Location> lastPositions = new HashMap<>();
    private static final int MAX_DISTANCE = 15; // Distancia máxima en bloques

    private static class GunData {
        Material material;
        int base, aim;
        int[] reloadFrames;
        double damage;
        long reloadTime;
        double recoil;
        String name;

        GunData(Material material, int base, int aim, int[] reloadFrames,
                double damage, long reloadTime, double recoil, String name) {
            this.material = material;
            this.base = base;
            this.aim = aim;
            this.reloadFrames = reloadFrames;
            this.damage = damage;
            this.reloadTime = reloadTime;
            this.recoil = recoil;
            this.name = name;
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

    private final GunData spikeShooter = new GunData(
            Material.GOLDEN_HORSE_ARMOR,
            2001,
            2002,
            new int[]{2003,2004,2005,2006,2007,2008,2009,2010,2011},
            2.0,
            3000,
            0,
            "Spikes Shooter"
    );

    private final List<GunData> guns = Collections.singletonList(spikeShooter);

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

                    if (inCooldown.contains(uuid) && recargaStartTime.containsKey(uuid)) {
                        long elapsed = System.currentTimeMillis() - recargaStartTime.get(uuid);

                        if (elapsed >= gun.reloadTime) {
                            setItemModelData(item, gun.base);
                            recargaStartTime.remove(uuid);
                            inCooldown.remove(uuid);
                            player.sendActionBar(ChatColor.GREEN + "Weapon Reloaded!");
                        } else {
                            double progress = (double) elapsed / gun.reloadTime;
                            player.sendActionBar(getProgressBar(progress, 20, ChatColor.RED, ChatColor.GRAY));

                            long phase = Math.max(1, gun.reloadTime / gun.reloadFrames.length);
                            int index = (int) (elapsed / phase);
                            if (index >= gun.reloadFrames.length) index = gun.reloadFrames.length - 1;
                            setItemModelData(item, gun.reloadFrames[index]);
                        }
                    }

                    if (gun.isApuntado(item) && !player.isSneaking()) {
                        setItemModelData(item, gun.base);
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
            setItemModelData(item, gun.aim);
        }

        if (!event.isSneaking() && shootingBurst.contains(player.getUniqueId())) {
            forceReload(player, gun);
        }
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        GunData gun = getGun(item);
        if (gun == null || !gun.isApuntado(item)) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        if (inCooldown.contains(uuid)) return;

        activateGunBurst(player, gun);

        player.setMetadata("spike_recent", new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("spike_recent", plugin);
            }
        }.runTaskLater(plugin, 100L);
    }

    private void activateGunBurst(Player player, GunData gun) {
        UUID uuid = player.getUniqueId();

        shootingBurst.add(uuid);
        lastPositions.put(uuid, player.getLocation().clone());

        applyRecoil(player, gun.recoil);

        new BukkitRunnable() {
            int shots = 0;

            @Override
            public void run() {

                if (!player.isSneaking()) {
                    forceReload(player, gun);
                    cancel();
                    return;
                }

                Location old = lastPositions.get(uuid);
                if (old != null && old.distanceSquared(player.getLocation()) > 0.05) {
                    forceReload(player, gun);
                    cancel();
                    return;
                }

                if (shots >= 3) {
                    shootingBurst.remove(uuid);
                    forceReload(player, gun);
                    cancel();
                    return;
                }

                fireSpike(player);
                shots++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void fireSpike(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1f, 1.2f);

        ItemStack ammo = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = ammo.getItemMeta();
        meta.setCustomModelData(2000);
        meta.setDisplayName(ChatColor.YELLOW + "Spike Projectile");
        ammo.setItemMeta(meta);

        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setItem(ammo);
        snowball.setMetadata("spike", new FixedMetadataValue(plugin, true));
        snowball.setMetadata("origin", new FixedMetadataValue(plugin, player.getLocation())); // Guarda la posición inicial
    }

    private void applyRecoil(Player player, double recoilStrength) {
        Vector dir = player.getLocation().getDirection().clone().multiply(-recoilStrength);
        player.setVelocity(player.getVelocity().add(new Vector(dir.getX(), recoilStrength / 2, dir.getZ())));
    }



    private void forceReload(Player player, GunData gun) {
        UUID uuid = player.getUniqueId();

        shootingBurst.remove(uuid);

        recargaStartTime.put(uuid, System.currentTimeMillis());
        inCooldown.add(uuid);

        setItemModelData(player.getInventory().getItemInMainHand(), gun.reloadFrames[0]);
        player.sendActionBar(ChatColor.RED + "Reloading...");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!shootingBurst.contains(uuid)) return;

        if (event.getFrom().distanceSquared(event.getTo()) > 0.05) {
            forceReload(player, spikeShooter);
            shootingBurst.remove(uuid);
        }
    }



    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        GunData gun = getGun(item);
        if (gun == null) return;

        if (!inCooldown.contains(player.getUniqueId()) && gun.isReloadFrame(item)) {
            startReload(player, item, gun);
        }
    }

    private void cancelReload(UUID uuid) {
        recargaStartTime.remove(uuid);
        inCooldown.remove(uuid);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        cancelReload(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        cancelReload(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (dropped == null || !dropped.hasItemMeta()) return;

        GunData gun = getGun(dropped);
        if (gun == null) return;

        int cmd = getCMD(dropped);
        if (cmd != gun.base && cmd != gun.aim) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your weapon while reloading!");
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!proj.hasMetadata("spike")) return;

        Location origin = (Location) proj.getMetadata("origin").get(0).value();
        if (origin.distance(proj.getLocation()) > MAX_DISTANCE) {
            proj.remove();
            return;
        }

        ProjectileSource shooter = proj.getShooter();
        if (!(shooter instanceof Player)) return;

        Player shooterPlayer = (Player) shooter;

        if (event.getHitEntity() instanceof LivingEntity target) {
            if (target.equals(shooterPlayer)) return;

            target.damage(spikeShooter.damage, shooterPlayer);

            int additionalDuration = 30; 
            int newDuration = additionalDuration;
            PotionEffect currentPoison = target.getPotionEffect(PotionEffectType.POISON);
            if (currentPoison != null) newDuration += currentPoison.getDuration();
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, newDuration, 0, false, false, true));

            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);

            if (target instanceof Player) {
                lastGunUsed.put(target.getUniqueId(), spikeShooter.name);
            }
        }

        proj.remove(); // Elimina el proyectil al impactar
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




    private GunData getGun(ItemStack item) {
        for (GunData gun : guns) {
            if (gun.isGun(item)) return gun;
        }
        return null;
    }

    private int getCMD(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()
                ? item.getItemMeta().getCustomModelData()
                : -1;
    }

    private void setItemModelData(ItemStack item, int cmd) {
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

        setItemModelData(item, gun.reloadFrames[0]);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.9f, 1.2f);
    }
}
