package dev.axis.base;

import dev.axis.annotation.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CommandManager {

  public static void registerCommands(Object plugin) {
    try {
      Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      commandMapField.setAccessible(true);
      CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

      for (Class<?> clazz : plugin.getClass().getDeclaredClasses()) {
        if (clazz.isAnnotationPresent(Command.class)) {
          Command commandInfo = clazz.getAnnotation(Command.class);
          BukkitCommand command = createCommand(clazz, commandInfo);
          if (command != null) {
            commandMap.register(plugin.getClass().getSimpleName(), command);
            Bukkit.getLogger().info("[SANCF] Registered command: " + commandInfo.name());
          }
        }
      }
    } catch (Exception e) {
      Bukkit.getLogger().severe("[SANCF] Failed to register commands: " + e.getMessage());
      e.printStackTrace(); // TODO: Remove in production
    }
  }

  private static BukkitCommand createCommand(Class<?> clazz, Command commandInfo) {
    try {
      Object instance = clazz.getDeclaredConstructor().newInstance();

      return new BukkitCommand(commandInfo.name()) {
        @Override
        public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
          try {
            Method method = clazz.getMethod("execute", CommandContext.class);

            if (!commandInfo.allowConsole() && !(sender instanceof org.bukkit.entity.Player)) {
              sender.sendMessage("§cThis command can only be used by players.");
              return true;
            }
            if (!commandInfo.permission().isEmpty() && !sender.hasPermission(commandInfo.permission())) {
              sender.sendMessage("§cYou don't have permission to use this command.");
              return true;
            }

            method.invoke(instance, new CommandContext(sender, args));
          } catch (Exception e) {
            Bukkit.getLogger().severe("[SANCF] Error executing command '" + commandInfo.name() + "': " + e.getMessage());
            e.printStackTrace(); // TODO: Remove in production
          }
          return true;
        }
      };
    } catch (Exception e) {
      Bukkit.getLogger().severe("[SANCF] Error creating command '" + commandInfo.name() + "': " + e.getMessage());
      return null;
    }
  }
}
