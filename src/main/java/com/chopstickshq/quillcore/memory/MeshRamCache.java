package com.chopstickshq.quillcore.memory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps a copy of chunk meshes in system RAM so sections we push out of VRAM can
 * come back without a rebuild. This is not extra VRAM: the driver still decides
 * what lives on the GPU, we only track our own uploads against a budget.
 *
 * once tracked bytes pass 95% of the budget we evict least recently drawn
 * sections until we are back under 85%. only sections with a RAM copy are ever
 * evicted, and RAM copies of evicted sections are never dropped, so nothing is
 * lost. re-uploads are capped per frame.
 */
public final class MeshRamCache {
	static final double HIGH = 0.95, LOW = 0.85;

	private final long vramBudget, ramCap, uploadPerFrame;
	private final LinkedHashMap<Long, Integer> onGpu = new LinkedHashMap<>(256, 0.75f, true);
	private final LinkedHashMap<Long, ByteBuffer> inRam = new LinkedHashMap<>(256, 0.75f, true);
	private final Set<Long> evicted = new HashSet<>();
	private long gpuBytes, ramBytes, uploadedThisFrame;

	public MeshRamCache(long vramBudget, long ramCap, long uploadPerFrame) {
		this.vramBudget = vramBudget;
		this.ramCap = ramCap;
		this.uploadPerFrame = uploadPerFrame;
	}

	public static long gb(double g) {
		return (long) (g * (1L << 30));
	}

	/** record an upload of mesh (not consumed). returns keys whose gpu buffers the caller must free now. */
	public long[] uploaded(long key, ByteBuffer mesh) {
		int bytes = mesh.remaining();
		Integer old = onGpu.put(key, bytes);
		gpuBytes += bytes - (old == null ? 0 : old);
		evicted.remove(key);
		dropRam(key);
		keep(key, mesh);
		return gpuBytes > vramBudget * HIGH ? trim() : new long[0];
	}

	public void drawn(long key) {
		onGpu.get(key);
	}

	public boolean isEvicted(long key) {
		return evicted.contains(key);
	}

	public void freed(long key) {
		Integer b = onGpu.remove(key);
		if (b != null) gpuBytes -= b;
		evicted.remove(key);
		dropRam(key);
	}

	/**
	 * mesh to re-upload for an evicted section that is visible again, or null
	 * when this frame's upload quota is spent (draw it next frame).
	 */
	public ByteBuffer restore(long key) {
		if (!evicted.contains(key)) return null;
		ByteBuffer b = inRam.get(key);
		if (uploadedThisFrame > 0 && uploadedThisFrame + b.capacity() > uploadPerFrame) return null;
		uploadedThisFrame += b.capacity();
		evicted.remove(key);
		onGpu.put(key, b.capacity());
		gpuBytes += b.capacity();
		return b.asReadOnlyBuffer();
	}

	public void endFrame() {
		uploadedThisFrame = 0;
	}

	public long gpuBytes() { return gpuBytes; }
	public long ramBytes() { return ramBytes; }
	public int evictedCount() { return evicted.size(); }
	boolean inRam(long key) { return inRam.containsKey(key); }

	private void keep(long key, ByteBuffer mesh) {
		int n = mesh.remaining();
		if (n == 0 || n > ramCap) return;
		Iterator<Map.Entry<Long, ByteBuffer>> it = inRam.entrySet().iterator();
		while (ramBytes + n > ramCap && it.hasNext()) {
			Map.Entry<Long, ByteBuffer> e = it.next();
			if (evicted.contains(e.getKey())) continue;
			ramBytes -= e.getValue().capacity();
			it.remove();
		}
		if (ramBytes + n > ramCap) return;
		ByteBuffer copy = ByteBuffer.allocateDirect(n);
		copy.put(mesh.duplicate()).flip();
		inRam.put(key, copy);
		ramBytes += n;
	}

	private long[] trim() {
		long target = (long) (vramBudget * LOW);
		long[] out = new long[8];
		int n = 0;
		Iterator<Map.Entry<Long, Integer>> it = onGpu.entrySet().iterator();
		while (gpuBytes > target && it.hasNext()) {
			Map.Entry<Long, Integer> e = it.next();
			if (!inRam.containsKey(e.getKey())) continue;
			gpuBytes -= e.getValue();
			evicted.add(e.getKey());
			if (n == out.length) out = Arrays.copyOf(out, n * 2);
			out[n++] = e.getKey();
			it.remove();
		}
		return Arrays.copyOf(out, n);
	}

	private void dropRam(long key) {
		ByteBuffer b = inRam.remove(key);
		if (b != null) ramBytes -= b.capacity();
	}
}
