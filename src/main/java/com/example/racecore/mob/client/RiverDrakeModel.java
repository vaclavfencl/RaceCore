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

/** Original River Dragon Blockbench geometry. No vanilla Wolf model. */
public final class RiverDrakeModel extends EntityModel<RiverDrakeRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(RaceCore.MODID, "river_drake"), "main");

    private final KeyframeAnimation idle;
    private final KeyframeAnimation walk;
    private final KeyframeAnimation bite;
    private final KeyframeAnimation tailswing;

    public RiverDrakeModel(ModelPart root) {
        super(root);
        this.idle = RiverDrakeAnimations.idle.bake(root);
        this.walk = RiverDrakeAnimations.walk.bake(root);
        this.bite = RiverDrakeAnimations.bite.bake(root);
        this.tailswing = RiverDrakeAnimations.tailswing.bake(root);
    }

    @Override
    public void setupAnim(RiverDrakeRenderState state) {
        super.setupAnim(state); // Restore original rest pose before each animation.

        if (state.spinAnimation.isStarted()) {
            // The swing keyframe lands at 1.0833 s, but existing ability damage
            // lands at 20 ticks (1.0 s): playback 1.0833x aligns those moments.
            this.tailswing.apply(state.spinAnimation, state.ageInTicks, 1.0833F);
        } else if (state.biteAnimation.isStarted()) {
            this.bite.apply(state.biteAnimation, state.ageInTicks);
        } else if (state.walking) {
            // Use the complete hand-authored loop instead of vanilla Wolf gait.
            this.walk.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        } else {
            this.idle.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        }
    }

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition dragon = partdefinition.addOrReplaceChild("dragon", CubeListBuilder.create(), PartPose.offset(0.0F, 18.3214F, 0.6429F));

		PartDefinition head = dragon.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 27).addBox(-1.0F, -1.0521F, -6.0193F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 28).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 34).addBox(-1.0F, -1.25F, -2.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, -0.3214F, -5.1429F));

		PartDefinition bot = head.addOrReplaceChild("bot", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0057F, -2.6861F));

		PartDefinition cube_r1 = bot.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(14, 21).addBox(-1.0F, 0.0027F, -4.8235F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 1.5F, 0.3054F, 0.0F, 0.0F));

		PartDefinition body = dragon.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -9.25F, -6.0F, 4.0F, 4.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.6786F, 0.8571F));

		PartDefinition legfl = dragon.addOrReplaceChild("legfl", CubeListBuilder.create().texOffs(28, 0).addBox(-1.0F, -0.75F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(28, 24).addBox(-1.0F, 3.0F, -2.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 1.6786F, -3.1429F));

		PartDefinition legfr = dragon.addOrReplaceChild("legfr", CubeListBuilder.create().texOffs(28, 6).addBox(-1.0F, -0.75F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(28, 28).addBox(-1.0F, 3.0F, -2.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 1.6786F, -3.1429F));

		PartDefinition legbl = dragon.addOrReplaceChild("legbl", CubeListBuilder.create().texOffs(28, 12).addBox(-1.0F, -0.75F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(14, 33).addBox(-1.0F, 3.0F, -2.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 1.6786F, 2.8571F));

		PartDefinition legbr = dragon.addOrReplaceChild("legbr", CubeListBuilder.create().texOffs(28, 18).addBox(-1.0F, -0.75F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(28, 32).addBox(-1.0F, 3.0F, -2.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 1.6786F, 2.8571F));

		PartDefinition tail = dragon.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.0714F, 4.8571F));

		PartDefinition mid = tail.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(14, 14).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.25F)), PartPose.offset(0.0F, 0.0F, 4.0F));

		PartDefinition tip = mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 21).addBox(-1.0F, -1.0F, -0.75F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.5F)), PartPose.offset(0.0F, 0.0F, 3.75F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}
}
