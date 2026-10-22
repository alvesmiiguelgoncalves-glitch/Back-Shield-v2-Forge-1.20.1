package net.backshield.network;

import net.backshield.BackShieldMod;
import net.backshield.slot.ShieldSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Logica de servidor: sacar/guardar o escudo e a defesa temporaria.
 *
 * Regras de seguranca (nada de duplicar/perder item):
 *  - o escudo SEMPRE e movido (nunca copiado): uma unica referencia muda de slot;
 *  - nunca descarta nem substitui o item da mao secundaria (so troca se for outro escudo);
 *  - qualquer situacao estranha (morte, quebra, tela aberta, desconexao) encerra o estado de defesa.
 */
@Mod.EventBusSubscriber(modid = BackShieldMod.MOD_ID)
public final class ShieldActions {

    /** Jogadores cujo escudo esta "emprestado" na mao secundaria pela defesa temporaria. */
    private static final Set<UUID> BLOCKING = new HashSet<>();

    private ShieldActions() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endBlock(player);
        }
    }

    static void handle(ServerPlayer player, ShieldActionPacket.Action action) {
        if (action == null) {
            return;
        }
        if (action == ShieldActionPacket.Action.BLOCK_STOP) {
            endBlock(player); // sempre permitido, mesmo se o jogador morreu/espectador
            return;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        switch (action) {
            case TOGGLE -> toggle(player);
            case BLOCK_START -> startBlock(player);
            default -> {
            }
        }
    }

    private static void toggle(ServerPlayer player) {
        if (BLOCKING.contains(player.getUUID())) {
            endBlock(player);
            return;
        }
        if (player.isUsingItem()) {
            return;
        }
        IDynamicStackHandler inventory = ShieldSlot.inventory(player);
        if (inventory == null) {
            return;
        }
        ItemStack back = inventory.getStackInSlot(0);
        ItemStack offhand = player.getOffhandItem();

        if (!back.isEmpty()) {
            if (offhand.isEmpty()) {
                player.setItemSlot(EquipmentSlot.OFFHAND, back);
                inventory.setStackInSlot(0, ItemStack.EMPTY);
            } else if (ShieldSlot.isShield(offhand)) {
                // dois escudos: troca de lugar (nada se perde)
                inventory.setStackInSlot(0, offhand);
                player.setItemSlot(EquipmentSlot.OFFHAND, back);
            } else {
                player.displayClientMessage(Component.translatable("message.backshield.offhand_occupied"), true);
                return;
            }
            playSound(player);
        } else if (ShieldSlot.isShield(offhand)) {
            inventory.setStackInSlot(0, offhand);
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            playSound(player);
        } else if (ShieldSlot.isShield(player.getMainHandItem())) {
            // Escudo na mao principal: guarda nas costas (movido, nunca copiado).
            inventory.setStackInSlot(0, player.getMainHandItem());
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            playSound(player);
        }
    }

    private static void startBlock(ServerPlayer player) {
        if (BLOCKING.contains(player.getUUID()) || player.isUsingItem()) {
            return;
        }
        IDynamicStackHandler inventory = ShieldSlot.inventory(player);
        if (inventory == null) {
            return;
        }
        ItemStack back = inventory.getStackInSlot(0);
        if (back.isEmpty()) {
            return;
        }
        if (!player.getOffhandItem().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.backshield.offhand_occupied"), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(back.getItem())) {
            return; // escudo desativado por machado
        }
        player.setItemSlot(EquipmentSlot.OFFHAND, back);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        BLOCKING.add(player.getUUID());
        // So move o escudo. O cliente (que esta com a tecla segura) usa o item pelo caminho normal do vanilla,
        // e o servidor entra em "usando item" ao receber esse pacote - sem forcar o estado daqui.
        playSound(player);
    }

    /** Encerra a defesa temporaria e devolve o escudo para as costas (se der). */
    private static void endBlock(ServerPlayer player) {
        if (!BLOCKING.remove(player.getUUID())) {
            return;
        }
        if (player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            player.stopUsingItem();
        }
        ItemStack offhand = player.getOffhandItem();
        if (!ShieldSlot.isShield(offhand)) {
            return; // quebrou ou foi movido: nada a devolver
        }
        IDynamicStackHandler inventory = ShieldSlot.inventory(player);
        if (inventory == null || !inventory.getStackInSlot(0).isEmpty()) {
            return; // slot ocupado: o escudo fica na mao, nada e perdido
        }
        inventory.setStackInSlot(0, offhand);
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        playSound(player);
    }

    private static void tick(MinecraftServer server) {
        if (BLOCKING.isEmpty()) {
            return;
        }
        for (UUID id : new ArrayList<>(BLOCKING)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive() || player.isSpectator()) {
                BLOCKING.remove(id); // morreu/saiu: regras normais de inventario valem
                continue;
            }
            if (!ShieldSlot.isShield(player.getOffhandItem())) {
                BLOCKING.remove(id); // escudo quebrou ou saiu da mao
                continue;
            }
            if (player.containerMenu != player.inventoryMenu) {
                endBlock(player); // abriu bau/mesa/etc.
            }
            // Se um machado desativar o escudo, o cliente volta a levanta-lo quando o cooldown acabar.
        }
    }

    private static void playSound(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
