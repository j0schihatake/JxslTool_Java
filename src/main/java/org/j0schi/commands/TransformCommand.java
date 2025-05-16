package org.j0schi.commands;


import org.j0schi.services.XsltService;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import javax.xml.transform.TransformerException;
import java.util.Map;

@CommandLine.Command(name = "transform", description = "Transform XML using XSLT")
public class TransformCommand extends BaseCommand {

    @Option(names = {"-x", "--xslt"}, description = "XSLT file path")
    private String xsltPath;

    @Option(names = {"-i", "--input"}, description = "Input XML file path")
    private String xmlPath;

    @Override
    public Integer call() {
        Map<String, String> config = loadConfig("transform");

        if (xsltPath == null) xsltPath = config.get("xslt");
        if (xmlPath == null) xmlPath = config.get("xml");

        if (xsltPath == null || xmlPath == null) {
            System.err.println("Required parameters: --xslt and --input");
            return 1;
        }

        try {
            String result = new XsltService().transform(xmlPath, xsltPath);
            System.out.println(result);

            saveConfig("transform", Map.of(
                    "xslt", xsltPath,
                    "xml", xmlPath
            ));
            return 0;
        } catch (TransformerException e) {
            System.err.println("Transform error: " + e.getMessage());
            return 2;
        }
    }
}