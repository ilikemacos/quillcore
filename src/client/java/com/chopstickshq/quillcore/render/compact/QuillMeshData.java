package com.chopstickshq.quillcore.render.compact;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * A vanilla SOLID {@link MeshData} whose vertex bytes were re-encoded in place into {@link QuillTerrainPipeline#FORMAT}.
 * Exposes only the compacted prefix of the original buffer and a draw state with the Quill format; ownership of the
 * underlying {@code ByteBufferBuilder.Result} stays with the original mesh, which this closes.
 */
public final class QuillMeshData extends MeshData {
	private final MeshData original;
	private final DrawState drawState;
	private final int[] faceGroups;

	/** @param faceGroups quads per face group (+X,-X,+Y,-Y,+Z,-Z) in buffer order, or null if not grouped */
	public QuillMeshData(MeshData original, int[] faceGroups) {
		super(null, quillState(original.drawState()));
		this.original = original;
		this.drawState = quillState(original.drawState());
		this.faceGroups = faceGroups;
	}

	public int[] faceGroups() {
		return faceGroups;
	}

	private static DrawState quillState(DrawState s) {
		return new DrawState(QuillTerrainPipeline.FORMAT, s.vertexCount(), s.indexCount(), s.mode(), s.indexType());
	}

	@Override
	public ByteBuffer vertexBuffer() {
		return original.vertexBuffer().slice(0, drawState.vertexCount() * QuillTerrainPipeline.FORMAT.getVertexSize());
	}

	@Nullable
	@Override
	public ByteBuffer indexBuffer() {
		return original.indexBuffer();
	}

	@Override
	public DrawState drawState() {
		return drawState;
	}

	/** SOLID is never sorted; refuse rather than read compact data as BLOCK floats. */
	@Nullable
	@Override
	public SortState sortQuads(ByteBufferBuilder byteBufferBuilder, VertexSorting vertexSorting) {
		return null;
	}

	@Override
	public void close() {
		original.close();
	}
}
