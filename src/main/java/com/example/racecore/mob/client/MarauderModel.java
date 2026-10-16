package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/** Original 128x128 Blockbench geometry, including the hair, belt, spikes and articulated limbs. */
public final class MarauderModel extends HumanoidModel<MarauderRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "marauder"), "main");

    private final KeyframeAnimation idleAnimation;
    private final KeyframeAnimation walkAnimation;
    private final KeyframeAnimation attackAnimation;
    private final KeyframeAnimation powerupAnimation;

    public MarauderModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
        idleAnimation = MarauderAnimations.idle.bake(root);
        walkAnimation = MarauderAnimations.walk.bake(root);
        attackAnimation = MarauderAnimations.attack.bake(root);
        powerupAnimation = MarauderAnimations.powerup.bake(root);
    }

    @Override
    public void setupAnim(MarauderRenderState state) {
        // Reset, apply head tracking and populate vanilla item-hand transforms.
        super.setupAnim(state);

        // Exclusive animation choice: movement must not fight with attack or power-up.
        if (state.powerupAnimation.isStarted()) {
            powerupAnimation.apply(state.powerupAnimation, state.ageInTicks);
        } else if (state.attackAnimation.isStarted()) {
            attackAnimation.apply(state.attackAnimation, state.ageInTicks);
        } else if (state.walking) {
            // Use the complete 1.25s custom walk cycle, not just vanilla walking arms.
            walkAnimation.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        } else {
            idleAnimation.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        }
    }

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, -10.3F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -18.0F, 1.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

		PartDefinition hair = head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(58, 72).addBox(3.5F, -11.0F, -5.2F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(24, 73).addBox(3.5F, -10.0F, -4.2F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(82, 32).addBox(3.5F, -9.0F, -3.2F, 2.0F, 2.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(24, 85).addBox(3.5F, -8.0F, -2.2F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(48, 84).addBox(-5.5F, -9.0F, -3.2F, 2.0F, 2.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(0, 76).addBox(-5.5F, -10.0F, -4.2F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(70, 86).addBox(-5.5F, -8.0F, -2.2F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(40, 32).addBox(-5.7F, -12.0F, -5.0F, 11.0F, 2.0F, 10.0F, new CubeDeformation(0.2F))
		.texOffs(0, 64).addBox(-5.9F, -11.0F, -5.2F, 2.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition hair_r1 = hair.addOrReplaceChild("hair_r1", CubeListBuilder.create().texOffs(32, 60).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 11.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(4.3F, -10.0F, 4.8F, 0.0F, -1.5708F, 0.0F));

		PartDefinition hair2 = head.addOrReplaceChild("hair2", CubeListBuilder.create().texOffs(40, 44).addBox(-4.5F, -12.0F, -5.2F, 9.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(58, 60).addBox(-1.4F, -12.0F, 5.7F, 3.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(0, 52).addBox(-3.0F, -12.0F, 0.8F, 6.0F, 2.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, -6.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition mustache = head.addOrReplaceChild("mustache", CubeListBuilder.create().texOffs(80, 25).addBox(-2.0F, -5.0F, -6.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(68, 101).addBox(2.0F, -5.0F, -6.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(44, 85).addBox(4.0F, -6.0F, -6.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(80, 101).addBox(-5.0F, -6.0F, -6.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(102, 25).addBox(1.0F, -3.0F, -6.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(100, 21).addBox(-1.0F, -2.0F, -6.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(48, 78).addBox(-1.0F, 0.0F, -6.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.2F))
		.texOffs(24, 71).addBox(-1.0F, 2.0F, -6.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(102, 43).addBox(-2.0F, -3.0F, -6.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(74, 101).addBox(-4.0F, -5.0F, -6.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -11.0F, -5.0F, 16.0F, 22.0F, 10.0F, new CubeDeformation(0.251F)), PartPose.offset(0.0F, -7.0F, 1.0F));

		PartDefinition belt = body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(52, 28).addBox(-15.5F, -6.0F, -2.0F, 17.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 56).addBox(-15.5F, -6.0F, 8.0F, 17.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(70, 84).addBox(-13.0F, -4.4F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F))
		.texOffs(48, 73).addBox(-14.0F, -4.1F, -2.4F, 3.0F, 3.0F, 2.0F, new CubeDeformation(-0.5F)), PartPose.offset(7.0F, 11.0F, -4.0F));

		PartDefinition axe_r1 = belt.addOrReplaceChild("axe_r1", CubeListBuilder.create().texOffs(24, 64).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(-2.2F, -5.9F, 8.6F, 0.0F, 0.0F, 0.1745F));

		PartDefinition axe_r2 = belt.addOrReplaceChild("axe_r2", CubeListBuilder.create().texOffs(98, 55).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.2F, -6.6F, 8.6F, 0.0F, 0.0F, 0.1745F));

		PartDefinition axe_r3 = belt.addOrReplaceChild("axe_r3", CubeListBuilder.create().texOffs(84, 98).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.3F, -7.0F, 8.6F, 0.0F, 0.0F, 0.1745F));

		PartDefinition axe_r4 = belt.addOrReplaceChild("axe_r4", CubeListBuilder.create().texOffs(60, 95).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(-5.5F, -1.9F, 8.6F, 0.0F, 0.0F, 0.1745F));

		PartDefinition belt_r1 = belt.addOrReplaceChild("belt_r1", CubeListBuilder.create().texOffs(84, 60).addBox(-11.0F, -2.0F, -1.0F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.5F, -4.0F, -1.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition belt_r2 = belt.addOrReplaceChild("belt_r2", CubeListBuilder.create().texOffs(70, 56).addBox(-11.0F, -2.0F, -1.0F, 12.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -4.0F, -1.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition left_shoulder = body.addOrReplaceChild("left_shoulder", CubeListBuilder.create().texOffs(52, 0).addBox(-2.6F, 0.8167F, -5.0F, 6.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-9.4F, -12.8167F, 1.0F));

		PartDefinition spike = left_shoulder.addOrReplaceChild("spike", CubeListBuilder.create().texOffs(100, 11).addBox(-1.0F, -3.125F, -0.4F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.9F))
		.texOffs(16, 100).addBox(-1.0F, -2.125F, -0.4F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-1.4F, -1.5583F, 0.1F, 0.0F, -1.2392F, 0.0F));

		PartDefinition spike_r1 = spike.addOrReplaceChild("spike_r1", CubeListBuilder.create().texOffs(92, 98).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(0.0F, 1.875F, 0.5F, -0.0436F, 0.0F, 0.0F));

		PartDefinition spike_r2 = spike.addOrReplaceChild("spike_r2", CubeListBuilder.create().texOffs(68, 96).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(0.0F, 3.375F, 0.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition spike2 = left_shoulder.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(8, 100).addBox(-1.0F, -0.725F, -0.4F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.8F)), PartPose.offset(1.0F, -2.2583F, 0.9F));

		PartDefinition spike_r3 = spike2.addOrReplaceChild("spike_r3", CubeListBuilder.create().texOffs(32, 52).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.55F)), PartPose.offsetAndRotation(0.0F, 2.475F, 0.5F, -0.0436F, 0.0F, 0.0F));

		PartDefinition spike_r4 = spike2.addOrReplaceChild("spike_r4", CubeListBuilder.create().texOffs(96, 64).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.3F)), PartPose.offsetAndRotation(0.0F, 4.375F, 0.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition right_shoulder = body.addOrReplaceChild("right_shoulder", CubeListBuilder.create().texOffs(52, 14).addBox(-3.45F, -0.1625F, -4.95F, 6.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(9.45F, -11.8375F, 0.95F));

		PartDefinition spike3 = right_shoulder.addOrReplaceChild("spike3", CubeListBuilder.create().texOffs(100, 16).addBox(-1.0F, -3.125F, -0.4F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.8F))
		.texOffs(100, 98).addBox(-1.0F, -2.125F, -0.4F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.6F)), PartPose.offsetAndRotation(0.45F, -2.8375F, 0.95F, 0.0F, 0.3491F, 0.0F));

		PartDefinition spike_r5 = spike3.addOrReplaceChild("spike_r5", CubeListBuilder.create().texOffs(0, 100).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.4F)), PartPose.offsetAndRotation(0.0F, 1.875F, 0.5F, -0.0436F, 0.0F, 0.0F));

		PartDefinition spike_r6 = spike3.addOrReplaceChild("spike_r6", CubeListBuilder.create().texOffs(76, 96).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.15F)), PartPose.offsetAndRotation(0.0F, 3.375F, 0.0F, -0.1745F, 0.0F, 0.0F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(90, 86).addBox(-3.0F, -1.75F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.8F)), PartPose.offsetAndRotation(-8.3F, -15.25F, 1.0F, 0.0F, 0.0F, 0.2618F));

		PartDefinition left_wrist = left_arm.addOrReplaceChild("left_wrist", CubeListBuilder.create().texOffs(24, 95).addBox(-1.6F, -0.3F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(-1.5F, 7.55F, 0.0F, -0.3491F, 0.0F, -0.1309F));

		PartDefinition left_hand = left_wrist.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(84, 64).addBox(-1.6F, -0.1F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, 8.8F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 95).addBox(0.15F, -0.75F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.8F)), PartPose.offsetAndRotation(8.3F, -16.25F, 1.0F, 0.0F, 0.0F, -0.1309F));

		PartDefinition right_wrist = right_arm.addOrReplaceChild("right_wrist", CubeListBuilder.create().texOffs(48, 95).addBox(-1.4F, 0.75F, -1.5F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.5F)), PartPose.offsetAndRotation(1.5F, 7.5F, 0.0F, -0.5233F, 0.0167F, 0.0403F));

		PartDefinition right_hand = right_wrist.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(90, 25).addBox(-1.4F, 0.9F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, 8.85F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(80, 11).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.5F, 3.75F, 0.5F));

		PartDefinition left_shin = left_leg.addOrReplaceChild("left_shin", CubeListBuilder.create().texOffs(0, 88).addBox(-1.5F, -0.3F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 9.3F, -0.5F));

		PartDefinition left_foot = left_shin.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(78, 44).addBox(-2.0F, -1.15F, -4.9F, 4.0F, 3.0F, 8.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, 8.85F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(82, 72).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5F, 3.75F, 0.5F));

		PartDefinition right_shin = right_leg.addOrReplaceChild("right_shin", CubeListBuilder.create().texOffs(12, 88).addBox(-1.5F, -0.3F, -1.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 9.3F, -0.5F));

		PartDefinition right_foot = right_shin.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(80, 0).addBox(-2.0F, -1.15F, -5.0F, 4.0F, 3.0F, 8.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, 8.85F, 0.1F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

}
