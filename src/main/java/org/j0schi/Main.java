package org.j0schi;

import org.j0schi.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "jox", mixinStandardHelpOptions = true, version = "1.0",
        description = "XSLT Transformer CLI Tool")
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                .addSubcommand(new TransformCommand())
                .addSubcommand(new TransformStrCommand())
                .addSubcommand(new FindXsltCommand())
                .addSubcommand(new UpdateXsltCommand()) // Добавляем новую команду
                .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("Usage: jox <command> [options]");
        new CommandLine(this).usage(System.out);
        return 0;
    }
}