package org.fentanylsolutions.hotncold.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentReport;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnControl;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnReport;

public final class CommandHotNCold extends CommandBase {

    @Override
    public String getCommandName() {
        return "hotncold";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/hotncold spawns dump <LOTR biome name or ID> [category]"
            + " OR /hotncold spawns explain <LOTR biome name or ID> <entity name>"
            + " OR /hotncold spawns example <LOTR biome name or ID> <entity name> [category]"
            + " OR /hotncold spawns reload"
            + " OR /hotncold equipment reload"
            + " OR /hotncold equipment explain <LOTR NPC name or faction:CODE>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 5) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        if ("equipment".equalsIgnoreCase(args[0])) {
            processEquipmentCommand(sender, args);
            return;
        }
        if (!"spawns".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        if ("reload".equalsIgnoreCase(args[1])) {
            reloadSpawnRules(sender, args);
            return;
        }

        List<String> lines;
        if ("dump".equalsIgnoreCase(args[1]) && (args.length == 3 || args.length == 4)) {
            String category = args.length == 4 ? args[3] : null;
            lines = LOTRSpawnReport.createBiomeDump(args[2], category);
        } else if ("explain".equalsIgnoreCase(args[1]) && args.length == 4) {
            lines = LOTRSpawnReport.createSpawnExplanation(args[2], args[3]);
        } else if ("example".equalsIgnoreCase(args[1]) && (args.length == 4 || args.length == 5)) {
            String category = args.length == 5 ? args[4] : null;
            lines = LOTRSpawnReport.createRuleExamples(args[2], args[3], category);
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
            return getListOfStringsMatchingLastWord(args, "equipment", "spawns");
        }
        if (args.length == 2 && "equipment".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "explain", "reload");
        }
        if (args.length == 3 && "equipment".equalsIgnoreCase(args[0]) && "explain".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTREquipmentReport.getLOTRNPCEntityNames());
        }
        if (args.length == 2 && "spawns".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "dump", "example", "explain", "reload");
        }
        if (args.length == 3 && "spawns".equalsIgnoreCase(args[0])
            && ("dump".equalsIgnoreCase(args[1]) || "example".equalsIgnoreCase(args[1])
                || "explain".equalsIgnoreCase(args[1]))) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getBiomeNamesAndIds());
        }
        if (args.length == 4 && "spawns".equalsIgnoreCase(args[0]) && "dump".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getCreatureTypeNames());
        }
        if (args.length == 4 && "spawns".equalsIgnoreCase(args[0]) && "explain".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getEntityNames());
        }
        if (args.length == 4 && "spawns".equalsIgnoreCase(args[0]) && "example".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getEntityNames());
        }
        if (args.length == 5 && "spawns".equalsIgnoreCase(args[0]) && "example".equalsIgnoreCase(args[1])) {
            return getListOfStringsMatchingLastWord(args, LOTRSpawnReport.getCreatureTypeNames());
        }
        return null;
    }

    private void reloadSpawnRules(ICommandSender sender, String[] args) {
        if (args.length != 2) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if (!sender.canCommandSenderUseCommand(2, getCommandName())) {
            throw new CommandException("commands.generic.permission");
        }

        LOTRSpawnControl.SpawnRuleResult result = LOTRSpawnControl.reloadConfiguredSpawnRules();
        if (result == null) {
            throw new CommandException("Hot N Cold's configuration file is not available.");
        }
        sender.addChatMessage(new ChatComponentText(result.describeReload()));
    }

    private void reloadEquipmentRules(ICommandSender sender, String[] args) {
        if (args.length != 2 || !"reload".equalsIgnoreCase(args[1])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if (!sender.canCommandSenderUseCommand(2, getCommandName())) {
            throw new CommandException("commands.generic.permission");
        }

        LOTREquipmentControl.EquipmentRuleReloadResult result = LOTREquipmentControl.reloadConfiguredRules();
        if (result == null) {
            throw new CommandException("Hot N Cold's configuration file is not available.");
        }
        sender.addChatMessage(new ChatComponentText(result.describeReload()));
    }

    private void processEquipmentCommand(ICommandSender sender, String[] args) {
        if ("reload".equalsIgnoreCase(args[1])) {
            reloadEquipmentRules(sender, args);
            return;
        }
        if ("explain".equalsIgnoreCase(args[1]) && args.length == 3) {
            for (String line : LOTREquipmentReport.createEquipmentExplanation(args[2], sender.getEntityWorld())) {
                sender.addChatMessage(new ChatComponentText(line));
            }
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }
}
