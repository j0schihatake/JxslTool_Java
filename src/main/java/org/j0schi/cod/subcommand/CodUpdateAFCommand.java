package org.j0schi.cod.subcommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.j0schi.commands.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

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

    @Override
    public Integer call() {
        try {
            // Загрузка сохраненных настроек
            Map<String, String> savedConfig = loadConfig("codUpdateAF");
            applyConfig(savedConfig);

            // Validate paths
            validatePaths();

            System.out.println("Starting AF update process...");

            // 1. Pull latest changes from COD Git repository
            if (codPathGit != null) {
                pullLatestChanges("COD repository", codPathGit);
            }

            // 2. Copy files from codPath to targetPath
            if (codPath != null && targetPath != null) {
                copyFilesWithReplace("COD directory", codPath, "AF directory", targetPath);
                System.out.println("Files copied successfully from COD to AF");
            }

            // 3. Execute migration (empty for now)
            updateMigration();

            // 4. Commit changes to target repository
            if (targetGitPath != null) {
                commitChanges(targetGitPath, "Перенос последних изменений xslt из COD.");
            }

            // Save successful configuration
            saveConfig("codUpdateAF", createConfigMap());

            System.out.println("AF update completed successfully!");
            return 0;
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.err.println("AF update failed!");
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

    private void updateMigration() {
        // TODO: Implement migration logic later
        System.out.println("Migration update skipped (not implemented)");
    }

    private void commitChanges(String repoPath, String message) throws GitAPIException, IOException {
        System.out.println("Preparing to commit changes to AF repository...");

        try (Git git = Git.open(Paths.get(repoPath).toFile())) {
            // Pull changes before commit
            System.out.println("Pulling latest changes from AF repository...");
            git.pull().setRebase(true).call();

            // Add all changes
            System.out.println("Adding files to commit...");
            git.add().addFilepattern(".").call();

            // Commit changes
            System.out.println("Creating commit: " + message);
            git.commit().setMessage(message).call();

            // Push changes
            System.out.println("Pushing changes to remote...");
            git.push().call();

            System.out.println("Changes successfully committed and pushed");
        }
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
