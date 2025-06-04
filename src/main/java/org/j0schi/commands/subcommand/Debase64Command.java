package org.j0schi.commands.subcommand;

import picocli.CommandLine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "debase64",
        description = "Decode Base64 string or file"
)
public class Debase64Command implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "Base64 string or file path (@file.txt)",
            paramLabel = "INPUT"
    )
    private String input;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Output file (default: print to console)"
    )
    private File outputFile;

    @CommandLine.Option(
            names = {"-ru", "--russian"},
            description = "Auto-set console to UTF-8 for Russian text"
    )
    private boolean useRussianOutput;

    @Override
    public Integer call() {
        try {
            String decoded = decodeInput();

            if (outputFile != null) {
                saveToFile(decoded);
            } else {
                printToConsole(decoded);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private String decodeInput() throws IOException {
        String base64Content = input.startsWith("@")
                ? readFile(input.substring(1))
                : input;

        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private void saveToFile(String content) throws IOException {
        Files.writeString(outputFile.toPath(), content, StandardCharsets.UTF_8);
        System.out.println("Saved to: " + outputFile.getAbsolutePath());
    }

    private void printToConsole(String content) {
        if (useRussianOutput || containsCyrillic(content)) {
            setConsoleToUTF8();
        }

        PrintWriter out = new PrintWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                true);

        out.println("Decoded result:");
        out.println(content);
    }

    private boolean containsCyrillic(String text) {
        return text.matches(".*[А-Яа-яЁё].*");
    }

    private void setConsoleToUTF8() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "chcp", "65001")
                        .inheritIO()
                        .start()
                        .waitFor();
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not set console to UTF-8");
        }
    }
}