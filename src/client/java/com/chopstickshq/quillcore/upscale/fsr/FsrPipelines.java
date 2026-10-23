package com.chopstickshq.quillcore.upscale.fsr;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

/**
 * FSR1 RenderPipelines (EASU, RCAS, blit). Registered into {@link RenderPipelines}
 * so ShaderManager can compile them on reload.
 */
public final class FsrPipelines {
	public static final ResourceLocation EASU_SHADER = ResourceLocation.fromNamespaceAndPath("quillcore", "core/fsr_easu");
	public static final ResourceLocation RCAS_SHADER = ResourceLocation.fromNamespaceAndPath("quillcore", "core/fsr_rcas");
	public static final ResourceLocation BLIT_SHADER = ResourceLocation.fromNamespaceAndPath("quillcore", "core/fsr_blit");

	public static final RenderPipeline EASU;
	public static final RenderPipeline RCAS;
	public static final RenderPipeline BLIT;

	static {
		EASU = RenderPipelines.register(
				RenderPipeline.builder()
						.withLocation(ResourceLocation.fromNamespaceAndPath("quillcore", "pipeline/fsr_easu"))
						.withVertexShader(EASU_SHADER)
						.withFragmentShader(EASU_SHADER)
						.withSampler("InSampler")
						.withUniform("FsrConfig", UniformType.UNIFORM_BUFFER)
						.withDepthWrite(false)
						.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
						.withColorWrite(true, false)
						.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
						.build()
		);
		RCAS = RenderPipelines.register(
				RenderPipeline.builder()
						.withLocation(ResourceLocation.fromNamespaceAndPath("quillcore", "pipeline/fsr_rcas"))
						.withVertexShader(RCAS_SHADER)
						.withFragmentShader(RCAS_SHADER)
						.withSampler("InSampler")
						.withUniform("FsrConfig", UniformType.UNIFORM_BUFFER)
						.withDepthWrite(false)
						.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
						.withColorWrite(true, false)
						.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
						.build()
		);
		BLIT = RenderPipelines.register(
				RenderPipeline.builder()
						.withLocation(ResourceLocation.fromNamespaceAndPath("quillcore", "pipeline/fsr_blit"))
						.withVertexShader(BLIT_SHADER)
						.withFragmentShader(BLIT_SHADER)
						.withSampler("InSampler")
						.withDepthWrite(false)
						.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
						.withColorWrite(true, false)
						.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
						.build()
		);
	}

	private FsrPipelines() {}

	/** Force class init / pipeline registration. */
	public static void bootstrap() {
		// touch static fields
		if (EASU == null || RCAS == null || BLIT == null) {
			throw new IllegalStateException("FSR pipelines failed to register");
		}
	}
}
