package ru.fozeton.diamondchat.gui.widgets;

import ru.dargen.evoplus.api.render.RenderContext;
import ru.dargen.evoplus.api.setting.SettingContainer;
import ru.dargen.evoplus.api.setting.type.WidgetElement;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.VoiceConfig;
import ru.fozeton.chatmanager.utils.stt.VoiceIndicator;

import java.util.function.IntToDoubleFunction;

public class VoiceIndicatorWidget {
    private static final int BARS = 5;
    private static final int BAR_W = 3;
    private static final int GAP = 2;
    private static final int MAX_H = 14;
    private static final int PAD = 4;
    private static final float[] SHAPE = {0.6f, 0.85f, 1.0f, 0.85f, 0.6f};
    private static final int PANEL_W = BARS * BAR_W + (BARS - 1) * GAP + PAD * 2;
    private static final int PANEL_H = MAX_H + PAD * 2;
    private final VoiceConfig voiceCfg = ChatConfigManager.getInstance().getVoiceConfig();
    private final WidgetElement element;

    public VoiceIndicatorWidget(SettingContainer container) {
        this.element = container.widget("voiceIndicator", "Индикатор голоса", PANEL_W, PANEL_H, this::render);
        this.element.setEnabled(true);
    }

    private static int argb(String hex) {
        return Long.decode(hex).intValue();
    }

    public static WidgetElement register(SettingContainer container) {
        return new VoiceIndicatorWidget(container).element;
    }

    private static void drawPanel(
            RenderContext ctx,
            int background,
            int border,
            int color,
            IntToDoubleFunction height
    ) {
        ctx.rect(0, 0, PANEL_W, PANEL_H, background);
        ctx.outline(0, 0, PANEL_W, PANEL_H, 1, border);

        for (int i = 0; i < BARS; i++) {
            int barH = Math.max(2, (int) (MAX_H * height.applyAsDouble(i)));
            int bx = PAD + i * (BAR_W + GAP);
            ctx.rect(bx, PAD + MAX_H - (double) barH, BAR_W, barH, color);
        }
    }

    private void render(RenderContext ctx) {
        int background = argb(voiceCfg.getBackgroundColor());

        if (ctx.isEditing()) {
            drawPanel(
                    ctx,
                    background,
                    argb(voiceCfg.getBorderColorDefault()),
                    argb(voiceCfg.getDefaultColor()),
                    i -> 0.5
            );
            return;
        }

        VoiceIndicator.render(
                ctx, (c, frame) -> {
                    long now = System.currentTimeMillis();

                    int border = switch (frame.state()) {
                        case LISTENING -> argb(voiceCfg.getBorderColorListening());
                        case ERROR -> 0xFFc04060;
                        default -> argb(voiceCfg.getBorderColorDefault());
                    };

                    int color = switch (frame.state()) {
                        case LOADING -> argb(voiceCfg.getLoadingColor());
                        case LISTENING -> argb(voiceCfg.getListeningColor());
                        default -> argb(voiceCfg.getDefaultColor());
                    };

                    drawPanel(
                            c, background, border, color, i -> switch (frame.state()) {
                                case LOADING -> 0.5 + 0.35 * Math.sin(now / 180.0 + i * 0.9);
                                case LISTENING -> 0.15 + 0.85 * Math.min(1f, frame.level() * SHAPE[i]);
                                default -> 0.5;
                            }
                    );
                }
        );
    }
}