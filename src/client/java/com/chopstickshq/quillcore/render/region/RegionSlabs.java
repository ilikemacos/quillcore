package com.chopstickshq.quillcore.render.region;

import com.chopstickshq.quillcore.render.compact.QuillBufferTag;
import com.chopstickshq.quillcore.render.compact.QuillTerrainPipeline;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.nio.ByteBuffer;

/**
 * One vertex buffer per region for compact SOLID meshes. Sections get a range of their region's buffer from a
 * {@link RegionArena}; ranges are quad aligned so a range start maps straight onto the sequential quad index buffer.
 * Render thread only.
 */
public final class RegionSlabs {
	private static final int QUAD = QuillTerrainPipeline.FORMAT.getVertexSize() * 4;
	private static final int START = 64 * 1024;
	private static final int USAGE = GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC;
	private static final Long2ObjectOpenHashMap<Slab> SLABS = new Long2ObjectOpenHashMap<>();

	private RegionSlabs() {
	}

	public static final class Slab {
		final long key;
		final RegionArena arena;
		GpuBuffer buffer;

		Slab(long key, int bytes) {
			this.key = key;
			int cap = START;
			while (cap < bytes) cap *= 2;
			arena = new RegionArena(cap, QUAD);
			buffer = create(cap);
		}

		public GpuBuffer buffer() {
			return buffer;
		}
	}

	/** A section's range; {@code offset} is in bytes from the start of the slab buffer. */
	public record Range(Slab slab, int offset, int bytes) {
		public int firstIndex() {
			return offset / QUAD * 6;
		}
	}

	public static Range upload(int sx, int sy, int sz, ByteBuffer data) {
		int bytes = data.remaining();
		long key = RegionPos.of(sx, sy, sz);
		Slab slab = SLABS.get(key);
		if (slab == null) {
			slab = new Slab(key, bytes);
			SLABS.put(key, slab);
		}
		int off = slab.arena.alloc(bytes);
		CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
		if (slab.arena.capacity() > slab.buffer.size()) {
			GpuBuffer old = slab.buffer;
			GpuBuffer grown = create(slab.arena.capacity());
			enc.copyToBuffer(old.slice(), grown.slice(0, old.size()));
			old.close();
			slab.buffer = grown;
		}
		enc.writeToBuffer(slab.buffer.slice(off, bytes), data);
		return new Range(slab, off, bytes);
	}

	public static void free(Range r) {
		Slab slab = r.slab();
		slab.arena.free(r.offset(), r.bytes());
		if (slab.arena.used() == 0 && SLABS.get(slab.key) == slab) {
			SLABS.remove(slab.key);
			slab.buffer.close();
		}
	}

	public static int count() {
		return SLABS.size();
	}

	private static GpuBuffer create(int size) {
		GpuBuffer b = RenderSystem.getDevice().createBuffer(() -> "quill region", USAGE, size);
		((QuillBufferTag) b).quillcore$setCompact(true);
		return b;
	}
}
