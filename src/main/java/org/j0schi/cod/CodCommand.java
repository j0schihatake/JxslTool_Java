package org.j0schi.cod;

import org.j0schi.cod.subcommand.CodUpdateAFCommand;
import org.j0schi.cod.subcommand.CodUpdateCommand;
import org.j0schi.commands.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "cod", description = "COD files synchronization commands",
        subcommands = {CodUpdateCommand.class, CodUpdateAFCommand.class})
public class CodCommand extends BaseCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        System.out.println("Usage: j0schi cod <update|updateAF> [options]");
        new CommandLine(this).usage(System.out);
        return 0;
    }
}