package dev.axis;

import dev.axis.base.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SANCF {

  private static JavaPlugin plugin;

  public static void init(JavaPlugin plugin) {
    SANCF.plugin = plugin;
  }

  public static void register(Object command) {
    if (plugin == null) {
      throw new IllegalStateException("[SANCF] Plugin not initialized. Call SANCF.init(plugin) first.");
    }
    CommandManager.registerCommand(plugin, command);
  }
}
