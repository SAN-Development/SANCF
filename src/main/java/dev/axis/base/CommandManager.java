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
      Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      commandMapField.setAccessible(true);
      CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

      for (Class<?> clazz : plugin.getClass().getDeclaredClasses()) {
        if (clazz.isAnnotationPresent(Command.class)) {
          Command commandInfo = clazz.getAnnotation(Command.class);
          Object instance = clazz.getDeclaredConstructor().newInstance();

          BukkitCommand command = new BukkitCommand(commandInfo.name()) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
              try {
                if (!commandInfo.allowConsole() && !(sender instanceof org.bukkit.entity.Player)) {
                  sender.sendMessage("§cThis command can only be used by players.");
                  return true;
                }

                if (!commandInfo.permission().isEmpty() && !sender.hasPermission(commandInfo.permission())) {
                  sender.sendMessage("§cYou don't have permission to use this command.");
                  return true;
                }

                CommandContext context = new CommandContext(sender, args);

                if (args.length > 0) {
                  String subCommandName = args[0];

                  for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(SubCommand.class)) {
                      SubCommand subCommand = method.getAnnotation(SubCommand.class);

                      if (subCommand.name().equalsIgnoreCase(subCommandName)) {
                        if (!subCommand.permission().isEmpty() &&
                                !sender.hasPermission(subCommand.permission())) {
                          context.sendMessage("&cYou don't have permission to use this subcommand.");
                          return true;
                        }

                        executeCommand(commandInfo.async(), () ->
                                invokeMethod(method, instance, context));
                        return true;
                      }
                    }
                  }
                }

                Method method = clazz.getMethod("execute", CommandContext.class);
                executeCommand(commandInfo.async(), () ->
                        invokeMethod(method, instance, context));

              } catch (Exception e) {
                Bukkit.getLogger().severe("[SANCF] Error executing command: " + e.getMessage());
              }
              return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
              List<String> completions = new ArrayList<>();

              if (args.length == 1) {
                for (Method method : clazz.getDeclaredMethods()) {
                  if (method.isAnnotationPresent(SubCommand.class)) {
                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    if (subCommand.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                      completions.add(subCommand.name());
                    }
                  }
                }
              } else if (args.length > 1) {
                for (Method method : clazz.getDeclaredMethods()) {
                  if (method.isAnnotationPresent(TabComplete.class)) {
                    TabComplete tabComplete = method.getAnnotation(TabComplete.class);

                    String[] tabOptions = tabComplete.value();
                    int tabIndex = args.length - 2;

                    if (tabIndex >= 0 && tabIndex < tabOptions.length) {
                      String option = tabOptions[tabIndex];

                      // ✅ Handle @players for player name completion
                      if (option.equalsIgnoreCase("@players")) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream()
                                .map(player -> player.getName())
                                .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                                .collect(Collectors.toList()));
                      }

                      else if (option.equalsIgnoreCase("@items")) {
                        completions.addAll(Arrays.stream(Material.values())
                                .map(Enum::name)
                                .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                                .collect(Collectors.toList()));
                      }
                      else {
                        completions.addAll(Arrays.stream(option.split(","))
                                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                                .collect(Collectors.toList()));
                      }
                    }
                  }
                }
              }

              return completions;
            }
          };

          command.setDescription(commandInfo.description());
          command.setPermission(commandInfo.permission());
          command.setUsage(commandInfo.usage());

          commandMap.register(plugin.getClass().getSimpleName(), command);
          commands.put(commandInfo.name(), command);

          Bukkit.getLogger().info("[SANCF] Registered command: " + commandInfo.name());
        }
      }
    } catch (Exception e) {
      Bukkit.getLogger().severe("[SANCF] Failed to register commands: " + e.getMessage());
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
