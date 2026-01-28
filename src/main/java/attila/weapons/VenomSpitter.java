package attila.weapons;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Particle.DustOptions;

import java.util.*;

public class VenomSpitter implements Listener {

    private final Plugin plugin;

    private final Map<UUID, Long> reloadStart = new HashMap<>();
    private final Map<UUID, Integer> reloadSlot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Set<UUID> shooting = new HashSet<>();

    private static final int AMMO_CMD = 3000;

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

                    if (reloading.contains(uuid)) {

                        if (player.getInventory().getHeldItemSlot() != reloadSlot.get(uuid)) {
                            cancelReload(player);
                            continue;
                        }

                        long elapsed = System.currentTimeMillis() - reloadStart.get(uuid);

                        if (elapsed >= gun.reloadTime) {
                            setAmmo(item, gun.maxAmmo, player);
                            consumeOneAmmo(player);
                            setCMD(item, gun.base, player);

                            reloading.remove(uuid);
                            reloadStart.remove(uuid);
                            reloadSlot.remove(uuid);

                            player.sendActionBar(ChatColor.GREEN + "Weapon Reloaded!");
                        } else {
                            double progress = (double) elapsed / gun.reloadTime;
                            player.sendActionBar(progressBar(progress));
                            setCMD(item, gun.reloadFrames[0], player);
                        }
                    }
                  
                    if (shooting.contains(uuid)) {
                        int ammo = getAmmo(item);

                        if (ammo <= 0) {
                            shooting.remove(uuid);
                            player.sendActionBar(ChatColor.RED + "No ammo! Right-click to reload");
                        } else {
                            shoot(player);
                            setAmmo(item, ammo - 1, player);
                            player.sendActionBar(ChatColor.YELLOW + "Ammo: " + (ammo - 1) + "/" + gun.maxAmmo);
                        }
                    }
                  
                    if (getCMD(item) == gun.aim && !player.isSneaking()) {
                        setCMD(item, gun.base, player);
                        shooting.remove(uuid);
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

        if (e.isSneaking()) {
            setCMD(item, gun.aim, p);
        } else {
            shooting.remove(p.getUniqueId());
        }
    }

    @EventHandler
    public void onShoot(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!gun.isGun(item)) return;
        if (getCMD(item) != gun.aim) return;
        if (reloading.contains(p.getUniqueId())) return;

        e.setCancelled(true);

        if (getAmmo(item) <= 0) {
            p.sendActionBar(ChatColor.RED + "No ammo! Right-click to reload");
            return;
        }

        shooting.add(p.getUniqueId());
    }

    @EventHandler
    public void onReload(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        UUID uuid = p.getUniqueId();

        if (!gun.isGun(item)) return;
        if (reloading.contains(uuid)) return;

        if (getAmmo(item) >= gun.maxAmmo) {
            p.sendActionBar(ChatColor.YELLOW + "Ammo full!");
            return;
        }

        if (!hasAmmo(p)) {
            p.sendActionBar(ChatColor.RED + "You need at least 1 ammo to reload!");
            return;
        }

        reloading.add(uuid);
        reloadStart.put(uuid, System.currentTimeMillis());
        reloadSlot.put(uuid, p.getInventory().getHeldItemSlot());

        setCMD(item, gun.reloadFrames[0], p);
        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (reloading.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your weapon while reloading!");
        }
    }

    private void shoot(Player p) {
        Location start = p.getEyeLocation();
        Vector dir = start.getDirection().normalize();
        World w = p.getWorld();

        for (double i = 1; i <= 7; i += 0.5) {
            Location loc = start.clone().add(dir.clone().multiply(i));
            w.spawnParticle(Particle.DUST, loc, 4, 0, 0, 0, 0, new DustOptions(Color.GREEN, 1.2f));
            w.spawnParticle(Particle.DUST, loc, 1, new DustOptions(Color.GREEN, 1.2f));

            for (Entity e : w.getNearbyEntities(loc, 1.5, 2, 1.5)) {
                if (e instanceof LivingEntity le && !le.equals(p)) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                }
            }
        }

        w.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
    }

    private void cancelReload(Player p) {
        UUID uuid = p.getUniqueId();
        reloading.remove(uuid);
        reloadStart.remove(uuid);
        reloadSlot.remove(uuid);
        p.sendActionBar(ChatColor.RED + "Reload canceled!");
    }

    private boolean hasAmmo(Player p) {
        for (ItemStack i : p.getInventory()) {
            if (i != null && i.getType() == Material.SNOWBALL &&
                i.hasItemMeta() && i.getItemMeta().getCustomModelData() == AMMO_CMD) {
                return true;
            }
        }
        return false;
    }

    private void consumeOneAmmo(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack != null && stack.getType() == Material.SNOWBALL &&
                stack.hasItemMeta() && stack.getItemMeta().getCustomModelData() == AMMO_CMD) {

                stack.setAmount(stack.getAmount() - 1);
                if (stack.getAmount() <= 0) p.getInventory().setItem(i, null);
                return;
            }
        }
    }


    private int getAmmo(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return 0;
        for (String l : item.getItemMeta().getLore()) {
            if (l.startsWith("Ammo: "))
                return Integer.parseInt(l.replace("Ammo: ", "").split("/")[0]);
        }
        return 0;
    }

    private void setAmmo(ItemStack item, int ammo, Player p) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(List.of("Ammo: " + ammo + "/" + gun.maxAmmo));
        item.setItemMeta(meta);
        p.getInventory().setItemInMainHand(item);
    }

    private int getCMD(ItemStack item) {
        return item.getItemMeta().getCustomModelData();
    }

    private void setCMD(ItemStack item, int cmd, Player p) {
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
        p.getInventory().setItemInMainHand(item);
    }

    private String progressBar(double p) {
        int bars = (int) (20 * p);
        return ChatColor.RED + "█".repeat(bars) + ChatColor.GRAY + "░".repeat(20 - bars);
    }
}
