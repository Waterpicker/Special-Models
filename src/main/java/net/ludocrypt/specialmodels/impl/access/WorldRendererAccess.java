package net.ludocrypt.specialmodels.impl.access;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

public interface WorldRendererAccess {

	public void render(PoseStack matrices, Matrix4f positionMatrix, float tickDelta, Camera camera, boolean outside);

}
