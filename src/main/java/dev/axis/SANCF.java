package dev.axis;

import dev.axis.base.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SANCF {
  public static void init(JavaPlugin plugin) {
    CommandManager.registerCommands(plugin);
  }
}
