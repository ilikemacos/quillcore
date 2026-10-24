#version 330 core
// FG2_P8Search block consensus: for one 8x8 group of tiles, the +-4 shift with the lowest summed
// 3x3 SAD over all live tiles, plus 0.05 per pixel of shift. Output .xy = that shift.
#include me_common
uniform ivec2 uTiles;
out vec4 fragColor;

void main() {
	ivec2 base = ivec2(gl_FragCoord.xy) * 8;
	float bc = 1e20;
	ivec2 bv = ivec2(0);
	for (int ci = 0; ci < 81; ci++) {
		ivec2 d = ivec2(ci % 9 - 4, ci / 9 - 4);
		float sum = 0.0;
		for (int t = 0; t < 64; t++) {
			ivec2 p = base + ivec2(t % 8, t / 8);
			if (p.x < uTiles.x && p.y < uTiles.y) sum += sad3(p, d);
		}
		sum += 0.05 * float(abs(d.x) + abs(d.y));
		if (sum < bc) {
			bc = sum;
			bv = d;
		}
	}
	fragColor = vec4(vec2(bv), 0.0, 0.0);
}
