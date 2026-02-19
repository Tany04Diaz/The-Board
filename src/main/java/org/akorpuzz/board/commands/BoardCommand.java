package org.akorpuzz.board.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BoardCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("board").executes(BoardCommand::executeCommand));
    }

    @OnlyIn(Dist.CLIENT)
    private static int executeCommand(CommandContext<CommandSourceStack> context) {
        // Ejecutar en el hilo principal del cliente para abrir la pantalla de forma segura
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(
                new org.akorpuzz.board.Screens.HubScreen(mc.player.getName().getString())
        ));
        return 1;
    }
}
