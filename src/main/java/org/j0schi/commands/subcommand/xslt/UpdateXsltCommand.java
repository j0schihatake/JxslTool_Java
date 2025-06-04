package org.j0schi.commands.subcommand.xslt;

import org.j0schi.commands.BaseCommand;
import org.j0schi.services.XsltUpdater;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(name = "update-xslt", description = "Update XSLT files")
public class UpdateXsltCommand extends BaseCommand {

    @Option(names = {"-s", "--source"}, description = "Source directory")
    private String sourceDir;

    @Option(names = {"-t", "--target"}, description = "Target directory")
    private String targetDir;

    @Option(names = {"-f", "--filter"}, description = "File filter")
    private String fileFilter = "*.xslt";

    @Override
    public Integer call() {
        // Загружаем сохраненную конфигурацию
        Map<String, String> config = loadConfig("update-xslt");

        // Устанавливаем значения из конфига, если параметры не заданы
        if (sourceDir == null) sourceDir = config.get("source");
        if (targetDir == null) targetDir = config.get("target");
        if (fileFilter == null) fileFilter = config.getOrDefault("filter", "*.xslt");

        // Проверяем обязательные параметры
        if (sourceDir == null || targetDir == null) {
            System.err.println("Required parameters missing!");
            return 1;
        }

        // Сохраняем параметры для будущего использования
        Map<String, String> currentConfig = new HashMap<>();
        currentConfig.put("source", sourceDir);
        currentConfig.put("target", targetDir);
        currentConfig.put("filter", fileFilter);
        saveConfig("update-xslt", currentConfig);

        // Выполняем основную логику команды
        try {
            new XsltUpdater(sourceDir, targetDir, fileFilter).update();
        } catch (IOException e) {
            System.err.println("Ошибка пи изменении: e: " + e.getMessage());
        }
        return 0;
    }
}