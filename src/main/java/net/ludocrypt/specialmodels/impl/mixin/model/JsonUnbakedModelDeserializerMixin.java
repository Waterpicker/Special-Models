package net.ludocrypt.specialmodels.impl.mixin.model;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.ludocrypt.specialmodels.impl.access.UnbakedModelAccess;

@Mixin(BlockModel.Deserializer.class)
public abstract class JsonUnbakedModelDeserializerMixin {

	@Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockModel;", at = @At("RETURN"), cancellable = true)
	private void specialModels$deserialize(JsonElement jsonElement, Type type,
			JsonDeserializationContext jsonDeserializationContext, CallbackInfoReturnable<BlockModel> ci) {
		Map<SpecialModelRenderer, ResourceLocation> map = Maps.newHashMap();
		JsonObject jsonObject = jsonElement.getAsJsonObject();

		if (jsonObject.has("specialmodels")) {
			JsonObject limlibExtra = jsonObject.get("specialmodels").getAsJsonObject();

			for (Entry<String, JsonElement> entry : limlibExtra.entrySet()) {

				if (SpecialModelRenderer.SPECIAL_MODEL_RENDERER
					.containsKey(ResourceKey.create(SpecialModelRenderer.SPECIAL_MODEL_RENDERER_KEY, new ResourceLocation(entry.getKey())))) {
					map
						.put(SpecialModelRenderer.SPECIAL_MODEL_RENDERER.get(new ResourceLocation(entry.getKey())),
							new ResourceLocation(entry.getValue().getAsString()));
				}

			}

		}

		((UnbakedModelAccess) ci.getReturnValue()).getSubModels().putAll(map);
	}

}
