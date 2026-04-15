package com.villager.bucket;

import org.bukkit.plugin.java.JavaPlugin;

public class VillagerBucket extends JavaPlugin {
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("villagerbucket").setExecutor(new BucketCommand(this));
        getServer().getPluginManager().registerEvents(new BucketListener(this), this);
        getLogger().info("VillagerBucket enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("VillagerBucket disabled!");
    }
}
