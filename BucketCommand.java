package com.villager.bucket;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class BucketCommand implements CommandExecutor {
    
    private final VillagerBucket plugin;
    
    public BucketCommand(VillagerBucket plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        if (!player.hasPermission("villagerbucket.give")) {
            player.sendMessage("§cYou don't have permission!");
            return true;
        }
        
        ItemStack bucket = new ItemStack(Material.BUCKET);
        ItemMeta meta = bucket.getItemMeta();
        meta.setDisplayName("§6Villager Bucket");
        meta.setLore(List.of("§7Right-click a villager to capture", "§7Right-click ground to place"));
        bucket.setItemMeta(meta);
        
        player.getInventory().addItem(bucket);
        player.sendMessage("§aYou received a Villager Bucket!");
        return true;
    }
}
