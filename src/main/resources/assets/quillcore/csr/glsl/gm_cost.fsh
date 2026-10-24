#version 330 core
// FG2_GlobalMotion step 2: one texel per shift (25x13 = +-12 x +-6 cells), mean |prev - curr| over the overlap.
uniform sampler2D uGrid;
out vec4 fragColor;

void main() {
	ivec2 c = ivec2(gl_FragCoord.xy);
	int dx = c.x - 12, dy = c.y - 6;
	int x0 = max(0, -dx), x1 = min(64, 64 - dx), y0 = max(0, -dy), y1 = min(36, 36 - dy);
	float sum = 0.0, n = 0.0;
	for (int y = y0; y < y1; y++) {
		for (int x = x0; x < x1; x++) {
			sum += abs(texelFetch(uGrid, ivec2(x + dx, y + dy), 0).r - texelFetch(uGrid, ivec2(x, y), 0).g);
			n += 1.0;
		}
	}
	fragColor = vec4((n >= 400.0 ? sum / n : 1e9) + 0.0005 * float(abs(dx) + abs(dy)));
}
