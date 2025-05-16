package org.j0schi.services;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class XsltUpdater {
    private final Path source;
    private final Path target;
    private final String filter;

    public XsltUpdater(String sourceDir, String targetDir, String filter) {
        this.source = Paths.get(sourceDir);
        this.target = Paths.get(targetDir);
        this.filter = filter;
    }

    public void update() throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                if (file.getFileName().toString().matches(convertGlobToRegex(filter))) {
                    Path relative = source.relativize(file);
                    Path destination = target.resolve(relative);

                    if (Files.exists(destination)) {
                        Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Updated: " + relative);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String convertGlobToRegex(String glob) {
        return glob.replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
    }
}