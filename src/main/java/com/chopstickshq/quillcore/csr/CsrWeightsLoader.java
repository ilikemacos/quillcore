package com.chopstickshq.quillcore.csr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Parses and validates the CSR weight files. Never throws. */
public final class CsrWeightsLoader {
	public static final String FLUX_RESOURCE = "/assets/quillcore/csr/csr_ai_x2.weights.json";
	public static final String FG_RESOURCE = "/assets/quillcore/csr/csr_ai_fg.weights.json";

	private CsrWeightsLoader() {}

	public static CsrResult<CsrFluxWeights> loadFlux() {
		return loadFluxResource(FLUX_RESOURCE);
	}

	public static CsrResult<CsrFgWeights> loadFrameGen() {
		return loadFrameGenResource(FG_RESOURCE);
	}

	public static CsrResult<CsrFluxWeights> loadFluxResource(String path) {
		CsrResult<JsonObject> root = readResource(path);
		return root.isOk() ? parseFlux(root.value()) : CsrResult.fail(root.error());
	}

	public static CsrResult<CsrFgWeights> loadFrameGenResource(String path) {
		CsrResult<JsonObject> root = readResource(path);
		return root.isOk() ? parseFrameGen(root.value()) : CsrResult.fail(root.error());
	}

	public static CsrResult<CsrFluxWeights> parseFlux(String json) {
		CsrResult<JsonObject> root = readJson(new StringReader(json == null ? "" : json));
		return root.isOk() ? parseFlux(root.value()) : CsrResult.fail(root.error());
	}

	public static CsrResult<CsrFgWeights> parseFrameGen(String json) {
		CsrResult<JsonObject> root = readJson(new StringReader(json == null ? "" : json));
		return root.isOk() ? parseFrameGen(root.value()) : CsrResult.fail(root.error());
	}

	static CsrResult<CsrFluxWeights> parseFlux(JsonObject root) {
		try {
			int version = root.has("version") ? root.get("version").getAsInt() : 0;
			if (version != CsrFluxWeights.VERSION) {
				return CsrResult.fail("unsupported Flux weights version " + version);
			}
			if (root.has("scale") && root.get("scale").getAsInt() != CsrFluxWeights.SCALE) {
				return CsrResult.fail("only scale 2 is supported");
			}
			float[] luma = root.has("luma") ? floats(root.getAsJsonArray("luma")) : new float[] {0.2126f, 0.7152f, 0.0722f};
			return CsrResult.ok(new CsrFluxWeights(luma, layers(root, "layers")));
		} catch (RuntimeException e) {
			return CsrResult.fail("invalid Flux weights: " + e.getMessage());
		}
	}

	static CsrResult<CsrFgWeights> parseFrameGen(JsonObject root) {
		try {
			int version = root.has("version") ? root.get("version").getAsInt() : 0;
			float maskA = root.has("maskA") ? root.get("maskA").getAsFloat() : 0.0f;
			float maskB = root.has("maskB") ? root.get("maskB").getAsFloat() : 0.0f;
			float flowScale = root.has("flowScale") ? root.get("flowScale").getAsFloat() : 32.0f;
			return CsrResult.ok(new CsrFgWeights(version, layers(root, "netA"), layers(root, "netB"), maskA, maskB, flowScale));
		} catch (RuntimeException e) {
			return CsrResult.fail("invalid frame-gen weights: " + e.getMessage());
		}
	}

	private static List<CsrLayer> layers(JsonObject root, String key) {
		if (!root.has(key) || !root.get(key).isJsonArray()) {
			throw new IllegalArgumentException("missing '" + key + "'");
		}
		List<CsrLayer> out = new ArrayList<>();
		for (JsonElement e : root.getAsJsonArray(key)) {
			JsonObject o = e.getAsJsonObject();
			String type = o.has("type") ? o.get("type").getAsString() : "";
			if (!"conv3x3".equals(type)) {
				throw new IllegalArgumentException("unsupported layer type '" + type + "'");
			}
			String act = o.has("act") ? o.get("act").getAsString() : "none";
			if (!"leaky0.1".equals(act) && !"none".equals(act)) {
				throw new IllegalArgumentException("unsupported activation '" + act + "'");
			}
			out.add(new CsrLayer(o.get("in").getAsInt(), o.get("out").getAsInt(), "leaky0.1".equals(act),
					floats(o.getAsJsonArray("w")), floats(o.getAsJsonArray("b"))));
		}
		return out;
	}

	private static float[] floats(JsonArray a) {
		if (a == null) {
			throw new IllegalArgumentException("missing array");
		}
		float[] f = new float[a.size()];
		for (int i = 0; i < f.length; i++) {
			f[i] = a.get(i).getAsFloat();
		}
		return f;
	}

	private static CsrResult<JsonObject> readResource(String path) {
		InputStream in = CsrWeightsLoader.class.getResourceAsStream(path);
		if (in == null) {
			return CsrResult.fail("weights resource not found: " + path);
		}
		try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return readJson(r);
		} catch (Exception e) {
			return CsrResult.fail("failed to read " + path + ": " + e.getMessage());
		}
	}

	private static CsrResult<JsonObject> readJson(Reader r) {
		try {
			JsonElement e = JsonParser.parseReader(r);
			if (e == null || !e.isJsonObject()) {
				return CsrResult.fail("weights file is empty or not a JSON object");
			}
			return CsrResult.ok(e.getAsJsonObject());
		} catch (RuntimeException e) {
			return CsrResult.fail("malformed weights JSON: " + e.getMessage());
		}
	}
}
