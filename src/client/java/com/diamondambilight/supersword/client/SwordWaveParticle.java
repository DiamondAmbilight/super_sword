package com.diamondambilight.supersword.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 沿发射方向绘制横向立体月牙，不使用始终朝向镜头的广告牌朝向。 */
public final class SwordWaveParticle extends TextureSheetParticle {
	// 3 游戏刻 = 0.15 秒，残影迅速淡出。
	private static final int LIFETIME_TICKS = 3;
	private static final int SEGMENTS = 40;
	private final Vec3 step;
	private final WaveVertex[] mesh;

	private SwordWaveParticle(ClientLevel level, double x, double y, double z,
		double stepX, double stepY, double stepZ, SpriteSet sprites) {
		super(level, x, y, z);
		this.step = new Vec3(stepX, stepY, stepZ);
		this.mesh = createGeometry(step);
		this.lifetime = LIFETIME_TICKS;
		this.hasPhysics = false;
		this.quadSize = 1.2F;
		pickSprite(sprites);
	}

	@Override
	public void tick() {
		// 坐标由 render 按服务端给出的单刻路径插值，不随机漂移，也不越过终点。
		if (++age >= lifetime) {
			remove();
		}
	}

	@Override
	public void render(VertexConsumer vertices, Camera camera, float partialTick) {
		if (!isAlive() || age >= lifetime) {
			remove();
			return;
		}
		float elapsed = age + partialTick;
		float progress = Math.min(elapsed, 1.0F);
		float remaining = Math.max(0.0F, 1.0F - elapsed / lifetime);
		float opacity = 0.9F * remaining * remaining;
		Vec3 cameraPosition = camera.getPosition();
		Vector3f center = new Vector3f(
			(float) (x + step.x * progress - cameraPosition.x),
			(float) (y + step.y * progress - cameraPosition.y),
			(float) (z + step.z * progress - cameraPosition.z));

		// 镜头只用于减去观察者的位置，不再用 camera.rotation() 旋转月牙。
		float u = (getU0() + getU1()) * 0.5F;
		float v = (getV0() + getV1()) * 0.5F;
		for (WaveVertex vertex : mesh) {
			addVertex(vertices, center, vertex, u, v, opacity);
		}
	}

	private void addVertex(VertexConsumer vertices, Vector3f center,
		WaveVertex vertex, float u, float v, float opacity) {
		Vec3 offset = vertex.position();
		// 采样原版粒子纹理的白色中心，以顶点颜色绘制金边、白色内缘。
		vertices.vertex(center.x() + offset.x, center.y() + offset.y, center.z() + offset.z).uv(u, v)
			.color(1.0F, vertex.outer() ? 0.80F : 0.98F, vertex.outer() ? 0.30F : 0.85F, opacity)
			.uv2(LightTexture.FULL_BRIGHT).endVertex();
	}

	static WaveVertex[] createGeometry(Vec3 step) {
		Vec3 forward = step.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 0.0, 1.0) : step.normalize();
		Vec3 sideways = new Vec3(forward.z, 0.0, -forward.x);
		if (sideways.lengthSqr() < 1.0E-10) {
			// 正朝上或朝下发射时也保持有效的朝向，避免归一化零向量。
			sideways = new Vec3(1.0, 0.0, 0.0);
		}
		sideways = sideways.normalize();
		Vec3 up = forward.cross(sideways).normalize();
		List<WaveVertex> vertices = new ArrayList<>(SEGMENTS * 16);
		for (int segment = 0; segment < SEGMENTS; segment++) {
			double first = -Math.PI / 2.0 + Math.PI * segment / SEGMENTS;
			double second = -Math.PI / 2.0 + Math.PI * (segment + 1) / SEGMENTS;
			WaveVertex outerTopFirst = shapeVertex(forward, sideways, up, first, true, 1.0);
			WaveVertex outerTopSecond = shapeVertex(forward, sideways, up, second, true, 1.0);
			WaveVertex innerTopFirst = shapeVertex(forward, sideways, up, first, false, 1.0);
			WaveVertex innerTopSecond = shapeVertex(forward, sideways, up, second, false, 1.0);
			WaveVertex outerBottomFirst = shapeVertex(forward, sideways, up, first, true, -1.0);
			WaveVertex outerBottomSecond = shapeVertex(forward, sideways, up, second, true, -1.0);
			WaveVertex innerBottomFirst = shapeVertex(forward, sideways, up, first, false, -1.0);
			WaveVertex innerBottomSecond = shapeVertex(forward, sideways, up, second, false, -1.0);

			// 上下表面和内外侧面闭合，平视也能看到约 0.15 格厚的发光刃口。
			vertices.addAll(List.of(outerTopFirst, outerTopSecond, innerTopSecond, innerTopFirst));
			vertices.addAll(List.of(innerBottomFirst, innerBottomSecond, outerBottomSecond, outerBottomFirst));
			vertices.addAll(List.of(outerTopFirst, outerBottomFirst, outerBottomSecond, outerTopSecond));
			vertices.addAll(List.of(innerBottomFirst, innerTopFirst, innerTopSecond, innerBottomSecond));
		}
		return vertices.toArray(WaveVertex[]::new);
	}

	private static WaveVertex shapeVertex(Vec3 forward, Vec3 sideways, Vec3 up,
		double angle, boolean outer, double surface) {
		double curve = Math.cos(angle);
		double front = (outer ? 0.90 : 0.52) * curve - 0.45;
		double lateral = 1.15 * Math.sin(angle);
		double height = surface * 0.075 * curve;
		return new WaveVertex(forward.scale(front).add(sideways.scale(lateral)).add(up.scale(height)), outer);
	}

	record WaveVertex(Vec3 position, boolean outer) {
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
			double x, double y, double z, double stepX, double stepY, double stepZ) {
			return new SwordWaveParticle(level, x, y, z, stepX, stepY, stepZ, sprites);
		}
	}
}
