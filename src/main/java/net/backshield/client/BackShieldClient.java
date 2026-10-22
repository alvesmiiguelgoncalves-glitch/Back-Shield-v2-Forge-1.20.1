package net.backshield.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.backshield.config.BackShieldConfig;
import net.backshield.network.ShieldActionPacket;
import net.backshield.network.ShieldNetwork;
import net.backshield.slot.ShieldSlot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public final class BackShieldClient {

    private static KeyMapping toggleKey;
    private static KeyMapping blockKey;
    private static boolean blocking;
    private static boolean forcingUseKey;
    /** Ticks ate a proxima tentativa de levantar o escudo (evita spam de pacotes). */
    private static int useRetry;

    private BackShieldClient() {
    }

    /** Chamado pelo construtor do mod (so no cliente). */
    public static void init(IEventBus modBus) {
        BackShieldConfig.load();

        modBus.addListener(BackShieldClient::onClientSetup);
        modBus.addListener(BackShieldClient::onRegisterKeys);

        MinecraftForge.EVENT_BUS.addListener(BackShieldClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(BackShieldClient::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> blocking = false);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Registra o renderizador para todo escudo (inclusive de outros mods) depois que todos os itens existem.
            BackShieldRenderer renderer = new BackShieldRenderer();
            for (var item : ForgeRegistries.ITEMS) {
                if (ShieldSlot.isShield(new ItemStack(item))) {
                    CuriosRendererRegistry.register(item, () -> renderer);
                }
            }
            // Botao "Config" na lista de mods do Forge.
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new BackShieldConfigScreen(parent)));
        });
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        toggleKey = new KeyMapping("key.backshield.toggle", GLFW.GLFW_KEY_G, "key.categories.backshield");
        blockKey = new KeyMapping("key.backshield.block", GLFW.GLFW_KEY_R, "key.categories.backshield");
        event.register(toggleKey);
        event.register(blockKey);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            holdUseKey(client);
        } else {
            tick(client);
        }
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, String line) {
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
    }

    private static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("backshield")
                .then(Commands.literal("show")
                        .executes(ctx -> {
                            BackShieldConfig.describe(null).forEach(line -> feedback(ctx, line));
                            return 1;
                        })
                        .then(Commands.argument("profile", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BackShieldConfig.PROFILES, builder))
                                .executes(ctx -> {
                                    BackShieldConfig.describe(StringArgumentType.getString(ctx, "profile"))
                                            .forEach(line -> feedback(ctx, line));
                                    return 1;
                                })))
                .then(Commands.literal("status").executes(ctx -> {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player == null) {
                        return 0;
                    }
                    String version = ModList.get().getModContainerById("backshield")
                            .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
                    feedback(ctx, "Back Shield v" + version);
                    ProfileSelector.explain(player).forEach(line -> feedback(ctx, line));
                    BackpackCompat.describe(player).forEach(line -> feedback(ctx, line));
                    BackSlotCompat.describe(player).forEach(line -> feedback(ctx, line));
                    feedback(ctx, "chest=" + player.getItemBySlot(EquipmentSlot.CHEST).getItem());
                    EquipmentCompat.describeCurios(player).forEach(line -> feedback(ctx, line));
                    return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    BackShieldConfig.load();
                    feedback(ctx, "backshield-client.json reloaded");
                    return 1;
                }))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            BackShieldConfig.reset(null);
                            feedback(ctx, "Todos os perfis restaurados ao padrao");
                            return 1;
                        })
                        .then(Commands.argument("profile", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BackShieldConfig.PROFILES, builder))
                                .executes(ctx -> {
                                    String profile = StringArgumentType.getString(ctx, "profile");
                                    if (!BackShieldConfig.PROFILES.contains(profile)) {
                                        feedback(ctx, "Perfil desconhecido: " + profile);
                                        return 0;
                                    }
                                    BackShieldConfig.reset(profile);
                                    feedback(ctx, "Perfil " + profile + " restaurado ao padrao");
                                    return 1;
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("profile", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BackShieldConfig.PROFILES, builder))
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BackShieldConfig.FIELDS, builder))
                                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                .executes(ctx -> {
                                                    String profile = StringArgumentType.getString(ctx, "profile");
                                                    String field = StringArgumentType.getString(ctx, "field");
                                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                                    boolean ok = BackShieldConfig.set(profile, field, value);
                                                    feedback(ctx, ok
                                                            ? profile + "." + field + " = " + value
                                                            : "Perfil ou campo desconhecido: " + profile + " " + field);
                                                    return ok ? 1 : 0;
                                                }))))));
    }

    private static boolean canSend() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && ShieldNetwork.CHANNEL.isRemotePresent(connection.getConnection());
    }

    private static void send(ShieldActionPacket.Action action) {
        ShieldNetwork.CHANNEL.sendToServer(new ShieldActionPacket(action));
    }

    /**
     * O vanilla solta o escudo quando o botao direito nao esta pressionado. Enquanto a defesa por tecla
     * esta ativa (e o escudo ja esta em uso na mao secundaria), mantemos o "usar" pressionado.
     * Roda no INICIO do tick, antes de handleKeybinds; so age depois que o servidor confirmou o uso,
     * entao nunca dispara um clique direito extra com a mao principal.
     */
    private static void holdUseKey(Minecraft client) {
        LocalPlayer player = client.player;
        if (blocking && player != null && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            client.options.keyUse.setDown(true);
            forcingUseKey = true;
        } else if (forcingUseKey) {
            forcingUseKey = false;
            KeyMapping.setAll(); // devolve o estado real das teclas
        }
    }

    private static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            blocking = false;
            return;
        }
        boolean canSend = canSend();

        while (toggleKey.consumeClick()) {
            if (canSend && client.screen == null) {
                send(ShieldActionPacket.Action.TOGGLE);
            }
        }

        // Defesa: enquanto a tecla esta segura e nenhuma tela esta aberta.
        boolean wantBlock = canSend && blockKey.isDown() && client.screen == null && !player.isSpectator();
        if (wantBlock && !blocking) {
            blocking = true;
            send(ShieldActionPacket.Action.BLOCK_START);
        } else if (!wantBlock && blocking) {
            blocking = false;
            useRetry = 0;
            if (canSend) {
                send(ShieldActionPacket.Action.BLOCK_STOP);
            }
        }

        // O servidor so MOVE o escudo para a mao secundaria. Quem levanta o escudo e o PROPRIO cliente, pelo mesmo
        // caminho do clique direito do vanilla (useItem): assim cliente e servidor entram em "usando item" juntos.
        if (blocking && client.screen == null && client.gameMode != null && !player.isUsingItem()) {
            ItemStack offhand = player.getOffhandItem();
            if (ShieldSlot.isShield(offhand) && !player.getCooldowns().isOnCooldown(offhand.getItem())) {
                if (useRetry-- <= 0) {
                    client.gameMode.useItem(player, InteractionHand.OFF_HAND);
                    useRetry = 3;
                }
            }
        }
    }
}
