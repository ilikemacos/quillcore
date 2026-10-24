package com.chopstickshq.quillcore.csr;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsrFailSafeTest {
	@Test
	void missingWeightsDisableWithReasonAndWarnOnce() {
		List<String> warnings = new ArrayList<>();
		CsrFeature f = new CsrFeature("CSR Flux", warnings::add);
		CsrResult<CsrFluxWeights> missing = CsrWeightsLoader.loadFluxResource("/assets/quillcore/csr/missing.json");
		assertNull(f.tryEnable(true, missing));
		assertTrue(f.isDisabled());
		assertTrue(f.reason().contains("not found"));
		assertNull(f.tryEnable(true, missing));
		f.disable("shader compile failed");
		assertEquals(1, warnings.size());
		assertTrue(f.reason().contains("not found"), "first reason is kept");
	}

	@Test
	void invalidWeightsDisable() {
		List<String> warnings = new ArrayList<>();
		CsrFeature f = new CsrFeature("CSR Frame Gen", warnings::add);
		assertNull(f.tryEnable(true, CsrWeightsLoader.parseFrameGen("{\"version\":9}")));
		assertTrue(f.isDisabled());
		assertEquals(1, warnings.size());
	}

	@Test
	void notRequestedDoesNothing() {
		List<String> warnings = new ArrayList<>();
		CsrFeature f = new CsrFeature("CSR Flux", warnings::add);
		assertNull(f.tryEnable(false, CsrResult.<CsrFluxWeights>fail("x")));
		assertFalse(f.isDisabled());
		assertTrue(warnings.isEmpty());
	}

	@Test
	void validWeightsEnable() {
		CsrFeature f = new CsrFeature("CSR Flux", s -> fail("unexpected warning " + s));
		assertNotNull(f.tryEnable(true, CsrWeightsLoader.loadFlux()));
		assertFalse(f.isDisabled());
	}

	@Test
	void throwingSinkNeverPropagates() {
		CsrFeature f = new CsrFeature("CSR Flux", s -> { throw new IllegalStateException("boom"); });
		assertDoesNotThrow(() -> f.disable("x"));
		assertTrue(f.isDisabled());
	}
}
