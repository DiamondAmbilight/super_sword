package com.diamondambilight.supersword;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CustomSwordItem extends SwordItem {
	public CustomSwordItem(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
		super(tier, attackDamage, attackSpeed, properties);
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
	}
}
