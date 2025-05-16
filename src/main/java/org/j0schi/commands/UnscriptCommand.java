package org.j0schi.commands;

import picocli.CommandLine;
import java.util.Base64;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "unscript",
        description = "Decode Base64 string and print result"
)
public class UnscriptCommand implements Callable<Integer> {

    @CommandLine.Parameters(
            index = "0",
            description = "Base64 encoded string to decode"
    )
    private String encodedString;

    @Override
    public Integer call() {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
            String decodedString = new String(decodedBytes);
            System.out.println("Decoded result:");
            System.out.println(decodedString);
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid Base64 input");
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 2;
        }
    }
}