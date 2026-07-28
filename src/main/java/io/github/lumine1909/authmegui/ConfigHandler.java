package io.github.lumine1909.authmegui;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigHandler {

    private final File dataConfigFile;
    private final FileConfiguration dataConfig;

    public ConfigHandler(JavaPlugin plugin) {
        try {
            dataConfigFile = new File(plugin.getDataFolder(), "data.yml");
            if (!dataConfigFile.exists()) {
                dataConfigFile.getParentFile().mkdirs();
                dataConfigFile.createNewFile();
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataConfigFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getEnabledPlayers() {
        return (Set<String>) dataConfig.getList("enabled", Collections.emptyList()).stream().collect(Collectors.toSet());
    }

    public void saveEnabledPlayers(Set<String> players) {
        dataConfig.set("enabled", players.stream().toList());
        try {
            dataConfig.save(dataConfigFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
