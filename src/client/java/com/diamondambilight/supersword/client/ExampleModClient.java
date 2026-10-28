package com.diamondambilight.supersword.client;

import com.diamondambilight.supersword.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleFactoryRegistry.getInstance().register(ModParticles.SWORD_WAVE, SwordWaveParticle.Provider::new);
	}
}
