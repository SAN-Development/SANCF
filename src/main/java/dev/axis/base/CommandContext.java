package dev.axis.base;

import org.bukkit.command.CommandSender;

public class CommandContext {

  private final CommandSender sender;
  private final String[] args;

  public CommandContext(CommandSender sender, String[] args) {
    this.sender = sender;
    this.args = args;
  }

  public CommandSender getSender() {
    return sender;
  }

  public String[] getArgs() {
    return args;
  }

  public String getArg(int index) {
    if (index < 0 || index >= args.length) {
      return null;
    }
    return args[index];
  }

  public int getArgCount() {
    return args.length;
  }

  public void sendMessage(String message) {
    sender.sendMessage(message.replace("&", "§"));
  }

  public boolean hasPermission(String permission) {
    return permission.isEmpty() || sender.hasPermission(permission);
  }

  public void sendNoPermissionMessage() {
    sendMessage("&cYou don't have permission to use this command.");
  }

  public void sendUsageMessage(String usage) {
    sendMessage("&cUsage: " + usage);
  }
}
