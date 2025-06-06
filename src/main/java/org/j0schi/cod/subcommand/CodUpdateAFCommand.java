package org.j0schi.cod.subcommand;

import org.j0schi.commands.BaseCommand;
import org.j0schi.services.GitService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Command(name = "updateAF", description = "Update AF files from COD")
public class CodUpdateAFCommand extends BaseCommand {

    @Option(names = {"-tgp", "--targetGitPath"}, description = "Target Git repository path")
    private String targetGitPath;

    @Option(names = {"-tp", "--targetPath"}, description = "Target files directory path")
    private String targetPath;

    @Option(names = {"-cpg", "--codPathGit"}, description = "COD Git repository path")
    private String codPathGit;

    @Option(names = {"-cp", "--codPath"}, description = "COD files directory path")
    private String codPath;

    // Новые опции
    @Option(names = {"--no-push"},
            description = "Skip pushing changes to remote repository",
            defaultValue = "false")
    private boolean noPush;

    @Option(names = {"--only-files", "--files-only", "-f"},
            description = "Copy only files (ignore subdirectories)",
            defaultValue = "false")
    private boolean onlyFiles;

    private final GitService gitService = new GitService();

    @Override
    public Integer call() {
        try {
            Map<String, String> savedConfig = loadConfig("codUpdateAF");
            applyConfig(savedConfig);
            validatePaths();

            System.out.println("Starting AF update process...");
            System.out.println("Options: noPush=" + noPush + ", onlyFiles=" + onlyFiles);

            // 1. Синхронизация COD репозитория
            if (codPathGit != null) {
                System.out.println("Syncing COD repository...");
                if (!gitService.pullRepository(codPathGit)) {
                    System.err.println("WARNING: Failed to sync COD repository");
                }
            }

            // 2. Копирование файлов
            if (codPath != null && targetPath != null) {
                copyFilesWithReplace("COD directory", codPath, "AF directory", targetPath);
            }

            // 3. Обновление миграций
            updateMigration();

            // 4. Работа с AF репозиторием
            if (targetGitPath != null) {
                // Синхронизируем перед коммитом
                System.out.println("Syncing AF repository...");
                if (!gitService.pullRepository(targetGitPath)) {
                    System.err.println("WARNING: Failed to sync AF repository");
                }

                // Проверяем есть ли изменения
                boolean hasChanges = gitService.hasChanges(targetGitPath);

                if (hasChanges) {
                    if (noPush) {
                        // Только коммит
                        System.out.println("Committing changes to AF repository (no push)...");
                        if (!gitService.commit(targetGitPath, "Automatic update from COD")) {
                            throw new Exception("Failed to commit AF changes");
                        }
                    } else {
                        // Коммит и пуш
                        System.out.println("Committing and pushing changes to AF repository...");
                        if (!gitService.commitAndPush(targetGitPath, "Automatic update from COD")) {
                            throw new Exception("Failed to commit and push AF changes");
                        }
                    }
                } else {
                    System.out.println("No changes detected in AF repository");
                }
            }

            // 5. Сохранение конфигурации
            saveConfig("codUpdateAF", createConfigMap());

            System.out.println("AF update completed successfully!");
            return 0;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private void applyConfig(Map<String, String> config) {
        if (targetGitPath == null) targetGitPath = config.get("targetGitPath");
        if (targetPath == null) targetPath = config.get("targetPath");
        if (codPathGit == null) codPathGit = config.get("codPathGit");
        if (codPath == null) codPath = config.get("codPath");
    }

    private void validatePaths() throws IOException {
        System.out.println("Validating paths...");
        if (codPathGit != null) {
            validateDirectory("COD Git repository", codPathGit);
        }
        if (codPath != null) {
            validateDirectory("COD files directory", codPath);
        }
        if (targetGitPath != null) {
            validateDirectory("AF Git repository", targetGitPath);
        }
        if (targetPath != null) {
            validateDirectory("AF files directory", targetPath);
        }
    }

    private void copyFilesWithReplace(String sourceName, String sourcePath,
                                      String destName, String destPath) throws IOException {
        System.out.printf("Copying files from %s (%s) to %s (%s) [mode: %s]...%n",
                sourceName, sourcePath, destName, destPath,
                onlyFiles ? "FILES ONLY" : "FULL DIRECTORY");

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
                            System.out.println("  Copied file: " + source.getFileName());
                        } catch (IOException e) {
                            System.err.println("  ERROR copying file: " + source.getFileName());
                            System.err.println("    Reason: " + e.getMessage());
                        }
                    });
        }
    }

    private void updateMigration() {
        // TODO: Implement migration logic later
        System.out.println("Migration update skipped (not implemented)");
    }

    private Map<String, String> createConfigMap() {
        Map<String, String> config = new HashMap<>();
        config.put("targetGitPath", targetGitPath != null ? targetGitPath : "");
        config.put("targetPath", targetPath != null ? targetPath : "");
        config.put("codPathGit", codPathGit != null ? codPathGit : "");
        config.put("codPath", codPath != null ? codPath : "");
        return config;
    }
}