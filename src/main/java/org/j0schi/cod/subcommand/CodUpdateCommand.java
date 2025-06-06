package org.j0schi.cod.subcommand;

import org.j0schi.commands.BaseCommand;
import org.j0schi.services.GitService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Stream;

@Command(name = "update", description = "Update COD files from AF")
public class CodUpdateCommand extends BaseCommand {

    @Option(names = {"-tgp", "--target-git-path"}, required = true,
            description = "Path to AF Git repository")
    private String targetGitPath;

    @Option(names = {"-tp", "--target-path"}, required = true,
            description = "Path to AF files directory")
    private String targetPath;

    @Option(names = {"-cgp","--cod-git-path"}, required = true,
            description = "Path to COD Git repository")
    private String codGitPath;

    @Option(names = {"-cp","--cod-path"}, required = true,
            description = "Path to COD files directory")
    private String codPath;

    // Опциональные параметры
    @Option(names = "--migration-file",
            description = "Path to migration file")
    private String migrationFilePath;

    @Option(names = "--no-push", defaultValue = "false",
            description = "Skip pushing changes")
    private boolean noPush;

    @Option(names = {"--only-files"},
            defaultValue = "false",
            description = "Copy only files (no directories)")
    private boolean onlyFiles;

    private final GitService gitService = new GitService();

    @Override
    public Integer call() {
        try {
            Map<String, String> savedConfig = loadConfig("codUpdate");
            applyConfig(savedConfig);
            validatePaths();

            System.out.println("Starting COD update process...");

            // 1. Синхронизация AF репозитория
            if (!gitService.pullRepository(targetGitPath)) {
                throw new Exception("Failed to sync AF repository");
            }

            // 2. Копирование файлов
            copyFilesWithReplace("AF files", targetPath, "COD directory", codPath);

            // 3. Обновление миграционного файла
            if (migrationFilePath != null) {
                updateMigrationFile();
            }

            // 4. Проверяем есть ли изменения
            boolean hasChanges = gitService.hasChanges(codGitPath);

            if (!hasChanges) {
                System.out.println("No changes detected, skipping commit and push");
                return 0;
            }

            // 5. Операции перед коммитом в COD
            if (gitService.hasRemotes(codGitPath)) {
                if (!gitService.pullRepository(codGitPath)) {
                    System.err.println("WARNING: Failed to pull from origin");
                }
            }

            // 6. Фиксация изменений в COD
            String relativePath = gitService.getRelativeCodPath(codGitPath, codPath);
            if (!noPush) {
                if (!gitService.commitAndPush(relativePath, "Automatic update from AF")) {
                    throw new Exception("Failed to commit and push COD changes");
                }
            } else {
                if (!gitService.commit(relativePath, "Automatic update from AF")) {
                    throw new Exception("Failed to commit COD changes");
                }
                System.out.println("Skipping push to remote (--no-push flag set)");
            }

            // 7. Сохранение конфигурации
            saveConfig("codUpdate", createConfigMap());

            System.out.println("COD update completed successfully!");
            return 0;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }


    // Остальные методы остаются без изменений
    private void applyConfig(Map<String, String> config) {
        if (targetGitPath == null) targetGitPath = config.get("targetGitPath");
        if (targetPath == null) targetPath = config.get("targetPath");
        if (codGitPath == null) codGitPath = config.get("codGitPath");
        if (codPath == null) codPath = config.get("codPath");
        if (migrationFilePath == null) migrationFilePath = config.get("migrationFilePath");
    }

    private void validatePaths() throws IOException {
        System.out.println("Validating paths...");
        validateDirectory("AF Git repository", targetGitPath);
        validateDirectory("AF files directory", targetPath);
        validateDirectory("COD Git repository", codGitPath);
        validateDirectory("COD files directory", codPath);

        if (migrationFilePath != null) {
            validateFile("Migration file", migrationFilePath);
        }
    }

    private void copyFilesWithReplace(String sourceName, String sourcePath,
                                      String destName, String destPath) throws IOException {
        System.out.printf("Copying files from %s (%s) to %s (%s)...%n",
                sourceName, sourcePath, destName, destPath);

        Path sourceDir = Paths.get(sourcePath);
        Path targetDir = Paths.get(destPath);

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        if (onlyFiles) {
            copyOnlyFiles(sourceDir, targetDir);
        } else {
            copyDirectory(sourceDir, targetDir);
        }
        System.out.println("All files copied successfully");
    }

    private void copyOnlyFiles(Path sourceDir, Path targetDir) throws IOException {
        System.out.println("Copying only files (ignoring subdirectories)");

        try (Stream<Path> stream = Files.list(sourceDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(source -> {
                        try {
                            Path target = targetDir.resolve(source.getFileName());
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Copied file: " + source.getFileName());
                        } catch (IOException e) {
                            System.err.println("Failed to copy file: " + source.getFileName());
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void updateMigrationFile() throws IOException {
        System.out.println("Updating migration file: " + migrationFilePath);

        Path migrationFile = Paths.get(migrationFilePath);
        String content = new String(Files.readAllBytes(migrationFile), StandardCharsets.UTF_8);

        Pattern pattern = Pattern.compile("<changeSet id=\"(\\d+)_modify_af_xslt\"");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String oldId = matcher.group(1);
            String newId = incrementMigrationId(oldId);

            content = content.replaceFirst(oldId + "_modify_af_xslt",
                    newId + "_modify_af_xslt");

            Files.write(migrationFile, content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Updated migration ID from " + oldId + " to " + newId);
        } else {
            String newId = generateNewMigrationId();
            String newEntry = String.format(MIGRATION_FILE_PATTERN, newId) + "\n";

            Files.write(migrationFile, newEntry.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
            System.out.println("Added new migration entry with ID: " + newId);
        }
    }

    private Map<String, String> createConfigMap() {
        Map<String, String> config = new HashMap<>();
        config.put("targetGitPath", targetGitPath);
        config.put("targetPath", targetPath);
        config.put("codGitPath", codGitPath);
        config.put("codPath", codPath);
        config.put("migrationFilePath", migrationFilePath != null ? migrationFilePath : "");
        return config;
    }

    private String incrementMigrationId(String oldId) {
        try {
            long num = Long.parseLong(oldId);
            return String.valueOf(num + 1);
        } catch (NumberFormatException e) {
            return generateNewMigrationId();
        }
    }

    private String generateNewMigrationId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }
}