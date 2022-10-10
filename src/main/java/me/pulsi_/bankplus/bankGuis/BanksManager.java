package me.pulsi_.bankplus.bankGuis;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.account.BankPlusPlayerFiles;
import me.pulsi_.bankplus.account.economy.MultiEconomyManager;
import me.pulsi_.bankplus.account.economy.SingleEconomyManager;
import me.pulsi_.bankplus.utils.BPChat;
import me.pulsi_.bankplus.utils.BPItems;
import me.pulsi_.bankplus.utils.BPLogger;
import me.pulsi_.bankplus.utils.BPMessages;
import me.pulsi_.bankplus.values.Values;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class BanksManager {
    
    private final BankGui bank;

    public BanksManager() {
        this.bank = null;
    }
    
    public BanksManager(String bankName) {
        this.bank = BankPlus.INSTANCE.getBankGuiRegistry().get(bankName);
    }

    public boolean exist(String bankName) {
        return BankPlus.INSTANCE.getBankGuiRegistry().contains(bankName);
    }

    public File getFile() {
        return bank.getBankFile();
    }

    public FileConfiguration getConfig() {
        return bank.getBankConfig();
    }

    public String getTitle() {
        return bank.getTitle();
    }

    public int getLines() {
        return bank.getSize();
    }

    public long getUpdateDelay() {
        return bank.getUpdateDelay();
    }

    public boolean isFillerEnabled() {
        return bank.hasFiller();
    }

    public String getFillerMaterial() {
        return bank.getFillerMaterial();
    }

    public boolean isFillerGlowing() {
        return bank.isFillerGlowing();
    }

    public ConfigurationSection getItems() {
        return bank.getItems();
    }

    public boolean hasSettings() {
        return bank.getSettings() != null;
    }

    public boolean hasPermission() {
        return bank.getPermission() != null;
    }

    public String getPermission() {
        return bank.getPermission();
    }

    public ConfigurationSection getBanksGuiItemSection() {
        return bank.getBanksListGuiItems();
    }

    public boolean hasUpgrades() {
        return bank.getUpgrades() != null;
    }

    public ConfigurationSection getUpgrades() {
        return bank.getUpgrades();
    }

    public BigDecimal getCapacity(Player p) {
        if (!hasUpgrades()) return Values.CONFIG.getMaxBankCapacity();

        FileConfiguration config = new BankPlusPlayerFiles(p).getPlayerConfig();
        int level = Math.max(config.getInt("Banks." + bank.getIdentifier() + ".Level"), 1);
        String capacity = getUpgrades().getString(level + ".Capacity");
        return new BigDecimal(capacity == null ? Values.CONFIG.getMaxBankCapacity().toString() : capacity);
    }

    public BigDecimal getCapacity(OfflinePlayer p) {
        if (!hasUpgrades()) return Values.CONFIG.getMaxBankCapacity();

        FileConfiguration config = new BankPlusPlayerFiles(p).getPlayerConfig();
        int level = Math.max(config.getInt("Banks." + bank.getIdentifier() + ".Level"), 1);
        String capacity = getUpgrades().getString(level + ".Capacity");
        return new BigDecimal(capacity == null ? Values.CONFIG.getMaxBankCapacity().toString() : capacity);
    }

    public BigDecimal getLevelCost(int level) {
        if (!hasUpgrades()) return new BigDecimal(0);

        String cost = getUpgrades().getString(level + ".Cost");
        return new BigDecimal(cost == null ? "0" : cost);
    }

    public int getLevel(Player p) {
        FileConfiguration config = new BankPlusPlayerFiles(p).getPlayerConfig();
        return Math.max(config.getInt("Banks." + bank.getIdentifier() + ".Level"), 1);
    }

    public List<String> getLevels() {
        List<String> levels = new ArrayList<>();
        if (!hasUpgrades()) {
            levels.add("1");
            return levels;
        }

        levels.addAll(getUpgrades().getKeys(false));
        return levels;
    }

    public boolean hasNextLevel(Player p) {
        ConfigurationSection section = getUpgrades();
        if (section == null) return false;

        return section.getConfigurationSection(String.valueOf(getLevel(p) + 1)) != null;
    }

    public boolean hasNextLevel(int currentLevel) {
        ConfigurationSection section = getUpgrades();
        if (section == null) return false;

        return section.getConfigurationSection(String.valueOf(currentLevel + 1)) != null;
    }

    public List<String> getAvailableBanks(Player p) {
        List<String> availableBanks = new ArrayList<>();
        for (String bankName : BankPlus.INSTANCE.getBankGuiRegistry().getBanks().keySet()) if (isAvailable(p)) availableBanks.add(bankName);
        return availableBanks;
    }

    public List<String> getAvailableBanks(OfflinePlayer p) {
        List<String> availableBanks = new ArrayList<>();
        for (String bankName : BankPlus.INSTANCE.getBankGuiRegistry().getBanks().keySet()) if (isAvailable(p)) availableBanks.add(bankName);
        return availableBanks;
    }

    public boolean isAvailable(Player p) {
        if (!hasPermission()) return true;
        else return p.hasPermission(getPermission());
    }

    public boolean isAvailable(OfflinePlayer p) {
        if (!hasPermission()) return true;
        else {
            String wName = Bukkit.getWorlds().get(0).getName();
            return BankPlus.INSTANCE.getPermissions().playerHas(wName, p, getPermission());
        }
    }

    public void upgradeBank(Player p) {
        if (!hasNextLevel(p)) {
            BPMessages.send(p, "Bank-Max-Level");
            return;
        }
        BigDecimal balance;

        SingleEconomyManager singleEconomyManager = new SingleEconomyManager(p);
        MultiEconomyManager multiEconomyManager = new MultiEconomyManager(p);

        if (Values.MULTIPLE_BANKS.isMultipleBanksModuleEnabled()) balance = multiEconomyManager.getBankBalance();
        else balance = singleEconomyManager.getBankBalance();

        int level = getLevel(p);
        BigDecimal cost = getLevelCost(level + 1);
        if (balance.doubleValue() < cost.doubleValue()) {
            BPMessages.send(p, "Insufficient-Money");
            return;
        }

        if (Values.MULTIPLE_BANKS.isMultipleBanksModuleEnabled()) multiEconomyManager.removeBankBalance(cost, bank.getIdentifier());
        else singleEconomyManager.removeBankBalance(cost);

        BankPlusPlayerFiles files = new BankPlusPlayerFiles(p);
        files.getPlayerConfig().set("Banks." + bank.getIdentifier() + ".Level", level + 1);
        files.savePlayerFile(true);

        BPMessages.send(p, "Bank-Upgraded");
    }

    public BankGui getBank() {
        return bank;
    }
}