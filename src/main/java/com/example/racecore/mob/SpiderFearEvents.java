package com.example.racecore.mob;

import com.example.racecore.RaceCore;
import com.example.racecore.mob.entity.RiverDrakeEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = RaceCore.MODID)
public final class SpiderFearEvents {
    private SpiderFearEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Spider spider)) {
            return;
        }

        if (spider.goalSelector.getAvailableGoals().stream()
                .noneMatch(goal -> goal.getGoal() instanceof AvoidRiverDrakeGoal)) {
            // Above the spider's leap and melee attack goals, below swimming.
            spider.goalSelector.addGoal(2, new AvoidRiverDrakeGoal(spider));
        }
    }

    private static final class AvoidRiverDrakeGoal extends AvoidEntityGoal<RiverDrakeEntity> {
        private AvoidRiverDrakeGoal(Spider spider) {
            super(spider, RiverDrakeEntity.class, 12.0F, 1.2, 1.6);
        }

        @Override
        public void start() {
            this.mob.setTarget(null);
            super.start();
        }
    }
}
