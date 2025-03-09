package dev.axis.base;

import dev.axis.annotation.Command;
import dev.axis.annotation.SubCommand;
import dev.axis.annotation.TabComplete;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {

  private static final Map<String, BukkitCommand> commands = new HashMap<>();

  public static void registerCommands(Object plugin) {
    try {
      CommandMap commandMap = getCommandMap();

      for (Class<?> clazz : plugin.getClass().getDeclaredClasses()) {
        if (clazz.isAnnotationPresent(Command.class)) {
          registerCommand(plugin, clazz, commandMap);
        }
      }
    } catch (Exception e) {
      Bukkit.getLogger().severe("[SANCF] Failed to register commands: " + e.getMessage());
    }
  }

  private static CommandMap getCommandMap() throws Exception {
    Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
    field.setAccessible(true);
    return (CommandMap) field.get(Bukkit.getServer());
  }

  private static void registerCommand(Object plugin, Class<?> clazz, CommandMap commandMap) throws Exception {
    Command commandInfo = clazz.getAnnotation(Command.class);
    Object instance = clazz.getDeclaredConstructor().newInstance();

    BukkitCommand command = new BukkitCommand(commandInfo.name()) {
      @Override
      public boolean execute(CommandSender sender, String label, String[] args) {
        if (!canExecute(sender, commandInfo)) return true;

        CommandContext context = new CommandContext(sender, args);
        if (args.length > 0 && executeSubCommand(clazz, instance, context, args[0], commandInfo.async())) {
          return true;
        }

        Method executeMethod = getExecuteMethod(clazz);
        if (executeMethod != null) {
          executeCommand(commandInfo.async(), () -> invokeMethod(executeMethod, instance, context));
        } else {
          context.sendMessage("&cInvalid command.");
        }
        return true;
      }

      @Override
      public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
          return getSubCommandCompletions(clazz, args[0]);
        } else if (args.length > 1) {
          return getTabCompleteOptions(clazz, args);
        }
        return Collections.emptyList();
      }
    };

    setCommandProperties(command, commandInfo);
    commandMap.register(plugin.getClass().getSimpleName(), command);
    commands.put(commandInfo.name(), command);
    Bukkit.getLogger().info("[SANCF] Registered command: " + commandInfo.name());
  }

  private static boolean canExecute(CommandSender sender, Command commandInfo) {
    if (!commandInfo.allowConsole() && !(sender instanceof org.bukkit.entity.Player)) {
      sender.sendMessage("§cThis command can only be used by players.");
      return false;
    }
    if (!commandInfo.permission().isEmpty() && !sender.hasPermission(commandInfo.permission())) {
      sender.sendMessage("§cYou don't have permission to use this command.");
      return false;
    }
    return true;
  }

  private static boolean executeSubCommand(Class<?> clazz, Object instance, CommandContext context, String subCommandName, boolean async) {
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(SubCommand.class)) {
        SubCommand subCommand = method.getAnnotation(SubCommand.class);
        if (subCommand.name().equalsIgnoreCase(subCommandName)) {
          if (!subCommand.permission().isEmpty() && !context.getSender().hasPermission(subCommand.permission())) {
            context.sendMessage("&cYou don't have permission to use this subcommand.");
            return true;
          }
          executeCommand(async, () -> invokeMethod(method, instance, context));
          return true;
        }
      }
    }
    return false;
  }

  private static List<String> getSubCommandCompletions(Class<?> clazz, String input) {
    return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(SubCommand.class))
            .map(method -> method.getAnnotation(SubCommand.class).name())
            .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
            .collect(Collectors.toList());
  }

  private static List<String> getTabCompleteOptions(Class<?> clazz, String[] args) {
    int index = args.length - 2;
    String input = args[args.length - 1];

    for (Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(TabComplete.class)) {
        String[] options = method.getAnnotation(TabComplete.class).value();

        if (index >= 0 && index < options.length) {
          String option = options[index];

          switch (option.toLowerCase()) {
            case "@players":
              return Bukkit.getOnlinePlayers().stream()
                      .map(player -> player.getName())
                      .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                      .collect(Collectors.toList());
            case "@items":
              return Arrays.stream(Material.values())
                      .map(Enum::name)
                      .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                      .collect(Collectors.toList());
            default:
              return Arrays.stream(option.split(","))
                      .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                      .collect(Collectors.toList());
          }
        }
      }
    }
    return Collections.emptyList();
  }

  private static void setCommandProperties(BukkitCommand command, Command commandInfo) {
    command.setDescription(commandInfo.description());
    command.setPermission(commandInfo.permission());
    command.setUsage(commandInfo.usage());
  }

  private static Method getExecuteMethod(Class<?> clazz) {
    try {
      return clazz.getMethod("execute", CommandContext.class);
    } catch (NoSuchMethodException ignored) {
      return null;
    }
  }

  private static void invokeMethod(Method method, Object instance, CommandContext context) {
    try {
      method.invoke(instance, context);
    } catch (Exception e) {
      Bukkit.getLogger().severe("[SANCF] Error executing command: " + e.getMessage());
    }
  }

  private static void executeCommand(boolean async, Runnable task) {
    if (async) {
      Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugins()[0], task);
    } else {
      task.run();
    }
  }
}
