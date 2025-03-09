package dev.axis.base;

import dev.axis.annotation.Command;
import dev.axis.annotation.SubCommand;
import dev.axis.annotation.TabComplete;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {

  private static final Map<String, BukkitCommand> commands = new HashMap<>();
  private static SimpleCommandMap commandMap;

  public static void registerCommand(JavaPlugin plugin, Object instance) {
    try {
      if (commandMap == null) {
        Field field = SimplePluginManager.class.getDeclaredField("commandMap");
        field.setAccessible(true);
        commandMap = (SimpleCommandMap) field.get(Bukkit.getPluginManager());
      }

      Class<?> clazz = instance.getClass();
      if (!clazz.isAnnotationPresent(Command.class)) {
        plugin.getLogger().warning("[SANCF] Class " + clazz.getName() + " is not annotated with @Command");
        return;
      }

      Command commandInfo = clazz.getAnnotation(Command.class);

      BukkitCommand command = new BukkitCommand(commandInfo.name()) {
        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
          if (!canExecute(sender, commandInfo)) return true;

          CommandContext context = new CommandContext(sender, args);
          if (args.length > 0 && executeSubCommand(plugin, instance, context, args[0], commandInfo.async())) {
            return true;
          }

          Method executeMethod = getExecuteMethod(clazz);
          if (executeMethod != null) {
            executeCommand(plugin, commandInfo.async(), () -> invokeMethod(executeMethod, instance, context));
          } else {
            context.sendMessage("&cInvalid usage! " + commandInfo.usage());
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
      commandMap.register(plugin.getName(), command);
      commands.put(commandInfo.name(), command);
      plugin.getLogger().info("[SANCF] Registered command: " + commandInfo.name());

    } catch (Exception e) {
      plugin.getLogger().severe("[SANCF] Failed to register command: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static boolean canExecute(CommandSender sender, Command commandInfo) {
    if (!commandInfo.allowConsole() && !(sender instanceof Player)) {
      sender.sendMessage("§cThis command can only be used by players.");
      return false;
    }
    if (!commandInfo.permission().isEmpty() && !sender.hasPermission(commandInfo.permission())) {
      sender.sendMessage("§cYou don't have permission to use this command.");
      return false;
    }
    return true;
  }

  private static boolean executeSubCommand(Plugin plugin, Object instance, CommandContext context, String subCommandName, boolean async) {
    for (Method method : instance.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(SubCommand.class)) {
        SubCommand subCommand = method.getAnnotation(SubCommand.class);
        if (subCommand.name().equalsIgnoreCase(subCommandName)) {
          if (!subCommand.permission().isEmpty() && !context.getSender().hasPermission(subCommand.permission())) {
            context.sendMessage("&cYou don't have permission to use this subcommand.");
            return true;
          }
          executeCommand(plugin, async, () -> invokeMethod(method, instance, context));
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
    String subCommandName = args[0];
    Optional<Method> subCommandMethodOpt = Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(SubCommand.class))
            .filter(method -> method.getAnnotation(SubCommand.class).name().equalsIgnoreCase(subCommandName))
            .findFirst();

    if (!subCommandMethodOpt.isPresent()) return Collections.emptyList();

    Method subCommandMethod = subCommandMethodOpt.get();
    if (!subCommandMethod.isAnnotationPresent(TabComplete.class)) return Collections.emptyList();

    String[] options = subCommandMethod.getAnnotation(TabComplete.class).value();
    int index = args.length - 2; // args[0] = subcommand
    if (index < 0 || index >= options.length) return Collections.emptyList();

    String option = options[index];
    String input = args[args.length - 1].toLowerCase();

    switch (option.toLowerCase()) {
      case "@players":
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
      case "@items":
        return Arrays.stream(Material.values())
                .map(Enum::name)
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
      default:
        return Arrays.stream(option.split(","))
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
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
      e.printStackTrace();
    }
  }

  private static void executeCommand(Plugin plugin, boolean async, Runnable task) {
    if (async) {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    } else {
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  private static void setCommandProperties(BukkitCommand command, Command commandInfo) {
    command.setDescription(commandInfo.description());
    command.setPermission(commandInfo.permission());
    command.setUsage(commandInfo.usage());
    command.setAliases(Arrays.asList(commandInfo.aliases()));
  }
}
