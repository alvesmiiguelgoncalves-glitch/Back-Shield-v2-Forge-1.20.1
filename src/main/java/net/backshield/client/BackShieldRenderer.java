package net.backshield.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.backshield.config.BackShieldConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Desenha o escudo nas costas.
 *
 * Em vez de usar uma coordenada fixa, usa o proprio ModelPart do torso (body.translateAndRotate): ele ja contem
 * a pose atual (agachar, nadar, animacoes de outros mods), entao o escudo fica preso ao corpo.
 *
 * Orientacao (item de escudo, ItemDisplayContext.NONE = sem transformacao de display):
 *  - o renderizador do escudo desenha a placa com a face para +Z e "em pe" no eixo Y do item;
 *  - o referencial do modelo tem Y para BAIXO e +Z para TRAS, entao uma rotacao de 180 graus em Z deixa o
 *    escudo em pe e com a face voltada para fora das costas (sem espelhar);
 *  - o ItemRenderer desloca o item em (-0.5, -0.5, -0.5) e a placa fica 1.5px a frente da origem do modelo:
 *    translate(0.5, 0.5, 0.40625) coloca o CENTRO da placa exatamente no pivo da nossa transformacao.
 */
@OnlyIn(Dist.CLIENT)
public final class BackShieldRenderer implements ICurioRenderer {

    /** Superficie das costas do torso (2px / 16). */
    private static final float BACK_SURFACE = 0.125F;
    /** Centro da placa em Z dentro do referencial do item (1.5px / 16). */
    private static final float PLATE_CENTER_Z = 0.09375F;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
                                                                          PoseStack poseStack,
                                                                          RenderLayerParent<T, M> renderLayerParent,
                                                                          MultiBufferSource buffer, int light,
                                                                          float limbSwing, float limbSwingAmount,
                                                                          float partialTicks, float ageInTicks,
                                                                          float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> biped)) {
            return;
        }
        LivingEntity entity = slotContext.entity();
        // Cada situacao usa SEU PROPRIO perfil; nenhum valor e somado a outro perfil.
        BackShieldConfig.Profile p = BackShieldConfig.profile(ProfileSelector.select(entity));

        if (!p.visible) {
            return; // perfil configurado para esconder o escudo (ex.: com elytra)
        }
        // Espelhar = trocar de lado: x, rotY e rotZ mudam de sinal (rotX nao muda).
        float side = p.mirror ? -1.0F : 1.0F;

        poseStack.pushPose();
        biped.body.translateAndRotate(poseStack);

        poseStack.translate(p.x * side, p.y, BACK_SURFACE + p.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(p.rotY * side));
        poseStack.mulPose(Axis.XP.rotationDegrees(p.rotX));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + p.rotZ * side));
        float scale = 1.0F + p.scale;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.5F, 0.5F, 0.5F - PLATE_CENTER_Z);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
    }
}
