package net.backshield.network;

import net.minecraft.network.FriendlyByteBuf;

/** Cliente -> servidor. O servidor decide tudo; o cliente so pede a acao. */
public record ShieldActionPacket(Action action) {

    public enum Action {
        /** Saca o escudo das costas para a mao secundaria (ou guarda de volta). */
        TOGGLE,
        /** Tecla de defesa pressionada. */
        BLOCK_START,
        /** Tecla de defesa solta (ou tela aberta). */
        BLOCK_STOP;

        public static Action byOrdinal(int ordinal) {
            Action[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
        }
    }

    public static void encode(ShieldActionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.action.ordinal());
    }

    public static ShieldActionPacket decode(FriendlyByteBuf buf) {
        return new ShieldActionPacket(Action.byOrdinal(buf.readVarInt()));
    }
}
