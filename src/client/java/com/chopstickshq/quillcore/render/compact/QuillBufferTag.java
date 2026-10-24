package com.chopstickshq.quillcore.render.compact;

/** Duck interface mixed into {@code GpuBuffer}: true while the buffer holds Quill-format SOLID vertices. */
public interface QuillBufferTag {
	boolean quillcore$isCompact();

	void quillcore$setCompact(boolean compact);

	static boolean isCompact(Object gpuBuffer) {
		return gpuBuffer instanceof QuillBufferTag t && t.quillcore$isCompact();
	}
}
