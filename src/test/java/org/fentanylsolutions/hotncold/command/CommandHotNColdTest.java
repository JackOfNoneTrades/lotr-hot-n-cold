package org.fentanylsolutions.hotncold.command;

import static org.junit.Assert.assertTrue;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.junit.Test;

public class CommandHotNColdTest {

    private static final ICommandSender UNPRIVILEGED_SENDER = new ICommandSender() {

        @Override
        public String getCommandSenderName() {
            return "test";
        }

        @Override
        public IChatComponent func_145748_c_() {
            return new ChatComponentText(getCommandSenderName());
        }

        @Override
        public void addChatMessage(IChatComponent message) {}

        @Override
        public boolean canCommandSenderUseCommand(int permissionLevel, String commandName) {
            return false;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates();
        }

        @Override
        public World getEntityWorld() {
            return null;
        }
    };

    @Test(expected = CommandException.class)
    public void rejectsReloadWithoutOperatorPermission() {
        new CommandHotNCold().processCommand(UNPRIVILEGED_SENDER, new String[] { "spawns", "reload" });
    }

    @Test(expected = CommandException.class)
    public void rejectsEquipmentReloadWithoutOperatorPermission() {
        new CommandHotNCold().processCommand(UNPRIVILEGED_SENDER, new String[] { "equipment", "reload" });
    }

    @Test
    public void offersReloadTabCompletion() {
        List<String> completions = new CommandHotNCold()
            .addTabCompletionOptions(UNPRIVILEGED_SENDER, new String[] { "spawns", "re" });

        assertTrue(completions.contains("reload"));
    }

    @Test
    public void offersEquipmentReloadTabCompletion() {
        List<String> sectionCompletions = new CommandHotNCold()
            .addTabCompletionOptions(UNPRIVILEGED_SENDER, new String[] { "equ" });
        List<String> actionCompletions = new CommandHotNCold()
            .addTabCompletionOptions(UNPRIVILEGED_SENDER, new String[] { "equipment", "re" });

        assertTrue(sectionCompletions.contains("equipment"));
        assertTrue(actionCompletions.contains("reload"));
    }

    @Test
    public void offersEquipmentExplainTabCompletion() {
        List<String> actionCompletions = new CommandHotNCold()
            .addTabCompletionOptions(UNPRIVILEGED_SENDER, new String[] { "equipment", "ex" });
        List<String> factionCompletions = new CommandHotNCold()
            .addTabCompletionOptions(UNPRIVILEGED_SENDER, new String[] { "equipment", "explain", "faction:GO" });

        assertTrue(actionCompletions.contains("explain"));
        assertTrue(factionCompletions.contains("faction:GONDOR"));
    }

    @Test
    public void offersExampleTabCompletion() {
        List<String> actionCompletions = new CommandHotNCold()
            .addTabCompletionOptions(UNPRIVILEGED_SENDER, new String[] { "spawns", "exa" });
        List<String> categoryCompletions = new CommandHotNCold()
            .addTabCompletionOptions(UNPRIVILEGED_SENDER, new String[] { "spawns", "example", "Plains", "Pig", "mon" });

        assertTrue(actionCompletions.contains("example"));
        assertTrue(categoryCompletions.contains("monster"));
    }
}
