package net.backshield.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * No Fabric, o Back Shield lia os slots extras do mod BackSlot (Globox_Z) para escolher os perfis item1/item2.
 * O BackSlot e exclusivo do Fabric, entao no Forge esses perfis nunca sao escolhidos automaticamente
 * (continuam existindo no arquivo de config e podem ser editados, mas ficam sem uso).
 */
@OnlyIn(Dist.CLIENT)
final class BackSlotCompat {

    private BackSlotCompat() {
    }

    /** @return sempre 0 no Forge. */
    static int backItems(LivingEntity entity) {
        return 0;
    }

    static List<String> describe(LivingEntity entity) {
        return List.of("backslot: nao existe para Forge (perfis item1/item2 inativos)");
    }
}
