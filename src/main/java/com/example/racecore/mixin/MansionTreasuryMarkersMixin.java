package com.example.racecore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the RaceCore-specific data markers spawn these guards. */
@Mixin(WoodlandMansionPieces.WoodlandMansionPiece.class)
public abstract class MansionTreasuryMarkersMixin {
    @Inject(method = "handleDataMarker", at = @At("HEAD"), cancellable = true)
    private void racecore$treasuryGuards(String marker, BlockPos position,
            ServerLevelAccessor level, RandomSource random, BoundingBox chunkBB,
            CallbackInfo ci) {
        if (marker.equals("RaceCoreWarrior")) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(
                    Identifier.fromNamespaceAndPath("minecraft", "vindicator"));
            if (type != null
                    && type.create(level.getLevel(), EntitySpawnReason.STRUCTURE) instanceof Vindicator guard) {
                guard.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
                guard.setYRot(random.nextFloat() * 360.0F);
                guard.setXRot(0.0F);
                guard.setPersistenceRequired();
                guard.setCanPickUpLoot(false);
                ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
                axe.enchant(level.getLevel().registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SHARPNESS), 5);
                guard.setItemSlot(EquipmentSlot.MAINHAND, axe);
                guard.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                level.addFreshEntityWithPassengers(guard);
            }
            ci.cancel();
        } else if (marker.equals("RaceCoreMage")) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(
                    Identifier.fromNamespaceAndPath("minecraft", "evoker"));
            if (type != null
                    && type.create(level.getLevel(), EntitySpawnReason.STRUCTURE) instanceof Evoker guard) {
                guard.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
                guard.setYRot(random.nextFloat() * 360.0F);
                guard.setXRot(0.0F);
                guard.setPersistenceRequired();
                level.addFreshEntityWithPassengers(guard);
            }
            ci.cancel();
        }
    }
}
