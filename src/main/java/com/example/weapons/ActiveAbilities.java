package com.example.weapons;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Material;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Registry of abilities that are triggered by clicking with a weapon.
 *
 * To add your own: ActiveAbilities.register("NAME", (plugin, player, options) -> { ...; return true; });
 * then use "ability: NAME" under right-click: or sneak-right-click: in config.yml.
 */
public final class ActiveAbilities {

    private static final Map<String, ActiveAbility> REGISTRY = new TreeMap<>();

    static {
        // ---- DASH: instantly move up to N blocks in the direction you're looking (any direction, incl. up/down) ----
        register("DASH", (plugin, player, options) -> {
            ConfigurationSection o = options != null ? options : new MemoryConfiguration();
            double maxDistance = o.getDouble("distance", 20);
            double boost = o.getDouble("boost", 0.4);
            boolean resetFall = o.getBoolean("reset-fall-distance", true);

            Location start = player.getLocation();
            World world = start.getWorld();
            Vector dir = player.getEyeLocation().getDirection().normalize();

            double step = 0.25;
            double bestX = start.getX(), bestY = start.getY(), bestZ = start.getZ();
            double travelled = 0;

            // Walk forward in small steps and stop right before the first obstacle
            for (double d = step; d <= maxDistance; d += step) {
                double x = start.getX() + dir.getX() * d;
                double y = start.getY() + dir.getY() * d;
                double z = start.getZ() + dir.getZ() * d;
                if (!isClear(world, x, y, z)) break;
                bestX = x;
                bestY = y;
                bestZ = z;
                travelled = d;
            }

            if (travelled < 1.0) {
                plugin.getWeaponManager().message(player, "dash-blocked");
                return false;
            }

            // Trail along the path
            for (double d = 0; d <= travelled; d += 1.0) {
                Location p = start.clone().add(dir.clone().multiply(d)).add(0, 1, 0);
                world.spawnParticle(Particle.CLOUD, p, 3, 0.15, 0.15, 0.15, 0.01);
            }
            world.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);

            Location dest = new Location(world, bestX, bestY, bestZ, start.getYaw(), start.getPitch());
            player.teleport(dest);
            player.setVelocity(dir.clone().multiply(boost));
            if (resetFall) player.setFallDistance(0f);

            world.playSound(dest, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
            return true;
        });

        // ---- BLIND_AREA: blind everyone within a radius ----
        register("BLIND_AREA", (plugin, player, options) -> {
            ConfigurationSection o = options != null ? options : new MemoryConfiguration();
            double radius = o.getDouble("radius", 50);
            int ticks = (int) Math.round(o.getDouble("duration", 30) * 20);
            int amplifier = o.getInt("amplifier", 255);
            boolean excludeSelf = o.getBoolean("exclude-self", true);
            boolean includeMobs = o.getBoolean("include-mobs", false);

            Location origin = player.getLocation();
            PotionEffect effect = new PotionEffect(PotionEffectType.BLINDNESS, ticks, amplifier);

            int count = 0;
            for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity living)) continue;
                if (!(e instanceof Player) && !includeMobs) continue;
                if (e instanceof Player p && p.getGameMode() == GameMode.SPECTATOR) continue;
                if (!e.getWorld().equals(origin.getWorld())) continue;
                if (e.getLocation().distanceSquared(origin) > radius * radius) continue;

                living.addPotionEffect(effect);
                count++;

                if (e instanceof Player victim) {
                    victim.playSound(victim.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1f);
                    plugin.getWeaponManager().message(victim, "blinded", "{player}", player.getName());
                }
            }

            if (count == 0) {
                plugin.getWeaponManager().message(player, "blind-no-targets");
                return false;
            }

            if (!excludeSelf) player.addPotionEffect(effect);

            player.getWorld().spawnParticle(Particle.CLOUD, origin.clone().add(0, 1, 0), 80, 1, 1, 1, 0.1);
            plugin.getWeaponManager().message(player, "blind-used", "{count}", String.valueOf(count));
            return true;
        });
    }

    private ActiveAbilities() {}

    /** Is there room for a player standing with feet at (x, y, z)? */
    private static boolean isClear(World world, double x, double y, double z) {
        if (y < world.getMinHeight() || y + 2 > world.getMaxHeight()) return false;
        double[] offsets = {-0.3, 0.3};
        double[] heights = {0.0, 1.0, 1.8};
        for (double dx : offsets) {
            for (double dz : offsets) {
                for (double dy : heights) {
                    Block b = world.getBlockAt(
                            (int) Math.floor(x + dx), (int) Math.floor(y + dy), (int) Math.floor(z + dz));
                    if (!b.isPassable() || b.getType() == Material.LAVA) return false;
                }
            }
        }
        return true;
    }

    public static void register(String name, ActiveAbility ability) {
        REGISTRY.put(name.toUpperCase(Locale.ROOT), ability);
    }

    public static ActiveAbility get(String name) {
        return name == null ? null : REGISTRY.get(name.toUpperCase(Locale.ROOT));
    }

    public static Set<String> names() {
        return REGISTRY.keySet();
    }
}
