package ru.fozeton.diamondchat.mixin.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fozeton.chatmanager.config.AiStyleTextConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.MacrosConfig;
import ru.fozeton.chatmanager.utils.Logger;
import ru.fozeton.diamondchat.utils.MacroManager;
import ru.fozeton.diamondchat.utils.notify.Notification;
import ru.fozeton.diamondchat.gui.CalculatorScreen;
import ru.fozeton.diamondchat.gui.widgets.DropdownButtonWidget;
import ru.fozeton.diamondchat.gui.MacrosScreen;
import ru.fozeton.diamondchat.gui.widgets.TabButtonWidget;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {
    @Unique
    private final AiStyleTextConfig styleTextConfig = ChatConfigManager.getInstance().load(AiStyleTextConfig.class);
    @Unique
    private final HttpClient client = HttpClient.newHttpClient();
    @Unique
    private final Gson gson = new Gson();
    @Shadow
    protected EditBox input;
    @Unique
    private TabButtonWidget macrosButton;
    @Unique
    private TabButtonWidget calculatorButton;
    @Unique
    private DropdownButtonWidget aiStyleText;
    @Unique
    private Logger log = new Logger(ChatScreenMixin.class);

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int y = this.height - 30;
        int h = 14;
        int x = 2;
        int gap = 54;

        MacrosConfig config = MacroManager.getConfig();

        this.macrosButton = new TabButtonWidget(x, y, 50, h,
                Component.literal("Macros"),
                () -> {
                    config.setEnabled(!config.isEnabled());
                    MacroManager.saveConfig();
                    if (this.macrosButton != null)
                        this.macrosButton.setActive(config.isEnabled());
                },
                () -> Minecraft.getInstance().setScreen(new MacrosScreen())
        );

        this.calculatorButton = new TabButtonWidget(x + gap, y, 80, h,
                Component.literal("Калькулятор"),
                () -> Minecraft.getInstance().setScreen(new CalculatorScreen()),
                null
        );

        this.aiStyleText = new DropdownButtonWidget(
                Minecraft.getInstance().getWindow().getGuiScaledWidth() - 102,
                y, 100, h, Component.literal("Стиль текста"));

        for (Map.Entry<String, AiStyleTextConfig.AiStyle> entry : styleTextConfig.getStyles().entrySet()) {
            AiStyleTextConfig.AiStyle aiStyle = entry.getValue();

            this.aiStyleText.addItem(entry.getKey(), () -> {
                aiStyle.getMessages().add(AiStyleTextConfig.Message.builder()
                        .role("user")
                        .content(this.input.getValue())
                        .build());

                String body = gson.toJson(aiStyle);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(styleTextConfig.getApiUrl()))
                        .headers(
                                "Authorization", "Bearer " + Objects.requireNonNull(styleTextConfig.getApiKey(), "Укажите api ключ"),
                                "Content-Type", "application/json"
                        )
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
                    int status = response.statusCode();
                    if (status >= 400) {
                        String msg = "Ошибка " + status + (status == 401 ? ": неверный API ключ" : status == 429 ? ": превышен лимит запросов" : "");
                        log.error("Запрос стиля текста не удался: " + status + " | ответ: " + response.body());
                        Notification.showError(msg);
                        return;
                    }
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    String content = json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                    log.info("Стиль текста применён, потрачено токенов: " +
                            json.getAsJsonObject("usage").get("total_tokens").getAsInt());
                    this.input.setValue(content);
                }).exceptionally(ex -> {
                    log.error("Ошибка соединения с сервером: " + ex.getMessage());
                    Notification.showError("Нет соединения с сервером");
                    return null;
                });
            });
        }
        this.macrosButton.setActive(config.isEnabled());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.macrosButton != null) this.macrosButton.render(graphics, mouseX, mouseY, delta);
        if (this.calculatorButton != null) this.calculatorButton.render(graphics, mouseX, mouseY, delta);
        if (this.aiStyleText != null) this.aiStyleText.render(graphics, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.macrosButton != null && this.macrosButton.mouseClicked(mouseX, mouseY, button))
            cir.setReturnValue(true);

        if (this.calculatorButton != null && this.calculatorButton.mouseClicked(mouseX, mouseY, button))
            cir.setReturnValue(true);

        if (this.aiStyleText != null && this.aiStyleText.mouseClicked(mouseX, mouseY, button))
            cir.setReturnValue(true);
    }


}