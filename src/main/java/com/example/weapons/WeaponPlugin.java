package com.example.weapons;

import org.bukkit.plugin.java.JavaPlugin;

public class WeaponPlugin extends JavaPlugin {

    private WeaponManager weaponManager;
    private TrackerManager trackerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        weaponManager = new WeaponManager(this);
        weaponManager.load();

        trackerManager = new TrackerManager(this);
        trackerManager.start();

        getServer().getPluginManager().registerEvents(new WeaponListener(this, weaponManager), this);
        getServer().getPluginManager().registerEvents(new TrackerListener(this, weaponManager, trackerManager), this);
        getServer().getPluginManager().registerEvents(new WeaponUseListener(this, weaponManager), this);

        WeaponCommand command = new WeaponCommand(this, weaponManager, trackerManager);
        getCommand("weapons").setExecutor(command);
        getCommand("weapons").setTabCompleter(command);

        getLogger().info("Loaded " + weaponManager.getWeapons().size() + " weapon(s).");
    }

    @Override
    public void onDisable() {
        if (trackerManager != null) trackerManager.stop();
    }

    public WeaponManager getWeaponManager() {
        return weaponManager;
    }

    public TrackerManager getTrackerManager() {
        return trackerManager;
    }
}
