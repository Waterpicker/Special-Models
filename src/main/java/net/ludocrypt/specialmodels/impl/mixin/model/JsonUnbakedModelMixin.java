package net.ludocrypt.specialmodels.impl.mixin.model;

import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Maps;

import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.ludocrypt.specialmodels.impl.access.BakedModelAccess;
import net.ludocrypt.specialmodels.impl.access.UnbakedModelAccess;

@Mixin(BlockModel.class)
public abstract class JsonUnbakedModelMixin implements UnbakedModelAccess {

	@Shadow
	@Final
	private static Logger LOGGER;
	@Unique
	private Map<SpecialModelRenderer, ResourceLocation> subModels = Maps.newHashMap();

	@Inject(method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/resources/model/BakedModel;", at = @At("RETURN"), cancellable = true)
	private void specialModels$bake(ModelBaker loader, BlockModel parent, Function<Material, TextureAtlasSprite> textureGetter,
									ModelState settings, ResourceLocation id, boolean hasDepth, CallbackInfoReturnable<BakedModel> ci) {
		this.getSubModels().forEach((modelRenderer, modelId) -> {

			if (!modelId.equals(id)) {
				UnbakedModel model = loader.getModel(modelId);
				model.resolveParents(loader::getModel);
				BakedModel bakedModel = model.bake(loader, textureGetter, settings, modelId);
				((BakedModelAccess) ci.getReturnValue()).addModel(modelRenderer, null, bakedModel);
			} else {
				LOGGER.warn("Model '{}' caught in chain! Renderer '{}' caught model '{}'", id, modelRenderer, modelId);
			}

		});
	}

	@Override
	public Map<SpecialModelRenderer, ResourceLocation> getSubModels() {
		return subModels;
	}

}
