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

	@GameTest(maxTicks = 120)
	public void brainWalkMatchesVanilla(GameTestHelper h) {
		for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++) h.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
		List<Villager> vs = new java.util.ArrayList<>();
		vs.add(h.spawn(EntityType.VILLAGER, new Vec3(1.5, 1, 1.5)));
		vs.add(h.spawn(EntityType.VILLAGER, new Vec3(3.5, 1, 3.5)));
		Villager baby = h.spawn(EntityType.VILLAGER, new Vec3(4.5, 1, 1.5));
		baby.setAge(-24000);
		vs.add(baby);
		h.onEachTick(() -> {
			for (Villager v : vs) {
				List<?> fast = v.getBrain().getRunningBehaviors();
				com.chopstickshq.quillcore.logic.BrainLoops.forceVanilla = true;
				List<?> slow;
				try {
					slow = v.getBrain().getRunningBehaviors();
				} finally {
					com.chopstickshq.quillcore.logic.BrainLoops.forceVanilla = false;
				}
				if (!fast.equals(slow)) h.fail(msg("running " + fast + " vanilla " + slow));
			}
		});
		h.runAtTickTime(50, () -> {
			// Adding behaviours later must show up in the walk, in vanilla's order.
			BehaviorControl<? super Villager> extra = net.minecraft.world.entity.ai.behavior.InteractWithDoor.create();
			vs.get(0).getBrain().addActivity(net.minecraft.world.entity.schedule.Activity.IDLE,
					com.google.common.collect.ImmutableList.of(com.mojang.datafixers.util.Pair.of(0, extra)));
		});
		h.runAtTickTime(110, h::succeed);
	}

	@GameTest(maxTicks = 20)
	public void nearestLivingSortMatchesVanilla(GameTestHelper h) {
		for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) h.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
		Villager v = h.spawn(EntityType.VILLAGER, new Vec3(4.5, 1, 4.5));
		v.setNoAi(true);
		// Ties on purpose: four cows at the same distance, plus a ring at another distance.
		double[][] at = {{2.5, 4.5}, {6.5, 4.5}, {4.5, 2.5}, {4.5, 6.5}, {1.5, 1.5}, {7.5, 7.5}, {1.5, 7.5}, {7.5, 1.5}, {3.5, 4.5}, {8.5, 0.5}};
		for (double[] p : at) h.spawn(EntityType.COW, new Vec3(p[0], 1, p[1])).setNoAi(true);
		h.runAtTickTime(2, () -> {
			var sensor = net.minecraft.world.entity.ai.sensing.SensorType.NEAREST_LIVING_ENTITIES.create();
			for (int i = 0; i < 30; i++) sensor.tick(h.getLevel(), v);
			List<net.minecraft.world.entity.LivingEntity> got = v.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElseThrow();
			double d = v.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
			List<net.minecraft.world.entity.LivingEntity> want = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					v.getBoundingBox().inflate(d, d, d), e -> e != v && e.isAlive());
			want.sort(java.util.Comparator.comparingDouble(v::distanceToSqr));
			if (got.size() < at.length || !got.equals(want)) h.fail(msg("sensor " + got.size() + " " + got + " vanilla " + want));
			h.succeed();
		});
	}

	@GameTest(maxTicks = 20)
	public void nearestLivingSortManyMatchesVanilla(GameTestHelper h) {
		// More than one 16-entry run so the merge path runs, on a grid so there are many exact ties.
		for (int x = 0; x < 13; x++) for (int z = 0; z < 13; z++) h.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
		Villager v = h.spawn(EntityType.VILLAGER, new Vec3(6.5, 1, 6.5));
		v.setNoAi(true);
		int count = 0;
		for (int x = 0; x < 13; x += 2) for (int z = 0; z < 13; z += 2) {
			if (x == 6 && z == 6) continue;
			h.spawn(EntityType.CHICKEN, new Vec3(x + 0.5, 1, z + 0.5)).setNoAi(true);
			count++;
		}
		final int n = count;
		h.runAtTickTime(2, () -> {
			var sensor = net.minecraft.world.entity.ai.sensing.SensorType.NEAREST_LIVING_ENTITIES.create();
			for (int i = 0; i < 30; i++) sensor.tick(h.getLevel(), v);
			List<net.minecraft.world.entity.LivingEntity> got = v.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElseThrow();
			double d = v.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
			List<net.minecraft.world.entity.LivingEntity> want = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					v.getBoundingBox().inflate(d, d, d), e -> e != v && e.isAlive());
			want.sort(java.util.Comparator.comparingDouble(v::distanceToSqr));
			if (got.size() < n || !got.equals(want)) h.fail(msg("sensor " + got.size() + " vanilla " + want.size() + " differs"));
			h.succeed();
		});
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

	@GameTest(maxTicks = 80)
	public void pushWalkMatchesVanilla(GameTestHelper h) {
		for (int x = 0; x < 7; x++) for (int z = 0; z < 7; z++) {
			h.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
			if (x == 0 || z == 0 || x == 6 || z == 6) h.setBlock(x, 1, z, Blocks.STONE.defaultBlockState());
		}
		var board = h.getLevel().getScoreboard();
		String tn = "qc_push_" + System.nanoTime() % 100000;
		var own = board.addPlayerTeam(tn + "o");
		own.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.PUSH_OWN_TEAM);
		var other = board.addPlayerTeam(tn + "x");
		other.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.PUSH_OTHER_TEAMS);
		java.util.Random r = new java.util.Random(7);
		List<net.minecraft.world.entity.LivingEntity> mobs = new java.util.ArrayList<>();
		for (int i = 0; i < 14; i++) {
			Cow c = h.spawn(EntityType.COW, new Vec3(2.4 + r.nextDouble() * 2.2, 1, 2.4 + r.nextDouble() * 2.2));
			if (i % 5 == 1) board.addPlayerToTeam(c.getScoreboardName(), own);
			if (i % 5 == 3) board.addPlayerToTeam(c.getScoreboardName(), other);
			mobs.add(c);
		}
		mobs.add(h.spawn(EntityType.ARMOR_STAND, new Vec3(3.5, 1, 3.5)));
		var boat = h.spawn(EntityType.OAK_BOAT, new Vec3(3.2, 1, 4.1));
		Cow rider = h.spawn(EntityType.COW, new Vec3(3.2, 1, 4.1));
		rider.startRiding(boat, true);
		mobs.add(rider);
		int[] compared = {0};
		h.onEachTick(() -> {
			List<Entity> all = h.getLevel().getEntitiesOfClass(Entity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(8), e -> true);
			for (var m : mobs) {
				Vec3[] before = new Vec3[all.size()];
				boolean[] imp = new boolean[all.size()];
				for (int i = 0; i < all.size(); i++) { before[i] = all.get(i).getDeltaMovement(); imp[i] = all.get(i).hasImpulse; }
				com.chopstickshq.quillcore.logic.PushWalk.forceVanilla = true;
				try {
					((com.chopstickshq.quillcore.gametest.mixin.LivingEntityPushInvoker) m).quillcore$pushEntities();
				} finally {
					com.chopstickshq.quillcore.logic.PushWalk.forceVanilla = false;
				}
				Vec3[] want = new Vec3[all.size()];
				for (int i = 0; i < all.size(); i++) { want[i] = all.get(i).getDeltaMovement(); all.get(i).setDeltaMovement(before[i]); all.get(i).hasImpulse = imp[i]; }
				((com.chopstickshq.quillcore.gametest.mixin.LivingEntityPushInvoker) m).quillcore$pushEntities();
				for (int i = 0; i < all.size(); i++) {
					Vec3 got = all.get(i).getDeltaMovement();
					if (Double.doubleToRawLongBits(got.x) != Double.doubleToRawLongBits(want[i].x)
							|| Double.doubleToRawLongBits(got.y) != Double.doubleToRawLongBits(want[i].y)
							|| Double.doubleToRawLongBits(got.z) != Double.doubleToRawLongBits(want[i].z)) {
						h.fail(msg("push by " + m + " on " + all.get(i) + " got " + got + " vanilla " + want[i]));
					}
					if (!before[i].equals(want[i])) compared[0]++;
				}
			}
		});
		h.runAtTickTime(70, () -> {
			board.removePlayerTeam(own);
			board.removePlayerTeam(other);
			if (compared[0] < 50) h.fail(msg("too few pushes compared: " + compared[0]));
			h.succeed();
		});
	}

	private static net.minecraft.world.level.entity.EntitySectionStorage<Entity> quillSections(GameTestHelper h) {
		var getter = ((com.chopstickshq.quillcore.logic.mixin.LevelEntitiesAccessor) h.getLevel()).quillcore$entities();
		@SuppressWarnings("unchecked")
		var st = (net.minecraft.world.level.entity.EntitySectionStorage<Entity>)
				((com.chopstickshq.quillcore.logic.mixin.LevelEntityGetterAdapterAccessor) getter).quillcore$sections();
		return st;
	}

	/** Every section around the test must have an item count equal to the items it really holds. */
	private static void checkItemCounts(GameTestHelper h, BlockPos o, String when) {
		var st = quillSections(h);
		int sx = o.getX() >> 4, sy = o.getY() >> 4, sz = o.getZ() >> 4;
		for (int x = sx - 2; x <= sx + 3; x++) for (int y = sy - 2; y <= sy + 2; y++) for (int z = sz - 2; z <= sz + 2; z++) {
			var sec = st.getSection(net.minecraft.core.SectionPos.asLong(x, y, z));
			if (sec == null) continue;
			long real = sec.getEntities().filter(e -> e instanceof net.minecraft.world.entity.item.ItemEntity).count();
			int counted = ((com.chopstickshq.quillcore.logic.ItemCount) sec).quillcore$items();
			if (counted != real) h.fail(msg(when + ": section " + x + "," + y + "," + z + " counts " + counted + " holds " + real));
		}
	}

	@GameTest(maxTicks = 80)
	public void itemCountFollowsItems(GameTestHelper h) {
		BlockPos o = h.absolutePos(BlockPos.ZERO);
		double border = ((o.getX() >> 4) + 1) << 4;
		double y = o.getY() + 2, z = o.getZ() + 1.5;
		var item = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), border - 6, y, z, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
		item.setNoGravity(true);
		item.setDeltaMovement(Vec3.ZERO);
		item.setNoPickUpDelay();
		h.getLevel().addFreshEntity(item);
		var zombie = new net.minecraft.world.entity.monster.Zombie(h.getLevel());
		zombie.setPos(border + 1.5, y, z);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		zombie.setCanPickUpLoot(true);
		zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
		h.getLevel().addFreshEntity(zombie);
		checkItemCounts(h, o, "after add");
		h.runAtTickTime(10, () -> {
			if (zombie.getMainHandItem().isEmpty() == false) h.fail(msg("picked up from the far section"));
			checkItemCounts(h, o, "before move");
			item.setPos(border + 1.2, y, z); // crosses into the zombie's section
			checkItemCounts(h, o, "after move");
		});
		h.runAtTickTime(30, () -> {
			if (zombie.getMainHandItem().isEmpty()) h.fail(msg("zombie did not pick up the moved item"));
			checkItemCounts(h, o, "after pickup");
			var second = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), border - 3, y, z, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE));
			second.setNoGravity(true);
			h.getLevel().addFreshEntity(second);
			checkItemCounts(h, o, "after second add");
			second.discard();
			checkItemCounts(h, o, "after discard");
			zombie.discard();
			h.succeed();
		});
	}
}
