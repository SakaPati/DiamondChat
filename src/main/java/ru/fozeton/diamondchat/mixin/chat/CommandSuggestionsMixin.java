package ru.fozeton.diamondchat.mixin.chat;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fozeton.chatmanager.module.MathEngine;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mixin for {@link CommandSuggestions} that introduces an embedded math evaluation engine directly into the chat UI.
 * <p>
 * It allows users to write mathematical expressions inside the chat input using the {@code ={...}} syntax
 * (e.g., {@code I have ={64 * 3} blocks}). It enhances the default chat UI by providing:
 * <ul>
 *     <li>Real-time syntax highlighting and evaluation previews overlaid on the text field.</li>
 *     <li>Brigadier-based autocompletion for mathematical symbols, constants, and functions.</li>
 *     <li>In-place evaluation and text replacement upon pressing the TAB key.</li>
 * </ul>
 */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Unique
    private static final Pattern CHATMANAGER$MATH_PATTERN = Pattern.compile("=\\{([^}]+)}");

    @Unique
    private final MathEngine chatmanager_core$mathEngine = MathEngine.getInstance();

    @Shadow
    @Final
    EditBox input;

    @Shadow
    private @Nullable CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    @Nullable
    private CommandSuggestions.@Nullable SuggestionsList suggestions;

    protected abstract void updateUsageInfo();

    @Shadow
    public abstract void showSuggestions(boolean bl);

    /**
     * Intercepts the command usage update tick to inject custom Brigadier suggestions for math expressions.
     * <p>
     * If the user's cursor is positioned inside a valid {@code ={...}} block, this method determines the word
     * boundary being actively typed. It then filters the available math symbols (e.g., {@code sin}, {@code cos},
     * {@code pi}) from the {@link MathEngine}. Finally, it overrides the default Minecraft command suggestions
     * by injecting a custom {@link Suggestions} future and forcing the overlay to render.
     *
     * @param ci Callback info allowing cancellation of standard command suggestion logic.
     */
    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    public void onMathSuggestion(CallbackInfo ci) {
        String fullText = this.input.getValue();
        int cursorPosition = this.input.getCursorPosition();
        Matcher matcher = CHATMANAGER$MATH_PATTERN.matcher(fullText);

        while (matcher.find()) {
            int mathStart = matcher.start(1);
            int mathEnd = matcher.end(1);

            if (cursorPosition >= mathStart && cursorPosition <= mathEnd) {
                String formulaUpToCursor = fullText.substring(mathStart, cursorPosition);

                int wordStart = formulaUpToCursor.length();
                while (wordStart > 0 && Character.isLetter(formulaUpToCursor.charAt(wordStart - 1))) wordStart--;

                String currentWord = formulaUpToCursor.substring(wordStart).toLowerCase();

                if (currentWord.isEmpty()) return;

                int startRange = mathStart + wordStart;
                StringRange range = StringRange.between(startRange, cursorPosition);

                List<Suggestion> matchedSuggestions = chatmanager_core$mathEngine.getAvailableSymbols().stream()
                        .filter(symbol -> symbol.toLowerCase().startsWith(currentWord))
                        .map(symbol -> new Suggestion(range, symbol))
                        .toList();

                if (!matchedSuggestions.isEmpty()) {
                    Suggestions suggestionsResult = Suggestions.create(fullText, matchedSuggestions);

                    this.pendingSuggestions = CompletableFuture.completedFuture(suggestionsResult);
                    this.pendingSuggestions.thenRun(() -> {
                        if (this.pendingSuggestions.isDone()) this.updateUsageInfo();
                    });

                    this.showSuggestions(false);
                    ci.cancel();
                    return;
                }
            }
        }
    }

    /**
     * Hooks into the chat box rendering pipeline to apply custom visual formatting
     * without altering the underlying raw string value sent to the server.
     * <p>
     * It intercepts the {@link FormattedCharSequence} generation and delegates the visual transformation
     * to {@link #chatmanager$parseMathFormula(String, FormattedCharSequence)}.
     */
    @Inject(method = "formatChat", at = @At("RETURN"), cancellable = true)
    private void onFormatChat(String string, int firstPosition, CallbackInfoReturnable<FormattedCharSequence> cir) {
        FormattedCharSequence originalResult = cir.getReturnValue();
        cir.setReturnValue(chatmanager$parseMathFormula(string, originalResult));
    }

    /**
     * Evaluates and replaces all valid math expressions in the chat input when the user presses the TAB key.
     * <p>
     * This logic is executed only if the standard suggestions overlay is closed ({@code suggestions == null}).
     * It iterates through all matched {@code ={...}} patterns, calculates their results via {@link MathEngine},
     * and seamlessly replaces the original formulas with their computed values (e.g., replacing "={2+2}" with "4").
     * It safely strips trailing decimal zeroes for integer results and re-adjusts the cursor to the end of the string.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onTabMathEval(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() == GLFW.GLFW_KEY_TAB && suggestions == null) {
            String currentText = this.input.getValue();
            Matcher matcher = CHATMANAGER$MATH_PATTERN.matcher(currentText);

            if (matcher.find()) {
                StringBuilder builder = new StringBuilder();
                matcher.reset();

                boolean hasReplacements = false;
                while (matcher.find()) {
                    String formula = matcher.group(1);
                    if (this.chatmanager_core$mathEngine.isValid(formula)) {
                        double result = this.chatmanager_core$mathEngine.eval(formula);

                        String resultStr = (result % 1 == 0) ? String.valueOf((long) result) : String.valueOf(result);

                        matcher.appendReplacement(builder, Matcher.quoteReplacement(resultStr));
                        hasReplacements = true;
                    }
                }

                if (hasReplacements) {
                    matcher.appendTail(builder);

                    this.input.setValue(builder.toString());
                    this.input.setCursorPosition(this.input.getValue().length());

                    cir.setReturnValue(true);
                    cir.cancel();
                }
            }
        }
    }

    /**
     * Parses the raw chat input and builds a visually formatted text sequence for GUI rendering.
     * <p>
     * - <b>Valid expressions:</b> Highlighted in green ({@code 0x55FF55}) with a gray ({@code 0xFF808080}) preview of the computed result.
     * <br>
     * - <b>Invalid expressions:</b> Highlighted in red ({@code 0xFF5555}) with no preview.
     * <p>
     * Plain text outside the expression blocks remains visually unaltered.
     *
     * @param msg      The raw string currently residing in the chat edit box.
     * @param fallback The original unmodified {@link FormattedCharSequence} to return if no math formulas are matched.
     * @return A stylized {@link FormattedCharSequence} ready for rendering on the screen.
     */
    @Unique
    private FormattedCharSequence chatmanager$parseMathFormula(String msg, FormattedCharSequence fallback) {
        Matcher matcher = CHATMANAGER$MATH_PATTERN.matcher(msg);
        if (!matcher.find()) return fallback;

        matcher.reset();
        MutableComponent rootText = Component.empty();
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) rootText.append(Component.literal(msg.substring(lastEnd, matcher.start())));

            String fullMatch = matcher.group(0);
            String formula = matcher.group(1);

            boolean mathEngineValid = chatmanager_core$mathEngine.isValid(formula);
            int color = mathEngineValid ? 0x55FF55 : 0xFF5555;
            rootText.append(Component.literal(fullMatch).withColor(color));

            if (mathEngineValid) {
                rootText.append(Component.literal("=" + chatmanager_core$mathEngine.eval(formula)).withColor(0xFF808080));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < msg.length()) rootText.append(Component.literal(msg.substring(lastEnd)));

        return rootText.getVisualOrderText();
    }
}