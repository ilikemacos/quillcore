package com.chopstickshq.quillcore.csr.client;

import com.chopstickshq.quillcore.csr.CsrLayer;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Raw GL helpers for the CSR passes. Minecraft 1.21.8's GpuTexture only offers 8-bit colour
 * formats, so the networks run on our own RGBA16F textures/FBOs. Every state change that
 * {@link GlStateManager} caches goes through it, and {@link Saved} restores everything else.
 */
final class CsrGl {
	/** Max RGBA slabs per conv pass (20 channels; netA needs 18). */
	static final int MAX_SLABS = 5;
	/** Texture units we may touch; GlStateManager tracks 12. */
	static final int UNITS = 12;
	static final int WEIGHT_TEX_WIDTH = 1024;
	private static final String GLSL_DIR = "/assets/quillcore/csr/glsl/";

	private CsrGl() {}

	static final class GlException extends RuntimeException {
		GlException(String msg) {
			super(msg);
		}
	}

	/** GLSL 3.30 is available; otherwise sources are compiled as 1.50 + explicit_attrib_location. */
	private static boolean glsl330 = true;
	private static boolean timers;

	/**
	 * Minecraft asks for a 3.2 core context, so check the features the passes use rather than a
	 * version: MRT float targets (3.0), sampler objects and explicit fragment output locations
	 * (3.3 or their ARB extensions). Timer queries are optional.
	 *
	 * @return null when the required GL features are present, otherwise the reason.
	 */
	static String capsProblem() {
		GLCapabilities caps = GL.getCapabilities();
		if (caps == null) {
			return "no GL capabilities on this thread";
		}
		String version = GL11.glGetString(GL11.GL_VERSION);
		if (!caps.OpenGL32) {
			return "needs OpenGL 3.2, driver reports " + version;
		}
		if (!caps.OpenGL33 && !caps.GL_ARB_explicit_attrib_location) {
			return "needs GL_ARB_explicit_attrib_location (" + version + ")";
		}
		if (!caps.OpenGL33 && !caps.GL_ARB_sampler_objects) {
			return "needs GL_ARB_sampler_objects (" + version + ")";
		}
		int drawBuffers = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
		int attachments = GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS);
		if (drawBuffers < MAX_SLABS || attachments < MAX_SLABS) {
			return "needs " + MAX_SLABS + " colour attachments, driver offers " + Math.min(drawBuffers, attachments);
		}
		if (GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS) < UNITS) {
			return "needs " + UNITS + " fragment texture units";
		}
		glsl330 = caps.OpenGL33;
		timers = caps.OpenGL33 || caps.GL_ARB_timer_query;
		return null;
	}

	static boolean timersAvailable() {
		return timers;
	}

	// ---- shaders ----

	static String source(String name) {
		try (InputStream in = CsrGl.class.getResourceAsStream(GLSL_DIR + name)) {
			if (in == null) {
				throw new GlException("missing shader source " + name);
			}
			String src = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			StringBuilder out = new StringBuilder();
			for (String line : src.split("\n", -1)) {
				String t = line.trim();
				if (t.startsWith("#version 330") && !glsl330) {
					out.append("#version 150 core\n#extension GL_ARB_explicit_attrib_location : require\n");
				} else if (t.startsWith("#include ")) {
					out.append(source(t.substring(9).trim() + ".glsl")).append('\n');
				} else {
					out.append(line).append('\n');
				}
			}
			return out.toString();
		} catch (IOException e) {
			throw new GlException("cannot read shader " + name + ": " + e.getMessage());
		}
	}

	static int program(String fragmentName) {
		int vs = shader(GL20.GL_VERTEX_SHADER, "fullscreen.vsh");
		int fs;
		try {
			fs = shader(GL20.GL_FRAGMENT_SHADER, fragmentName);
		} catch (GlException e) {
			GL20.glDeleteShader(vs);
			throw e;
		}
		int p = GL20.glCreateProgram();
		GL20.glAttachShader(p, vs);
		GL20.glAttachShader(p, fs);
		GL20.glLinkProgram(p);
		GL20.glDeleteShader(vs);
		GL20.glDeleteShader(fs);
		if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			String log = GL20.glGetProgramInfoLog(p, 4096);
			GL20.glDeleteProgram(p);
			throw new GlException("link failed for " + fragmentName + ": " + log.trim());
		}
		return p;
	}

	private static int shader(int type, String name) {
		int s = GL20.glCreateShader(type);
		GL20.glShaderSource(s, source(name));
		GL20.glCompileShader(s);
		if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			String log = GL20.glGetShaderInfoLog(s, 4096);
			GL20.glDeleteShader(s);
			throw new GlException("compile failed for " + name + ": " + log.trim());
		}
		return s;
	}

	// ---- textures / framebuffers ----

	static int texture(int w, int h, int internalFormat, int format, int type, boolean linear) {
		int t = GlStateManager._genTexture();
		GlStateManager._activeTexture(GL13.GL_TEXTURE0);
		GlStateManager._bindTexture(t);
		int filter = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
		withUnpackDefaults(() -> GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, format, type, (IntBuffer) null));
		return t;
	}

	static int featureTexture(int w, int h) {
		return texture(w, h, GL30.GL_RGBA16F, GL11.GL_RGBA, GL30.GL_HALF_FLOAT, true);
	}

	static int colorTexture(int w, int h) {
		return texture(w, h, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, true);
	}

	/** Allow a full mip chain (used for the frame-mean reduction). */
	static void enableMips(int tex) {
		GlStateManager._activeTexture(GL13.GL_TEXTURE0);
		GlStateManager._bindTexture(tex);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 1000);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST);
		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
	}

	static void generateMips(int tex) {
		GlStateManager._activeTexture(GL13.GL_TEXTURE0);
		GlStateManager._bindTexture(tex);
		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
	}

	/**
	 * Concatenate all layer weights then biases into one R32F texture.
	 * Fills {@code wOff}/{@code bOff} with each layer's texel offsets.
	 */
	static int weightTexture(List<CsrLayer> layers, int[] wOff, int[] bOff) {
		int n = 0;
		for (int i = 0; i < layers.size(); i++) {
			wOff[i] = n;
			n += layers.get(i).w().length;
			bOff[i] = n;
			n += layers.get(i).b().length;
		}
		int rows = (n + WEIGHT_TEX_WIDTH - 1) / WEIGHT_TEX_WIDTH;
		float[] data = new float[rows * WEIGHT_TEX_WIDTH];
		for (int i = 0; i < layers.size(); i++) {
			System.arraycopy(layers.get(i).w(), 0, data, wOff[i], layers.get(i).w().length);
			System.arraycopy(layers.get(i).b(), 0, data, bOff[i], layers.get(i).b().length);
		}
		int t = texture(WEIGHT_TEX_WIDTH, rows, GL30.GL_R32F, GL11.GL_RED, GL11.GL_FLOAT, false);
		withUnpackDefaults(() -> {
			try (MemoryStack stack = MemoryStack.stackPush()) {
				FloatBuffer fb = data.length * 4 <= 32 * 1024 ? stack.mallocFloat(data.length) : null;
				if (fb == null) {
					fb = org.lwjgl.BufferUtils.createFloatBuffer(data.length);
				}
				fb.put(data).flip();
				GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, WEIGHT_TEX_WIDTH, rows, GL11.GL_RED, GL11.GL_FLOAT, fb);
			}
		});
		return t;
	}

	/** Run an upload with no PBO bound and default unpack alignment, then restore both. */
	private static void withUnpackDefaults(Runnable r) {
		int pbo = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
		int align = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
		int rowLen = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
		int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
		int skipPx = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
		GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
		GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
		GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
		try {
			r.run();
		} finally {
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, align);
			GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLen);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPx);
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pbo);
		}
	}

	/** FBO with the given colour textures on attachments 0..n-1, all enabled as draw buffers. */
	static int fbo(int... colorTex) {
		int f = GlStateManager.glGenFramebuffers();
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, f);
		int[] bufs = new int[colorTex.length];
		for (int i = 0; i < colorTex.length; i++) {
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, GL11.GL_TEXTURE_2D, colorTex[i], 0);
			bufs[i] = GL30.GL_COLOR_ATTACHMENT0 + i;
		}
		GL20.glDrawBuffers(bufs);
		int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			GlStateManager._glDeleteFramebuffers(f);
			throw new GlException("framebuffer incomplete (0x" + Integer.toHexString(status) + ")");
		}
		return f;
	}

	static void deleteTextures(int... tex) {
		for (int t : tex) {
			if (t != 0) {
				GlStateManager._deleteTexture(t);
			}
		}
	}

	static void deleteFbos(int... fbos) {
		for (int f : fbos) {
			if (f != 0) {
				GlStateManager._glDeleteFramebuffers(f);
			}
		}
	}

	static void deleteProgram(int p) {
		if (p != 0) {
			GL20.glDeleteProgram(p);
		}
	}

	static void drainErrors() {
		for (int i = 0; i < 16 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
			// discard errors raised before our section
		}
	}

	static void check(String where) {
		int e = GL11.glGetError();
		if (e != GL11.GL_NO_ERROR) {
			drainErrors();
			throw new GlException("GL error 0x" + Integer.toHexString(e) + " in " + where);
		}
	}

	// ---- drawing ----

	/** Bind {@code tex} on {@code unit} with an optional sampler object (0 = texture's own params). */
	static void bind(int unit, int tex, int sampler) {
		GlStateManager._activeTexture(GL13.GL_TEXTURE0 + unit);
		GlStateManager._bindTexture(tex);
		GL33.glBindSampler(unit, sampler);
	}

	/** Neutral raster state for fullscreen passes. */
	static void neutralState() {
		GlStateManager._disableBlend();
		GlStateManager._disableDepthTest();
		GlStateManager._disableCull();
		GlStateManager._disableScissorTest();
		GlStateManager._disableColorLogicOp();
		GlStateManager._disablePolygonOffset();
		GlStateManager._colorMask(true, true, true, true);
	}

	static void draw(int fbo, int w, int h, int vao) {
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
		GlStateManager._viewport(0, 0, w, h);
		GlStateManager._glBindVertexArray(vao);
		GlStateManager._drawArrays(GL11.GL_TRIANGLES, 0, 3);
	}

	/** Copy colour between framebuffers (0 = default). */
	static void blit(int srcFbo, int sw, int sh, int dstFbo, int dw, int dh, boolean linear) {
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFbo);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstFbo);
		GlStateManager._glBlitFrameBuffer(0, 0, sw, sh, 0, 0, dw, dh, GL11.GL_COLOR_BUFFER_BIT,
				linear && (sw != dw || sh != dh) ? GL11.GL_LINEAR : GL11.GL_NEAREST);
	}

	static int linearClampSampler() {
		int s = GL33.glGenSamplers();
		GL33.glSamplerParameteri(s, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(s, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL33.glSamplerParameteri(s, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL33.glSamplerParameteri(s, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		return s;
	}

	/**
	 * Snapshot of the GL state the CSR passes touch. {@link #restore()} writes it back,
	 * through GlStateManager where it keeps a cache so the cache matches the driver again.
	 */
	static final class Saved {
		private int readFbo;
		private int drawFbo;
		private int program;
		private int vao;
		private int activeUnit;
		private final int[] viewport = new int[4];
		private final int[] tex = new int[UNITS];
		private final int[] samplers = new int[UNITS];
		private boolean blend;
		private boolean depth;
		private boolean cull;
		private boolean scissor;
		private boolean logicOp;
		private boolean polyOffset;
		private final boolean[] mask = new boolean[4];

		static Saved capture() {
			Saved s = new Saved();
			s.readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
			s.drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
			s.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
			s.vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
			s.activeUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
			try (MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer vp = stack.mallocInt(4);
				GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
				vp.get(s.viewport);
				java.nio.ByteBuffer m = stack.malloc(4);
				GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, m);
				for (int i = 0; i < 4; i++) {
					s.mask[i] = m.get(i) != 0;
				}
			}
			for (int u = 0; u < UNITS; u++) {
				GL13.glActiveTexture(GL13.GL_TEXTURE0 + u);
				s.tex[u] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
				s.samplers[u] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
			}
			GL13.glActiveTexture(s.activeUnit);
			s.blend = GL11.glIsEnabled(GL11.GL_BLEND);
			s.depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
			s.cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
			s.scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
			s.logicOp = GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
			s.polyOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
			return s;
		}

		void restore() {
			for (int u = 0; u < UNITS; u++) {
				GlStateManager._activeTexture(GL13.GL_TEXTURE0 + u);
				GlStateManager._bindTexture(tex[u]);
				GL33.glBindSampler(u, samplers[u]);
			}
			GlStateManager._activeTexture(activeUnit);
			GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
			GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
			GlStateManager._glUseProgram(program);
			GlStateManager._glBindVertexArray(vao);
			GlStateManager._viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
			GlStateManager._colorMask(mask[0], mask[1], mask[2], mask[3]);
			set(blend, GlStateManager::_enableBlend, GlStateManager::_disableBlend);
			set(depth, GlStateManager::_enableDepthTest, GlStateManager::_disableDepthTest);
			set(cull, GlStateManager::_enableCull, GlStateManager::_disableCull);
			set(scissor, GlStateManager::_enableScissorTest, GlStateManager::_disableScissorTest);
			set(logicOp, GlStateManager::_enableColorLogicOp, GlStateManager::_disableColorLogicOp);
			set(polyOffset, GlStateManager::_enablePolygonOffset, GlStateManager::_disablePolygonOffset);
		}

		private static void set(boolean on, Runnable enable, Runnable disable) {
			(on ? enable : disable).run();
		}
	}
}
