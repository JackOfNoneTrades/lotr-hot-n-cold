package org.fentanylsolutions.hotncold.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

import org.fentanylsolutions.hotncold.compat.LOTRSpawnReport;

public final class CommandHotNCold extends CommandBase {

    @Override
    public String getCommandName() {
        return "hotncold";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/hotncold spawns dump <LOTR biome name or ID> [category]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 3 || args.length > 4
            || !"spawns".equalsIgnoreCase(args[0])
            || !"dump".equalsIgnoreCase(args[1])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String category = args.length == 4 ? args[3] : null;
        for (String line : LOTRSpawnReport.createBiomeDump(args[2], category)) {
            sender.addChatMessage(new ChatComponentText(line));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "spawns");
        }
        if (args.length == 2 && "spawns".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "dump");
        }
        if (args.length == 3 && "spawns".equalsIgnoreCase(args[0]) && "dump".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getBiomeNamesAndIds());
        }
        if (args.length == 4 && "spawns".equalsIgnoreCase(args[0]) && "dump".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getCreatureTypeNames());
        }
        return null;
    }
}
