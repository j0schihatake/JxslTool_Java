package org.j0schi.commands;

import org.j0schi.xslt.XsltService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import javax.xml.transform.TransformerException;

@Command(name = "transform", description = "Transform XML file using XSLT")
public class TransformCommand implements Runnable {

    @Parameters(index = "0", description = "Path to XSLT file")
    private String xsltPath;

    @Parameters(index = "1", description = "Path to XML file")
    private String xmlPath;

    @Override
    public void run() {
        try {
            String result = new XsltService().transform(xmlPath, xsltPath);
            System.out.println(result);
        } catch (TransformerException e) {
            System.err.println("Error during transformation: " + e.getMessage());
        }
    }
}