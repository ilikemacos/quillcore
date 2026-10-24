package com.chopstickshq.quillcore.csr.client;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

/** A fullscreen fragment program with a uniform-location cache. */
final class CsrPass implements AutoCloseable {
	private final String name;
	private final int program;
	private final Map<String, Integer> locations = new HashMap<>();

	CsrPass(String fragmentName) {
		this.name = fragmentName;
		this.program = CsrGl.program(fragmentName);
	}

	String name() {
		return name;
	}

	CsrPass use() {
		GlStateManager._glUseProgram(program);
		return this;
	}

	private int loc(String uniform) {
		// -1 (optimised out) is cached too; glUniform ignores it.
		return locations.computeIfAbsent(uniform, u -> GL20.glGetUniformLocation(program, u));
	}

	CsrPass i(String u, int v) {
		GL20.glUniform1i(loc(u), v);
		return this;
	}

	CsrPass i(String u, int x, int y) {
		GL20.glUniform2i(loc(u), x, y);
		return this;
	}

	CsrPass f(String u, float v) {
		GL20.glUniform1f(loc(u), v);
		return this;
	}

	CsrPass f(String u, float x, float y) {
		GL20.glUniform2f(loc(u), x, y);
		return this;
	}

	CsrPass f(String u, float x, float y, float z) {
		GL20.glUniform3f(loc(u), x, y, z);
		return this;
	}

	/** Bind a texture to {@code unit} and point sampler uniform {@code u} at it. */
	CsrPass tex(String u, int unit, int texture, int sampler) {
		CsrGl.bind(unit, texture, sampler);
		return i(u, unit);
	}

	@Override
	public void close() {
		CsrGl.deleteProgram(program);
	}
}
