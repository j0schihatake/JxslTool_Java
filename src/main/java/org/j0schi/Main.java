package org.j0schi;

import org.j0schi.xslt.XsltService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import javax.xml.transform.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "jox", mixinStandardHelpOptions = true, version = "1.0",
        description = "XSLT Transformer CLI Tool")
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                .addSubcommand(new TransformCommand())
                .addSubcommand(new TransformStrCommand())
                .addSubcommand(new FindXsltCommand())
                .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("Используйте: jox <command> [options]");
        new CommandLine(this).usage(System.out);
        return 0;
    }
}

// Команда: jox transform <xslt> <xml>
@Command(name = "transform", description = "Transform XML file using XSLT")
class TransformCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to XSLT file")
    private String xsltPath;

    @Parameters(index = "1", description = "Path to XML file")
    private String xmlPath;

    @Override
    public Integer call() throws TransformerException {
        String result = new XsltService().transform(xmlPath, xsltPath);
        System.out.println(result);
        return 0;
    }
}

// Команда: jox transform-str <xslt> "<xml-text>"
@Command(name = "transform-str", description = "Transform XML string using XSLT")
class TransformStrCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to XSLT file")
    private String xsltPath;

    @Parameters(index = "1", description = "XML text", arity = "1..*")
    private String xmlText;

    @Override
    public Integer call() throws Exception {
        String xsltContent = Files.readString(Paths.get(xsltPath));
        String result = new XsltService().transformFromString(xmlText, xsltContent);
        System.out.println(result);
        return 0;
    }
}

// Команда: jox find-xslt <xslt-dir> <xml>
@Command(name = "find-xslt", description = "Find matching XSLT for XML")
class FindXsltCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Directory with XSLT files")
    private String xsltDir;

    @Parameters(index = "1", description = "Path to XML file")
    private String xmlPath;

    @Override
    public Integer call() throws Exception {
        List<String> matches = new XsltService().findMatchingXslt(xmlPath, xsltDir);
        matches.forEach(System.out::println);
        return 0;
    }
}