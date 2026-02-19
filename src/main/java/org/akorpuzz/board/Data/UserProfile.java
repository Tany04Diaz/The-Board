package org.akorpuzz.board.Data;

import java.util.UUID;

public record UserProfile(
        UUID playerUUID,
        String playerName,
        String description,
        String profileImageId
) {
    public static  UserProfile createDefault(UUID uuid,String name){
            return new UserProfile(uuid,name,"!hola¡ Soy nuevo =D","none");
    }
}
