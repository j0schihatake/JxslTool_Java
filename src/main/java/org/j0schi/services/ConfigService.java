package org.j0schi.services;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ConfigService {
    private static final String CONFIG_FILE = ".jox-config.properties";
    private final Properties props = new Properties();

    public ConfigService() {
        loadConfig();
    }

    public void saveCommandConfig(String commandName, Map<String, String> params) {
        // Сохраняем с префиксом команды
        params.forEach((key, value) ->
                props.setProperty(commandName + "." + key, value));
        saveConfig();
    }

    public Map<String, String> loadCommandConfig(String commandName) {
        Map<String, String> config = new HashMap<>();

        props.stringPropertyNames().stream()
                .filter(name -> name.startsWith(commandName + "."))
                .forEach(name ->
                        config.put(name.substring(commandName.length() + 1),
                                props.getProperty(name)));

        return config;
    }

    private void loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                props.load(input);
            } catch (IOException e) {
                System.err.println("Error loading config: " + e.getMessage());
            }
        }
    }

    private void saveConfig() {
        try (OutputStream output = Files.newOutputStream(Paths.get(CONFIG_FILE))) {
            props.store(output, "JOX Configuration");
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }
}