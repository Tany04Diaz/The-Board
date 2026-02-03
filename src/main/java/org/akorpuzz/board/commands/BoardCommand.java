package org.akorpuzz.board.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.akorpuzz.board.Screens.HubScreen;

public class BoardCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(
                Commands.literal("board").executes(BoardCommand::ExecuteCommand));
    }

    private static int ExecuteCommand(CommandContext<CommandSourceStack> context) {
        Minecraft.getInstance().setScreen(new HubScreen(Minecraft.getInstance().player.getName().getString()));
        return 1;
    }
}
