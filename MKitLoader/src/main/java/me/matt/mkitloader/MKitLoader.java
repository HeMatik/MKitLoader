package me.matt.mkitloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MKitLoader extends JavaPlugin implements CommandExecutor {
    private final Map<UUID, Map<String, ItemStack[]>> playerKits = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info(ChatColor.GREEN + "Plugin enabled");
        Objects.requireNonNull(getCommand("kit")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.RED + "Plugin disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("kit")) {
            if (args.length == 0) {
                sender.sendMessage("§fИспользование§a:\n §f/kit §a<§fsave§a/§fdelete§a/§fload§a/§flist§a/§fauto§a> <§fНазвание кита§a>\n§fsave §a- §fСохранить кит\n§fdelete §a- §fУдалить кит\n§fload §a- §fЗагрузить кит\n§flist §a- §fСписок китов\n");
                return true;
            }
            if (args[0].equalsIgnoreCase("save")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /kit save <название кита>");
                    return true;
                }
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String kitName = args[1];
                    Map<String, ItemStack[]> playerKits = getPlayerKits(player.getUniqueId());
                    int maxKits = 3;
                    if (player.hasPermission("mkitloader.save.5")) {
                        maxKits = 5;
                    }
                    if (player.hasPermission("mkitloader.save.10")) {
                        maxKits = 10;
                    }
                    if (player.hasPermission("mkitloader.save.unlimited")) {
                        maxKits = Integer.MAX_VALUE;
                    }
                    if (playerKits.size() >= maxKits) {
                        player.sendMessage(ChatColor.RED + "Вы не можете сохранить больше " + maxKits + " китов.");
                        return true;
                    }
                    ItemStack[] kitContents = player.getInventory().getContents();
                    playerKits.put(kitName, kitContents);
                    savePlayerKits(player.getUniqueId(), playerKits);
                    player.sendMessage(ChatColor.GREEN + "Кит " + kitName + " сохранен.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Команда доступна только для игроков.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /kit delete <название кита>");
                    return true;
                }
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String kitName = args[1];
                    Map<String, ItemStack[]> playerKits = getPlayerKits(player.getUniqueId());
                    if (playerKits.containsKey(kitName)) {
                        playerKits.remove(kitName);
                        savePlayerKits(player.getUniqueId(), playerKits);
                        player.sendMessage(ChatColor.GREEN + "Кит " + kitName + " удален.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Кит " + kitName + " не найден.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Команда доступна только для игроков.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("load")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /kit load <название кита>");
                    return true;
                }
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    String kitName = args[1];
                    Map<String, ItemStack[]> playerKits = getPlayerKits(player.getUniqueId());
                    if (playerKits.containsKey(kitName)) {
                        ItemStack[] kitContents = playerKits.get(kitName);
                        player.getInventory().setContents(kitContents);
                        player.sendMessage(ChatColor.GREEN + "Кит " + kitName + " загружен.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Кит " + kitName + " не найден.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Команда доступна только для игроков.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("list")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Map<String, ItemStack[]> playerKits = getPlayerKits(player.getUniqueId());
                    if (playerKits.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "У вас нет сохраненных китов.");
                    } else {
                        player.sendMessage(ChatColor.WHITE + "Ваши сохраненные киты:");
                        for (String kitName : playerKits.keySet()) {
                            player.sendMessage(ChatColor.GREEN + "- " + ChatColor.WHITE + kitName);
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Команда доступна только для игроков.");
                }
                return true;
            }
    }
    return false;
}

    private Map<String, ItemStack[]> getPlayerKits(UUID playerId) {
        File playerFile = new File(getDataFolder(), playerId.toString() + ".yml");
        if (!playerFile.exists()) {
            return new HashMap<>();
        }
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        Map<String, ItemStack[]> playerKits = new HashMap<>();
        ConfigurationSection kitsSection = playerConfig.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String kitName : kitsSection.getKeys(false)) {
                ItemStack[] kitContents = ((List<ItemStack>) kitsSection.getList(kitName)).toArray(new ItemStack[0]);
                playerKits.put(kitName, kitContents);
            }
        }
        return playerKits;
    }

    private void savePlayerKits(UUID playerId, Map<String, ItemStack[]> playerKits) {
        File playerFile = new File(getDataFolder(), playerId.toString() + ".yml");
        YamlConfiguration playerConfig = new YamlConfiguration();
        ConfigurationSection kitsSection = playerConfig.createSection("kits");
        for (Map.Entry<String, ItemStack[]> entry : playerKits.entrySet()) {
            kitsSection.set(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}