package org.j0schi.cod.subcommand;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.j0schi.commands.BaseCommand;
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

@Command(name = "update", description = "Update COD files from AF")
public class CodUpdateCommand extends BaseCommand {

    @Option(names = {"--target-git-path"},
            description = "Path to AF Git repository (where to pull changes from)",
            required = true)
    private String targetGitPath;

    @Option(names = {"--target-path"},
            description = "Path to AF files directory (source for copying)",
            required = true)
    private String targetPath;

    @Option(names = {"--cod-git-path"},
            description = "Path to COD Git repository (where to commit changes)",
            required = true)
    private String codGitPath;

    @Option(names = {"--cod-path"},
            description = "Path to COD files directory (destination for copying)",
            required = true)
    private String codPath;

    @Option(names = {"--migration-file"},
            description = "Path to migration file to update")
    private String migrationFilePath;

    @Override
    public Integer call() {
        try {
            Map<String, String> savedConfig = loadConfig("codUpdate");
            applyConfig(savedConfig);
            validatePaths();

            System.out.println("Starting COD update process...");

            // 1. Синхронизация AF репозитория
            if (!syncWithRemote("AF repository", targetGitPath)) {
                throw new Exception("Failed to sync AF repository");
            }

            // 2. Копирование файлов
            copyFilesWithReplace("AF files", targetPath, "COD directory", codPath);

            // 3. Обновление миграционного файла
            if (migrationFilePath != null) {
                updateMigrationFile();
            }

            // 4. Операции перед коммитом в COD
            Path codRepo = Paths.get(codGitPath);
            try (Git git = Git.open(codRepo.toFile())) {
                // Push в upstream (если есть)
                if (git.getRepository().getRemoteNames().contains("upstream")) {
                    if (!pushToUpstream(codGitPath)) {
                        System.err.println("WARNING: Failed to push to upstream");
                    }
                }

                // Pull из origin (если есть)
                if (git.getRepository().getRemoteNames().contains("origin")) {
                    if (!pullFromOrigin(codGitPath)) {
                        System.err.println("WARNING: Failed to pull from origin");
                    }
                }
            }

            // 5. Фиксация изменений в COD
            if (!commitAndPush("COD repository", codGitPath, "Automatic update from AF")) {
                throw new Exception("Failed to commit COD changes");
            }

            // 6. Сохранение конфигурации
            saveConfig("codUpdate", createConfigMap());

            System.out.println("COD update completed successfully!");
            return 0;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }

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

    private void pullLatestChanges(String repoName, String repoPath) throws GitAPIException, IOException {
        System.out.printf("Pulling latest changes from %s (%s)...%n", repoName, repoPath);
        try (Git git = Git.open(Paths.get(repoPath).toFile())) {
            PullResult pullResult = git.pull().setRebase(true).call();
            System.out.println("Pull result: " + pullResult);
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

        copyDirectory(sourceDir, targetDir);
        System.out.println("All files copied successfully");
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

    private void commitAndPushChanges() throws GitAPIException, IOException {
        System.out.println("Preparing to commit changes to COD repository...");

        try (Git git = Git.open(Paths.get(codGitPath).toFile())) {
            // Pull changes before commit
            System.out.println("Pulling latest changes from COD repository...");
            git.pull().setRebase(true).call();

            // Add all changes
            System.out.println("Adding files to commit...");
            git.add().addFilepattern(".").call();

            // Commit changes
            String commitMessage = "Перенос последних изменений xslt из AF.";
            System.out.println("Creating commit: " + commitMessage);
            git.commit().setMessage(commitMessage).call();

            // Push changes
            System.out.println("Pushing changes to remote...");
            git.push().call();

            System.out.println("Changes successfully committed and pushed");
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