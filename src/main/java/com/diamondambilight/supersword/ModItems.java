package com.diamondambilight.supersword;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

public class ModItems {
	// 注册“超强之剑”：
	// Tiers.NETHERITE  下界合金级别的工具属性(耐久、挖掘等级)
	// 20               攻击力修正值(越高越痛,配合材质加成最终 25)
	// -2.0f            攻击速度
	public static final Item CUSTOM_SWORD = Registry.register(
		BuiltInRegistries.ITEM,
		new ResourceLocation(ExampleMod.MOD_ID, "custom_sword"),
		new CustomSwordItem(Tiers.NETHERITE, 95, -2.0f, new Item.Properties().durability(9999))
	);

	// 创建一个属于本 mod 自己的创造模式物品栏标签
	public static final CreativeModeTab CUSTOM_TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		new ResourceLocation(ExampleMod.MOD_ID, "super_sword_tab"),
		FabricItemGroup.builder()
			.icon(() -> new ItemStack(CUSTOM_SWORD))
			.title(Component.translatable("itemGroup.super_sword"))
			.displayItems((enabledFeatures, entries) -> {
				entries.accept(CUSTOM_SWORD);
			})
			.build()
	);

	// 游戏加载时调用一次,触达静态字段即完成注册
	public static void register() {
		CUSTOM_TAB.toString();
	}
}
