package me.pulsi_.bankplus.bankSystem;

import me.clip.placeholderapi.PlaceholderAPI;
import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.account.BankPlusPlayer;
import me.pulsi_.bankplus.utils.BPChat;
import me.pulsi_.bankplus.utils.BPItems;
import me.pulsi_.bankplus.utils.BPLogger;
import me.pulsi_.bankplus.utils.BPMethods;
import me.pulsi_.bankplus.values.Values;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BankListGui {

    public static final String multipleBanksGuiID = "MultipleBanksGui";
    private static final Material DEFAULT_MATERIAL = Material.CHEST;

    public static void openMultipleBanksGui(Player p) {
        BankPlusPlayer player = BankPlus.INSTANCE.getPlayerRegistry().get(p);

        Bank openedBank = player.getOpenedBank();
        if (openedBank != null) {
            BukkitTask task = openedBank.getInventoryUpdateTask();
            if (task != null) task.cancel();
        }

        if (Values.MULTIPLE_BANKS.isDirectlyOpenIf1IsAvailable()) {
            BankReader reader = new BankReader();

            if (reader.getAvailableBanks(p).size() == 1) {
                BankUtils.openBank(p, reader.getAvailableBanks(p).get(0), false);
                return;
            }
        }

        Bank baseBanksListGui = BankPlus.INSTANCE.getBankGuiRegistry().bankListGui;

        String title = baseBanksListGui.getTitle();
        if (!BankPlus.INSTANCE.isPlaceholderAPIHooked()) title = BPChat.color(title);
        else title = PlaceholderAPI.setPlaceholders(p, BPChat.color(title));

        Inventory banksListGui = Bukkit.createInventory(new BankHolder(), baseBanksListGui.getSize(), title);
        banksListGui.setContents(baseBanksListGui.getContent());
        placeBanks(banksListGui, p);
        updateMeta(banksListGui, p);

        long delay = Values.MULTIPLE_BANKS.getUpdateDelay();
        if (delay >= 0)
            baseBanksListGui.setInventoryUpdateTask(Bukkit.getScheduler().runTaskTimer(BankPlus.INSTANCE, () -> updateMeta(banksListGui, p), delay, delay));

        player.setOpenedBank(baseBanksListGui);
        BPMethods.playSound("PERSONAL", p);
        p.openInventory(banksListGui);
    }

    private static void placeBanks(Inventory banksListGui, Player p) {
        BankPlusPlayer player = BankPlus.INSTANCE.getPlayerRegistry().get(p);
        HashMap<String, String> banksClickHolder = new HashMap<>();
        int slot = 0;

        for (String bankName : BankPlus.INSTANCE.getBankGuiRegistry().getBanks().keySet()) {
            BankReader reader = new BankReader(bankName);
            if (Values.MULTIPLE_BANKS.isShowNotAvailableBanks() && !reader.isAvailable(p)) continue;

            Material material;
            ItemStack bankItem;
            boolean glow = false;
            ConfigurationSection section = reader.getBanksGuiItemSection();

            if (section == null) material = DEFAULT_MATERIAL;
            else {
                String path = reader.isAvailable(p) ? "Available" : "Unavailable";
                glow = section.getBoolean(path + ".Glowing");
                try {
                    material = Material.valueOf(section.getString(path + ".Material"));
                } catch (IllegalArgumentException e) {
                    material = BPItems.UNKNOWN_MATERIAL;
                }
            }
            bankItem = new ItemStack(material);

            if (glow) glow(bankItem);
            banksListGui.setItem(slot, bankItem);
            banksClickHolder.put(p.getName() + "." + slot, bankName);
            slot++;
        }
        player.setPlayerBankClickHolder(banksClickHolder);
    }

    private static void updateMeta(Inventory banksList, Player p) {
        int slot = 0;
        for (String bankName : BankPlus.INSTANCE.getBankGuiRegistry().getBanks().keySet()) {
            BankReader reader = new BankReader(bankName);

            if (Values.MULTIPLE_BANKS.isShowNotAvailableBanks() && !reader.isAvailable(p)) continue;

            ItemStack item = banksList.getItem(slot);
            slot++;

            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            String displayname = "&c&l* DISPLAYNAME NOT FOUND *";
            List<String> lore = new ArrayList<>();

            ConfigurationSection section = reader.getBanksGuiItemSection();
            if (section != null) {
                String path = (!reader.hasPermissionSection() || p.hasPermission(reader.getPermission())) ? "Available" : "Unavailable";

                String dName = section.getString(path + ".Displayname");
                if (dName != null) displayname = dName;

                lore = section.getStringList(path + ".Lore");
                int modelData = section.getInt(path + ".CustomModelData");

                if (modelData > 0) {
                    try {
                        meta.setCustomModelData(modelData);
                    } catch (NoSuchMethodError e) {
                        BPLogger.warn("Cannot set custom model data to the item: \"" + displayname + "\"&e. Custom model data is only available on 1.14.4+ servers!");
                    }
                }
            }
            setMeta(displayname, lore, meta, p);
            item.setItemMeta(meta);
        }
    }

    private static void setMeta(String displayname, List<String> lore, ItemMeta meta, Player p) {
        List<String> newLore = new ArrayList<>();
        for (String lines : lore) newLore.add(BPChat.color(lines));

        if (BankPlus.INSTANCE.isPlaceholderAPIHooked()) {
            meta.setDisplayName(PlaceholderAPI.setPlaceholders(p, BPChat.color(displayname)));
            meta.setLore(PlaceholderAPI.setPlaceholders(p, newLore));
        } else {
            meta.setDisplayName(BPChat.color(displayname));
            meta.setLore(newLore);
        }
    }

    private static void glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }
}