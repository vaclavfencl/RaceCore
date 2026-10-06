package com.example.racecore.progression;

import com.example.racecore.RaceCore;
import com.example.racecore.race.RaceDefinition;
import com.example.racecore.race.RaceManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * RaceCore Ancient Debris mining:
 *
 * - Valid pickaxe, no Silk Touch -> Netherite Scrap.
 * - Fortune uses the same reduced curve as before:
 *   roll vanilla ore Fortune, then retain only 25% of bonus items.
 * - Silk Touch -> exactly one Ancient Debris block.
 */
@EventBusSubscriber(modid = RaceCore.MODID)
public final class AncientDebrisFortuneEvents {
    private static final double BONUS_KEEP_CHANCE = 0.25;

    private AncientDebrisFortuneEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!event.getState().is(Blocks.ANCIENT_DEBRIS)) {
            return;
        }

        if (!(event.getBreaker() instanceof Player player)) {
            return;
        }

        ItemStack tool = event.getTool();

        if (tool.isEmpty() || !tool.is(ItemTags.PICKAXES)) {
            return;
        }

        // Respect vanilla harvest validation / insufficient-tier tools.
        if (event.getDrops().isEmpty()) {
            return;
        }

        var enchantments = event.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);

        Holder.Reference<Enchantment> silkTouch =
                enchantments.getOrThrow(Enchantments.SILK_TOUCH);

        if (tool.getEnchantmentLevel(silkTouch) > 0) {
            // Silk Touch is the only mining route that returns a placeable block.
            event.getDrops().clear();
            event.getDrops().add(new ItemEntity(
                    event.getLevel(),
                    event.getPos().getX() + 0.5,
                    event.getPos().getY() + 0.5,
                    event.getPos().getZ() + 0.5,
                    new ItemStack(Items.ANCIENT_DEBRIS, 1)
            ));
            return;
        }

        Holder.Reference<Enchantment> fortune =
                enchantments.getOrThrow(Enchantments.FORTUNE);

        int effectiveFortune =
                tool.getEnchantmentLevel(fortune);

        RaceDefinition race = RaceManager.getRace(player);
        effectiveFortune += race.fortuneBonus();

        int scrapCount = 1;

        if (effectiveFortune > 0) {
            /*
             * Vanilla ore_drops multiplier:
             * max(1, randomInt(0 .. fortune+1)).
             *
             * Then only 25% of bonus items survive.
             */
            int vanillaMultiplier = Math.max(
                    1,
                    event.getLevel()
                            .getRandom()
                            .nextInt(effectiveFortune + 2)
            );

            int bonusCandidates = vanillaMultiplier - 1;

            for (int i = 0; i < bonusCandidates; i++) {
                if (event.getLevel()
                        .getRandom()
                        .nextDouble() < BONUS_KEEP_CHANCE) {
                    scrapCount++;
                }
            }
        }

        event.getDrops().clear();
        event.getDrops().add(new ItemEntity(
                event.getLevel(),
                event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5,
                new ItemStack(Items.NETHERITE_SCRAP, scrapCount)
        ));
    }
}
