package net.ludocrypt.specialmodels.impl.chunk;

import java.util.Map;
import java.util.stream.Collectors;

import net.ludocrypt.specialmodels.api.SpecialModelRenderer;
import net.minecraft.client.renderer.RenderType;

public class SpecialBufferBuilderStorage {

	private final Map<SpecialModelRenderer, SpecialBufferBuilder> specialModelBuffers = SpecialModelRenderer.SPECIAL_MODEL_RENDERER
		.entrySet()
		.stream()
		.collect(Collectors
			.toMap(Map.Entry::getValue,
				entry -> new SpecialBufferBuilder(RenderType.solid().bufferSize())));

	public SpecialBufferBuilder get(SpecialModelRenderer renderer) {
		return this.specialModelBuffers.get(renderer);
	}

	public void clear() {
		this.specialModelBuffers.values().forEach(SpecialBufferBuilder::clear);
	}

	public void reset() {
		this.specialModelBuffers.values().forEach(SpecialBufferBuilder::discard);
	}

	public Map<SpecialModelRenderer, SpecialBufferBuilder> getSpecialModelBuffers() {
		return specialModelBuffers;
	}

}
