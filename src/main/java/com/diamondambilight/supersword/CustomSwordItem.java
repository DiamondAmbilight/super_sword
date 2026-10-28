package com.diamondambilight.supersword;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CustomSwordItem extends SwordItem {
	// Minecraft 每秒运行 20 个游戏刻。
	public static final int CHARGE_TICKS = 40;
	public static final int COOLDOWN_TICKS = 100;
	private static final int USE_DURATION = 72000;

	public CustomSwordItem(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
		super(tier, attackDamage, attackSpeed, properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		// 蓄满后可以继续按住，只有松开右键才释放。
		return USE_DURATION;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
		if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) {
			return;
		}
		int chargedTicks = getUseDuration(stack) - remainingUseDuration;
		if (chargedTicks % 4 == 0) {
			serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0,
				player.getZ(), 3, 0.35, 0.4, 0.35, 0.02);
		}
		if (chargedTicks % 5 == 0) {
			Component message = chargedTicks >= CHARGE_TICKS
				? Component.translatable("message.super_sword.charge_ready").withStyle(ChatFormatting.GOLD)
				: Component.translatable("message.super_sword.charging", chargedTicks * 100 / CHARGE_TICKS)
					.withStyle(ChatFormatting.YELLOW);
			player.displayClientMessage(message, true);
		}
		if (chargedTicks == CHARGE_TICKS) {
			serverLevel.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
				SoundSource.PLAYERS, 0.8F, 1.5F);
		}
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)
			|| !player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(this)) {
			return;
		}
		if (getUseDuration(stack) - timeLeft < CHARGE_TICKS) {
			player.displayClientMessage(Component.translatable("message.super_sword.charge_cancelled")
				.withStyle(ChatFormatting.GRAY), true);
			return;
		}

		SwordWave.launch(serverLevel, player);
		player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
		player.awardStat(Stats.ITEM_USED.get(this));
		InteractionHand hand = player.getUsedItemHand();
		player.swing(hand, true);
		stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(hand));
		player.displayClientMessage(Component.translatable("message.super_sword.wave_released")
			.withStyle(ChatFormatting.AQUA), true);
		serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
			SoundSource.PLAYERS, 1.0F, 0.7F);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);

		// 被击中者减速
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));

		// 圣光效果(服务器端生成，广播给附近玩家)
		if (!target.level().isClientSide()) {
			ServerLevel serverLevel = (ServerLevel) target.level();
			double x = target.getX();
			double baseY = target.getY();
			double z = target.getZ();

			// 落点闪白
			serverLevel.sendParticles(ParticleTypes.FLASH, x, baseY + 0.2, z, 1, 0.0, 0.0, 0.0, 0.0);

			// 圣光柱：密集亮光点，范围窄、纵向很高，向上飘
			serverLevel.sendParticles(ParticleTypes.END_ROD, x, baseY + 1.5, z, 220, 0.3, 2.8, 0.3, 0.3);
			// 金色圣光/星辰
			serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, baseY + 1.5, z, 100, 0.4, 2.2, 0.4, 0.4);
			// 淡淡辉光
			serverLevel.sendParticles(ParticleTypes.GLOW, x, baseY + 1.5, z, 90, 0.5, 1.4, 0.5, 0.4);

			// 柱顶再爆一簇光
			serverLevel.sendParticles(ParticleTypes.GLOW, x, baseY + 4.0, z, 40, 0.6, 0.3, 0.6, 0.8);
		}

		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.translatable("item.super_sword.custom_sword.desc").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.super_sword.custom_sword.desc2").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.super_sword.custom_sword.skill").withStyle(ChatFormatting.AQUA));
	}
}
