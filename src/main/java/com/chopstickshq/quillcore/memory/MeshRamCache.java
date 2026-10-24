package com.chopstickshq.quillcore.memory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongConsumer;

/**
 * Keeps chunk meshes in system RAM so sections that leave VRAM can come back
 * without a rebuild. This is not extra VRAM: the driver still decides what lives
 * on the GPU, we only track our own uploads against a budget.
 *
 * gpu side: once tracked bytes pass 95% of the budget we evict least recently
 * drawn sections until we are back under 85%. evicted meshes stay in the RAM
 * pool (off-heap, LRU, capped at ramCapBytes) and re-upload at most
 * uploadBytesPerFrame per frame.
 */
public final class MeshRamCache {
	static final double HIGH = 0.95, LOW = 0.85;

	private final long vramBudget, ramCap, uploadPerFrame;
	private final LinkedHashMap<Long, Integer> onGpu = new LinkedHashMap<>(256, 0.75f, true);
	private final LinkedHashMap<Long, ByteBuffer> inRam = new LinkedHashMap<>(256, 0.75f, true);
	private long gpuBytes, ramBytes, uploadedThisFrame;

	public MeshRamCache(long vramBudget, long ramCap, long uploadPerFrame) {
		this.vramBudget = vramBudget;
		this.ramCap = ramCap;
		this.uploadPerFrame = uploadPerFrame;
	}

	public static long gb(double g) {
		return (long) (g * (1L << 30));
	}

	/** record an upload. returns section keys the caller must free on the gpu. */
	public long[] uploaded(long key, int bytes) {
		Integer old = onGpu.put(key, bytes);
		gpuBytes += bytes - (old == null ? 0 : old);
		dropRam(key);
		return gpuBytes > vramBudget * HIGH ? trim() : new long[0];
	}

	public void drawn(long key) {
		onGpu.get(key);
	}

	public void freed(long key) {
		Integer b = onGpu.remove(key);
		if (b != null) gpuBytes -= b;
		dropRam(key);
	}

	/** hand the mesh bytes of a section about to be evicted, so it can come back cheaply. */
	public void park(long key, ByteBuffer mesh) {
		if (ramCap <= 0 || mesh.remaining() > ramCap) return;
		dropRam(key);
		ByteBuffer copy = ByteBuffer.allocateDirect(mesh.remaining());
		copy.put(mesh.duplicate()).flip();
		inRam.put(key, copy);
		ramBytes += copy.capacity();
		Iterator<Map.Entry<Long, ByteBuffer>> it = inRam.entrySet().iterator();
		while (ramBytes > ramCap && it.hasNext()) {
			ramBytes -= it.next().getValue().capacity();
			it.remove();
		}
	}

	/**
	 * mesh to re-upload for a section that became visible again, or null if the
	 * caller has to rebuild it (not parked) or wait (frame upload quota spent,
	 * or uploading would push us back over the high mark).
	 */
	public ByteBuffer take(long key) {
		ByteBuffer b = inRam.get(key);
		if (b == null) return null;
		if (uploadedThisFrame + b.capacity() > uploadPerFrame && uploadedThisFrame > 0) return null;
		if (gpuBytes + b.capacity() > vramBudget * HIGH) return null;
		uploadedThisFrame += b.capacity();
		inRam.remove(key);
		ramBytes -= b.capacity();
		return b.asReadOnlyBuffer();
	}

	public void endFrame() {
		uploadedThisFrame = 0;
	}

	public void forEachEvictable(LongConsumer c) {
		onGpu.keySet().forEach(c::accept);
	}

	public long gpuBytes() { return gpuBytes; }
	public long ramBytes() { return ramBytes; }
	public boolean parked(long key) { return inRam.containsKey(key); }

	private long[] trim() {
		long target = (long) (vramBudget * LOW);
		long[] out = new long[8];
		int n = 0;
		Iterator<Map.Entry<Long, Integer>> it = onGpu.entrySet().iterator();
		while (gpuBytes > target && it.hasNext()) {
			Map.Entry<Long, Integer> e = it.next();
			gpuBytes -= e.getValue();
			if (n == out.length) out = java.util.Arrays.copyOf(out, n * 2);
			out[n++] = e.getKey();
			it.remove();
		}
		return java.util.Arrays.copyOf(out, n);
	}

	private void dropRam(long key) {
		ByteBuffer b = inRam.remove(key);
		if (b != null) ramBytes -= b.capacity();
	}
}
