package com.villager.bucket;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BucketListener implements Listener {
    
    private final VillagerBucket plugin;
    private final NamespacedKey professionKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey nameKey;
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    
    public BucketListener(VillagerBucket plugin) {
        this.plugin = plugin;
        this.professionKey = new NamespacedKey(plugin, "profession");
        this.levelKey = new NamespacedKey(plugin, "level");
        this.typeKey = new NamespacedKey(plugin, "type");
        this.nameKey = new NamespacedKey(plugin, "customname");
    }
    
    @EventHandler
    public void onEntityClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() != Material.BUCKET) return;
        if (!item.hasItemMeta()) return;
        if (!"§6Villager Bucket".equals(item.getItemMeta().getDisplayName())) return;
        if (!player.hasPermission("villagerbucket.capture")) return;
        
        if (event.getRightClicked().getType() != EntityType.VILLAGER) return;
        
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendMessage("§c⏱ Cooldown: " + timeLeft + "s remaining!");
                event.setCancelled(true);
                return;
            }
        }
        
        Villager villager = (Villager) event.getRightClicked();
        event.setCancelled(true);
        
        ItemStack filledBucket = new ItemStack(Material.WATER_BUCKET);
        ItemMeta meta = filledBucket.getItemMeta();
        meta.setDisplayName("§6Villager Bucket §7(Filled)");
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(professionKey, PersistentDataType.STRING, villager.getProfession().name());
        pdc.set(levelKey, PersistentDataType.INTEGER, villager.getVillagerLevel());
        pdc.set(typeKey, PersistentDataType.STRING, villager.getVillagerType().name());
        
        if (villager.getCustomName() != null) {
            pdc.set(nameKey, PersistentDataType.STRING, villager.getCustomName());
        }
        
        meta.setLore(List.of(
            "§7Profession: §e" + villager.getProfession().name(),
            "§7Level: §e" + villager.getVillagerLevel(),
            "§7Type: §e" + villager.getVillagerType().name(),
            "§7Right-click ground to place"
        ));
        
        filledBucket.setItemMeta(meta);
        
        item.setAmount(item.getAmount() - 1);
        player.getInventory().addItem(filledBucket);
        
        Location loc = villager.getLocation();
        villager.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.3, 0.5, 0.3, 0.05);
        villager.getWorld().playSound(loc, Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        villager.remove();
        
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 15000);
        player.sendMessage("§a✔ Villager captured! §7(15s cooldown)");
    }
    
    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        
        if (item.getType() != Material.WATER_BUCKET) return;
        if (!item.hasItemMeta()) return;
        if (!"§6Villager Bucket §7(Filled)".equals(item.getItemMeta().getDisplayName())) return;
        if (!event.getPlayer().hasPermission("villagerbucket.place")) return;
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        if (cooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendMessage("§c⏱ Cooldown: " + timeLeft + "s remaining!");
                return;
            }
        }
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (!pdc.has(professionKey, PersistentDataType.STRING)) return;
        
        Location spawnLoc = getSafeLocation(player);
        if (spawnLoc == null) {
            player.sendMessage("§c✖ No safe location found to place villager!");
            return;
        }
        
        Villager villager = (Villager) player.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);
        
        villager.setProfession(Villager.Profession.valueOf(pdc.get(professionKey, PersistentDataType.STRING)));
        villager.setVillagerLevel(pdc.get(levelKey, PersistentDataType.INTEGER));
        villager.setVillagerType(Villager.Type.valueOf(pdc.get(typeKey, PersistentDataType.STRING)));
        
        if (pdc.has(nameKey, PersistentDataType.STRING)) {
            villager.setCustomName(pdc.get(nameKey, PersistentDataType.STRING));
        }
        
        spawnLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, spawnLoc.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        
        ItemStack emptyBucket = new ItemStack(Material.BUCKET);
        ItemMeta meta = emptyBucket.getItemMeta();
        meta.setDisplayName("§6Villager Bucket");
        meta.setLore(List.of("§7Right-click a villager to capture", "§7Right-click ground to place"));
        emptyBucket.setItemMeta(meta);
        
        item.setAmount(item.getAmount() - 1);
        player.getInventory().addItem(emptyBucket);
        
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + 15000);
        player.sendMessage("§a✔ Villager placed! §7(15s cooldown)");
    }
    
    private Location getSafeLocation(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null || target.getType() == Material.AIR) {
            target = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        }
        
        Location spawnLoc = target.getRelative(BlockFace.UP).getLocation().add(0.5, 0, 0.5);
        
        Block spawnBlock = spawnLoc.getBlock();
        Block above = spawnBlock.getRelative(BlockFace.UP);
        
        if (spawnBlock.getType() != Material.AIR || above.getType() != Material.AIR) {
            spawnLoc.add(0, 1, 0);
            spawnBlock = spawnLoc.getBlock();
            above = spawnBlock.getRelative(BlockFace.UP);
            
            if (spawnBlock.getType() != Material.AIR || above.getType() != Material.AIR) {
                return null;
            }
        }
        
        return spawnLoc;
    }
}
