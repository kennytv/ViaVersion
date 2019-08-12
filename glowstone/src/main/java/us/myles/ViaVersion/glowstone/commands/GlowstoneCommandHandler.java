package us.myles.ViaVersion.glowstone.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import us.myles.ViaVersion.commands.ViaCommandHandler;

import java.util.List;

public class GlowstoneCommandHandler extends ViaCommandHandler implements CommandExecutor, TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return onCommand(new GlowstoneCommandSender(sender), args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return onTabComplete(new GlowstoneCommandSender(sender), args);
    }
}
