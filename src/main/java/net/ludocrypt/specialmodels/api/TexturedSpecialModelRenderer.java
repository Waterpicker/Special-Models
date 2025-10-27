package net.ludocrypt.specialmodels.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;

public class TexturedSpecialModelRenderer extends SpecialModelRenderer {

	public static final SpecialModelRenderer TEXTURED = Registry
		.register(SpecialModelRenderer.SPECIAL_MODEL_RENDERER, ResourceLocation.fromNamespaceAndPath("specialmodels", "textured"),
			new TexturedSpecialModelRenderer());

	public TexturedSpecialModelRenderer() {
		super();
	}

	public TexturedSpecialModelRenderer(boolean performOutside) {
		super(performOutside);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void setup(PoseStack matrices, Matrix4f viewMatrix, Matrix4f positionMatrix, float tickDelta,
					  ShaderInstance shader, BlockPos origin) {
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
	}

	public static void init() {
	}

}
