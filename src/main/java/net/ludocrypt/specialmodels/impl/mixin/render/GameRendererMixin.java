package net.ludocrypt.specialmodels.impl.mixin.render;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.mojang.datafixers.util.Pair;

import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.ludocrypt.specialmodels.impl.SpecialModels;
import net.ludocrypt.specialmodels.impl.render.SpecialVertexFormats;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

	@Inject(method = "reloadShaders", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 58, shift = Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
	private void specialModels$loadShaders(ResourceProvider manager, CallbackInfo ci, List<Program> list,
										   List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list2) {
		SpecialModels.LOADED_SHADERS.clear();
		SpecialModelRenderer.SPECIAL_MODEL_RENDERER
			.entrySet()
			.stream()
			.map(Entry::getKey)
			.map(ResourceKey::location)
			.forEach((id) -> {

				SpecialModelRenderer renderer = SpecialModelRenderer.SPECIAL_MODEL_RENDERER.get(id);

				if (!renderer.performOutside) {
					return;
				}

				try {
					list2
						.add(Pair
							.of(new ShaderInstance(manager, "rendertype_" + id.getNamespace() + "_" + id.getPath(),
								SpecialVertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL_STATE),
								(shader) -> SpecialModels.LOADED_SHADERS.put(renderer, shader)));
				} catch (IOException e) {
					SpecialModels.LOGGER.error("Could not reload shader: {}", id);
					e.printStackTrace();

					try {
						list2
							.add(Pair
								.of(new ShaderInstance(manager, "rendertype_specialmodels_textured",
									SpecialVertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL_STATE),
									(shader) -> SpecialModels.LOADED_SHADERS.put(renderer, shader)));
					} catch (IOException e2) {
						list2.forEach((pair) -> pair.getFirst().close());
						e2.printStackTrace();
						throw new RuntimeException();
					}

				}

			});
	}

}
