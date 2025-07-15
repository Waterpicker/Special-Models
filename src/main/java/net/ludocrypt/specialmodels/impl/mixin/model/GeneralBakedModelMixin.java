package net.ludocrypt.specialmodels.impl.mixin.model;

import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BuiltInModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.ludocrypt.specialmodels.impl.access.BakedModelAccess;

@Mixin(value = { SimpleBakedModel.class, BuiltInModel.class, WeightedBakedModel.class, ForwardingBakedModel.class })
public class GeneralBakedModelMixin implements BakedModelAccess {

	private final Map<BlockState, List<Pair<SpecialModelRenderer, BakedModel>>> modelsMap = Maps.newHashMap();
	private List<Pair<SpecialModelRenderer, BakedModel>> defaultModels = Lists.newArrayList();

	@Override
	public List<Pair<SpecialModelRenderer, BakedModel>> getModels(@Nullable BlockState state) {
		return modelsMap.getOrDefault(state, defaultModels);
	}

	@Override
	public void addModel(SpecialModelRenderer modelRenderer, @Nullable BlockState state, BakedModel model) {

		if (state == null) {
			defaultModels.add(Pair.of(modelRenderer, model));
		} else {
			List<Pair<SpecialModelRenderer, BakedModel>> list = modelsMap.get(state);

			if (list == null) {
				list = Lists.newArrayList();
				modelsMap.put(state, list);
			}

			list.add(Pair.of(modelRenderer, model));
		}

	}

}
