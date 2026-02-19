package org.akorpuzz.board.Data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.akorpuzz.board.Network.S2CUpdateProfilePayload;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ProfileStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getProfileDir(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data/board/profiles/");
    }

    /**
     * Carga el perfil de un jugador. Si no existe, crea uno por defecto.
     */
    public static UserProfile load(ServerLevel level, UUID playerUUID, String playerName) {
        Path path = getProfileDir(level).resolve(playerUUID + ".json");
        if (!Files.exists(path)) {
            return UserProfile.createDefault(playerUUID, playerName);
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, UserProfile.class);
        } catch (IOException e) {
            LOGGER.error("Error cargando perfil de {}", playerUUID, e);
            return UserProfile.createDefault(playerUUID, playerName);
        }
    }

    /**
     * Guarda el perfil en un archivo JSON nombrado con el UUID.
     */
    public static void save(ServerLevel level, UserProfile profile) {
        Path dir = getProfileDir(level);
        Path path = dir.resolve(profile.playerUUID() + ".json");
        try {
            Files.createDirectories(dir);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(profile, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Error guardando perfil de {}", profile.playerUUID(), e);
        }
    }

    /**
     * Carga el perfil del UUID solicitado y lo envía como S2C al jugador.
     * Intenta obtener el nombre real del jugador si está online.
     */
    public static void sendProfileSync(ServerPlayer requester, UUID targetUUID) {
        // Intentar obtener el nombre real si el jugador objetivo está online
        String playerName = "Desconocido";
        ServerPlayer targetPlayer = requester.getServer().getPlayerList().getPlayer(targetUUID);
        if (targetPlayer != null) {
            playerName = targetPlayer.getName().getString();
        } else {
            // Cargar el nombre desde el perfil guardado si existe
            UserProfile existing = load(requester.serverLevel(), targetUUID, "Desconocido");
            playerName = existing.playerName();
        }

        UserProfile profile = load(requester.serverLevel(), targetUUID, playerName);

        S2CUpdateProfilePayload response = new S2CUpdateProfilePayload(
                profile.playerUUID(),
                profile.playerName(),
                profile.description(),
                profile.profileImageId()
        );

        PacketDistributor.sendToPlayer(requester, response);
    }
}
