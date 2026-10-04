package com.example.racecore.loot;

import com.example.racecore.RaceCore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class LootLuckEvents {
    private LootLuckEvents() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(
            LootTableLoadEvent event
    ) {
        if (!LootLuckRegistry.enabledForPath(
                event.getName().getPath()
        )) {
            return;
        }

        var ops =
                RegistryOps.create(
                        JsonOps.INSTANCE,
                        registryInfoLookup(event.getRegistries())
                );

        JsonElement encoded =
                LootTable.DIRECT_CODEC
                        .encodeStart(
                                ops,
                                event.getTable()
                        )
                        .resultOrPartial(
                                message ->
                                        RaceCore.LOGGER.warn(
                                                "Could not inspect loot table {}: {}",
                                                event.getName(),
                                                message
                                        )
                        )
                        .orElse(null);

        if (encoded == null
                || !encoded.isJsonObject()) {
            return;
        }

        LootQualityRegistry.put(
                event.getName(),
                encoded.getAsJsonObject()
        );
    }

    private static RegistryOps.RegistryInfoLookup registryInfoLookup(HolderGetter.Provider provider) {
        return new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<HolderGetter<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> registryKey
            ) {
                Optional<? extends HolderGetter<T>> result =
                        provider.<T>lookup(registryKey);

                if (result.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(result.get());
            }
        };
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal("luck")
                        .executes(context -> {
                            ServerPlayer player =
                                    context
                                            .getSource()
                                            .getPlayerOrException();

                            float luck =
                                    player.getLuck();

                            context.getSource()
                                    .sendSuccess(
                                            () ->
                                                    Component.literal(
                                                            "Luck: "
                                                                    + format(
                                                                    luck
                                                            )
                                                    ),
                                            false
                                    );

                            return 1;
                        })
        );
    }

    private static String format(
            double value
    ) {
        if (value == Math.rint(value)) {
            return Long.toString(
                    Math.round(value)
            );
        }

        return String.format(
                        java.util.Locale.ROOT,
                        "%.2f",
                        value
                )
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
