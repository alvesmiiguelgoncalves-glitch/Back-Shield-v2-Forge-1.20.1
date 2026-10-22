package net.backshield.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Deteccao de equipamentos que mudam a posicao do escudo.
 *
 * A elytra NAO fica so no slot vanilla de peitoral: mods de "slot de elytra" colocam ela em um slot Curios
 * separado. Por isso procuramos em TODOS os slots Curios, alem do peitoral vanilla.
 */
@OnlyIn(Dist.CLIENT)
final class EquipmentCompat {

    private EquipmentCompat() {
    }

    static boolean isElytra(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ElytraItem) {
            return true;
        }
        // Elytras de outros mods que nao estendem ElytraItem (ex.: "xxx_elytra").
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getPath().contains("elytra");
    }

    /** Elytra no peitoral vanilla OU em qualquer slot Curios (slot de elytra de outros mods). */
    static boolean hasElytra(LivingEntity entity) {
        if (isElytra(entity.getItemBySlot(EquipmentSlot.CHEST))) {
            return true;
        }
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.findFirstCurio((ItemStack stack) -> isElytra(stack)))
                .isPresent();
    }

    /** Lista tudo que esta em slots Curios (para /backshield status). */
    static List<String> describeCurios(LivingEntity entity) {
        List<String> lines = new ArrayList<>();
        var handler = CuriosApi.getCuriosInventory(entity).resolve();
        if (handler.isEmpty()) {
            lines.add("curios: sem inventario");
            return lines;
        }
        handler.get().getCurios().forEach((slotId, stacksHandler) -> {
            var stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    lines.add("curio " + slotId + "[" + i + "] = " + ForgeRegistries.ITEMS.getKey(stack.getItem()));
                }
            }
        });
        if (lines.isEmpty()) {
            lines.add("curios equipados: (nenhum)");
        }
        return lines;
    }
}
