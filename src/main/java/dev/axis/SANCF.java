package dev.axis;

import dev.axis.base.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SANCF {

  private static JavaPlugin plugin;
  public static void init(JavaPlugin plugin) {

    SANCF.plugin = plugin;
    CommandManager.registerCommands(plugin);
  }

  public static JavaPlugin getPlugin() {
    return plugin;
  }
}
