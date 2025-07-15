package net.ludocrypt.specialmodels.impl.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.ludocrypt.specialmodels.impl.access.WorldChunkBuilderAccess;
import net.ludocrypt.specialmodels.impl.access.WorldRendererAccess;
import net.ludocrypt.specialmodels.impl.bridge.IrisBridge;

@Mixin(value = LevelRenderer.class, priority = 1050)
public abstract class WorldRendererAfterMixin implements WorldRendererAccess, WorldChunkBuilderAccess {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private Frustum cullingFrustum;

	@Inject(method = "renderLevel", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
	private void specialModels$render$clear(PoseStack matrices, float tickDelta, long limitTime,
											boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
											LightTexture lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {

		if (IrisBridge.IRIS_LOADED) {

			if (IrisBridge.areShadersInUse()) {

				this.setupSpecialTerrain(camera, this.cullingFrustum, false, this.minecraft.player.isSpectator());
				this.findSpecialChunksToRebuild(camera);
				this.render(matrices, positionMatrix, tickDelta, camera, true);
			}

		}

	}

	@Shadow
	abstract void captureFrustum(Matrix4f matrix4f, Matrix4f matrix4f2, double d, double e, double f, Frustum frustum);

}
