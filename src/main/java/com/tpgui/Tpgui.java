package com.tpgui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Tpgui extends JavaPlugin implements Listener {
    private static final int GUI_SIZE = 54; // 6行网格
    private static final String GUI_NAME = "在线玩家";
    private static final int ITEMS_PER_PAGE = 45; // 每页45个头颅
    private Map<Player, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tpgui")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("tpgui.use")) {
                    player.openInventory(createGUI(0));
                    return true;
                }
            }
        }
        return false;
    }

    private Inventory createGUI(int page) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_NAME);
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        int totalPages = (int) Math.ceil((double) players.length / ITEMS_PER_PAGE);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, players.length);

        for (int i = start; i < end; i++) {
            Player player = players[i];
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            meta.setLore(Arrays.asList("点击强制传送到该玩家身边"));
            head.setItemMeta(meta);
            gui.setItem(i - start, head);
        }

        // "下一页"按钮始终显示
        ItemStack nextPage = createPageItem("下一页", "点击以浏览下一页", true);
        gui.setItem(50, nextPage); // 第6行第6格

        // "上一页"按钮仅当不在第一页时显示
        if (page > 0) {
            ItemStack prevPage = createPageItem("上一页", "点击以返回上一页", true);
            gui.setItem(48, prevPage); // 第6行第4格
        }

        return gui;
    }

    private ItemStack createPageItem(String name, String lore, boolean isActive) {
        ItemStack item = new ItemStack(isActive ? Material.PAPER : Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_NAME) && event.getCurrentItem() != null) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (cooldowns.containsKey(player) && cooldowns.get(player) > System.currentTimeMillis()) {
                player.sendMessage("请等待冷却结束。还有5秒。");
                return;
            }

            if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                int currentPage = getCurrentPage(player.getOpenInventory().getTopInventory());
                int lastPageIndex = getLastPageIndex();
                if ("上一页".equals(clicked.getItemMeta().getDisplayName()) && currentPage > 0) {
                    player.openInventory(createGUI(currentPage - 1));
                } else if ("下一页".equals(clicked.getItemMeta().getDisplayName()) && currentPage < lastPageIndex) {
                    player.openInventory(createGUI(currentPage + 1));
                }
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                Player target = Bukkit.getPlayer(meta.getOwningPlayer().getName());
                if (target != null) {
                    player.teleport(target.getLocation());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                    player.sendMessage("已将您传送到玩家 " + target.getName() + "身边。");
                    cooldowns.put(player, System.currentTimeMillis() + 5000); // 5秒冷却
                }
            }
        }
    }

    private int getCurrentPage(Inventory inventory) {
        // This should be enhanced to track pages accurately
        return 0; // Placeholder
    }

    private int getLastPageIndex() {
        int totalPlayers = Bukkit.getOnlinePlayers().size();
        return (int) Math.ceil((double) totalPlayers / ITEMS_PER_PAGE) - 1;
    }
}
