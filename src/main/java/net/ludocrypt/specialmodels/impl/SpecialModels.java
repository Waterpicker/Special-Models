package net.ludocrypt.specialmodels.impl;

import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.renderer.ShaderInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.ludocrypt.specialmodels.api.TexturedSpecialModelRenderer;

public class SpecialModels implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("Special-Models");

	@Environment(EnvType.CLIENT)
	public static final Map<SpecialModelRenderer, ShaderInstance> LOADED_SHADERS = Maps.newHashMap();

	@Override
	public void onInitialize() {
		TexturedSpecialModelRenderer.init();
	}

}
