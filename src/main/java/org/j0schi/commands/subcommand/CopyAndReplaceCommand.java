package org.j0schi.commands.subcommand;

import org.j0schi.commands.BaseCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@Command(name = "copyAndReplace", description = "Copy files between directories with replacement")
public class CopyAndReplaceCommand extends BaseCommand {

    @Option(names = {"-p", "--path"}, description = "Base directory path", required = true)
    private String basePath;

    @Option(names = {"-t", "--target"}, description = "Target directory path", required = true)
    private String targetPath;

    @Option(names = {"-rev", "--reverse"}, description = "Reverse copy direction (target -> base)")
    private boolean reverse;

    @Override
    public Integer call() {
        Map<String, String> config = loadConfig("copyAndReplace");

        if (basePath == null) basePath = config.get("basePath");
        if (targetPath == null) targetPath = config.get("targetPath");

        if (basePath == null || targetPath == null) {
            System.err.println("Required parameters: --path and --target");
            return 1;
        }

        try {
            Path sourceDir = reverse ? Paths.get(targetPath) : Paths.get(basePath);
            Path destinationDir = reverse ? Paths.get(basePath) : Paths.get(targetPath);

            copyFilesWithReplace(sourceDir, destinationDir);

            String direction = reverse ? "from target to base" : "from base to target";
            System.out.printf("Files copied successfully %s%n", direction);

            saveConfig("copyAndReplace", Map.of(
                    "basePath", basePath,
                    "targetPath", targetPath
            ));
            return 0;
        } catch (IOException e) {
            System.err.println("Error copying files: " + e.getMessage());
            return 2;
        }
    }

    private void copyFilesWithReplace(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(sourceDir)) {
            throw new IOException("Source directory does not exist: " + sourceDir);
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path sourceFile : stream) {
                if (Files.isRegularFile(sourceFile)) {
                    Path targetFile = targetDir.resolve(sourceFile.getFileName());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}