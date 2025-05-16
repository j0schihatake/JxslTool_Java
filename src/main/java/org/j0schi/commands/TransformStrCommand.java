package org.j0schi.commands;

import org.j0schi.xslt.XsltService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.nio.file.Files;
import java.nio.file.Paths;

@Command(name = "transform-str", description = "Transform XML string using XSLT")
public class TransformStrCommand implements Runnable {

    @Parameters(index = "0", description = "Path to XSLT file")
    private String xsltPath;

    @Parameters(index = "1", description = "XML text", arity = "1..*")
    private String xmlText;

    @Override
    public void run() {
        try {
            String xsltContent = Files.readString(Paths.get(xsltPath));
            String result = new XsltService().transformFromString(xmlText, xsltContent);
            System.out.println(result);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}