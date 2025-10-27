package net.ludocrypt.specialmodels.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.compress.archivers.sevenz.CLI;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexBuffer;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.ludocrypt.specialmodels.impl.SpecialModels;
import net.ludocrypt.specialmodels.impl.render.MutableQuad;
import net.ludocrypt.specialmodels.impl.render.Vec4b;

public abstract class SpecialModelRenderer {

	public static final ResourceKey<Registry<SpecialModelRenderer>> SPECIAL_MODEL_RENDERER_KEY = ResourceKey
		.createRegistryKey(ResourceLocation.parse("limlib/special_model_renderer"));

	public static final MappedRegistry<SpecialModelRenderer> SPECIAL_MODEL_RENDERER = FabricRegistryBuilder
		.createDefaulted(SPECIAL_MODEL_RENDERER_KEY, ResourceLocation.fromNamespaceAndPath("specialmodels", "textured"))
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	public final boolean performOutside;

	public SpecialModelRenderer() {
		this.performOutside = true;
	}

	public SpecialModelRenderer(boolean performOutside) {
		this.performOutside = performOutside;
	}

	@Environment(EnvType.CLIENT)
	public abstract void setup(PoseStack matrices, Matrix4f viewMatrix, Matrix4f positionMatrix, float tickDelta,
							   ShaderInstance shader, BlockPos chunkOrigin);

	@Environment(EnvType.CLIENT)
	public MutableQuad modifyQuad(RenderChunkRegion chunkRenderRegion, BlockPos pos, BlockState state, BakedModel model,
								  BakedQuad quadIn, long modelSeed, MutableQuad quad) {
		return quad;
	}

	@Environment(EnvType.CLIENT)
	public Matrix4f positionMatrix(Matrix4f in) {
		return in;
	}

	@Environment(EnvType.CLIENT)
	public Matrix4f viewMatrix(Matrix4f in) {
		return in;
	}

	@Environment(EnvType.CLIENT)
	public Vec4b appendState(RenderChunkRegion chunkRenderRegion, BlockPos pos, BlockState state, BakedModel model,
			long modelSeed) {
		return new Vec4b(0, 0, 0, 0);
	}

	@Environment(EnvType.CLIENT)
	public ShaderInstance getShaderProgram(PoseStack matrices, float tickDelta, Camera camera, Matrix4f positionMatrix,
										  SpecialModelRenderer modelRenderer, VertexBuffer vertexBuffer, BlockPos origin) {
		return SpecialModels.LOADED_SHADERS.get(this);
	}

}
