package com.chopstickshq.quillcore.csr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsrWeightsLoaderTest {
	private static void assertShape(CsrLayer l, int in, int out, boolean leaky) {
		assertEquals(in, l.in());
		assertEquals(out, l.out());
		assertEquals(leaky, l.leaky());
		assertEquals(in * out * 9, l.w().length);
		assertEquals(out, l.b().length);
	}

	@Test
	void fluxWeightsHaveExpectedShapes() {
		CsrResult<CsrFluxWeights> r = CsrWeightsLoader.loadFlux();
		assertTrue(r.isOk(), r.error());
		List<CsrLayer> l = r.value().layers();
		assertEquals(4, l.size());
		assertShape(l.get(0), 1, 8, true);
		assertShape(l.get(1), 8, 8, true);
		assertShape(l.get(2), 8, 8, true);
		assertShape(l.get(3), 8, 4, false);
		assertArrayEquals(new float[] {0.2126f, 0.7152f, 0.0722f}, r.value().luma());
	}

	@Test
	void frameGenWeightsHaveExpectedShapes() {
		CsrResult<CsrFgWeights> r = CsrWeightsLoader.loadFrameGen();
		assertTrue(r.isOk(), r.error());
		CsrFgWeights w = r.value();
		assertEquals(3, w.version());
		assertTrue(w.lpSigmoid());
		assertEquals(32.0f, w.flowScale());
		assertEquals(-0.1067f, w.maskA(), 1e-4);
		assertEquals(-0.0730f, w.maskB(), 1e-4);
		assertEquals(4, w.netA().size());
		assertShape(w.netA().get(0), 8, 18, true);
		assertShape(w.netA().get(1), 18, 18, true);
		assertShape(w.netA().get(2), 18, 18, true);
		assertShape(w.netA().get(3), 18, 7, false);
		assertEquals(2, w.netB().size());
		assertShape(w.netB().get(0), 12, 6, true);
		assertShape(w.netB().get(1), 6, 5, false);
	}

	private static String fluxJson(int version, int w0Len, String act) {
		StringBuilder sb = new StringBuilder("{\"version\":" + version + ",\"layers\":[");
		sb.append(layer(1, 4, w0Len, act));
		return sb.append("]}").toString();
	}

	private static String layer(int in, int out, int wLen, String act) {
		StringBuilder sb = new StringBuilder("{\"type\":\"conv3x3\",\"in\":" + in + ",\"out\":" + out + ",\"act\":\"" + act + "\",\"w\":[");
		for (int i = 0; i < wLen; i++) {
			sb.append(i == 0 ? "" : ",").append("0.0");
		}
		sb.append("],\"b\":[");
		for (int i = 0; i < out; i++) {
			sb.append(i == 0 ? "" : ",").append("0.0");
		}
		return sb.append("]}").toString();
	}

	@Test
	void minimalValidFluxParses() {
		assertTrue(CsrWeightsLoader.parseFlux(fluxJson(1, 36, "none")).isOk());
	}

	@Test
	void corruptFluxInputsAreRejected() {
		assertFalse(CsrWeightsLoader.parseFlux(fluxJson(1, 35, "none")).isOk(), "wrong weight count");
		assertFalse(CsrWeightsLoader.parseFlux(fluxJson(2, 36, "none")).isOk(), "wrong version");
		assertFalse(CsrWeightsLoader.parseFlux(fluxJson(1, 36, "gelu")).isOk(), "unknown activation");
		assertFalse(CsrWeightsLoader.parseFlux("{\"version\":1}").isOk(), "missing layers");
		assertFalse(CsrWeightsLoader.parseFlux("").isOk(), "empty");
		assertFalse(CsrWeightsLoader.parseFlux("{not json").isOk(), "malformed");
		assertFalse(CsrWeightsLoader.parseFlux((String) null).isOk(), "null");
		String nan = fluxJson(1, 36, "none").replaceFirst("0\\.0", "\"NaN\"");
		assertFalse(CsrWeightsLoader.parseFlux(nan).isOk(), "NaN weight");
	}

	@Test
	void corruptFrameGenInputsAreRejected() {
		assertFalse(CsrWeightsLoader.parseFrameGen("{\"version\":3}").isOk());
		assertFalse(CsrWeightsLoader.parseFrameGen("{\"version\":1,\"netA\":[],\"netB\":[]}").isOk());
		assertFalse(CsrWeightsLoader.parseFrameGen("").isOk());
	}

	@Test
	void missingResourceIsAFailureNotAnException() {
		CsrResult<CsrFluxWeights> r = CsrWeightsLoader.loadFluxResource("/assets/quillcore/csr/does_not_exist.json");
		assertFalse(r.isOk());
		assertTrue(r.error().contains("not found"));
	}
}
