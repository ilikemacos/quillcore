#version 330 core
// FG2_GlobalMotion step 3: cheapest shift + parabolic sub-cell refine, written as a uv offset.
uniform sampler2D uCost;
out vec4 fragColor;

float cost(int x, int y) {
	return texelFetch(uCost, ivec2(x, y), 0).r;
}

void main() {
	int bx = 0, by = 0;
	float bc = 1e20;
	for (int y = 0; y < 13; y++) {
		for (int x = 0; x < 25; x++) {
			float c = cost(x, y);
			if (c < bc) {
				bc = c;
				bx = x;
				by = y;
			}
		}
	}
	vec2 g = vec2(bx - 12, by - 6);
	if (bx > 0 && bx < 24) {
		float cl = cost(bx - 1, by), cr = cost(bx + 1, by), d = cl + cr - 2.0 * bc;
		if (d > 1e-9) g.x += clamp(0.5 * (cl - cr) / d, -0.5, 0.5);
	}
	if (by > 0 && by < 12) {
		float cu = cost(bx, by - 1), cd = cost(bx, by + 1), d = cu + cd - 2.0 * bc;
		if (d > 1e-9) g.y += clamp(0.5 * (cu - cd) / d, -0.5, 0.5);
	}
	fragColor = vec4(g / vec2(64.0, 36.0), 0.0, 0.0);
}
