package org.j0schi.commands.subcommand;

import picocli.CommandLine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "base64",
        description = "Encode string or file to Base64"
)
public class Base64Command implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "Text or file path (@file.txt)",
            paramLabel = "INPUT"
    )
    private String input;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Output file (default: print to console)"
    )
    private File outputFile;

    @Override
    public Integer call() {
        try {
            String encoded = encodeInput();

            if (outputFile != null) {
                saveToFile(encoded);
            } else {
                System.out.println("Base64 encoded:");
                System.out.println(encoded);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private String encodeInput() throws IOException {
        String content = input.startsWith("@")
                ? readFile(input.substring(1))
                : input;

        return Base64.getEncoder()
                .encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private void saveToFile(String content) throws IOException {
        Files.writeString(outputFile.toPath(), content, StandardCharsets.UTF_8);
        System.out.println("Saved to: " + outputFile.getAbsolutePath());
    }
}
