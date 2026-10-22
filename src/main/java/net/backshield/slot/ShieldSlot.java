package net.backshield.slot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.common.ToolActions;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/** Acesso ao slot Curios "back_shield". Armazenamento/sync/morte ficam por conta do Curios. */
public final class ShieldSlot {

    public static final String SLOT = "back_shield";

    private ShieldSlot() {
    }

    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof ShieldItem || stack.canPerformAction(ToolActions.SHIELD_BLOCK));
    }

    /** @return o inventario do slot ou null se a entidade nao tem o slot. */
    public static IDynamicStackHandler inventory(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.getStacksHandler(SLOT))
                .map(ICurioStacksHandler::getStacks)
                .filter(stacks -> stacks.getSlots() > 0)
                .orElse(null);
    }

    public static ItemStack get(LivingEntity entity) {
        IDynamicStackHandler inventory = inventory(entity);
        return inventory == null ? ItemStack.EMPTY : inventory.getStackInSlot(0);
    }
}
