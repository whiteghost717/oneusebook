package me.rosey.oneusebook;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OneUseBook extends JavaPlugin implements Listener {
    private NamespacedKey bookIdKey;
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        bookIdKey = new NamespacedKey(this, "book_id");
        dataFile = new File(getDataFolder(), "data.yml");
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create plugin data folder.");
        }
        reloadData();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("OneUseBook enabled.");
    }

    private void reloadData() {
        data = YamlConfiguration.loadConfiguration(dataFile);
        if (!data.isList("used")) data.set("used", new ArrayList<String>());
        if (!data.isConfigurationSection("books")) data.createSection("books");
        saveData();
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("oneusebook.admin")) {
            sender.sendMessage(ChatColor.RED + "Only an operator/admin can bind command books.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Hold a signed book and run /oneusebook bind in-game.");
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("bind")) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /oneusebook bind");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WRITTEN_BOOK || !(item.getItemMeta() instanceof BookMeta meta)) {
            player.sendMessage(ChatColor.RED + "Hold a WRITTEN (signed) book in your main hand.");
            return true;
        }
        if (meta.getAuthor() == null || !meta.getAuthor().equalsIgnoreCase(player.getName())) {
            player.sendMessage(ChatColor.RED + "The book must be signed by your own Minecraft account.");
            return true;
        }

        List<String> commands = new ArrayList<>();
        for (String page : meta.getPages()) {
            if (page == null) continue;
            for (String line : page.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("/")) trimmed = trimmed.substring(1).trim();
                if (!trimmed.isEmpty()) commands.add(trimmed);
            }
        }
        if (commands.isEmpty()) {
            player.sendMessage(ChatColor.RED + "The book has no commands. Put one command on each line, sign it, and try again.");
            return true;
        }

        String id = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(bookIdKey, PersistentDataType.STRING, id);
        meta.setDisplayName(ChatColor.GOLD + "One-Time Command Book");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Authorized by " + player.getName());
        lore.add(ChatColor.RED + "Usable once globally");
        meta.setLore(lore);
        item.setItemMeta(meta);

        data.set("books." + id + ".commands", commands);
        data.set("books." + id + ".creator", player.getName());
        data.set("books." + id + ".used", false);
        saveData();

        player.sendMessage(ChatColor.GREEN + "Book authorized! It will run these console commands once in total:");
        for (String cmd : commands) player.sendMessage(ChatColor.GRAY + " - " + cmd);
        player.sendMessage(ChatColor.YELLOW + "Use {player} in a command to substitute the person who clicks it.");
        return true;
    }

    @EventHandler
    public void onBookUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !(item.getItemMeta() instanceof BookMeta meta)) return;

        String id = meta.getPersistentDataContainer().get(bookIdKey, PersistentDataType.STRING);
        if (id == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (data.getBoolean("books." + id + ".used", false) || data.getStringList("used").contains(id)) {
            player.sendMessage(ChatColor.RED + "This command book has already been used.");
            consumeMainHand(player, item);
            return;
        }

        List<String> commands = data.getStringList("books." + id + ".commands");
        if (commands.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This book has no valid saved commands. Contact an operator.");
            return;
        }

        // Persist the used state before executing anything to prevent double-clicks or duplicated books.
        data.set("books." + id + ".used", true);
        List<String> used = data.getStringList("used");
        if (!used.contains(id)) used.add(id);
        data.set("used", used);
        saveData();

        consumeMainHand(player, item);
        player.sendMessage(ChatColor.GREEN + "Command book activated. It is now consumed.");

        for (String stored : commands) {
            String cmd = stored.replace("{player}", player.getName())
                               .replace("{uuid}", player.getUniqueId().toString());
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Exception ex) {
                getLogger().severe("Error executing a command from one-use book " + id + ": " + ex.getMessage());
                player.sendMessage(ChatColor.RED + "A command failed. Contact an operator.");
            }
        }
    }

    private void consumeMainHand(Player player, ItemStack eventItem) {
        ItemStack current = player.getInventory().getItemInMainHand();
        if (current.isSimilar(eventItem)) {
            if (current.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
            else current.setAmount(current.getAmount() - 1);
        }
    }
}
