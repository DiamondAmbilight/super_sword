package com.diamondambilight.supersword;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {
	public static final SimpleParticleType SWORD_WAVE = Registry.register(
		BuiltInRegistries.PARTICLE_TYPE, ExampleMod.id("sword_wave"), FabricParticleTypes.simple(true));

	private ModParticles() {
	}

	public static void register() {
		// 触发静态字段初始化，在服务端与客户端注册相同的粒子类型。
	}
}
