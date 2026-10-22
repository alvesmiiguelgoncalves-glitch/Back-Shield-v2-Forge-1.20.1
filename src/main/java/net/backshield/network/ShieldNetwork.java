package net.backshield.network;

import net.backshield.BackShieldMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ShieldNetwork {

    private static final String PROTOCOL = "1";

    // acceptMissingOr: o cliente com o mod ainda entra em servidor sem ele (so nao consegue enviar acoes).
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(BackShieldMod.id("main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(PROTOCOL))
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(PROTOCOL))
            .simpleChannel();

    private ShieldNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(ShieldActionPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ShieldActionPacket::encode)
                .decoder(ShieldActionPacket::decode)
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        ShieldActions.handle(player, packet.action());
                    }
                })
                .add();
    }
}
