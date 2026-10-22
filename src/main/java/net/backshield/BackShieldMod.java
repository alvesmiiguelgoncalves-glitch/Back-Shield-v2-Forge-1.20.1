package net.backshield;

import net.backshield.client.BackShieldClient;
import net.backshield.network.ShieldNetwork;
import net.backshield.slot.ShieldSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.theillusivec4.curios.api.CuriosApi;

@Mod(BackShieldMod.MOD_ID)
public class BackShieldMod {

    public static final String MOD_ID = "backshield";

    /** Id do validador de slot referenciado em data/backshield/curios/slots/back_shield.json. */
    public static final ResourceLocation SHIELD_PREDICATE = id("shield");

    public BackShieldMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> BackShieldClient.init(modBus));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // O slot so aceita escudos (qualquer ShieldItem, inclusive de outros mods).
            CuriosApi.registerCurioPredicate(SHIELD_PREDICATE, result -> ShieldSlot.isShield(result.stack()));
            ShieldNetwork.init();
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
