package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Custom Stone Sentinel geometry exported from Blockbench and updated for Minecraft 26.3. */
public final class StoneSentinelModel extends EntityModel<StoneSentinelRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "stone_sentinel"), "main");

    private final KeyframeAnimation idle;
    private final KeyframeAnimation walk;
    private final KeyframeAnimation attack;
    private final KeyframeAnimation throwRock;
    private final KeyframeAnimation summon;

    public StoneSentinelModel(ModelPart root) {
        super(root);
        this.idle = StoneSentinelAnimations.idle.bake(root);
        this.walk = StoneSentinelAnimations.walk.bake(root);
        this.attack = StoneSentinelAnimations.attack.bake(root);
        this.throwRock = StoneSentinelAnimations.throw_rock.bake(root);
        this.summon = StoneSentinelAnimations.summon.bake(root);
    }

    @Override
    public void setupAnim(StoneSentinelRenderState state) {
        super.setupAnim(state);
        boolean casting = state.throwAnimation.isStarted() || state.summonAnimation.isStarted();
        boolean attacking = state.attackAnimation.isStarted();
        if (!casting && !attacking) {
            if (state.walking) {
                // Play the authored 1-second Blockbench walk loop at full strength.
                // This avoids almost-invisible movement on slow sentinels.
                this.walk.apply((long) (state.ageInTicks * 50.0F), 1.0F);
            } else {
                this.idle.apply((long)(state.ageInTicks * 50.0f), 1.0f);
            }
        }
        // Ability and attack animations start via client entity events.
        this.attack.apply(state.attackAnimation, state.ageInTicks);
        this.throwRock.apply(state.throwAnimation, state.ageInTicks);
        // Summon's source keyframes last 1 second, but the existing server windup lasts 2 seconds.
        this.summon.apply(state.summonAnimation, state.ageInTicks, 0.5f);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -20.0F, -10.0F, 20.0F, 40.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition eyes = body.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(56, 60).addBox(-2.6667F, -9.0F, -1.0F, 8.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(56, 70).addBox(-7.6667F, -1.0F, -1.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(68, 70).addBox(2.3333F, 2.0F, -1.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.3333F, -7.0F, -9.75F));

        PartDefinition left_hand = partdefinition.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(0, 60).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 20.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-19.0F, -4.0F, 0.0F));

        PartDefinition righthand = partdefinition.addOrReplaceChild("righthand", CubeListBuilder.create().texOffs(32, 60).addBox(-3.0F, -7.25F, -3.0F, 6.0F, 16.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(17.0F, 6.25F, -1.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
