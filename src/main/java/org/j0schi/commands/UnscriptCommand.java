package org.j0schi.commands;

import picocli.CommandLine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "unscript",
        description = "Decode Base64 string and print result"
)
public class UnscriptCommand implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "Base64 encoded string to decode",
            paramLabel = "BASE64_STRING"
    )
    private String encodedString;

    @CommandLine.Option(
            names = {"-ru", "--russian"},
            description = "Auto-set console to UTF-8 for Russian text",
            defaultValue = "false"
    )
    private boolean useRussianOutput;

    @Override
    public Integer call() {
        try {
            // Устанавливаем кодировку консоли если нужно
            if (useRussianOutput) {
                setConsoleToUTF8();
            }

            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

            // Используем PrintWriter с UTF-8
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                    true);

            out.println("Decoded result:");
            out.println(decodedString);
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid Base64 input");
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 2;
        }
    }

    private void setConsoleToUTF8() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Запускаем chcp 65001 в новом процессе
                new ProcessBuilder("cmd", "/c", "chcp", "65001")
                        .inheritIO()
                        .start()
                        .waitFor();

                // Даем консоли время на переключение
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not set console to UTF-8");
        }
    }
}