package net.ludocrypt.specialmodels.impl.mixin.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.ludocrypt.specialmodels.impl.access.BakedModelAccess;

@Mixin(MultiPartBakedModel.class)
public class MultipartBakedModelMixin implements BakedModelAccess {

	@Shadow
	@Final
	private List<org.apache.commons.lang3.tuple.Pair<Predicate<BlockState>, BakedModel>> selectors;
	@Unique
	private final Map<BlockState, List<Pair<SpecialModelRenderer, BakedModel>>> subModelCache = new Reference2ReferenceOpenHashMap<>();

	@Override
	public List<Pair<SpecialModelRenderer, BakedModel>> getModels(@Nullable BlockState state) {

		if (state == null) {
			return Lists.newArrayList();
		}

		List<Pair<SpecialModelRenderer, BakedModel>> models;

		synchronized (this.subModelCache) {
			models = this.subModelCache.get(state);

			if (models == null) {
				models = new ArrayList<>(this.selectors.size());

				for (org.apache.commons.lang3.tuple.Pair<Predicate<BlockState>, BakedModel> pair : this.selectors) {

					if ((pair.getLeft()).test(state)) {
						models.addAll(((BakedModelAccess) pair.getRight()).getModels(state));
					}

				}

				this.subModelCache.put(state, models);
			}

		}

		return models;
	}

	@Override
	public void addModel(SpecialModelRenderer modelRenderer, @Nullable BlockState state, BakedModel model) {
	}

}
