package org.akorpuzz.board.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientFeedCache {
    private static List<FeedEntry> entries = new ArrayList<>();

    // Guarda el feed y permite que otras clases lo usen
    public static void setEntries(List<FeedEntry> newEntries) {
        entries = new ArrayList<>(newEntries);
    }

    // Alias para compatibilidad con tus handlers existentes
    public static void setInventory(List<FeedEntry> newEntries) {
        setEntries(newEntries);
    }

    public static List<FeedEntry> getEntries() {
        return entries;
    }

    // Para el FeedScreen
    public static List<FeedEntry> getGlobalFeed() {
        return entries;
    }

    // NUEVA LÓGICA: Filtro funcional para perfiles
    public static List<FeedEntry> getEntriesByPlayer(String playerName) {
        return entries.stream()
                .filter(e -> e.playerName().equalsIgnoreCase(playerName))
                .collect(Collectors.toList());
    }
}