package com.chopstickshq.quillcore.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StateTableTest {
	// three "properties": 2 x 3 x 4 values, states named by their ordinals
	private static StateTable<String, String> grid() {
		StateTable<String, String> t = new StateTable<>(new String[]{"a", "b", "c"}, new int[]{2, 3, 4});
		for (int a = 0; a < 2; a++)
			for (int b = 0; b < 3; b++)
				for (int c = 0; c < 4; c++)
					t.put(t.index(new int[]{a, b, c}), "" + a + b + c);
		return t;
	}

	@Test
	void sizeIsProduct() {
		assertEquals(24, grid().size());
	}

	@Test
	void everyNeighbourChangesOneOrdinal() {
		StateTable<String, String> t = grid();
		String[] keys = {"a", "b", "c"};
		int[] sizes = {2, 3, 4};
		for (int a = 0; a < 2; a++)
			for (int b = 0; b < 3; b++)
				for (int c = 0; c < 4; c++) {
					int[] o = {a, b, c};
					int idx = t.index(o);
					for (int k = 0; k < 3; k++)
						for (int v = 0; v < sizes[k]; v++) {
							int[] want = o.clone();
							want[k] = v;
							assertEquals("" + want[0] + want[1] + want[2], t.neighbour(idx, keys[k], o[k], v));
						}
				}
	}

	@Test
	void keysCompareByIdentity() {
		StateTable<String, String> t = grid();
		String copy = new String("a");
		assertThrows(IllegalArgumentException.class, () -> t.neighbour(0, copy, 0, 1));
	}

	@Test
	void noKeysIsOneState() {
		StateTable<String, String> t = new StateTable<>(new String[0], new int[0]);
		assertEquals(1, t.size());
		assertEquals(0, t.index(new int[0]));
	}
}
