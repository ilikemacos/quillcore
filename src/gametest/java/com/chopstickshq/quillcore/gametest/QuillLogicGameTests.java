package com.chopstickshq.quillcore.gametest;

import com.chopstickshq.quillcore.common.QuillConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Behaviour-equivalence GameTests. */
public class QuillLogicGameTests {
	private static Component msg(String s) {
		return Component.literal(s);
	}

	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(x, 0, z, Blocks.STONE);
	}

	@GameTest
	public void modeMatchesRequest(GameTestHelper h) {
		boolean want = QuillTestMode.logicOn();
		QuillConfig c = QuillConfig.get();
		h.assertValueEqual(c.masterEnabled, want, msg("masterEnabled"));
		h.assertValueEqual(c.itemMergeThrottleEnabled, want, msg("itemMergeThrottleEnabled"));
		h.succeed();
	}

	/**
	 * Resting items scan for merges every 40 ticks; with maxSkip=10 the throttle may defer a merge by
	 * up to ~440 ticks. Outcome must still be identical (one stack of 2), so allow 1000 ticks.
	 */
	@GameTest(maxTicks = 1000)
	public void itemStacksStillMerge(GameTestHelper h) {
		floor(h);
		h.spawnItem(Items.DIRT, 3.5f, 1.2f, 3.5f);
		h.spawnItem(Items.DIRT, 3.6f, 1.2f, 3.4f);
		h.succeedWhen(() -> {
			List<ItemEntity> items = h.getEntities(EntityType.ITEM);
			h.assertValueEqual(items.size(), 1, msg("item entity count"));
			h.assertValueEqual(items.get(0).getItem().getCount(), 2, msg("merged stack size"));
		});
	}

	@GameTest(maxTicks = 200)
	public void differentItemsNeverMerge(GameTestHelper h) {
		floor(h);
		h.spawnItem(Items.DIRT, 3.5f, 1.2f, 3.5f);
		h.spawnItem(Items.COBBLESTONE, 3.5f, 1.2f, 3.5f);
		h.runAfterDelay(120, () -> {
			h.assertValueEqual(h.getEntities(EntityType.ITEM).size(), 2, msg("distinct items stay separate"));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void itemAgeAdvancesEveryTick(GameTestHelper h) {
		floor(h);
		ItemEntity item = h.spawnItem(Items.STICK, 2.5f, 1.2f, 2.5f);
		int start = item.getAge();
		h.runAfterDelay(100, () -> {
			int aged = item.getAge() - start;
			h.assertTrue(aged >= 99 && aged <= 101, msg("item age advanced " + aged + " in 100 ticks"));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void hopperCollectsDroppedItem(GameTestHelper h) {
		floor(h);
		BlockPos hopper = new BlockPos(3, 1, 3);
		h.setBlock(hopper, Blocks.HOPPER);
		h.spawnItem(Items.DIAMOND, 3.5f, 2.5f, 3.5f);
		h.succeedWhen(() -> {
			if (!(h.getLevel().getBlockEntity(h.absolutePos(hopper)) instanceof Container c)) {
				h.fail(msg("hopper block entity missing"));
				return;
			}
			h.assertValueEqual(c.countItem(Items.DIAMOND), 1, msg("diamonds in hopper"));
			h.assertValueEqual(h.getEntities(EntityType.ITEM).size(), 0, msg("item entity consumed"));
		});
	}

	@GameTest(maxTicks = 400)
	public void mobFollowsActivePath(GameTestHelper h) {
		floor(h);
		Cow cow = h.spawn(EntityType.COW, new BlockPos(1, 1, 1));
		Vec3 target = h.absoluteVec(new Vec3(6.5, 1, 6.5));
		h.succeedWhen(() -> {
			if (cow.getNavigation().isDone()) {
				cow.getNavigation().moveTo(target.x, target.y, target.z, 1.2);
			}
			double dx = cow.getX() - target.x, dz = cow.getZ() - target.z;
			h.assertTrue(dx * dx + dz * dz < 1.5 * 1.5, msg("cow reached target"));
		});
	}

	@GameTest(maxTicks = 200)
	public void villagerBrainKeepsTicking(GameTestHelper h) {
		floor(h);
		Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(4, 1, 4));
		h.runAfterDelay(100, () -> {
			h.assertTrue(v.isAlive(), msg("villager alive"));
			h.assertTrue(!v.getBrain().getActiveActivities().isEmpty(), msg("brain has an active activity"));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void mobFallsWithGravity(GameTestHelper h) {
		floor(h);
		Cow cow = h.spawn(EntityType.COW, new BlockPos(4, 6, 4));
		h.succeedWhen(() -> h.assertTrue(cow.onGround() && cow.getY() < h.absoluteVec(new Vec3(0, 1.5, 0)).y,
				msg("cow landed on floor")));
	}
}
