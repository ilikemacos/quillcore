package com.chopstickshq.quillcore.csr;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CsrFluxCpuTest {
	private static final int W = 7, H = 5;

	private static CsrFluxWeights real() {
		CsrResult<CsrFluxWeights> r = CsrWeightsLoader.loadFlux();
		assertTrue(r.isOk(), r.error());
		return r.value();
	}

	private static float[] noise(long seed) {
		Random rnd = new Random(seed);
		float[] img = new float[W * H * 3];
		for (int i = 0; i < img.length; i++) {
			img[i] = rnd.nextFloat();
		}
		return img;
	}

	@Test
	void outputIsTwiceTheSize() {
		assertEquals(2 * W * 2 * H * 3, CsrFluxCpu.upscale(real(), noise(1), W, H).length);
	}

	@Test
	void zeroedWeightsGiveBilinearIdentity() {
		float[] img = noise(2);
		float[] out = CsrFluxCpu.upscale(real().zeroed(), img, W, H);
		float[] ref = CsrFluxCpu.bilinearUpscale(img, W, H);
		assertArrayEquals(ref, out, 1e-6f);
	}

	@Test
	void constantImageStaysInRangeAndIsDeterministic() {
		float[] img = new float[W * H * 3];
		for (int i = 0; i < img.length; i += 3) {
			img[i] = 0.3f;
			img[i + 1] = 0.6f;
			img[i + 2] = 0.9f;
		}
		float[] a = CsrFluxCpu.upscale(real(), img, W, H);
		float[] b = CsrFluxCpu.upscale(real(), img, W, H);
		assertArrayEquals(a, b);
		for (float v : a) {
			assertTrue(v >= 0.0f && v <= 1.0f && Float.isFinite(v));
		}
	}

	@Test
	void realWeightsChangeTheImage() {
		float[] img = noise(3);
		float[] out = CsrFluxCpu.upscale(real(), img, W, H);
		float[] ref = CsrFluxCpu.bilinearUpscale(img, W, H);
		double diff = 0;
		for (int i = 0; i < out.length; i++) {
			diff += Math.abs(out[i] - ref[i]);
		}
		assertTrue(diff > 1e-3, "network should not be a no-op");
	}

	@Test
	void lastLayerBiasChannel0ShiftsOnlyEvenEvenPixels() {
		CsrFluxWeights z = real().zeroed();
		List<CsrLayer> layers = new ArrayList<>(z.layers());
		CsrLayer last = layers.get(layers.size() - 1);
		float[] b = new float[last.out()];
		b[0] = 0.1f;
		layers.set(layers.size() - 1, new CsrLayer(last.in(), last.out(), last.leaky(), last.w(), b));
		CsrFluxWeights w = new CsrFluxWeights(z.luma(), layers);

		float[] img = new float[W * H * 3];
		java.util.Arrays.fill(img, 0.5f);
		float[] out = CsrFluxCpu.upscale(w, img, W, H);
		float[] ref = CsrFluxCpu.bilinearUpscale(img, W, H);
		for (int y = 0; y < 2 * H; y++) {
			for (int x = 0; x < 2 * W; x++) {
				float expected = ((x & 1) == 0 && (y & 1) == 0) ? 0.1f : 0.0f;
				for (int k = 0; k < 3; k++) {
					int i = (y * 2 * W + x) * 3 + k;
					assertEquals(expected, out[i] - ref[i], 1e-6f, "pixel " + x + "," + y);
				}
			}
		}
	}

	@Test
	void costMatchesDocs() {
		CsrFluxWeights f = real();
		assertEquals(1512, CsrCost.macs(f.layers()));
		assertEquals(63, CsrCost.convFetches(f.layers()));
		CsrFgWeights g = CsrWeightsLoader.loadFrameGen().value();
		assertEquals(8262, CsrCost.macs(g.netA()));
		assertEquals(918, CsrCost.macs(g.netB()));
		assertEquals(18 + 45 + 45 + 45, CsrCost.convFetches(g.netA()));
		assertEquals(27 + 18, CsrCost.convFetches(g.netB()));
		assertEquals(0.784, CsrCost.gmac(f.layers(), 960L * 540), 1e-3);
	}
}
