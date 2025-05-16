package org.j0schi.commands;

import org.j0schi.services.ConfigService;
import picocli.CommandLine.Command;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class BaseCommand implements Callable<Integer> {
    protected final ConfigService configService = new ConfigService();

    protected void saveConfig(String commandName, Map<String, String> params) {
        configService.saveCommandConfig(commandName, params);
    }

    protected Map<String, String> loadConfig(String commandName) {
        return configService.loadCommandConfig(commandName);
    }
}
