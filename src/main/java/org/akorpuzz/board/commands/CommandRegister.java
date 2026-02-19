package org.akorpuzz.board.commands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

// Los comandos de cliente se registran en el bus de juego (GAME), no en el de MOD
@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class CommandRegister {

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        BoardCommand.register(event.getDispatcher());
    }
}
