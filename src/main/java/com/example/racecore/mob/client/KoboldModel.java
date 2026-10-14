package com.example.racecore.mob.client;

import com.example.racecore.RaceCore;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * The user's original 64x64 Blockbench Kobold, with its textured cube UVs preserved.
 * Visual geometry is turned 180 degrees to face Minecraft forward (-Z).
 * Original left/right geometry is swapped onto the appropriate humanoid arm bones.
 * The renderer scales the ~25-pixel model to one block high without altering UVs.
 * Basic walking, arms, attack swing, and head movement come from HumanoidModel.
 */
public final class KoboldModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "kobold"), "main");

    public KoboldModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition headBone = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0000F, 12.0000F, 0.5000F));
        // HumanoidModel in Minecraft 26.3 looks for hat under head.
        headBone.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition headVisual = headBone.addOrReplaceChild("visual", CubeListBuilder.create().texOffs(0, 0).addBox(-4.25F, 1.0F, -4.5F, 8.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(54, 10).addBox(-4.5F, 2.0F, 3.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(54, 10).addBox(3.0F, 2.0F, 3.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0000F, -9.0000F, 0.0000F, 0.0F, (float) Math.PI, 0.0F));
        PartDefinition candle = headVisual.addOrReplaceChild("candle", CubeListBuilder.create().texOffs(24, 48).addBox(0.0F, -1.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(22, 37).addBox(0.5F, -1.75F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F)), PartPose.offset(-0.25F, -2.0F, -0.5F));
        PartDefinition snout = headVisual.addOrReplaceChild("snout", CubeListBuilder.create(), PartPose.offset(-0.25F, 6.5F, 8.0F));
        PartDefinition top = snout.addOrReplaceChild("top", CubeListBuilder.create().texOffs(26, 31).addBox(-2.5F, -1.5F, -3.0F, 5.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(36, 14).addBox(0.25F, -2.0F, 0.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(44, 14).addBox(-2.25F, -2.0F, 0.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -0.25F));
        PartDefinition bottom = snout.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, 0.5F, -3.0F, 5.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.25F, -0.25F, -0.1309F, 0.0F, 0.0F));
        PartDefinition teeth = bottom.addOrReplaceChild("teeth", CubeListBuilder.create().texOffs(48, 37).addBox(0.0F, -1.5F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 41).addBox(0.0F, -2.0F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 43).addBox(0.0F, -2.25F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 39).addBox(0.0F, -1.75F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 29).addBox(0.0F, -1.5F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 45).addBox(0.0F, -2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 37).addBox(0.0F, -1.75F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 23).addBox(0.0F, -2.5F, 3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(40, 53).addBox(5.0F, -1.6F, 2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(44, 53).addBox(5.0F, -2.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(54, 8).addBox(5.0F, -1.5F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(54, 0).addBox(3.5F, -1.5F, 3.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(54, 2).addBox(3.5F, -2.0F, 3.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(48, 53).addBox(5.0F, -1.75F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(4, 54).addBox(1.25F, -1.75F, 3.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 53).addBox(5.0F, -2.25F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(0, 54).addBox(5.0F, -1.75F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(8, 54).addBox(5.0F, -1.5F, -1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F))
		.texOffs(52, 26).addBox(5.0F, -2.5F, 3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.25F)), PartPose.offset(-3.0F, 1.75F, -1.0F));
        PartDefinition ears = headVisual.addOrReplaceChild("ears", CubeListBuilder.create().texOffs(0, 50).addBox(-4.5F, 0.5F, -5.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.25F))
		.texOffs(8, 50).addBox(-4.5F, 0.25F, -6.75F, 1.0F, 1.0F, 3.0F, new CubeDeformation(-0.0001F))
		.texOffs(40, 48).addBox(-4.5F, 0.5F, -3.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.251F))
		.texOffs(16, 50).addBox(3.0F, 0.5F, -5.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.25F))
		.texOffs(48, 48).addBox(3.0F, 0.5F, -3.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.251F))
		.texOffs(52, 14).addBox(3.0F, 0.25F, -6.75F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition bodyBone = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0000F, 12.0000F, 0.0000F));
        PartDefinition bodyVisual = bodyBone.addOrReplaceChild("visual", CubeListBuilder.create().texOffs(0, 18).addBox(-4.25F, -14.0F, -3.75F, 8.0F, 9.0F, 5.0F, new CubeDeformation(0.0001F))
		.texOffs(26, 18).addBox(-4.25F, -13.0F, -6.25F, 8.0F, 8.0F, 5.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(0.0000F, 12.0000F, 0.0000F, 0.0F, (float) Math.PI, 0.0F));
        PartDefinition candle2 = bodyVisual.addOrReplaceChild("candle2", CubeListBuilder.create().texOffs(48, 31).addBox(0.0F, -1.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.5F))
		.texOffs(54, 4).addBox(0.5F, -1.25F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-0.5F, -13.5F, -6.5F, 0.2801F, -0.1019F, 0.0433F));
        PartDefinition candle3 = bodyVisual.addOrReplaceChild("candle3", CubeListBuilder.create().texOffs(32, 48).addBox(0.0F, -1.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(-0.5F))
		.texOffs(54, 6).addBox(0.5F, -1.25F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-1.75F, -13.25F, -6.5F, 0.2782F, -0.0569F, -0.1251F));
        PartDefinition right_armBone = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.7500F, 11.0000F, 1.7500F));
        PartDefinition right_armVisual = right_armBone.addOrReplaceChild("visual", CubeListBuilder.create().texOffs(0, 39).addBox(-7.25F, -13.5F, -1.75F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.7500F, 12.5000F, 0.0000F, 0.0F, (float) Math.PI, 0.0F));
        PartDefinition left_armBone = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.5000F, 11.0000F, 1.7500F));
        PartDefinition left_armVisual = left_armBone.addOrReplaceChild("visual", CubeListBuilder.create().texOffs(12, 39).addBox(4.0F, -13.5F, -1.75F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5000F, 12.5000F, 0.0000F, 0.0F, (float) Math.PI, 0.0F));
        PartDefinition right_legBone = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.5000F, 18.0000F, 1.5000F));
        PartDefinition right_legVisual = right_legBone.addOrReplaceChild("visual", CubeListBuilder.create().texOffs(24, 39).addBox(-1.5F, -4.0F, -2.5F, 3.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-1.5F, 1.0F, -2.5F, 3.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0000F, 4.0000F, 0.0000F, 0.0F, (float) Math.PI, 0.0F));
        PartDefinition left_legBone = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2.0000F, 18.0000F, 1.5000F));
        PartDefinition left_legVisual = left_legBone.addOrReplaceChild("visual", CubeListBuilder.create().texOffs(38, 39).addBox(-1.5F, -4.0F, -2.5F, 3.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(36, 7).addBox(-1.5F, 1.0F, -2.5F, 3.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0000F, 4.0000F, 0.0000F, 0.0F, (float) Math.PI, 0.0F));
        PartDefinition tail = bodyVisual.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.25F));
        PartDefinition cube_r1 = tail.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(52, 18).addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.6609F, -0.4244F, -6.274F, -0.0945F, 0.147F, 0.9966F));
        PartDefinition cube_r2 = tail.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(22, 32).addBox(-1.25F, -4.25F, -0.25F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.25F, -1.884F, -6.782F, -0.1745F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
