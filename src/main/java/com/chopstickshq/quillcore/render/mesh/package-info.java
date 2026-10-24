/**
 * Quill Render compact section mesh format (M1, clean-room).
 *
 * <p>Pure Java, no Minecraft imports. Written in-house from the public "quantized section-local vertex"
 * idea only; no code, shader, class name, or comment from any other renderer mod was read or ported.
 * Live use: {@link com.chopstickshq.quillcore.render.mesh.QuillSectionTranscoder} re-encodes built SOLID section
 * meshes when {@code compactMeshEnabled} is on (client mixins in {@code render.mixin}).
 */
package com.chopstickshq.quillcore.render.mesh;
