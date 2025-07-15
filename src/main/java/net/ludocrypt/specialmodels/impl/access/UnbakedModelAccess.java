package net.ludocrypt.specialmodels.impl.access;

import java.util.Map;

import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;

public interface UnbakedModelAccess {

	public Map<SpecialModelRenderer, ResourceLocation> getSubModels();

}
