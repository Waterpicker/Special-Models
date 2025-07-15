package net.ludocrypt.specialmodels.impl.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;

@Mixin(VertexBuffer.class)
public interface VertexBufferAccessor {

	@Accessor
	int getIndexCount();

	@Accessor
	void setIndexCount(int indexCount);

	@Accessor
	Mode getMode();

	@Accessor
	void setMode(Mode drawMode);

	@Accessor
	Usage getUsage();

	@Mutable
	@Accessor
	void setUsage(Usage usage);

	@Accessor
	int getVertexBufferId();

	@Accessor
	void setVertexBufferId(int vertexBufferId);

	@Accessor
	int getIndexBufferId();

	@Accessor
	void setIndexBufferId(int indexBufferId);

	@Accessor
	VertexFormat getFormat();

	@Accessor
	void setFormat(VertexFormat vertexFormat);

	@Accessor
	AutoStorageIndexBuffer getSequentialIndices();

	@Accessor
	void setSequentialIndices(AutoStorageIndexBuffer sequentialIndices);

	@Accessor
	IndexType getIndexType();

	@Accessor
	void setIndexType(IndexType indexType);

}
