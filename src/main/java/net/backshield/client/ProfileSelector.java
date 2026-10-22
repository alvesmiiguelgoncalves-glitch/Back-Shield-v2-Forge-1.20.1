package net.backshield.client;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Escolhe QUAL perfil de posicao vale agora. Prioridade:
 *   voando com elytra (asas abertas)  >  planando (voando com as asas FECHADAS)  >  elytra em pe  >  peitoral  >  nada
 * e, em cada caso, mochila > 2 itens nas costas > 1 item nas costas.
 * (Itens do BackSlot so contam sem elytra e sem mochila; com elytra/mochila vale o perfil de elytra/mochila.)
 */
@OnlyIn(Dist.CLIENT)
public final class ProfileSelector {

    /**
     * Abaixo deste valor de abertura das asas (0 = fechadas, 1 = abertas) o perfil vira "gliding".
     * E o mesmo fator que o modelo da elytra usa: as asas fecham quando o mergulho fica bem inclinado para baixo.
     */
    private static final float GLIDE_CLOSED_BELOW = 0.5F;

    private ProfileSelector() {
    }

    public static String select(LivingEntity entity) {
        boolean backpack = BackpackCompat.hasBackpack(entity);

        if (entity.isFallFlying()) {
            if (wingSpread(entity) < GLIDE_CLOSED_BELOW) {
                return backpack ? "gliding_backpack" : "gliding";
            }
            return backpack ? "flying_backpack" : "flying";
        }

        boolean chest = hasChestplate(entity);
        if (EquipmentCompat.hasElytra(entity)) {
            if (chest) {
                return backpack ? "elytra_chest_backpack" : "elytra_chest";
            }
            return backpack ? "elytra_backpack" : "elytra";
        }

        int items = BackSlotCompat.backItems(entity);
        if (chest) {
            if (backpack) {
                return "chest_backpack";
            }
            return items >= 2 ? "chest_item2" : items == 1 ? "chest_item1" : "chest";
        }
        if (backpack) {
            return "backpack";
        }
        return items >= 2 ? "item2" : items == 1 ? "item1" : "normal";
    }

    /** Peitoral de armadura equipado (elytra nao conta: ela tem perfis proprios). */
    private static boolean hasChestplate(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return !chest.isEmpty() && !EquipmentCompat.isElytra(chest);
    }

    /**
     * Abertura das asas, calculada igual ao modelo da elytra do vanilla (ElytraModel):
     * 1 = totalmente abertas (voo horizontal ou subindo), 0 = fechadas (mergulho vertical).
     */
    static float wingSpread(LivingEntity entity) {
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.y < 0.0D) {
            Vec3 normalized = velocity.normalize();
            return 1.0F - (float) Math.pow(-normalized.y, 1.5D);
        }
        return 1.0F;
    }

    public static List<String> explain(LivingEntity entity) {
        return List.of(
                "perfil ativo = " + select(entity),
                "voando=" + entity.isFallFlying() + ", asas(1=abertas,0=fechadas)=" + String.format("%.2f", wingSpread(entity))
                        + ", elytra=" + EquipmentCompat.hasElytra(entity) + ", peitoral=" + hasChestplate(entity)
                        + ", mochila=" + BackpackCompat.hasBackpack(entity)
                        + ", itensNasCostas=" + BackSlotCompat.backItems(entity));
    }
}
