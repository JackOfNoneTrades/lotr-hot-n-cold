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
        return "/hotncold spawns dump <LOTR biome name or ID> [category]"
            + " OR /hotncold spawns explain <LOTR biome name or ID> <entity name>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 3 || args.length > 4 || !"spawns".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        List<String> lines;
        if ("dump".equalsIgnoreCase(args[1])) {
            String category = args.length == 4 ? args[3] : null;
            lines = LOTRSpawnReport.createBiomeDump(args[2], category);
        } else if ("explain".equalsIgnoreCase(args[1]) && args.length == 4) {
            lines = LOTRSpawnReport.createSpawnExplanation(args[2], args[3]);
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        for (String line : lines) {
            sender.addChatMessage(new ChatComponentText(line));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "spawns");
        }
        if (args.length == 2 && "spawns".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "dump", "explain");
        }
        if (args.length == 3 && "spawns".equalsIgnoreCase(args[0])
            && ("dump".equalsIgnoreCase(args[1]) || "explain".equalsIgnoreCase(args[1]))) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getBiomeNamesAndIds());
        }
        if (args.length == 4 && "spawns".equalsIgnoreCase(args[0]) && "dump".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getCreatureTypeNames());
        }
        if (args.length == 4 && "spawns".equalsIgnoreCase(args[0]) && "explain".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getEntityNames());
        }
        return null;
    }
}
