package com.example.racecore;

import com.example.racecore.death.DeathAttachments;
import com.example.racecore.race.RaceAttachments;
import com.example.racecore.race.RaceRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RaceCore.MODID)
public final class RaceCore {
    public static final String MODID = "racecore";
    public static final Logger LOGGER = LoggerFactory.getLogger(RaceCore.class);

    public RaceCore(IEventBus modBus, ModContainer modContainer) {
        RaceAttachments.ATTACHMENT_TYPES.register(modBus);
        DeathAttachments.ATTACHMENT_TYPES.register(modBus);
        RaceRegistry.load();

        LOGGER.info("RaceCore loaded {} race definitions.", RaceRegistry.ids().size());
    }
}
