package com.chopstickshq.quillcore.csr;

import com.chopstickshq.quillcore.common.QuillConfig;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CsrConfigDefaultsTest {
	private static final Gson GSON = new Gson();

	private static QuillConfig parse(String json) {
		QuillConfig c = GSON.fromJson(json, QuillConfig.class);
		c.migrate();
		c.sanitize();
		return c;
	}

	@Test
	void freshConfigHasCsrOff() {
		QuillConfig c = parse("{}");
		assertFalse(c.csrFlux);
		assertFalse(c.csrFrameGen);
	}

	@Test
	void oldConfigWithoutKeysStaysOffAfterRoundTrip() {
		QuillConfig c = parse("{\"configVersion\":5,\"upscaleBackendId\":\"fsr1\"}");
		assertFalse(c.csrFlux);
		assertFalse(c.csrFrameGen);
		assertEquals(6, c.configVersion);
		QuillConfig again = parse(GSON.toJson(c));
		assertFalse(again.csrFlux);
		assertFalse(again.csrFrameGen);
	}

	@Test
	void explicitOptInSurvives() {
		QuillConfig c = parse("{\"csrFlux\":true,\"csrFrameGen\":true}");
		assertTrue(c.csrFlux);
		assertTrue(c.csrFrameGen);
		assertTrue(c.statusSummary().contains("csrFlux=true"));
	}
}
