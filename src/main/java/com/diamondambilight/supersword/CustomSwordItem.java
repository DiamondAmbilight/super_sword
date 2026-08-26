package com.diamondambilight.supersword;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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

		// 召唤一道闪电(只在服务器端生成，避免两端重复)
		if (!target.level().isClientSide()) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(target.level());
			if (bolt != null) {
				bolt.moveTo(target.getX(), target.getY(), target.getZ());
				target.level().addFreshEntity(bolt);
			}
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
