package com.diamondambilight.supersword;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class ModItems {
	// 注册一把“超强之剑”：
	// Tiers.NETHERITE  下界合金级别的工具属性(耐久、挖掘等级)
	// 20               攻击力修正值(越高越痛)
	// -2.0f            攻击速度(比普通剑略快)
	public static final Item CUSTOM_SWORD = Registry.register(
		BuiltInRegistries.ITEM,
		new ResourceLocation(ExampleMod.MOD_ID, "custom_sword"),
		new SwordItem(Tiers.NETHERITE, 20, -2.0f, new Item.Properties())
	);

	// 这个方法是整个 mod 的“注册入口”，会在游戏加载时被调用一次
	public static void register() {
		// 上面那个静态字段在第一次被访问时才初始化，这里引用它一下就算完成注册
		// 再把剑加到“战斗”创造模式物品栏里，方便你游戏里直接拿
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(
			entries -> entries.accept(CUSTOM_SWORD)
		);
	}
}
