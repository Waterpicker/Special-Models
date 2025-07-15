package net.ludocrypt.specialmodels.impl.mixin.render;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.ludocrypt.specialmodels.impl.access.WorldChunkBuilderAccess;
import net.ludocrypt.specialmodels.impl.chunk.SpecialBufferBuilderStorage;
import net.ludocrypt.specialmodels.impl.chunk.SpecialBuiltChunkStorage;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder.BuiltChunk;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder.ChunkData;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder.ChunkInfo;
import net.ludocrypt.specialmodels.impl.chunk.SpecialChunkBuilder.RenderableChunks;

@Mixin(LevelRenderer.class)
public class WorldRendererChunkMixin implements WorldChunkBuilderAccess {

	@Shadow
	private ClientLevel level;
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private int lastViewDistance;
	@Shadow
	@Final
	private static double CEILED_SECTION_DIAGONAL;
	@Unique
	private SpecialChunkBuilder specialChunkBuilder;
	@Unique
	private Future<?> lastFullSpecialBuiltChunkUpdate;
	@Unique
	private final BlockingQueue<SpecialChunkBuilder.BuiltChunk> recentlyCompiledSpecialChunks = new LinkedBlockingQueue<BuiltChunk>();
	@Unique
	private final AtomicReference<SpecialChunkBuilder.RenderableChunks> renderableSpecialChunks = new AtomicReference<RenderableChunks>();
	@Unique
	private final ObjectArrayList<SpecialChunkBuilder.ChunkInfo> specialChunkInfoList = new ObjectArrayList<>(10000);
	@Unique
	private SpecialBuiltChunkStorage specialChunks;
	@Unique
	private SpecialBufferBuilderStorage specialBufferBuilderStorage = new SpecialBufferBuilderStorage();
	@Unique
	private boolean needsFullSpecialBuiltChunkUpdate = true;
	@Unique
	private final AtomicBoolean needsSpecialFrustumUpdate = new AtomicBoolean(false);
	@Unique
	private final AtomicLong nextFullSpecialUpdateMilliseconds = new AtomicLong(0L);
	@Unique
	private int cameraSpecialChunkX = Integer.MIN_VALUE;
	@Unique
	private int cameraSpecialChunkY = Integer.MIN_VALUE;
	@Unique
	private int cameraSpecialChunkZ = Integer.MIN_VALUE;
	@Unique
	private double lastSpecialCameraX = Double.MIN_VALUE;
	@Unique
	private double lastSpecialCameraY = Double.MIN_VALUE;
	@Unique
	private double lastSpecialCameraZ = Double.MIN_VALUE;
	@Unique
	private double lastSpecialCameraPitch = Double.MIN_VALUE;
	@Unique
	private double lastSpecialCameraYaw = Double.MIN_VALUE;

	@Inject(method = "setLevel", at = @At("TAIL"))
	private void specialModels$setlevel(ClientLevel level, CallbackInfo ci) {
		this.setWorldSpecial(level);
	}

	@Inject(method = "allChanged", at = @At("HEAD"))
	private void specialModels$reload(CallbackInfo ci) {
		this.reloadSpecial();
	}

	@Inject(method = "blockChanged", at = @At("TAIL"))
	private void specialModels$updateBlock(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState,
										   int flags, CallbackInfo ci) {
		this.scheduleSpecialSectionRender(pos, (flags & 8) != 0);
	}

	@Inject(method = "setSectionDirty*", at = @At("TAIL"))
	private void specialModels$scheduleBlockRender(int x, int y, int z, CallbackInfo ci) {
		this.scheduleSpecialBlockRender(x, y, z);
	}

	@Inject(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("TAIL"))
	private void specialModels$scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated,
			CallbackInfo ci) {

		if (this.minecraft.getModelManager().requiresRender(old, updated)) {
			this.scheduleSpecialBlockRenders(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
		}

	}

	@Inject(method = "needsUpdate", at = @At("TAIL"))
	private void specialModels$scheduleBlockRerenderIfNeeded(CallbackInfo ci) {
		this.needsFullSpecialBuiltChunkUpdate = true;
	}

	private void scheduleSpecialSectionRender(BlockPos pos, boolean important) {

		for (int i = pos.getZ() - 1; i <= pos.getZ() + 1; ++i) {

			for (int j = pos.getX() - 1; j <= pos.getX() + 1; ++j) {

				for (int k = pos.getY() - 1; k <= pos.getY() + 1; ++k) {
					this
						.scheduleSpecialChunkRender(SectionPos.posToSectionCoord(j), SectionPos.posToSectionCoord(k),
							SectionPos.posToSectionCoord(i), important);
				}

			}

		}

	}

	public void scheduleSpecialBlockRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

		for (int i = minZ - 1; i <= maxZ + 1; ++i) {

			for (int j = minX - 1; j <= maxX + 1; ++j) {

				for (int k = minY - 1; k <= maxY + 1; ++k) {
					this
						.scheduleSpecialBlockRender(SectionPos.posToSectionCoord(j), SectionPos.posToSectionCoord(k),
							SectionPos.posToSectionCoord(i));
				}

			}

		}

	}

	public void scheduleSpecialBlockRenders(int x, int y, int z) {

		for (int i = z - 1; i <= z + 1; ++i) {

			for (int j = x - 1; j <= x + 1; ++j) {

				for (int k = y - 1; k <= y + 1; ++k) {
					this.scheduleSpecialBlockRender(j, k, i);
				}

			}

		}

	}

	public void scheduleSpecialBlockRender(int x, int y, int z) {
		this.scheduleSpecialChunkRender(x, y, z, false);
	}

	private void scheduleSpecialChunkRender(int x, int y, int z, boolean important) {
		this.specialChunks.scheduleRebuild(x, y, z, important);
	}

	@Override
	public void setWorldSpecial(ClientLevel level) {
		if (level == null) {

			if (this.specialChunks != null) {
				this.specialChunks.clear();
				this.specialChunks = null;
			}

			if (this.specialChunkBuilder != null) {
				this.specialChunkBuilder.stop();
			}

			this.specialChunkBuilder = null;
			this.renderableSpecialChunks.set(null);
			this.specialChunkInfoList.clear();
		} else {
			this.reloadSpecial();
		}

	}

	@Override
	public void reloadSpecial() {

		if (this.level != null) {

			if (this.specialChunkBuilder == null) {
				this.specialChunkBuilder = new SpecialChunkBuilder(this.level, ((LevelRenderer) (Object) this),
					Util.backgroundExecutor(), this.minecraft.is64Bit(), this.specialBufferBuilderStorage);
			} else {
				this.specialChunkBuilder.setWorld(this.level);
			}

			this.needsFullSpecialBuiltChunkUpdate = true;
			this.recentlyCompiledSpecialChunks.clear();
			ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
			this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();

			if (this.specialChunks != null) {
				this.specialChunks.clear();
			}

			this.specialChunkBuilder.reset();
			this.specialChunks = new SpecialBuiltChunkStorage(this.specialChunkBuilder, this.level,
				this.minecraft.options.getEffectiveRenderDistance(), ((LevelRenderer) (Object) this));

			if (this.lastFullSpecialBuiltChunkUpdate != null) {

				try {
					this.lastFullSpecialBuiltChunkUpdate.get();
					this.lastFullSpecialBuiltChunkUpdate = null;
				} catch (Exception var3) {
				}

			}

			this.renderableSpecialChunks.set(new SpecialChunkBuilder.RenderableChunks(this.specialChunks.chunks.length));
			this.specialChunkInfoList.clear();
			Entity entity = this.minecraft.getCameraEntity();

			if (entity != null) {
				this.specialChunks.updateCameraPosition(entity.getX(), entity.getZ());
			}

		}

	}

	@Override
	public void setupSpecialTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator) {
		Vec3 Vec3 = camera.getPosition();

		if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
			this.reloadSpecial();
		}

		this.level.getProfiler().push("camera");
		double d = this.minecraft.player.getX();
		double e = this.minecraft.player.getY();
		double f = this.minecraft.player.getZ();
		int i = SectionPos.posToSectionCoord(d);
		int j = SectionPos.posToSectionCoord(e);
		int k = SectionPos.posToSectionCoord(f);

		if (this.cameraSpecialChunkX != i || this.cameraSpecialChunkY != j || this.cameraSpecialChunkZ != k) {
			this.cameraSpecialChunkX = i;
			this.cameraSpecialChunkY = j;
			this.cameraSpecialChunkZ = k;
			this.specialChunks.updateCameraPosition(d, f);
		}

		this.specialChunkBuilder.setCameraPosition(Vec3);
		this.level.getProfiler().popPush("cull");
		this.minecraft.getProfiler().popPush("culling");
		BlockPos blockPos = camera.getBlockPosition();
		double g = Math.floor(Vec3.x / 8.0);
		double h = Math.floor(Vec3.y / 8.0);
		double l = Math.floor(Vec3.z / 8.0);
		this.needsFullSpecialBuiltChunkUpdate = this.needsFullSpecialBuiltChunkUpdate || g != this.lastSpecialCameraX || h != this.lastSpecialCameraY || l != this.lastSpecialCameraZ;
		this.nextFullSpecialUpdateMilliseconds.updateAndGet(lx -> {

			if (lx > 0L && System.currentTimeMillis() > lx) {
				this.needsFullSpecialBuiltChunkUpdate = true;
				return 0L;
			} else {
				return lx;
			}

		});
		this.lastSpecialCameraX = g;
		this.lastSpecialCameraY = h;
		this.lastSpecialCameraZ = l;
		this.minecraft.getProfiler().popPush("update");
		boolean bl = this.minecraft.smartCull;

		if (spectator && this.level.getBlockState(blockPos).isSolidRender(this.level, blockPos)) {
			bl = false;
		}

		if (!hasForcedFrustum) {

			if (this.needsFullSpecialBuiltChunkUpdate && (this.lastFullSpecialBuiltChunkUpdate == null || this.lastFullSpecialBuiltChunkUpdate
				.isDone())) {
				this.minecraft.getProfiler().push("full_update_schedule");
				this.needsFullSpecialBuiltChunkUpdate = false;
				boolean bl2 = bl;
				this.lastFullSpecialBuiltChunkUpdate = Util.backgroundExecutor().submit(() -> {
					Queue<SpecialChunkBuilder.ChunkInfo> queue = Queues.<SpecialChunkBuilder.ChunkInfo>newArrayDeque();
					this.addSpecialChunksToBuild(camera, queue);
					SpecialChunkBuilder.RenderableChunks renderableChunksx = new SpecialChunkBuilder.RenderableChunks(
						this.specialChunks.chunks.length);
					this
						.updateSpecialBuiltChunks(renderableChunksx.builtChunks, renderableChunksx.builtChunkMap, Vec3,
							queue, bl2);
					this.renderableSpecialChunks.set(renderableChunksx);
					this.needsSpecialFrustumUpdate.set(true);
				});
				this.minecraft.getProfiler().pop();
			}

			SpecialChunkBuilder.RenderableChunks renderableChunks = (SpecialChunkBuilder.RenderableChunks) this.renderableSpecialChunks
				.get();

			if (!this.recentlyCompiledSpecialChunks.isEmpty()) {
				this.minecraft.getProfiler().push("partial_update");
				Queue<SpecialChunkBuilder.ChunkInfo> queue = Queues.<SpecialChunkBuilder.ChunkInfo>newArrayDeque();

				while (!this.recentlyCompiledSpecialChunks.isEmpty()) {
					SpecialChunkBuilder.BuiltChunk builtChunk = (SpecialChunkBuilder.BuiltChunk) this.recentlyCompiledSpecialChunks
						.poll();
					SpecialChunkBuilder.ChunkInfo chunkInfo = renderableChunks.builtChunkMap.getInfo(builtChunk);

					if (chunkInfo != null && chunkInfo.chunk == builtChunk) {
						queue.add(chunkInfo);
					}

				}

				this
					.updateSpecialBuiltChunks(renderableChunks.builtChunks, renderableChunks.builtChunkMap, Vec3, queue,
						bl);
				this.needsSpecialFrustumUpdate.set(true);
				this.minecraft.getProfiler().pop();
			}

			double m = Math.floor((double) (camera.getXRot() / 2.0F));
			double n = Math.floor((double) (camera.getYRot() / 2.0F));

			if (this.needsSpecialFrustumUpdate
				.compareAndSet(true, false) || m != this.lastSpecialCameraPitch || n != this.lastSpecialCameraYaw) {
				this.applySpecialFrustum(new Frustum(frustum).offsetToFullyIncludeCameraCube(8));
				this.lastSpecialCameraPitch = m;
				this.lastSpecialCameraYaw = n;
			}

		}

		this.minecraft.getProfiler().pop();
	}

	@Override
	public void addSpecialChunksToBuild(Camera camera, Queue<SpecialChunkBuilder.ChunkInfo> chunkInfoQueue) {
		net.minecraft.world.phys.Vec3 vec3 = camera.getPosition();
		BlockPos blockPos = camera.getBlockPosition();
		SpecialChunkBuilder.BuiltChunk builtChunk = this.specialChunks.getRenderedChunk(blockPos);

		if (builtChunk == null) {
			boolean bl = blockPos.getY() > this.level.getMinBuildHeight();
			int j = bl ? this.level.getHeight() - 8 : this.level.getMinBuildHeight() + 8;
			int k = Mth.floor(vec3.x / 16.0) * 16;
			int l = Mth.floor(vec3.z / 16.0) * 16;
			List<SpecialChunkBuilder.ChunkInfo> list = Lists.<SpecialChunkBuilder.ChunkInfo>newArrayList();

			for (int m = -this.lastViewDistance; m <= this.lastViewDistance; ++m) {

				for (int n = -this.lastViewDistance; n <= this.lastViewDistance; ++n) {
					SpecialChunkBuilder.BuiltChunk builtChunk2 = this.specialChunks
						.getRenderedChunk(
							new BlockPos(k + SectionPos.sectionToBlockCoord(m, 8), j, l + SectionPos.sectionToBlockCoord(n, 8)));

					if (builtChunk2 != null) {
						list.add(new SpecialChunkBuilder.ChunkInfo(builtChunk2, null, 0));
					}

				}

			}

			list
				.sort(Comparator
					.comparingDouble(chunkInfo -> blockPos.distSqr(chunkInfo.chunk.getOrigin().offset(8, 8, 8))));
			chunkInfoQueue.addAll(list);
		} else {
			chunkInfoQueue.add(new SpecialChunkBuilder.ChunkInfo(builtChunk, null, 0));
		}

	}

	@Override
	public void addSpecialBuiltChunk(SpecialChunkBuilder.BuiltChunk builtChunk) {
		this.recentlyCompiledSpecialChunks.add(builtChunk);
	}

	@Override
	public void updateSpecialBuiltChunks(LinkedHashSet<ChunkInfo> builtChunks,
										 SpecialChunkBuilder.ChunkInfoListMap builtChunkMap, net.minecraft.world.phys.Vec3 cameraPos, Queue<ChunkInfo> chunksToBuild,
										 boolean chunkCullingEnabled) {
		BlockPos blockPos = new BlockPos(Mth.floor(cameraPos.x / 16.0) * 16,
			Mth.floor(cameraPos.y / 16.0) * 16, Mth.floor(cameraPos.z / 16.0) * 16);
		BlockPos blockPos2 = blockPos.offset(8, 8, 8);
		Entity
			.setViewScale(Mth
				.clamp((double) this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0,
					2.5) * this.minecraft.options.entityDistanceScaling().get());

		while (!chunksToBuild.isEmpty()) {
			ChunkInfo chunkInfo = chunksToBuild.poll();
			SpecialChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;
			builtChunks.add(chunkInfo);
			boolean bl = Math.abs(builtChunk.getOrigin().getX() - blockPos.getX()) > 60 || Math
				.abs(builtChunk.getOrigin().getY() - blockPos.getY()) > 60 || Math
					.abs(builtChunk.getOrigin().getZ() - blockPos.getZ()) > 60;
			Direction[] DIRECTIONS = Direction.values();

			for (Direction direction : DIRECTIONS) {
				SpecialChunkBuilder.BuiltChunk builtChunk2 = this.getAdjacentSpecialChunk(blockPos, builtChunk, direction);

				if (builtChunk2 != null && (!chunkCullingEnabled || !chunkInfo.canCull(direction.getOpposite()))) {

					if (chunkCullingEnabled && chunkInfo.hasAnyDirection()) {
						ChunkData chunkData = builtChunk.getData();
						boolean bl2 = false;

						for (int j = 0; j < DIRECTIONS.length; ++j) {

							if (chunkInfo.hasDirection(j) && chunkData
								.isVisibleThrough(DIRECTIONS[j].getOpposite(), direction)) {
								bl2 = true;
								break;
							}

						}

						if (!bl2) {
							continue;
						}

					}

					if (chunkCullingEnabled && bl) {
						BlockPos blockPos3;
						byte var10001;

						label126: {

							label125: {
								blockPos3 = builtChunk2.getOrigin();

								if (direction.getAxis() == Direction.Axis.X) {

									if (blockPos2.getX() > blockPos3.getX()) {
										break label125;
									}

								} else if (blockPos2.getX() < blockPos3.getX()) {
									break label125;
								}

								var10001 = 0;
								break label126;
							}

							var10001 = 16;
						}

						byte var10002;

						label118: {

							label117: {

								if (direction.getAxis() == Direction.Axis.Y) {

									if (blockPos2.getY() > blockPos3.getY()) {
										break label117;
									}

								} else if (blockPos2.getY() < blockPos3.getY()) {
									break label117;
								}

								var10002 = 0;
								break label118;
							}

							var10002 = 16;
						}

						byte var10003;

						label110: {

							label109: {

								if (direction.getAxis() == Direction.Axis.Z) {

									if (blockPos2.getZ() > blockPos3.getZ()) {
										break label109;
									}

								} else if (blockPos2.getZ() < blockPos3.getZ()) {
									break label109;
								}

								var10003 = 0;
								break label110;
							}

							var10003 = 16;
						}

						BlockPos blockPos4 = blockPos3.offset(var10001, var10002, var10003);
						Vec3 Vec3 = new Vec3((double) blockPos4.getX(), (double) blockPos4.getY(),
							(double) blockPos4.getZ());
						Vec3 Vec32 = cameraPos.subtract(Vec3).normalize().scale(CEILED_SECTION_DIAGONAL);
						boolean bl3 = true;

						while (cameraPos.subtract(Vec3).lengthSqr() > 3600.0) {
							Vec3 = Vec3.add(Vec32);

							if (Vec3.y > (double) this.level.getMaxBuildHeight() || Vec3.y < (double) this.level.getMinBuildHeight()) {
								break;
							}

							SpecialChunkBuilder.BuiltChunk builtChunk3 = this.specialChunks
								.getRenderedChunk(BlockPos.containing(Vec3.x, Vec3.y, Vec3.z));

							if (builtChunk3 == null || builtChunkMap.getInfo(builtChunk3) == null) {
								bl3 = false;
								break;
							}

						}

						if (!bl3) {
							continue;
						}

					}

					ChunkInfo chunkInfo2 = builtChunkMap.getInfo(builtChunk2);

					if (chunkInfo2 != null) {
						chunkInfo2.addDirection(direction);
					} else if (!builtChunk2.shouldBuild()) {

						if (!this.isSpecialChunkNearMaxViewDistance(blockPos, builtChunk)) {
							this.nextFullSpecialUpdateMilliseconds.set(System.currentTimeMillis() + 500L);
						}

					} else {
						ChunkInfo chunkInfo3 = new ChunkInfo(builtChunk2, direction, chunkInfo.propagationLevel + 1);
						chunkInfo3.updateCullingState(chunkInfo.cullingState, direction);
						chunksToBuild.add(chunkInfo3);
						builtChunkMap.setInfo(builtChunk2, chunkInfo3);
					}

				}

			}

		}

	}

	@Nullable
	@Override
	public SpecialChunkBuilder.BuiltChunk getAdjacentSpecialChunk(BlockPos pos, SpecialChunkBuilder.BuiltChunk chunk,
			Direction direction) {
		BlockPos blockPos = chunk.getNeighborPosition(direction);

		if (Mth.abs(pos.getX() - blockPos.getX()) > this.lastViewDistance * 16) {
			return null;
		} else if (Mth.abs(pos.getY() - blockPos.getY()) > this.lastViewDistance * 16 || blockPos.getY() < this.level
			.getMinBuildHeight() || blockPos.getY() >= this.level.getMaxBuildHeight()) {
			return null;
		} else {
			return Mth.abs(pos.getZ() - blockPos.getZ()) > this.lastViewDistance * 16 ? null
					: this.specialChunks.getRenderedChunk(blockPos);
		}

	}

	@Override
	public boolean isSpecialChunkNearMaxViewDistance(BlockPos blockPos, BuiltChunk builtChunk) {
		int i = SectionPos.posToSectionCoord(blockPos.getX());
		int j = SectionPos.posToSectionCoord(blockPos.getZ());
		BlockPos blockPos2 = builtChunk.getOrigin();
		int k = SectionPos.posToSectionCoord(blockPos2.getX());
		int l = SectionPos.posToSectionCoord(blockPos2.getZ());
		return !ChunkMap.isChunkInRange(k, l, i, j, this.lastViewDistance - 2);
	}

	@Override
	public void applySpecialFrustum(Frustum frustum) {

		if (!Minecraft.getInstance().isSameThread()) {
			throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
		} else {
			this.minecraft.getProfiler().push("apply_frustum");
			this.specialChunkInfoList.clear();

			for (SpecialChunkBuilder.ChunkInfo chunkInfo : ((SpecialChunkBuilder.RenderableChunks) this.renderableSpecialChunks
				.get()).builtChunks) {

				if (frustum.isVisible(chunkInfo.chunk.getBoundingBox())) {
					this.specialChunkInfoList.add(chunkInfo);
				}

			}

			this.minecraft.getProfiler().pop();
		}

	}

	@Override
	public void findSpecialChunksToRebuild(Camera camera) {
		this.minecraft.getProfiler().push("populate_chunks_to_compile");
		LevelLightEngine lightingProvider = this.level.getLightEngine();
		RenderRegionCache chunkRenderRegionCache = new RenderRegionCache();
		BlockPos blockPos = camera.getBlockPosition();
		List<SpecialChunkBuilder.BuiltChunk> list = Lists.<SpecialChunkBuilder.BuiltChunk>newArrayList();

		for (SpecialChunkBuilder.ChunkInfo chunkInfo : this.specialChunkInfoList) {
			SpecialChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;
			SectionPos chunkSectionPos = SectionPos.of(builtChunk.getOrigin());

			if (builtChunk.needsRebuild() && lightingProvider.lightOnInSection(chunkSectionPos)) {
				boolean bl = false;

				if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY) {
					BlockPos blockPos2 = builtChunk.getOrigin().offset(8, 8, 8);
					bl = blockPos2.distSqr(blockPos) < 768.0 || builtChunk.needsImportantRebuild();
				} else if (this.minecraft.options
					.prioritizeChunkUpdates()
					.get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
					bl = builtChunk.needsImportantRebuild();
				}

				if (bl) {
					this.minecraft.getProfiler().push("build_near_sync");
					this.specialChunkBuilder.rebuild(builtChunk, chunkRenderRegionCache);
					builtChunk.cancelRebuild();
					this.minecraft.getProfiler().pop();
				} else {
					list.add(builtChunk);
				}

			}

		}

		this.minecraft.getProfiler().popPush("upload");
		this.specialChunkBuilder.upload();
		this.minecraft.getProfiler().popPush("schedule_async_compile");

		for (SpecialChunkBuilder.BuiltChunk builtChunk2 : list) {
			builtChunk2.scheduleRebuild(this.specialChunkBuilder, chunkRenderRegionCache);
			builtChunk2.cancelRebuild();
		}

		this.minecraft.getProfiler().pop();
	}

	@Override
	public SpecialChunkBuilder getSpecialChunkBuilder() {
		return specialChunkBuilder;
	}

	@Override
	public Future<?> getLastFullSpecialBuiltChunkUpdate() {
		return lastFullSpecialBuiltChunkUpdate;
	}

	@Override
	public BlockingQueue<BuiltChunk> getRecentlyCompiledSpecialChunks() {
		return recentlyCompiledSpecialChunks;
	}

	@Override
	public AtomicReference<RenderableChunks> getRenderableSpecialChunks() {
		return renderableSpecialChunks;
	}

	@Override
	public ObjectArrayList<ChunkInfo> getSpecialChunkInfoList() {
		return specialChunkInfoList;
	}

	@Override
	public SpecialBuiltChunkStorage getSpecialChunks() {
		return specialChunks;
	}

	@Override
	public SpecialBufferBuilderStorage getSpecialBufferBuilderStorage() {
		return specialBufferBuilderStorage;
	}

	@Override
	public boolean shouldNeedsFullSpecialBuiltChunkUpdate() {
		return needsFullSpecialBuiltChunkUpdate;
	}

	@Override
	public AtomicBoolean shouldNeedsSpecialFrustumUpdate() {
		return needsSpecialFrustumUpdate;
	}

	@Override
	public AtomicLong getNextFullSpecialUpdateMilliseconds() {
		return nextFullSpecialUpdateMilliseconds;
	}

}
