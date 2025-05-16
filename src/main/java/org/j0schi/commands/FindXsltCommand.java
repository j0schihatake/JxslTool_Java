package org.j0schi.commands;

import org.j0schi.services.XsltService;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "find-xslt", description = "Find matching XSLT")
public class FindXsltCommand extends BaseCommand {

    @Option(names = {"-d", "--dir"}, description = "XSLT directory")
    private String xsltDir;

    @Option(names = {"-i", "--input"}, description = "Input XML file")
    private String xmlPath;

    @Override
    public Integer call() {
        Map<String, String> config = loadConfig("find-xslt");

        if (xsltDir == null) xsltDir = config.get("dir");
        if (xmlPath == null) xmlPath = config.get("input");

        if (xsltDir == null || xmlPath == null) {
            System.err.println("Required parameters: --dir and --input");
            return 1;
        }

        try {
            List<String> matches = new XsltService().findMatchingXslt(xmlPath, xsltDir);
            matches.forEach(System.out::println);

            saveConfig("find-xslt", Map.of(
                    "dir", xsltDir,
                    "input", xmlPath
            ));
            return 0;
        } catch (Exception e) {
            System.err.println("Find error: " + e.getMessage());
            return 2;
        }
    }
}