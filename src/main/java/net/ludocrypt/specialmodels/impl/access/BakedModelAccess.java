package net.ludocrypt.specialmodels.impl.access;

import java.util.List;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import net.ludocrypt.specialmodels.api.SpecialModelRenderer;

public interface BakedModelAccess {

	public List<Pair<SpecialModelRenderer, BakedModel>> getModels(@Nullable BlockState state);

	public void addModel(SpecialModelRenderer modelRenderer, @Nullable BlockState state, BakedModel model);

}
