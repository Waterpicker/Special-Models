package net.ludocrypt.specialmodels.impl.render;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class MutableVertice {

	private Vec3 pos;
	private Vec2 uv;

	public MutableVertice() {
		this.pos = Vec3.ZERO;
		this.uv = Vec2.ZERO;
	}

	public MutableVertice(Vec3 pos, Vec2 uv) {
		this.pos = pos;
		this.uv = uv;
	}

	public MutableVertice(double x, double y, double z, double u, double v) {
		this.pos = new Vec3(x, y, z);
		this.uv = new Vec2((float) u, (float) v);
	}

	public Vec3 getPos() {
		return pos;
	}

	public Vec2 getUv() {
		return uv;
	}

	public void setPos(Vec3 pos) {
		this.pos = pos;
	}

	public void setUv(Vec2 uv) {
		this.uv = uv;
	}

	public void add(double x, double y, double z) {
		this.pos = this.pos.add(x, y, z);
	}

	public void shift(double u, double v) {
		this.uv = new Vec2(this.uv.x + (float) u, this.uv.y + (float) v);
	}

}
