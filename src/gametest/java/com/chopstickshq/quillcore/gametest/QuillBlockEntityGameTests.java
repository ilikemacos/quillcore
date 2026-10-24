package com.chopstickshq.quillcore.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

/** Idle block entity fast paths must still wake up and behave like vanilla. */
public class QuillBlockEntityGameTests {
	private static final BlockPos AT = new BlockPos(1, 1, 1);

	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) h.setBlock(x, 0, z, Blocks.STONE);
	}

	private static AbstractFurnaceBlockEntity furnace(GameTestHelper h) {
		floor(h);
		h.setBlock(AT, Blocks.FURNACE);
		return h.getBlockEntity(AT, AbstractFurnaceBlockEntity.class);
	}

	@GameTest(maxTicks = 300)
	public void idleFurnaceWakesAndSmelts(GameTestHelper h) {
		AbstractFurnaceBlockEntity f = furnace(h);
		f.setItem(0, new ItemStack(Items.RAW_IRON));
		h.runAfterDelay(20, () -> {
			h.assertTrue(f.getItem(2).isEmpty(), Component.literal("no fuel, no output"));
			f.setItem(1, new ItemStack(Items.COAL));
			h.succeedWhen(() -> h.assertTrue(f.getItem(2).is(Items.IRON_INGOT), Component.literal("smelted")));
		});
	}

	@GameTest(maxTicks = 100)
	public void fuelAloneDoesNotBurn(GameTestHelper h) {
		AbstractFurnaceBlockEntity f = furnace(h);
		f.setItem(1, new ItemStack(Items.COAL));
		h.runAfterDelay(40, () -> {
			h.assertTrue(f.getItem(1).getCount() == 1, Component.literal("coal kept"));
			h.assertBlockProperty(AT, net.minecraft.world.level.block.AbstractFurnaceBlock.LIT, false);
			h.succeed();
		});
	}

	@GameTest(maxTicks = 700) // beef takes 600 ticks
	public void campfireCooksAfterIdle(GameTestHelper h) {
		floor(h);
		h.setBlock(AT, Blocks.CAMPFIRE);
		CampfireBlockEntity c = h.getBlockEntity(AT, CampfireBlockEntity.class);
		h.runAfterDelay(20, () -> {
			c.placeFood(h.getLevel(), null, new ItemStack(Items.BEEF));
			h.succeedWhen(() -> h.assertItemEntityPresent(Items.COOKED_BEEF));
		});
	}
}
