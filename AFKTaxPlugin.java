
package com.example.afktax;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class AFKTaxPlugin extends JavaPlugin {

    private Economy economy;
    private final Map<Player, Location> lastLocations = new HashMap<>();
    private double baseTaxRate = 0.1; // 10% Startwert
    private int timeElapsed = 0; // Zeit in Minuten, um Steuererhöhung zu berechnen

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("AFKTaxPlugin aktiviert.");
        startTaxTask();
    }

    @Override
    public void onDisable() {
        getLogger().info("AFKTaxPlugin deaktiviert.");
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void startTaxTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            timeElapsed += 30; // 30 Minuten vergangen
            double currentTaxRate = calculateTaxRate();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isAFK(player)) {
                    double balance = economy.getBalance(player);
                    double tax = balance * currentTaxRate; // Aktueller Steuersatz
                    economy.withdrawPlayer(player, tax);

                    player.sendMessage(ChatColor.RED + String.format(
                            "Du hast %.2f%% deines Guthabens als Steuer bezahlt (AFK-Steuersatz: %.2f%%).",
                            currentTaxRate * 100, currentTaxRate * 100));
                }
                // Aktuelle Position speichern
                lastLocations.put(player, player.getLocation());
            }
        }, 0L, 36000L); // Alle 30 Minuten (36000 Ticks)
    }

    private double calculateTaxRate() {
        int hoursPassed = timeElapsed / 60;
        return baseTaxRate * Math.pow(1.02, hoursPassed); // Exponentielle Erhöhung um 2% pro Stunde
    }

    private boolean isAFK(Player player) {
        Location lastLocation = lastLocations.get(player);
        if (lastLocation == null) {
            return false; // Keine vorherige Position bekannt
        }
        return lastLocation.equals(player.getLocation());
    }
}
