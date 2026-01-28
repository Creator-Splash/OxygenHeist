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

public class ReefHarpoonGun implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Long> recargaStartTime = new HashMap<>();
    private final Set<UUID> inCooldown = new HashSet<>();
    private static final Map<UUID, String> lastGunUsed = new HashMap<>();

    private static class GunData {
        Material material;
        int base, aim;
        int[] reloadFrames;
        double damage;
        long reloadTime;
        double recoil;
        String name;

        GunData(Material material, int base, int aim, int[] reloadFrames, double damage, long reloadTime, double recoil, String name) {
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

    private final GunData harpoon = new GunData(
            Material.GOLDEN_HORSE_ARMOR,
            1001,
            1002,
            new int[]{1003},
            10.0,
            5000,
            0.5,
            "Reef Harpoon Gun"
    );

    private final List<GunData> guns = Collections.singletonList(harpoon);

    public ReefHarpoonGun(Plugin plugin) {
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
                            player.sendActionBar(ChatColor.GREEN + "reloaded weapon!");
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
        if (inCooldown.contains(uuid)) {
            return;
        }

        if (activateGun(player, item, gun)) {
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

    private boolean activateGun(Player player, ItemStack gunItem, GunData gun) {
        applyRecoil(player, gun.recoil);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.2f, 1f);

        ItemStack ammo = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = ammo.getItemMeta();
        meta.setCustomModelData(1000);
        meta.setDisplayName(ChatColor.AQUA + "Harpoon Shot");
        ammo.setItemMeta(meta);

        Snowball snowball = player.launchProjectile(Snowball.class);
        snowball.setItem(ammo);
        snowball.setMetadata("harpoon", new FixedMetadataValue(plugin, true));

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
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.1f);
    }
}
