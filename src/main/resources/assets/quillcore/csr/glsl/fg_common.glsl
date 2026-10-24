// Shared by fg_packa/fg_packb/fg_compose (prepended by the loader after #version).
uniform vec3 uLuma;
uniform vec2 uFullSizeF;
uniform ivec2 uFullSize;
uniform sampler2D uCurrFinal;
uniform sampler2D uCurrWorld;

float luma709(vec3 c) {
	return dot(c, uLuma);
}

// HUD mask: the GUI pass changed this pixel of the current frame. Replaces the reference's HUD detector.
float hudAt(ivec2 c) {
	c = clamp(c, ivec2(0), uFullSize - 1);
	vec3 d = abs(texelFetch(uCurrFinal, c, 0).rgb - texelFetch(uCurrWorld, c, 0).rgb);
	return max(max(d.r, d.g), d.b) > (2.0 / 255.0) ? 1.0 : 0.0;
}

bool inside(vec2 uv) {
	return all(greaterThanEqual(uv, vec2(0.0))) && all(lessThanEqual(uv, vec2(1.0)));
}
