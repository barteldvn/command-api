package me.gleeming.command;

import com.google.common.reflect.ClassPath;
import lombok.SneakyThrows;
import me.gleeming.command.help.Help;
import me.gleeming.command.help.HelpNode;
import me.gleeming.command.node.CommandNode;
import me.gleeming.command.paramter.ParamProcessor;
import me.gleeming.command.paramter.Processor;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.Arrays;

public class CommandHandler {
    private static Plugin plugin;

    public CommandHandler() {
    }

    /**
     * Registers commands based off a file path
     * @param path Path
     */
    public static void registerCommands(String path, Plugin plugin) {
        try {
            ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                    .filter(info -> info.getPackageName().startsWith(path))
                    .forEach(info -> registerCommands(info.load(), plugin));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers the commands in the class
     * @param commandClass Class
     */
    public static void registerCommands(Class<?> commandClass, Plugin plugin) {
        CommandHandler.setPlugin(plugin);
        try {
            registerCommands(commandClass.newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers the commands in the class
     * @param commandClass Class
     */
    private static void registerCommands(Object commandClass) {
        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Command command = method.getAnnotation(Command.class);
            if(command == null) return;

            new CommandNode(commandClass, method, command);
        });

        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Help help = method.getAnnotation(Help.class);
            if(help == null) return;

            HelpNode helpNode = new HelpNode(commandClass, help.names(), help.permission(), method);
            CommandNode.getNodes().forEach(node -> node.getNames().forEach(name -> Arrays.stream(help.names())
                    .map(String::toLowerCase)
                    .filter(helpName -> name.toLowerCase().startsWith(helpName))
                    .forEach(helpName -> node.getHelpNodes().add(helpNode))));
        });
    }

    public static void registerProcessors(Processor<?> processorClass, Plugin plugin) {
        try {
            CommandHandler.setPlugin(plugin);
            ParamProcessor.createProcessor(processorClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers processors based off a file path
     * @param path Path
     */
    public static void registerProcessors(String path, Plugin plugin) {
        try {
            ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                    .filter(info -> info.getPackageName().startsWith(path))
                    .filter(info -> info.load().getSuperclass().equals(Processor.class))
                    .forEach(info -> {
                        try { ParamProcessor.createProcessor((Processor<?>) info.load().newInstance());
                        } catch(Exception exception) { exception.printStackTrace(); }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Plugin getPlugin() {
        return CommandHandler.plugin;
    }

    public static void setPlugin(Plugin plugin) {
        CommandHandler.plugin = plugin;
    }
}