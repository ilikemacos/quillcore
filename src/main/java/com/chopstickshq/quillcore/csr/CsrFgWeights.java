package com.chopstickshq.quillcore.csr;

import java.util.List;

/**
 * CSR Frame Gen weights (format version 2/3). netA runs at 1/4 res once per frame pair
 * (8 packed inputs → flow delta, occlusion, 4 hints); netB runs at 1/2 res per generated
 * frame (12 inputs → mask delta, blend logit, rgb residual).
 */
public record CsrFgWeights(int version, List<CsrLayer> netA, List<CsrLayer> netB,
		float maskA, float maskB, float flowScale) {
	public static final int NET_A_IN = 8;
	public static final int NET_A_OUT = 7;
	public static final int NET_B_IN = 12;

	public CsrFgWeights {
		if (version != 2 && version != 3) {
			throw new IllegalArgumentException("unsupported frame-gen weights version " + version);
		}
		if (netA == null || netA.isEmpty() || netB == null || netB.isEmpty()) {
			throw new IllegalArgumentException("netA and netB are required");
		}
		netA = List.copyOf(netA);
		netB = List.copyOf(netB);
		CsrFluxWeights.checkChain(netA);
		CsrFluxWeights.checkChain(netB);
		if (netA.get(0).in() != NET_A_IN || netA.get(netA.size() - 1).out() != NET_A_OUT) {
			throw new IllegalArgumentException("netA must map " + NET_A_IN + "->" + NET_A_OUT);
		}
		int bOut = netB.get(netB.size() - 1).out();
		if (netB.get(0).in() != NET_B_IN) {
			throw new IllegalArgumentException("netB must take " + NET_B_IN + " inputs");
		}
		// The GLSL compose pass reads md, lp and an rgb residual (5 channels), as the reference does for v3.
		if (bOut != 5) {
			throw new IllegalArgumentException("netB must output 5 channels, got " + bOut);
		}
		if (!Float.isFinite(maskA) || !Float.isFinite(maskB) || !Float.isFinite(flowScale) || flowScale <= 0.0f) {
			throw new IllegalArgumentException("invalid maskA/maskB/flowScale");
		}
	}

	/** v3 squashes the low-pass blend weight with a sigmoid; v2 saturates it. */
	public boolean lpSigmoid() {
		return version >= 3;
	}
}
