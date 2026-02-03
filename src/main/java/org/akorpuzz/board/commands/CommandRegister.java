package org.akorpuzz.board.commands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class CommandRegister {

    @SubscribeEvent
    public static void RegisterCommands(RegisterCommandsEvent event){
    }

    @SubscribeEvent
    public static void RegisterClientCommands(RegisterClientCommandsEvent event){
        BoardCommand.register(event.getDispatcher());
    }



}
