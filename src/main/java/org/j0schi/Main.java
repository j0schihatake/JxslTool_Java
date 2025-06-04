package org.j0schi;

import org.j0schi.cod.CodCommand;
import org.j0schi.commands.subcommand.Base64Command;
import org.j0schi.commands.subcommand.CopyAndReplaceCommand;
import org.j0schi.commands.subcommand.Debase64Command;
import org.j0schi.commands.subcommand.xslt.FindXsltCommand;
import org.j0schi.commands.subcommand.xslt.TransformCommand;
import org.j0schi.commands.subcommand.xslt.TransformStrCommand;
import org.j0schi.commands.subcommand.xslt.UpdateXsltCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "j0schi", mixinStandardHelpOptions = true, version = "1.0",
        description = "J0schi CLI Tool")
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                .addSubcommand(new TransformCommand())
                .addSubcommand(new TransformStrCommand())
                .addSubcommand(new FindXsltCommand())
                .addSubcommand(new Debase64Command())
                .addSubcommand(new Base64Command())
                .addSubcommand(new UpdateXsltCommand())
                .addSubcommand(new CopyAndReplaceCommand())
                .addSubcommand(new CodCommand())
                .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("Usage: j0schi <command> [options]");
        new CommandLine(this).usage(System.out);
        return 0;
    }
}