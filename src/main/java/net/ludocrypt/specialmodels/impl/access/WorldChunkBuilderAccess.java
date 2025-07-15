package net.ludocrypt.specialmodels.impl.access;

import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.ludocrypt.specialmodels.impl.chunk.SpecialBufferBuilderStorage;
import net.ludocrypt.specialmodels.impl.chunk.SpecialBuiltChunkStorage;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder;

public interface WorldChunkBuilderAccess {

	public SpecialChunkBuilder getSpecialChunkBuilder();

	public Future<?> getLastFullSpecialBuiltChunkUpdate();

	public BlockingQueue<SpecialChunkBuilder.BuiltChunk> getRecentlyCompiledSpecialChunks();

	public AtomicReference<SpecialChunkBuilder.RenderableChunks> getRenderableSpecialChunks();

	public ObjectArrayList<SpecialChunkBuilder.ChunkInfo> getSpecialChunkInfoList();

	public SpecialBuiltChunkStorage getSpecialChunks();

	public SpecialBufferBuilderStorage getSpecialBufferBuilderStorage();

	public boolean shouldNeedsFullSpecialBuiltChunkUpdate();

	public AtomicBoolean shouldNeedsSpecialFrustumUpdate();

	public AtomicLong getNextFullSpecialUpdateMilliseconds();

	public void setWorldSpecial(ClientLevel world);

	public void reloadSpecial();

	public void setupSpecialTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

	public void addSpecialChunksToBuild(Camera camera, Queue<SpecialChunkBuilder.ChunkInfo> chunkInfoQueue);

	public void addSpecialBuiltChunk(SpecialChunkBuilder.BuiltChunk builtChunk);

	public void updateSpecialBuiltChunks(LinkedHashSet<SpecialChunkBuilder.ChunkInfo> builtChunks,
			SpecialChunkBuilder.ChunkInfoListMap builtChunkMap, Vec3 cameraPos,
			Queue<SpecialChunkBuilder.ChunkInfo> chunksToBuild, boolean chunkCullingEnabled);

	@Nullable
	public SpecialChunkBuilder.BuiltChunk getAdjacentSpecialChunk(BlockPos pos, SpecialChunkBuilder.BuiltChunk chunk,
																  Direction direction);

	public boolean isSpecialChunkNearMaxViewDistance(BlockPos blockPos, SpecialChunkBuilder.BuiltChunk builtChunk);

	public void applySpecialFrustum(Frustum frustum);

	public void findSpecialChunksToRebuild(Camera camera);

}
