package com.example.weapons;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Handles right-click / sneak-right-click abilities on weapons (swords etc). */
public class WeaponUseListener implements Listener {

    private final WeaponPlugin plugin;
    private final WeaponManager manager;

    public WeaponUseListener(WeaponPlugin plugin, WeaponManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action clickType = event.getAction();
        if (clickType != Action.RIGHT_CLICK_AIR && clickType != Action.RIGHT_CLICK_BLOCK) return;

        Weapon weapon = manager.getWeapon(event.getItem());
        if (weapon == null) return;

        Player player = event.getPlayer();

        Weapon.WeaponAction action = (player.isSneaking() && weapon.getSneakRightClick() != null)
                ? weapon.getSneakRightClick()
                : weapon.getRightClick();
        if (action == null) return;

        // Let normal block interactions (chests, doors, buttons...) work when not sneaking
        if (clickType == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()
                && event.getClickedBlock() != null && event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        if (!player.hasPermission("customweapons.use")) {
            manager.message(player, "no-permission");
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        String key = weapon.getId() + ":" + action.getName();
        double remaining = manager.getRemainingCooldown(player.getUniqueId(), key);
        if (remaining > 0) {
            String raw = plugin.getConfig().getString("messages.cooldown", "");
            if (!raw.isBlank()) {
                // Action bar so holding right-click doesn't spam chat
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                        manager.format("cooldown", "{time}", String.format("%.0f", Math.ceil(remaining)))));
            }
            return;
        }

        ActiveAbility ability = ActiveAbilities.get(action.getAbility());
        if (ability == null) return;

        boolean used = ability.use(plugin, player, action.getOptions());
        if (used) {
            manager.startCooldown(player.getUniqueId(), key, action.getCooldown());
        }
    }
}
