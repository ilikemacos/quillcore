package com.chopstickshq.quillcore.render.compact;

import com.chopstickshq.quillcore.common.QuillCore;
import com.chopstickshq.quillcore.render.mesh.QuillCompactVertex;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

/**
 * GPU side of the Quill compact SOLID mesh: the 20 B/vertex {@link VertexFormat} (layout in {@link QuillCompactVertex})
 * and the {@code quillcore:pipeline/quill_solid} pipeline. The pipeline keeps vanilla's SOLID uniform/sampler contract
 * (DynamicTransforms, Projection, Fog, Sampler0 block atlas, Sampler2 lightmap) so it drops into the same render pass.
 */
public final class QuillTerrainPipeline {
	public static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath("quillcore", "core/quill_solid");

	/** 4×SHORT, integer attribute (POSITION usage + non-float type ⇒ ivec4 in GLSL). */
	public static final VertexFormatElement POSITION_S16 = registerFree(VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.POSITION, 4);
	/** 2×SHORT texture coordinate, integer attribute (ivec2); low 16 bits are the unsigned value. */
	public static final VertexFormatElement UV0_U16 = registerFree(VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);

	public static final VertexFormat FORMAT = VertexFormat.builder()
			.add("Position", POSITION_S16)
			.add("Color", VertexFormatElement.COLOR)
			.add("UV0", UV0_U16)
			.add("UV2", VertexFormatElement.UV2)
			.build();

	public static final RenderPipeline SOLID = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
					.withLocation(ResourceLocation.fromNamespaceAndPath("quillcore", "pipeline/quill_solid"))
					.withVertexShader(SHADER)
					.withFragmentShader(SHADER)
					.withSampler("Sampler0")
					.withSampler("Sampler2")
					.withVertexFormat(FORMAT, VertexFormat.Mode.QUADS)
					.build());

	static {
		if (FORMAT.getVertexSize() != QuillCompactVertex.BYTES) {
			throw new IllegalStateException("Quill vertex format is " + FORMAT.getVertexSize() + " B, expected " + QuillCompactVertex.BYTES);
		}
	}

	private QuillTerrainPipeline() {}

	/** Forces class init (format + pipeline registration) during client init, before the first resource reload. */
	public static void bootstrap() {
		QuillCore.LOGGER.info("[quill-render] compact SOLID format {} B/vertex, pipeline {}", FORMAT.getVertexSize(), SOLID.getLocation());
	}

	/** Element ids are a global 32-slot table; take the highest free one so we don't collide with vanilla's low ids. */
	private static VertexFormatElement registerFree(VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
		for (int id = VertexFormatElement.MAX_COUNT - 1; id >= 0; id--) {
			if (VertexFormatElement.byId(id) != null) continue;
			return VertexFormatElement.register(id, 0, type, usage, count);
		}
		throw new IllegalStateException("No free VertexFormatElement id");
	}
}
