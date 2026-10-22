package net.backshield.client;

import net.backshield.config.BackShieldConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Tela de configuracao (botao "Config" da lista de mods): escolhe o perfil com < >, edita x/y/z/rotacoes/escala,
 * liga/desliga "visivel" e "espelhar" e restaura o padrao do perfil. Cada alteracao ja e salva no arquivo de config.
 */
@OnlyIn(Dist.CLIENT)
public final class BackShieldConfigScreen extends Screen {

    private static final List<String> LEFT = List.of("x", "y", "z", "scale");
    private static final List<String> RIGHT = List.of("rotX", "rotY", "rotZ");

    private final Screen parent;
    private int index;

    public BackShieldConfigScreen(Screen parent) {
        super(Component.translatable("screen.backshield.config"));
        this.parent = parent;
    }

    private String profileName() {
        return BackShieldConfig.PROFILES.get(index);
    }

    private BackShieldConfig.Profile profile() {
        return BackShieldConfig.profile(profileName());
    }

    private static float read(BackShieldConfig.Profile p, String field) {
        return switch (field) {
            case "x" -> p.x;
            case "y" -> p.y;
            case "z" -> p.z;
            case "rotX" -> p.rotX;
            case "rotY" -> p.rotY;
            case "rotZ" -> p.rotZ;
            default -> p.scale;
        };
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> change(-1))
                .bounds(cx - 150, 36, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> change(1))
                .bounds(cx + 130, 36, 20, 20).build());

        addNumberFields(LEFT, cx - 150, 70);
        addNumberFields(RIGHT, cx + 10, 70);

        BackShieldConfig.Profile p = profile();
        this.addRenderableWidget(Button.builder(toggleText("options.backshield.visible", p.visible), b -> {
            BackShieldConfig.set(profileName(), "visible", profile().visible ? 0.0F : 1.0F);
            b.setMessage(toggleText("options.backshield.visible", profile().visible));
        }).bounds(cx - 150, 168, 140, 20).build());
        this.addRenderableWidget(Button.builder(toggleText("options.backshield.mirror", p.mirror), b -> {
            BackShieldConfig.set(profileName(), "mirror", profile().mirror ? 0.0F : 1.0F);
            b.setMessage(toggleText("options.backshield.mirror", profile().mirror));
        }).bounds(cx + 10, 168, 140, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("button.backshield.reset"), b -> {
            BackShieldConfig.reset(profileName());
            this.rebuildWidgets();
        }).bounds(cx - 150, 198, 140, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(cx + 10, 198, 140, 20).build());
    }

    private void addNumberFields(List<String> names, int x, int y0) {
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            EditBox field = new EditBox(this.font, x + 50, y0 + i * 24, 90, 18, Component.literal(name));
            field.setMaxLength(12);
            field.setValue(String.valueOf(read(profile(), name)));
            field.setResponder(text -> {
                try {
                    BackShieldConfig.set(profileName(), name, Float.parseFloat(text.trim().replace(',', '.')));
                } catch (NumberFormatException ignored) {
                    // texto incompleto (ex.: "-"): espera o proximo caractere
                }
            });
            this.addRenderableWidget(field);
        }
    }

    private static Component toggleText(String key, boolean value) {
        return Component.translatable(key).append(": ")
                .append(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private void change(int delta) {
        int size = BackShieldConfig.PROFILES.size();
        index = (index + delta + size) % size;
        this.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, 16, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal(profileName()), cx, 42, 0xFFFFAA);
        for (int i = 0; i < LEFT.size(); i++) {
            graphics.drawString(this.font, LEFT.get(i), cx - 150, 75 + i * 24, 0xAAAAAA);
        }
        for (int i = 0; i < RIGHT.size(); i++) {
            graphics.drawString(this.font, RIGHT.get(i), cx + 10, 75 + i * 24, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
