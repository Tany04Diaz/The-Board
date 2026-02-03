package org.akorpuzz.board.Data;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<FeedEntry>>() {}.getType();
    public static Path getPath(Level level){
        return level.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data/board/feed.json");
    }
    public static List<FeedEntry> load(ServerLevel level) throws IOException {
        Path path = getPath(level);
        if (!Files.exists(path))return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(path)){
            return GSON.fromJson(reader,LIST_TYPE);
        }catch (IOException e){
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    public static FeedEntry getEntryById(ServerLevel level, UUID id) throws IOException {
        List<FeedEntry> entries= FeedStorage.load(level);
        for(FeedEntry entry : entries){
            if (entry.id().equals(id)){
                return entry;
            }
        }
        return null;
    }
    public static void save(ServerLevel level, List<FeedEntry> entries){
        Path path = getPath(level);
        try{
            Files.createDirectories(path.getParent());
            try(Writer writer = Files.newBufferedWriter(path)){
                GSON.toJson(entries,LIST_TYPE,writer);
                System.out.println("Guardado en "+ path.toAbsolutePath());
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
