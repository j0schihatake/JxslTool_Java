package org.j0schi.commands;

import org.j0schi.services.XsltUpdater;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "update-xslt",
        description = "Update XSLT files from source directory"
)
public class UpdateXsltCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-s", "--source"},
            description = "Source directory with updated XSLT files",
            required = true)
    private String sourceDir;

    @CommandLine.Option(
            names = {"-t", "--target"},
            description = "Target directory to update",
            required = true)
    private String targetDir;

    @CommandLine.Option(
            names = {"-f", "--filter"},
            description = "File filter (e.g. *.xslt)",
            defaultValue = "*.xslt")
    private String fileFilter;

    @Override
    public Integer call() throws IOException {
        new XsltUpdater(sourceDir, targetDir, fileFilter).update();
        return 0;
    }
}