package com.chopstickshq.quillcore.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.PathfinderMob;

import com.chopstickshq.quillcore.logic.HardColliders;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Vanilla outcomes the always-on logic patches must not change. */
public class QuillVanillaGameTests {
	private static Component msg(String s) {
		return Component.literal(s);
	}

	private static void floor(GameTestHelper h) {
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) h.setBlock(x, 0, z, Blocks.STONE);
	}

	private static Villager villager(GameTestHelper h, BlockPos pos, net.minecraft.resources.ResourceKey<VillagerProfession> job) {
		Villager v = h.spawn(EntityType.VILLAGER, pos);
		v.setVillagerXp(1); // keeps the job without a workstation
		v.setVillagerData(v.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(job)));
		// setVillagerData keeps the brain built for the spawn profession (NONE), which still hunts any job site and
		// fills POTENTIAL_JOB_SITE on its own schedule; rebuild it as a villager loaded with this job would have.
		v.refreshBrain(h.getLevel());
		return v;
	}

	// drive the sensor by hand; brain timing in a test world is not what we're checking
	private static void scan(GameTestHelper h, Villager v) {
		net.minecraft.world.entity.ai.sensing.SecondaryPoiSensor s = new net.minecraft.world.entity.ai.sensing.SecondaryPoiSensor();
		for (int i = 0; i < 41; i++) s.tick(h.getLevel(), v);
	}

	@GameTest(maxTicks = 200)
	public void farmerStillFindsFarmland(GameTestHelper h) {
		floor(h);
		h.setBlock(5, 0, 4, Blocks.FARMLAND);
		h.setBlock(5, 1, 4, Blocks.WHEAT);
		Villager v = villager(h, new BlockPos(4, 1, 4), VillagerProfession.FARMER);
		GlobalPos want = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(5, 0, 4)));
		h.runAtTickTime(1, () -> {
			scan(h, v);
			Optional<List<GlobalPos>> sites = v.getBrain().getMemoryInternal(MemoryModuleType.SECONDARY_JOB_SITE);
			h.assertTrue(sites.isPresent() && sites.get().contains(want), msg("farmer remembers farmland, has " + sites + " want " + want + " job " + v.getVillagerData().profession()));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void otherJobsIgnoreFarmland(GameTestHelper h) {
		floor(h);
		h.setBlock(5, 0, 4, Blocks.FARMLAND);
		Villager v = villager(h, new BlockPos(4, 1, 4), VillagerProfession.TOOLSMITH);
		v.getBrain().setMemory(MemoryModuleType.SECONDARY_JOB_SITE, List.of(GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(5, 0, 4)))));
		h.runAtTickTime(1, () -> {
			scan(h, v);
			h.assertTrue(v.getBrain().getMemoryInternal(MemoryModuleType.SECONDARY_JOB_SITE).isEmpty(), msg("no secondary site"));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void wireLightsAndUnlightsLamp(GameTestHelper h) {
		floor(h);
		h.setBlock(4, 1, 2, Blocks.REDSTONE_LAMP);
		h.setBlock(2, 1, 2, Blocks.REDSTONE_WIRE);
		h.setBlock(3, 1, 2, Blocks.REDSTONE_WIRE);
		h.setBlock(1, 1, 2, Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(4, () -> {
			h.assertBlockProperty(new BlockPos(4, 1, 2), RedstoneLampBlock.LIT, true);
			h.destroyBlock(new BlockPos(3, 1, 2));
			h.runAfterDelay(10, () -> {
				h.assertBlockProperty(new BlockPos(4, 1, 2), RedstoneLampBlock.LIT, false);
				h.assertBlockProperty(new BlockPos(2, 1, 2), net.minecraft.world.level.block.RedStoneWireBlock.POWER, 15);
				h.succeed();
			});
		});
	}

	@GameTest(maxTicks = 100)
	public void pistonPushesAndSlimeDrags(GameTestHelper h) {
		floor(h);
		BlockState east = Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST);
		h.setBlock(new BlockPos(1, 1, 2), east);
		h.setBlock(2, 1, 2, Blocks.COBBLESTONE);
		// slime row sits a block above the floor so it only drags what we put on it
		h.setBlock(new BlockPos(1, 2, 5), east);
		h.setBlock(2, 2, 5, Blocks.SLIME_BLOCK);
		h.setBlock(2, 3, 5, Blocks.COBBLESTONE);
		h.setBlock(0, 1, 2, Blocks.REDSTONE_BLOCK);
		h.setBlock(0, 2, 5, Blocks.REDSTONE_BLOCK);
		h.succeedWhen(() -> {
			h.assertBlockPresent(Blocks.COBBLESTONE, 3, 1, 2);
			h.assertBlockPresent(Blocks.SLIME_BLOCK, 3, 2, 5);
			h.assertBlockPresent(Blocks.COBBLESTONE, 3, 3, 5);
			h.assertBlockNotPresent(Blocks.COBBLESTONE, 2, 3, 5);
		});
	}

	@GameTest
	public void sectionWritesReadBack(GameTestHelper h) {
		BlockState[] pick = {Blocks.STONE.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), Blocks.AIR.defaultBlockState()};
		for (int x = 0; x < 8; x++) for (int y = 1; y < 8; y++) for (int z = 0; z < 8; z++)
			h.setBlock(new BlockPos(x, y, z), pick[(x * 7 + y * 3 + z) & 3]);
		for (int x = 0; x < 8; x++) for (int y = 1; y < 8; y++) for (int z = 0; z < 8; z++) {
			BlockState got = h.getBlockState(new BlockPos(x, y, z));
			if (got != pick[(x * 7 + y * 3 + z) & 3]) {
				h.fail(msg("block at " + x + "," + y + "," + z + " is " + got));
			}
		}
		h.succeed();
	}

	// run a job-site search by hand every tick and collect what it remembers.
	// Read the memory right after the search; the villager's brain is built for its real job (see villager()).
	private static void searchJobs(GameTestHelper h, Villager v, java.util.function.Predicate<net.minecraft.core.Holder<PoiType>> type, java.util.function.Consumer<List<GlobalPos>> check) {
		BehaviorControl<PathfinderMob> search = AcquirePoi.create(type, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true, Optional.empty(), (l, p) -> true);
		List<GlobalPos> found = new java.util.ArrayList<>();
		for (int t = 1; t <= 60; t++) h.runAtTickTime(t, () -> {
			search.tryStart(h.getLevel(), v, h.getLevel().getGameTime());
			v.getBrain().getMemoryInternal(MemoryModuleType.POTENTIAL_JOB_SITE).ifPresent(found::add);
		});
		h.runAtTickTime(61, () -> check.accept(found));
	}

	@GameTest(maxTicks = 200)
	public void jobSearchStillFindsLectern(GameTestHelper h) {
		floor(h);
		h.setBlock(5, 1, 4, Blocks.LECTERN);
		Villager v = villager(h, new BlockPos(4, 1, 4), VillagerProfession.NITWIT);
		searchJobs(h, v, p -> p.is(PoiTypes.LIBRARIAN), found -> {
			GlobalPos want = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(5, 1, 4)));
			// the search takes the lectern's ticket when it claims it, so a taken ticket also counts as found
			boolean claimed = h.getLevel().getPoiManager().getType(want.pos()).isPresent()
					&& h.getLevel().getPoiManager().getFreeTickets(want.pos()) == 0;
			h.assertTrue(found.contains(want) || claimed, msg("lectern found, got " + found));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void nitwitFindsNoJob(GameTestHelper h) {
		floor(h);
		h.setBlock(5, 1, 4, Blocks.LECTERN);
		Villager v = villager(h, new BlockPos(4, 1, 4), VillagerProfession.NITWIT);
		searchJobs(h, v, PoiType.NONE, found -> {
			h.assertTrue(found.isEmpty(), msg("no job site, got " + found));
			h.assertTrue(v.getVillagerData().profession().is(VillagerProfession.NITWIT), msg("still a nitwit"));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void shulkerStillBlocksWalkers(GameTestHelper h) {
		floor(h);
		Shulker s = h.spawn(EntityType.SHULKER, 5, 1, 3);
		s.setNoAi(true);
		Cow c = h.spawn(EntityType.COW, 1, 1, 3);
		c.setNoAi(true);
		if (!HardColliders.mayCollide(s) || HardColliders.mayCollide(c)) h.fail(msg("collider classes misread"));
		h.onEachTick(() -> c.move(MoverType.SELF, new Vec3(0.2, 0, 0)));
		h.runAtTickTime(60, () -> {
			double gap = s.getBoundingBox().minX - c.getBoundingBox().maxX;
			if (gap < -1.0E-6) h.fail(msg("cow walked into the shulker, gap " + gap));
			if (gap > 0.5) h.fail(msg("cow never reached the shulker, gap " + gap));
			h.succeed();
		});
	}
}
