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
import net.minecraft.world.entity.Entity;
import com.chopstickshq.quillcore.logic.CollisionKindHolder;
import com.chopstickshq.quillcore.logic.CollisionKinds;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.core.Direction;
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

	private static Cow still(GameTestHelper h, double x, double y, double z) {
		Cow c = h.spawn(EntityType.COW, new Vec3(x, y, z));
		c.setNoAi(true);
		h.onEachTick(() -> c.move(MoverType.SELF, new Vec3(0, -0.15, 0)));
		return c;
	}

	private static void near(GameTestHelper h, String what, double got, double want) {
		if (Math.abs(got - want) > 1.0E-6) h.fail(msg(what + " is " + got + ", want " + want));
	}

	@GameTest(maxTicks = 100)
	public void wallStopsWalkerAtFace(GameTestHelper h) {
		floor(h);
		for (int z = 0; z < 8; z++) for (int y = 1; y < 3; y++) h.setBlock(5, y, z, Blocks.STONE);
		Cow c = still(h, 1.5, 1, 3.5);
		h.onEachTick(() -> c.move(MoverType.SELF, new Vec3(0.2, 0, 0)));
		h.runAtTickTime(40, () -> {
			near(h, "cow max x", c.getBoundingBox().maxX, h.absolutePos(new BlockPos(5, 1, 3)).getX());
			h.succeed();
		});
	}

	@GameTest(maxTicks = 400)
	public void fallThroughAirSectionsLandsOnFloor(GameTestHelper h) {
		floor(h);
		Cow c = still(h, 3.5, 40, 3.5);
		h.runAtTickTime(350, () -> {
			BlockPos.MutableBlockPos p = h.absolutePos(new BlockPos(3, 39, 3)).mutable();
			while (h.getLevel().getBlockState(p).getCollisionShape(h.getLevel(), p).isEmpty()) p.move(Direction.DOWN);
			h.assertTrue(p.getY() < h.absolutePos(new BlockPos(3, 24, 3)).getY(), msg("no air section crossed, landed at " + p));
			near(h, "cow y", c.getY(), p.getY() + 1);
			h.assertTrue(c.onGround(), msg("cow not on ground"));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void scaffoldingHoldsStander(GameTestHelper h) {
		floor(h);
		h.setBlock(3, 1, 3, Blocks.SCAFFOLDING);
		h.setBlock(3, 2, 3, Blocks.SCAFFOLDING);
		Cow c = still(h, 3.5, 4, 3.5);
		h.runAtTickTime(60, () -> {
			near(h, "cow y on scaffolding", c.getY(), h.absolutePos(new BlockPos(3, 3, 3)).getY());
			h.succeed();
		});
	}

	@GameTest(maxTicks = 200)
	public void powderSnowLetsCowSink(GameTestHelper h) {
		floor(h);
		h.setBlock(3, 1, 3, Blocks.POWDER_SNOW);
		h.setBlock(3, 2, 3, Blocks.POWDER_SNOW);
		Cow c = still(h, 3.5, 3.2, 3.5);
		h.runAtTickTime(150, () -> {
			near(h, "cow y in powder snow", c.getY(), h.absolutePos(new BlockPos(3, 1, 3)).getY());
			h.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void bambooOffsetStopsWalker(GameTestHelper h) {
		floor(h);
		h.setBlock(5, 0, 3, Blocks.DIRT);
		h.setBlock(5, 1, 3, Blocks.BAMBOO);
		Cow c = still(h, 1.5, 1, 3.5);
		h.onEachTick(() -> c.move(MoverType.SELF, new Vec3(0.2, 0, 0)));
		h.runAtTickTime(40, () -> {
			BlockPos b = h.absolutePos(new BlockPos(5, 1, 3));
			double want = h.getLevel().getBlockState(b).getCollisionShape(h.getLevel(), b).move(b).min(Direction.Axis.X);
			near(h, "cow max x at bamboo", c.getBoundingBox().maxX, want);
			h.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void pistonLiftsRider(GameTestHelper h) {
		floor(h);
		h.setBlock(3, 1, 3, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP));
		h.setBlock(3, 2, 3, Blocks.STONE);
		Cow c = still(h, 3.5, 3, 3.5);
		h.runAtTickTime(10, () -> h.setBlock(2, 1, 3, Blocks.REDSTONE_BLOCK));
		h.runAtTickTime(60, () -> {
			h.assertBlockPresent(Blocks.STONE, 3, 3, 3);
			near(h, "cow y after push", c.getY(), h.absolutePos(new BlockPos(3, 4, 3)).getY());
			h.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void supportingBlockTieKeepsVanillaOrder(GameTestHelper h) {
		floor(h);
		h.setBlock(2, 0, 3, Blocks.DIRT);
		BlockPos base = h.absolutePos(BlockPos.ZERO);
		Cow c = still(h, 2, 1, 3.5);
		c.setPos(base.getX() + 2.0, base.getY() + 1.0, base.getZ() + 3.5);
		h.runAtTickTime(10, () -> {
			BlockPos want = h.absolutePos(new BlockPos(2, 0, 3));
			h.assertTrue(c.mainSupportingBlockPos.equals(java.util.Optional.of(want)), msg("supporting " + c.mainSupportingBlockPos + " want " + want));
			h.succeed();
		});
	}

	private static byte kind(net.minecraft.world.level.block.Block b) {
		return ((CollisionKindHolder) b.defaultBlockState()).quillcore$collisionKind();
	}

	@GameTest
	public void collisionKindsSortContextBlocksSlow(GameTestHelper h) {
		h.assertTrue(kind(Blocks.STONE) == CollisionKinds.FULL, msg("stone not full"));
		h.assertTrue(kind(Blocks.AIR) == CollisionKinds.EMPTY && kind(Blocks.SHORT_GRASS) == CollisionKinds.EMPTY, msg("air/grass not empty " + kind(Blocks.AIR) + " " + kind(Blocks.SHORT_GRASS)));
		for (var b : List.of(Blocks.SCAFFOLDING, Blocks.POWDER_SNOW, Blocks.MOVING_PISTON, Blocks.BAMBOO, Blocks.STONE_SLAB, Blocks.OAK_FENCE)) {
			h.assertTrue(kind(b) == CollisionKinds.SLOW, msg(b + " should be slow, is " + kind(b)));
		}
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void blockCollisionsMatchVanilla(GameTestHelper h) {
		BlockState[] mix = {Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE_SLAB.defaultBlockState(),
				Blocks.SCAFFOLDING.defaultBlockState(), Blocks.POWDER_SNOW.defaultBlockState(), Blocks.OAK_FENCE.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(),
				Blocks.GLASS.defaultBlockState(), Blocks.HOPPER.defaultBlockState()};
		java.util.Random r = new java.util.Random(42);
		for (int x = 0; x < 8; x++) for (int y = 0; y < 6; y++) for (int z = 0; z < 8; z++) h.setBlock(x, y, z, mix[r.nextInt(mix.length)]);
		Cow c = h.spawn(EntityType.COW, new Vec3(4, 7, 4));
		c.setNoAi(true);
		BlockPos o = h.absolutePos(BlockPos.ZERO);
		h.runAtTickTime(2, () -> {
			for (int i = 0; i < 2000; i++) {
				double x0 = o.getX() - 1 + r.nextDouble() * 9, y0 = o.getY() - 1 + r.nextDouble() * 7, z0 = o.getZ() - 1 + r.nextDouble() * 9;
				AABB box = new AABB(x0, y0, z0, x0 + r.nextDouble() * 2.5, y0 + r.nextDouble() * 2.5, z0 + r.nextDouble() * 2.5);
				Entity who = r.nextBoolean() ? c : null;
				List<String> fast = shapes(h, who, box);
				CollisionKinds.forceVanilla = true;
				List<String> slow;
				try {
					slow = shapes(h, who, box);
				} finally {
					CollisionKinds.forceVanilla = false;
				}
				if (!fast.equals(slow)) h.fail(msg("box " + box + " fast " + fast + " vanilla " + slow));
			}
			h.succeed();
		});
	}

	private static List<String> shapes(GameTestHelper h, Entity who, AABB box) {
		List<String> out = new java.util.ArrayList<>();
		for (var s : h.getLevel().getBlockCollisions(who, box)) out.add(s.bounds().toString() + s.toAabbs().size());
		return out;
	}
}
