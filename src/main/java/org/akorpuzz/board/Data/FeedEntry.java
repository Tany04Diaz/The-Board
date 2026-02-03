package org.akorpuzz.board.Data;

import java.util.UUID;

public record FeedEntry (long day, String text, String playerName, UUID id,String imageId){
}
