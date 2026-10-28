package com.diamondambilight.supersword;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 服务端推进剑气和命中判定，客户端粒子仅负责绘制月牙与短残影。 */
public final class SwordWave {
	public static final float DAMAGE = 20.0F;
	public static final double MAX_RANGE = 16.0;
	private static final double SPEED = 1.0;
	private static final double HIT_RADIUS = 0.6;
	private static final double RANGE_EPSILON = 1.0E-6;
	private static final int MAX_LIFETIME_TICKS = (int) Math.ceil(MAX_RANGE / SPEED) + 1;
	private static final List<SwordWave> ACTIVE_WAVES = new ArrayList<>();

	private final ServerLevel level;
	private final Player owner;
	private final Vec3 direction;
	private final Set<UUID> hitEntities = new HashSet<>();
	private Vec3 position;
	private double distanceTravelled;
	private int ticksLived;

	private SwordWave(ServerLevel level, Player owner) {
		this.level = level;
		this.owner = owner;
		// 从视线稍下方出剑，第一人称能看到横向刃面，而不是与眼睛完全共面。
		this.position = owner.getEyePosition().add(0.0, -0.35, 0.0);
		this.direction = owner.getLookAngle().normalize();
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> ACTIVE_WAVES.removeIf(wave -> wave.tick()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE_WAVES.clear());
	}

	public static void launch(ServerLevel level, Player owner) {
		ACTIVE_WAVES.add(new SwordWave(level, owner));
	}

	/** 返回 true 表示剑气已经结束，可以移除。 */
	private boolean tick() {
		if (owner.isRemoved() || !owner.isAlive() || owner.level() != level) {
			return true;
		}
		double plannedDistance = planStepDistance(distanceTravelled, ticksLived++);
		if (plannedDistance <= 0.0) {
			return true;
		}
		Vec3 next = position.add(direction.scale(plannedDistance));
		// 不为技能加载新区块。
		BlockPos nextBlock = BlockPos.containing(next);
		if (!level.getChunkSource().hasChunk(nextBlock.getX() >> 4, nextBlock.getZ() >> 4)) {
			return true;
		}
		BlockHitResult blockHit = level.clip(new ClipContext(position, next,
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
		boolean blocked = blockHit.getType() != HitResult.Type.MISS;
		Vec3 end = blocked ? blockHit.getLocation() : next;
		double stepLength = position.distanceTo(end);
		if (stepLength <= RANGE_EPSILON) {
			// 终点或障碍处不再生成原地静止的月牙。
			return true;
		}

		// 对整段路径检测，防止剑气速度较快时穿过目标却没有命中。
		AABB bounds = new AABB(position, end).inflate(HIT_RADIUS);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
			entity -> entity != owner && entity.isAlive() && !entity.isSpectator()
				&& !owner.isAlliedTo(entity) && !hitEntities.contains(entity.getUUID()))) {
			AABB hitbox = target.getBoundingBox().inflate(HIT_RADIUS);
			if (!hitbox.contains(position) && hitbox.clip(position, end).isEmpty()) {
				continue;
			}
			// 宽度判定不能让墙后、拐角后的实体受到伤害。
			Vec3 targetPoint = target.getBoundingBox().getCenter();
			if (level.clip(new ClipContext(position, targetPoint, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, owner)).getType() != HitResult.Type.MISS) {
				continue;
			}
			hitEntities.add(target.getUUID());
			// 命中生物只记录伤害，不移除剑气；同一道剑气可继续穿透后续目标。
			if (target.hurt(level.damageSources().playerAttack(owner), DAMAGE)) {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), owner);
				level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 0.8,
					target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		// count=0 时，三个偏移量作为精确速度传给客户端。
		// 每刻只发送一个完整月牙，客户端在这一段路径上插值，不使用长寿命末地烛粒子。
		Vec3 step = end.subtract(position);
		level.sendParticles(ModParticles.SWORD_WAVE, position.x, position.y, position.z,
			0, step.x, step.y, step.z, 1.0);
		// 无碰撞时累计计划步长，避免世界坐标的浮点误差让射程永远差一丁点。
		distanceTravelled = Math.min(MAX_RANGE, distanceTravelled + (blocked ? stepLength : plannedDistance));
		position = end;
		return blocked || planStepDistance(distanceTravelled, ticksLived) <= 0.0;
	}

	static double planStepDistance(double distanceTravelled, int ticksLived) {
		if (ticksLived >= MAX_LIFETIME_TICKS || !Double.isFinite(distanceTravelled)) {
			return 0.0;
		}
		double remaining = MAX_RANGE - distanceTravelled;
		return remaining <= RANGE_EPSILON ? 0.0 : Math.min(SPEED, remaining);
	}
}
