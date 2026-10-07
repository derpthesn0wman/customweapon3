package com.example.weapons;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Registry of all abilities a weapon can use.
 *
 * To add a brand new ability in code, call Abilities.register("NAME", (plugin, shooter, target, options) -> { ... });
 * (the static block below is a good place), then use "ability: NAME" in config.yml.
 */
public final class Abilities {

    private static final Map<String, Ability> REGISTRY = new TreeMap<>();

    static {
        // ---- SWAP: shooter and target trade places ----
        register("SWAP", (plugin, shooter, target, options) -> {
            boolean keepRotation = options == null || options.getBoolean("keep-rotation", true);

            Location shooterLoc = shooter.getLocation();
            Location targetLoc = target.getLocation();

            Location shooterDest = targetLoc.clone();
            Location targetDest = shooterLoc.clone();

            if (keepRotation) {
                shooterDest.setYaw(shooterLoc.getYaw());
                shooterDest.setPitch(shooterLoc.getPitch());
                targetDest.setYaw(targetLoc.getYaw());
                targetDest.setPitch(targetLoc.getPitch());
            }

            // Particles at the old positions, sounds at the new ones
            shooterLoc.getWorld().spawnParticle(Particle.PORTAL, shooterLoc.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.2);
            targetLoc.getWorld().spawnParticle(Particle.PORTAL, targetLoc.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.2);

            shooter.teleport(shooterDest);
            target.teleport(targetDest);

            shooterDest.getWorld().playSound(shooterDest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            targetDest.getWorld().playSound(targetDest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            plugin.getWeaponManager().message(shooter, "swapped", "{target}", target.getName());
            if (target instanceof Player targetPlayer) {
                plugin.getWeaponManager().message(targetPlayer, "was-swapped", "{shooter}", shooter.getName());
            }
        });

        // ---- LAUNCH: throw the target into the air ----
        register("LAUNCH", (plugin, shooter, target, options) -> {
            double power = options == null ? 1.2 : options.getDouble("power", 1.2);
            target.setVelocity(target.getVelocity().setY(power));
        });

        // ---- LIGHTNING: strike the target ----
        register("LIGHTNING", (plugin, shooter, target, options) -> {
            boolean harmless = options != null && options.getBoolean("harmless", false);
            if (harmless) {
                target.getWorld().strikeLightningEffect(target.getLocation());
            } else {
                target.getWorld().strikeLightning(target.getLocation());
            }
        });

        // ---- EFFECT: apply a potion effect to target or shooter ----
        register("EFFECT", (plugin, shooter, target, options) -> {
            if (options == null) return;
            String name = options.getString("effect", "slowness").toLowerCase(Locale.ROOT);
            PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.minecraft(name));
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect in config: " + name);
                return;
            }
            int ticks = (int) (options.getDouble("duration", 5) * 20);
            int amplifier = options.getInt("amplifier", 0);
            boolean onShooter = "SHOOTER".equalsIgnoreCase(options.getString("apply-to", "TARGET"));

            LivingEntity recipient = onShooter ? shooter : target;
            recipient.addPotionEffect(new PotionEffect(type, ticks, amplifier));
        });
    }

    private Abilities() {}

    public static void register(String name, Ability ability) {
        REGISTRY.put(name.toUpperCase(Locale.ROOT), ability);
    }

    public static Ability get(String name) {
        return name == null ? null : REGISTRY.get(name.toUpperCase(Locale.ROOT));
    }

    public static Set<String> names() {
        return REGISTRY.keySet();
    }
}
