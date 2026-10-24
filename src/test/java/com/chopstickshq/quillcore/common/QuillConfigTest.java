package com.chopstickshq.quillcore.common;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuillConfigTest {
	private static final Gson GSON = new Gson();

	private static QuillConfig parse(String json) {
		QuillConfig c = GSON.fromJson(json, QuillConfig.class);
		c.migrate();
		c.sanitize();
		return c;
	}

	@Test
	void defaultsAreSafe() {
		QuillConfig c = parse("{}");
		assertEquals(QuillConfig.CURRENT_VERSION, c.configVersion);
		assertTrue(c.masterEnabled);
		assertFalse(c.frameGenEnabled);
		assertFalse(c.cloudDistanceCapEnabled);
		assertFalse(c.weatherRadiusCapEnabled);
		assertEquals("none", c.upscaleBackendId);
	}

	@Test
	void skipTicksClamped() {
		QuillConfig c = parse("{\"idleAiThrottleMaxSkipTicks\":99,\"brainIdleThrottleMaxSkipTicks\":-4,"
				+ "\"itemMergeThrottleMaxSkipTicks\":11,\"pathNavThrottleMaxSkipTicks\":-1}");
		assertEquals(10, c.idleAiThrottleMaxSkipTicks);
		assertEquals(0, c.brainIdleThrottleMaxSkipTicks);
		assertEquals(10, c.itemMergeThrottleMaxSkipTicks);
		assertEquals(0, c.pathNavThrottleMaxSkipTicks);
	}

	@Test
	void negativeOrNanRadiiReset() {
		QuillConfig c = parse("{\"idleAiThrottlePlayerRadiusBlocks\":-1,\"entityRenderCutoffBlocks\":\"NaN\","
				+ "\"particleRenderCutoffBlocks\":-3,\"pathNavThrottlePlayerRadiusBlocks\":-0.5}");
		assertEquals(32.0, c.idleAiThrottlePlayerRadiusBlocks);
		assertEquals(128.0, c.entityRenderCutoffBlocks);
		assertEquals(64.0, c.particleRenderCutoffBlocks);
		assertEquals(32.0, c.pathNavThrottlePlayerRadiusBlocks);
	}

	@Test
	void capsClamped() {
		QuillConfig c = parse("{\"cloudDistanceCapChunks\":0,\"weatherRadiusCapBlocks\":50}");
		assertEquals(1, c.cloudDistanceCapChunks);
		assertEquals(10, c.weatherRadiusCapBlocks);
	}

	@Test
	void legacyFsrIdMigrated() {
		assertEquals("fsr1", parse("{\"configVersion\":1,\"upscaleBackendId\":\"fsr\"}").upscaleBackendId);
		assertEquals("fsr1", parse("{\"upscaleBackendId\":\"  FSR  \"}").upscaleBackendId);
		assertEquals("none", parse("{\"upscaleBackendId\":\"\"}").upscaleBackendId);
	}

	@Test
	void fsrValuesSanitized() {
		QuillConfig c = parse("{\"fsrSharpness\":5,\"fsrRenderScaleOverride\":3,\"fsrQualityPreset\":\"\"}");
		assertEquals(0.2f, c.fsrSharpness);
		assertEquals(1.0f, c.fsrRenderScaleOverride);
		assertEquals("QUALITY", c.fsrQualityPreset);
		assertEquals(0.0f, parse("{\"fsrRenderScaleOverride\":-1}").fsrRenderScaleOverride);
	}

	@Test
	void frameGenAlwaysForcedOff() {
		assertFalse(parse("{\"frameGenEnabled\":true}").frameGenEnabled);
	}

	@Test
	void nullFrameFxPathBecomesEmpty() {
		assertEquals("", parse("{\"frameFxExecutablePath\":null}").frameFxExecutablePath);
	}

	@Test
	void userTogglesSurviveMigration() {
		QuillConfig c = parse("{\"configVersion\":2,\"masterEnabled\":false,\"logicEnabled\":false}");
		assertFalse(c.masterEnabled);
		assertFalse(c.logicEnabled);
		assertEquals(QuillConfig.CURRENT_VERSION, c.configVersion);
	}

	@Test
	void statusSummaryMentionsKeyToggles() {
		String s = parse("{}").statusSummary();
		assertTrue(s.contains("master=true"));
		assertTrue(s.contains("upscale=none"));
	}

	@Test
	void supportFieldsDefaultAndSanitize() {
		QuillConfig c = parse("{}");
		assertEquals(QuillConfig.DEFAULT_SUPPORT_URL, c.supportUrl);
		assertEquals(0, c.bmcOpenCount);
		assertFalse(c.bmcNeverShow);
		c = parse("{\"supportUrl\":\"javascript:alert(1)\",\"bmcOpenCount\":-3,\"bmcLastShownEpochMs\":-9}");
		assertEquals(QuillConfig.DEFAULT_SUPPORT_URL, c.supportUrl);
		assertEquals(0, c.bmcOpenCount);
		assertEquals(0L, c.bmcLastShownEpochMs);
	}
}
