package com.chopstickshq.quillcore.render.ramcache;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.memory.MeshRamCache;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

/** render thread glue between vanilla section buffers and {@link MeshRamCache}. */
public final class MeshCacheHook {
	private static final long UPLOAD_PER_FRAME = 8L << 20;
	private static final Long2ObjectOpenHashMap<SectionBuffers> LIVE = new Long2ObjectOpenHashMap<>();
	private static MeshRamCache cache;
	private static double cacheGb = -1;
	private static long nextId = 1;
	private static long frames;

	private MeshCacheHook() {}

	public static long newId() {
		return nextId++;
	}

	private static MeshRamCache cache() {
		double gb = QuillConfig.get().meshRamCacheGb;
		if (gb != cacheGb) {
			cacheGb = gb;
			cache = null;
			LIVE.clear();
			if (gb > 0) {
				long budget = meshBudget();
				cache = new MeshRamCache(budget, MeshRamCache.gb(gb), UPLOAD_PER_FRAME);
				QuillCore.LOGGER.info("mesh ram cache: {} GB ram, {} MB mesh vram budget", gb, budget >> 20);
			}
		}
		return cache;
	}

	/** half of what the driver reports as dedicated, since textures and targets need the rest. 1 GB if unknown. */
	static long meshBudget() {
		long forced = Long.getLong("quillcore.meshVramBudgetMb", 0);
		if (forced > 0) return forced << 20;
		var caps = GL.getCapabilities();
		long kb = 0;
		if (caps.GL_NVX_gpu_memory_info) kb = GL11.glGetInteger(0x9047);
		else if (caps.GL_ATI_meminfo) {
			int[] v = new int[4];
			GL11.glGetIntegerv(0x87FB, v);
			kb = v[0];
		}
		return kb > 0 ? kb * 1024 / 2 : 1L << 30;
	}

	public static void uploaded(SectionBuffers b, long id, ByteBuffer mesh) {
		MeshRamCache c = cache();
		if (c == null) return;
		LIVE.put(id, b);
		long[] out = c.uploaded(id, mesh);
		if (out.length > 0 && Boolean.getBoolean("quillcore.meshCacheLog")) QuillCore.LOGGER.info("mesh ram cache evicted {}: {}", out.length, stats());
		for (long gone : out) {
			SectionBuffers s = LIVE.get(gone);
			if (s != null) s.getVertexBuffer().close();
		}
	}

	public static void untrack(long id) {
		if (cache == null) return;
		cache.freed(id);
		LIVE.remove(id);
	}

	public static boolean evicted(long id) {
		return cache != null && cache.isEvicted(id);
	}

	/** false means skip this section for one frame, its re-upload did not fit the frame quota. */
	public static boolean ready(SectionBuffers b, long id) {
		if (cache == null) return true;
		if (!cache.isEvicted(id)) {
			cache.drawn(id);
			return true;
		}
		ByteBuffer mesh = cache.restore(id);
		if (mesh == null) return false;
		b.setVertexBuffer(RenderSystem.getDevice().createBuffer(() -> "quillcore parked section", 40, mesh));
		return true;
	}

	public static String stats() {
		MeshRamCache c = cache;
		return c == null ? "off" : String.format("gpu %d MB, ram %d MB, evicted %d", c.gpuBytes() >> 20, c.ramBytes() >> 20, c.evictedCount());
	}

	public static void endFrame() {
		MeshRamCache c = cache();
		if (c == null) return;
		c.endFrame();
		if (++frames % 1200 == 0) QuillCore.LOGGER.debug("mesh ram cache: {}", stats());
	}
}
