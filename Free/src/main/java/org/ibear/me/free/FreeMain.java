package org.ibear.me.free;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class FreeMain extends JavaPlugin implements Listener {

    public static Inventory inv;
    public ItemStack current, food, armor, tool, close;
    public ItemMeta im1, im2, im3, im4;
    public ChatColor CC;
    public UUID uuid;
    private HashMap<UUID, Long> foodCooldown = new HashMap<>();
    private HashMap<UUID, Long> armorCooldown = new HashMap<>();
    private HashMap<UUID, Long> toolCooldown = new HashMap<>();
    private Long lastSec, now, leftSec;

    // INT TIME COOLDOWN
    private int intTimeFOOD = 86400; // 1 DAY
    private int intTimeARMOR = 1800; // 30 MINUTES
    private int intTimeTOOL = 43200; // 12 HOURS


    private String formatCooldown(long milliseconds) {
        long sec = milliseconds / 1000L;
        long min = sec / 60L;
        long h = min / 60L;
        long d = h / 24L;

        StringBuilder timeString = new StringBuilder();

        if (d > 0) {
            timeString.append(d).append(" D ");
        }
        if (h > 0) {
            timeString.append(h % 24).append(" H ");
        }
        if (min > 0) {
            timeString.append(min % 60).append(" MIN ");
        }
        if (sec > 0) {
            timeString.append(sec % 60).append(" SEC ");
        }

        return timeString.toString().trim();
    }

    private Connection connection;
    private File dataFile;

    @Override

    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getLogger().info("v1.3");

        // CONNECT DATABASE
        try {
            openConnection();
            createTable();
            loadCooldowns();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    // OPEN DATABASE
    public void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + File.separator + "data.db");
        }
    }

    // CREATE TABLE
    public void createTable() throws SQLException {
        PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS cooldowns (uuid VARCHAR(36) PRIMARY KEY, foodCooldown BIGINT, armorCooldown BIGINT, toolCooldown BIGINT)");
        ps.executeUpdate();
        ps.close();
    }

    // LOAD COOLDOWN
    private void loadCooldowns() throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM cooldowns");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            long foodCooldown = rs.getLong("foodCooldown");
            long armorCooldown = rs.getLong("armorCooldown");
            long toolCooldown = rs.getLong("toolCooldown");
            this.foodCooldown.put(uuid, foodCooldown);
            this.armorCooldown.put(uuid, armorCooldown);
            this.toolCooldown.put(uuid, toolCooldown);
        }
        ps.close();
        rs.close();
    }

    // SAVE COOLDOWN
    private void saveCooldowns() throws SQLException {
        for (Map.Entry<UUID, Long> entry : foodCooldown.entrySet()) {
            UUID uuid = entry.getKey();
            long foodCooldown = entry.getValue();
            Long armorCooldown = this.armorCooldown.get(uuid);
            Long toolCooldown = this.toolCooldown.get(uuid);

            // Check if armorCooldown or toolCooldown is null, then set it to 0
            if (armorCooldown == null) {
                armorCooldown = 0L;
            }
            if (toolCooldown == null) {
                toolCooldown = 0L;
            }


            PreparedStatement ps = connection.prepareStatement("REPLACE INTO cooldowns (uuid, foodCooldown, armorCooldown, toolCooldown) VALUES (?, ?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setLong(2, foodCooldown);
            ps.setLong(3, armorCooldown);
            ps.setLong(4, toolCooldown);
            ps.executeUpdate();
            ps.close();
        }
    }

    // CREATE MENU
    public void createMenu(Player p) {
        // GUI
        inv = Bukkit.getServer().createInventory(null, 9, "Admin");

        // ITEM 1
        food = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
        im1 = food.getItemMeta();
        im1.setDisplayName(CC.GREEN + "FOOD");
        im1.setLore(Arrays.asList(
                CC.WHITE + "- ENCHANTED GOLDEN APPLE 1 ITEM",
                CC.WHITE + "- BREAD 16 ITEM",
                CC.YELLOW + "TIME COOLDOWN: 1 DAY"
        ));
        food.setItemMeta(im1);
        inv.setItem(2, food);

        // ITEM 2
        armor = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        im2 = armor.getItemMeta();
        im2.setDisplayName(CC.GREEN + "ARMOR");
        im2.setLore(Arrays.asList(
                CC.WHITE + "- LEATHER CHESTPLATE 1 ITEM",
                CC.YELLOW + "TIME COOLDOWN: 30 MINUTES"
        ));
        armor.setItemMeta(im2);
        inv.setItem(4, armor);

        // ITEM 3
        tool = new ItemStack(Material.IRON_AXE, 1);
        im3 = tool.getItemMeta();
        im3.setDisplayName(CC.GREEN + "TOOL");
        im3.setLore(Arrays.asList(
                CC.WHITE + "- IRON AXE 1 ITEM",
                CC.YELLOW + "COOLDOWN: 12 HOURS"
        ));
        tool.setItemMeta(im3);
        inv.setItem(6, tool);

        // ITEM 4 X
        close = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
        im4 = close.getItemMeta();
        im4.setDisplayName(CC.RED + "X");
        tool.setItemMeta(im4);
        close.setItemMeta(im4);
        inv.setItem(0, close);
        inv.setItem(1, close);
        inv.setItem(3, close);
        inv.setItem(5, close);
        inv.setItem(7, close);
        inv.setItem(8, close);

        p.openInventory(inv);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (command.getName().equalsIgnoreCase("free")) {
                createMenu(p);
            }
            if (command.getName().equalsIgnoreCase("clearcooldown")) {
                if (args.length == 0) {
                    // กรณีไม่ระบุผู้เล่นเฉพาะคนเรียกใช้คำสั่ง
                    clearCooldown(p);
                    p.sendMessage(ChatColor.GREEN + "Cleared cooldown for yourself.");
                } else {
                    // กรณีระบุผู้เล่น
                    Player targetPlayer = Bukkit.getPlayer(args[0]);
                    if (targetPlayer != null) {
                        clearCooldown(targetPlayer);
                        p.sendMessage(ChatColor.GREEN + "Cleared cooldown for " + targetPlayer.getName() + ".");
                    } else {
                        p.sendMessage(ChatColor.RED + "Player not found.");
                    }
                }
            }
        }
        return true;
    }

    // CLEAR COOLDOWN
    private void clearCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        foodCooldown.remove(uuid);
        armorCooldown.remove(uuid);
        toolCooldown.remove(uuid);

        try {
            saveCooldowns();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryDragEvent(InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        current = e.getCurrentItem();
        uuid = p.getUniqueId();
        now = System.currentTimeMillis();

        if (current == null || !e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        // CHECK ITEM1
        if (current.getType() == Material.ENCHANTED_GOLDEN_APPLE) {

            if (!foodCooldown.containsKey(uuid) || now - foodCooldown.get(uuid) > intTimeFOOD * 1000) {

                p.sendMessage(CC.GREEN + "RECEIVE: " + CC.WHITE + "ENCHANTED GOLDEN APPLE 1 ITEM");
                p.sendMessage(CC.GREEN + "RECEIVE: " + CC.WHITE + "BREAD 16 ITEM");
                e.getWhoClicked().getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                e.getWhoClicked().getInventory().addItem(new ItemStack(Material.BREAD, 16));
                foodCooldown.put(uuid, now);

                // SAVE COOLDOWN
                try {
                    saveCooldowns();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                leftSec = (intTimeFOOD - (now - foodCooldown.get(uuid)) / 1000);
                p.closeInventory();
                p.sendMessage(CC.RED + "CAN RECEIVE AGAIN IN: " + CC.YELLOW + formatCooldown(leftSec * 1000));
            }
        }
        // CHECK ITEM 2
        if (current.getType() == Material.LEATHER_CHESTPLATE) {

            if (!armorCooldown.containsKey(uuid) || now - armorCooldown.get(uuid) > intTimeARMOR * 1000) {

                p.sendMessage(CC.GREEN + "RECEIVE: " + CC.WHITE + "LEATHER CHESTPLATE 1 ITEM");
                e.getWhoClicked().getInventory().addItem(new ItemStack(Material.LEATHER_CHESTPLATE, 1));
                armorCooldown.put(uuid, now);

                // SAVE COOLDOWN
                try {
                    saveCooldowns();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                leftSec = (intTimeARMOR - (now - armorCooldown.get(uuid)) / 1000);
                p.closeInventory();
                p.sendMessage(CC.RED + "CAN RECEIVE AGAIN IN: " + CC.YELLOW + formatCooldown(leftSec * 1000));
            }
        }

        // CHECK ITEM 3
        if (current.getType() == Material.IRON_AXE) {

            if (!toolCooldown.containsKey(uuid) || now - toolCooldown.get(uuid) > intTimeTOOL * 1000) {

                p.sendMessage(CC.GREEN + "RECEIVE: " + CC.WHITE + "IRON AXE 1 ITEM");
                e.getWhoClicked().getInventory().addItem(new ItemStack(Material.IRON_AXE, 1));
                toolCooldown.put(uuid, now);

                // SAVE COOLDOWN
                try {
                    saveCooldowns();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                leftSec = (intTimeTOOL - (now - toolCooldown.get(uuid)) / 1000);
                p.closeInventory();
                p.sendMessage(CC.RED + "CAN RECEIVE AGAIN IN: " + CC.YELLOW + formatCooldown(leftSec * 1000));
            }
        }
        // CHECK BARRIER
        if (current.getType() == Material.RED_STAINED_GLASS_PANE) {
            p.closeInventory();
        }

    }

    @Override
    public void onDisable() {
        try {
            saveCooldowns();
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}