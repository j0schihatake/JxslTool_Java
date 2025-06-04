package org.j0schi.commands;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

public abstract class BaseCommand {

    protected static final String MIGRATION_FILE_PATTERN =
            "<changeSet id=\"%s_modify_af_xslt\" author=\"Renue\">";

    private static final Preferences PREFS = Preferences.userNodeForPackage(BaseCommand.class);
    private static final Path CONFIG_FILE = Paths.get(System.getProperty("user.home"), ".j0schi", "config.properties");

    // Загрузка конфигурации из файла
    protected Map<String, String> loadConfig(String commandName) {
        Map<String, String> config = new HashMap<>();

        // Пробуем загрузить из файла
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
                Properties props = new Properties();
                props.load(input);

                // Фильтруем свойства по имени команды
                String prefix = commandName + ".";
                for (String key : props.stringPropertyNames()) {
                    if (key.startsWith(prefix)) {
                        config.put(key.substring(prefix.length()), props.getProperty(key));
                    }
                }
            } catch (IOException e) {
                System.err.println("Warning: Failed to load config file - " + e.getMessage());
            }
        }

        // Если в файле ничего не найдено, пробуем Preferences
        if (config.isEmpty()) {
            try {
                String savedOptions = PREFS.get(commandName, "");
                if (!savedOptions.isEmpty()) {
                    String[] pairs = savedOptions.split(";");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            config.put(keyValue[0], keyValue[1]);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to load saved options - " + e.getMessage());
            }
        }

        return config;
    }

    // Сохранение конфигурации в файл и Preferences
    protected void saveConfig(String commandName, Map<String, String> config) {
        // Сохранение в Preferences
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : config.entrySet()) {
                if (sb.length() > 0) sb.append(";");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            PREFS.put(commandName, sb.toString());
        } catch (Exception e) {
            System.err.println("Warning: Failed to save options - " + e.getMessage());
        }

        // Сохранение в файл
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            Properties props = new Properties();
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
                    props.load(input);
                }
            }

            // Добавляем свойства с префиксом команды
            String prefix = commandName + ".";
            for (Map.Entry<String, String> entry : config.entrySet()) {
                props.setProperty(prefix + entry.getKey(), entry.getValue());
            }

            try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
                props.store(output, "j0schi configuration");
            }
        } catch (IOException e) {
            System.err.println("Warning: Failed to save config file - " + e.getMessage());
        }
    }

    protected void validatePath(String pathName, String pathValue) throws IOException {
        if (pathValue == null || pathValue.isEmpty()) {
            throw new IOException("Path '" + pathName + "' is not specified");
        }

        Path path = Paths.get(pathValue);
        if (!Files.exists(path)) {
            throw new IOException("Path '" + pathName + "' does not exist: " + pathValue);
        }

        if (!Files.isReadable(path)) {
            throw new IOException("No read access to path '" + pathName + "': " + pathValue);
        }
    }

    protected void validateDirectory(String pathName, String pathValue) throws IOException {
        validatePath(pathName, pathValue);
        Path path = Paths.get(pathValue);
        if (!Files.isDirectory(path)) {
            throw new IOException("Path '" + pathName + "' is not a directory: " + pathValue);
        }
    }

    protected void validateFile(String pathName, String pathValue) throws IOException {
        validatePath(pathName, pathValue);
        Path path = Paths.get(pathValue);
        if (!Files.isRegularFile(path)) {
            throw new IOException("Path '" + pathName + "' is not a file: " + pathValue);
        }
    }

    // Рекурсивное копирование директорий
    protected void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    if (!Files.exists(dest)) {
                        Files.createDirectories(dest);
                    }
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public abstract Integer call();
}
