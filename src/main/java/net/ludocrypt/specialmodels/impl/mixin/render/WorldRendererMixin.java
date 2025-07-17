package net.ludocrypt.specialmodels.impl.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.ludocrypt.specialmodels.impl.access.WorldChunkBuilderAccess;
import net.ludocrypt.specialmodels.impl.access.WorldRendererAccess;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder.BuiltChunk;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder.ChunkInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class WorldRendererMixin implements WorldRendererAccess, WorldChunkBuilderAccess {

	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private ClientLevel level;

	@Shadow
	@Nullable
	private RenderTarget translucentTarget;

	@Unique
	private double lastSpecialSortX;
	@Unique
	private double lastSpecialSortY;
	@Unique
	private double lastSpecialSortZ;

	@Override
	public void render(PoseStack matrices, Matrix4f positionMatrix, float tickDelta, Camera camera, boolean outside) {

		ObjectListIterator<ChunkInfo> chunkInfos = this
			.getSpecialChunkInfoList()
			.listIterator(this.getSpecialChunkInfoList().size());

		while (chunkInfos.hasPrevious()) {
			ChunkInfo chunkInfo = chunkInfos.previous();
			BuiltChunk builtChunk = chunkInfo.chunk;
			builtChunk.getSpecialModelBuffers().forEach((modelRenderer, vertexBuffer) -> {

				if (modelRenderer.performOutside == outside) {

					if (builtChunk.getData().renderedBuffers.containsKey(modelRenderer)) {

						specialModels$renderBuffer(matrices, tickDelta, camera, positionMatrix, modelRenderer, vertexBuffer,
							builtChunk.getOrigin().immutable());

					}

				}

			});
		}

	}

	@Unique
	public void specialModels$renderBuffer(PoseStack matrices, float tickDelta, Camera camera, Matrix4f positionMatrix,
			SpecialModelRenderer modelRenderer, VertexBuffer vertexBuffer, BlockPos origin) {
		ShaderInstance shader = modelRenderer
			.getShaderProgram(matrices, tickDelta, camera, positionMatrix, modelRenderer, vertexBuffer, origin);

		if (shader != null && ((VertexBufferAccessor) vertexBuffer).getIndexCount() > 0) {

			this.minecraft.getProfiler().push("translucent_sort");
			double d = camera.getPosition().x() - this.lastSpecialSortX;
			double e = camera.getPosition().y() - this.lastSpecialSortY;
			double f = camera.getPosition().z() - this.lastSpecialSortZ;

			if (d * d + e * e + f * f > 1.0) {
				int i = SectionPos.posToSectionCoord(camera.getPosition().x());
				int j = SectionPos.posToSectionCoord(camera.getPosition().y());
				int k = SectionPos.posToSectionCoord(camera.getPosition().z());
				boolean bl = i != SectionPos.posToSectionCoord(this.lastSpecialSortX) ||
							 k != SectionPos.posToSectionCoord(this.lastSpecialSortZ) ||
							 j != SectionPos.posToSectionCoord(this.lastSpecialSortY);
				this.lastSpecialSortX = camera.getPosition().x();
				this.lastSpecialSortY = camera.getPosition().y();
				this.lastSpecialSortZ = camera.getPosition().z();
				int l = 0;

				for (ChunkInfo chunkInfo : this.getSpecialChunkInfoList()) {

					if (l < 15 && (bl || chunkInfo.isAxisAlignedWith(i, j, k)) && chunkInfo.chunk
						.scheduleSort(modelRenderer, this.getSpecialChunkBuilder())) {
						++l;
					}

				}

			}

			this.minecraft.getProfiler().pop();

			RenderSystem.depthMask(true);
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem
				.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.polygonOffset(3.0F, 3.0F);
			RenderSystem.enablePolygonOffset();
			RenderSystem.setShader(() -> shader);
			minecraft.gameRenderer.lightTexture().turnOnLightLayer();
			vertexBuffer.bind();
			Matrix4f viewMatrix = modelRenderer.viewMatrix(new Matrix4f(matrices.last().pose()));
			Matrix4f projectionMatrix = modelRenderer.positionMatrix(new Matrix4f(positionMatrix));
			modelRenderer
				.setup(matrices, new Matrix4f(viewMatrix), new Matrix4f(projectionMatrix), tickDelta, shader, origin);

			if (origin != null) {

				if (shader.CHUNK_OFFSET != null) {
					BlockPos blockPos = origin;
					float vx = (float) (blockPos.getX() - camera.getPosition().x());
					float vy = (float) (blockPos.getY() - camera.getPosition().y());
					float vz = (float) (blockPos.getZ() - camera.getPosition().z());
					shader.CHUNK_OFFSET.set(vx, vy, vz);
				}

			}

			vertexBuffer.drawWithShader(viewMatrix, projectionMatrix, shader);

			if (shader.CHUNK_OFFSET != null) {
				shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
			}

			VertexBuffer.unbind();
			minecraft.gameRenderer.lightTexture().turnOffLightLayer();
			RenderSystem.polygonOffset(0.0F, 0.0F);
			RenderSystem.disablePolygonOffset();
			RenderSystem.disableBlend();
		}

	}
}
