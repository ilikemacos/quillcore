uniform sampler2D uPackA0;
uniform sampler2D uNetA0;
uniform float uFlowScale;

// csraiFgV: (packed bwd + netA delta) * flowScale, as a GL-orientation uv offset.
vec2 fgV(vec2 uv) {
	vec2 base = texture(uPackA0, uv).zw;
	vec2 dv = texture(uNetA0, uv).xy;
	vec2 v = (base + dv) * uFlowScale / uFullSizeF;
	return vec2(v.x, -v.y);
}
