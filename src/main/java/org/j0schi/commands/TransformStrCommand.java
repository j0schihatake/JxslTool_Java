package org.j0schi.commands;

import org.j0schi.services.XsltService;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@CommandLine.Command(name = "transform-str", description = "Transform XML string")
public class TransformStrCommand extends BaseCommand {

    @Option(names = {"-x", "--xslt"}, description = "XSLT file path")
    private String xsltPath;

    @Option(names = {"-t", "--text"}, description = "XML text")
    private String xmlText;

    @Override
    public Integer call() {
        Map<String, String> config = loadConfig("transform-str");

        if (xsltPath == null) xsltPath = config.get("xslt");
        if (xmlText == null) xmlText = config.get("text");

        if (xsltPath == null || xmlText == null) {
            System.err.println("Required parameters: --xslt and --text");
            return 1;
        }

        try {
            String xsltContent = Files.readString(Paths.get(xsltPath));
            String result = new XsltService().transformFromString(xmlText, xsltContent);
            System.out.println(result);

            saveConfig("transform-str", Map.of(
                    "xslt", xsltPath,
                    "text", xmlText
            ));
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 2;
        }
    }
}