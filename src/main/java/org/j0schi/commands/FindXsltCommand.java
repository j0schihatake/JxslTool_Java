package org.j0schi.commands;

import org.j0schi.xslt.XsltService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import java.util.List;

@Command(name = "find-xslt", description = "Find matching XSLT for XML")
public class FindXsltCommand implements Runnable {

    @Parameters(index = "0", description = "Directory with XSLT files")
    private String xsltDir;

    @Parameters(index = "1", description = "Path to XML file")
    private String xmlPath;

    @Override
    public void run() {
        try {
            List<String> matches = new XsltService().findMatchingXslt(xmlPath, xsltDir);
            matches.forEach(System.out::println);
        } catch (Exception e) {
            System.err.println("Error finding XSLT: " + e.getMessage());
        }
    }
}
