// FgV2 motion search helpers. uA = "prev" role, uB = "curr" role (swapped for the forward search).
// Vectors point from a B pixel to its match in A, in this level's pixels, GL orientation (y up).
uniform sampler2D uA;
uniform sampler2D uB;
uniform ivec2 uLSize;

const float SAD_W = 1.0;
const float CENSUS_W = 0.3;

float lA(ivec2 q) {
	return texelFetch(uA, clamp(q, ivec2(0), uLSize - 1), 0).r;
}

float lB(ivec2 q) {
	return texelFetch(uB, clamp(q, ivec2(0), uLSize - 1), 0).r;
}

// ring8 order of the reference: top row left->right, middle left/right, bottom row.
ivec2 ring8(int k) {
	return ivec2(k < 3 ? k - 1 : (k < 5 ? (k == 3 ? -1 : 1) : k - 6), k < 3 ? -1 : (k < 5 ? 0 : 1));
}

uint popc8(uint x) {
	x = x - ((x >> 1u) & 0x55u);
	x = (x & 0x33u) + ((x >> 2u) & 0x33u);
	return (x + (x >> 4u)) & 0x0Fu;
}

uint censusA(ivec2 p) {
	float c = lA(p);
	uint b = 0u;
	for (int k = 0; k < 8; k++) {
		if (lA(p + ring8(k)) > c) b |= 1u << uint(k);
	}
	return b;
}

uint censusB(ivec2 p) {
	float c = lB(p);
	uint b = 0u;
	for (int k = 0; k < 8; k++) {
		if (lB(p + ring8(k)) > c) b |= 1u << uint(k);
	}
	return b;
}

float sad3(ivec2 p, ivec2 d) {
	float s = 0.0;
	for (int k = 0; k < 9; k++) {
		ivec2 q = p + ivec2(k % 3 - 1, k / 3 - 1);
		s += abs(lA(q + d) - lB(q));
	}
	return s;
}

float hamming(ivec2 p, ivec2 d) {
	return float(popc8(censusA(p + d) ^ censusB(p)));
}

float matchCost(ivec2 p, vec2 v) {
	ivec2 d = ivec2(round(v));
	return SAD_W * sad3(p, d) + CENSUS_W * hamming(p, d) + 0.004 * (abs(v.x) + abs(v.y));
}
