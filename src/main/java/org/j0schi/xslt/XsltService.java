package org.j0schi.xslt;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class XsltService {

    // 1. Трансформация из файлов
    public String transform(String xmlPath, String xsltPath) throws TransformerException {
        Source xmlSource = new StreamSource(xmlPath);
        Source xsltSource = new StreamSource(xsltPath);
        return transformInternal(xmlSource, xsltSource);
    }

    // 2. Трансформация из строк
    public String transformFromString(String xmlString, String xsltString) throws TransformerException {
        Source xmlSource = new StreamSource(new StringReader(xmlString));
        Source xsltSource = new StreamSource(new StringReader(xsltString));
        return transformInternal(xmlSource, xsltSource);
    }

    // 3. Поиск подходящего XSLT в директории
    public List<String> findMatchingXslt(String xmlPath, String xsltDir) throws IOException, TransformerException {
        Source xmlSource = new StreamSource(xmlPath);
        List<String> matchingXsltFiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(xsltDir))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".xsl") || path.toString().endsWith(".xslt"))
                    .forEach(path -> {
                        try {
                            Source xsltSource = new StreamSource(path.toFile());
                            TransformerFactory.newInstance().newTransformer(xsltSource).transform(xmlSource, new StreamResult(new StringWriter()));
                            matchingXsltFiles.add(path.toString()); // Если трансформация успешна
                        } catch (Exception ignored) {}
                    });
        }
        return matchingXsltFiles;
    }

    // Общая логика трансформации
    private String transformInternal(Source xmlSource, Source xsltSource) throws TransformerException {
        StringWriter writer = new StringWriter();
        TransformerFactory.newInstance().newTransformer(xsltSource).transform(xmlSource, new StreamResult(writer));
        return writer.toString();
    }
}
