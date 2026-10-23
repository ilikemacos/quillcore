package com.chopstickshq.quillcore.upscale.fsr;

import com.chopstickshq.quillcore.common.QuillCore;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MappableRingBuffer;

import java.util.OptionalInt;

/**
 * Runs AMD FSR1 EASU then RCAS fullscreen passes via Minecraft RenderPass.
 */
public final class Fsr1Upscaler implements AutoCloseable {
	private static final int UBO_SIZE = new Std140SizeCalculator()
			.putVec2().putVec2().putVec2().putFloat().putFloat()
			.get();

	/** USAGE_MAP_WRITE | USAGE_UNIFORM */
	private static final int UBO_USAGE = GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM;

	private final MappableRingBuffer configUbo;
	private TextureTarget easuTarget;
	private TextureTarget rcasTarget;
	private boolean failed;

	public Fsr1Upscaler() {
		this.configUbo = new MappableRingBuffer(() -> "QuillCore FSR1 FsrConfig", UBO_USAGE, UBO_SIZE);
	}

	public boolean hasFailed() {
		return failed;
	}

	public void markFailed(String reason, Throwable t) {
		if (!failed) {
			failed = true;
			QuillCore.LOGGER.error("FSR1 upscaler disabled: {}", reason, t);
		}
	}

	private void ensureTargets(int width, int height) {
		if (easuTarget == null) {
			easuTarget = new TextureTarget("quillcore_fsr_easu", width, height, false);
		} else if (easuTarget.width != width || easuTarget.height != height) {
			easuTarget.resize(width, height);
		}
		if (rcasTarget == null) {
			rcasTarget = new TextureTarget("quillcore_fsr_rcas", width, height, false);
		} else if (rcasTarget.width != width || rcasTarget.height != height) {
			rcasTarget.resize(width, height);
		}
	}

	/**
	 * EASU+RCAS from low-res {@code main} into full-res scratch, resize {@code main} to native,
	 * blit result back. Call while {@code main} still holds the low-res world color.
	 */
	public void upscaleIntoRestoredMain(RenderTarget main, int inW, int inH, int outW, int outH, float sharpness) {
		if (failed) {
			return;
		}
		ensureTargets(outW, outH);
		main.setFilterMode(FilterMode.LINEAR);

		writeConfig(inW, inH, inW, inH, outW, outH, sharpness);
		drawPass("FSR1 EASU", FsrPipelines.EASU, main, easuTarget, true);

		writeConfig(outW, outH, outW, outH, outW, outH, sharpness);
		easuTarget.setFilterMode(FilterMode.LINEAR);
		drawPass("FSR1 RCAS", FsrPipelines.RCAS, easuTarget, rcasTarget, true);

		main.resize(outW, outH);
		rcasTarget.setFilterMode(FilterMode.LINEAR);
		drawPass("FSR1 blit", FsrPipelines.BLIT, rcasTarget, main, false);

		configUbo.rotate();
	}

	private void writeConfig(float viewportW, float viewportH, float inputW, float inputH,
			float outputW, float outputH, float sharpness) {
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView mapped = encoder.mapBuffer(configUbo.currentBuffer(), false, true)) {
			Std140Builder b = Std140Builder.intoBuffer(mapped.data());
			b.putVec2(viewportW, viewportH);
			b.putVec2(inputW, inputH);
			b.putVec2(outputW, outputH);
			b.putFloat(sharpness);
			b.putFloat(0.0f);
		}
	}

	private void drawPass(String label, RenderPipeline pipeline, RenderTarget in, RenderTarget out, boolean bindConfig) {
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		GpuBuffer quad = RenderSystem.getQuadVertexBuffer();
		RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		GpuBuffer indexBuf = indices.getBuffer(6);

		try (RenderPass pass = encoder.createRenderPass(
				() -> label,
				out.getColorTextureView(),
				OptionalInt.empty()
		)) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			if (bindConfig) {
				pass.setUniform("FsrConfig", configUbo.currentBuffer());
			}
			pass.setVertexBuffer(0, quad);
			pass.setIndexBuffer(indexBuf, indices.type());
			pass.bindSampler("InSampler", in.getColorTextureView());
			pass.drawIndexed(0, 0, 6, 1);
		}
	}

	public static int uboSize() {
		return UBO_SIZE;
	}

	@Override
	public void close() {
		configUbo.close();
		if (easuTarget != null) {
			easuTarget.destroyBuffers();
			easuTarget = null;
		}
		if (rcasTarget != null) {
			rcasTarget.destroyBuffers();
			rcasTarget = null;
		}
	}
}
