package ru.fozeton.diamondchat.gui.widgets;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import ru.fozeton.chatmanager.module.Gif;
import ru.fozeton.chatmanager.module.gif.GifsResponse;
import ru.fozeton.chatmanager.module.gif.McAnimCodec;
import ru.fozeton.diamondchat.chat.utils.GifTexture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class GifPickerWidget extends AbstractWidget {
    private static final int MAX_W = 164;
    private static final int MAX_H = 340;
    private static final int PAD = 4;
    private static final int GAP = 3;
    private static final int COLS = 2;
    private static final int CELL_H = 62;
    private static final int SEARCH_H = 16;
    private static final int FOOTER_H = 12;
    private static final int ICON_GAP = 2;
    private static final int MAX_PARALLEL = 4;
    private static final int SCROLL_STEP = 45;
    private static final long SEARCH_DEBOUNCE_MS = 350;

    private static final int COL_BG = 0xFF212128;
    private static final int COL_ROW = 0xFF1a1a24;
    private static final int COL_MUTED = 0xFF6b6b88;
    private static final int COL_PURPLE = 0xFF7c6ef5;
    private static final int COL_BORDER = 0xFF3a2e6e;
    private static final int COL_BORDER_HOVER = 0xFF5a4a9e;
    private static final int COL_TEXT_ACTIVE = 0xFFa78bfa;
    private static final int COL_TEXT_HOVER = 0xFF8878cc;
    private static final int COL_ICON_BG = 0xFF18181f;
    private static final int COL_TRACK = 0xFF2e2e3d;

    private static final ExecutorService IO = Executors.newVirtualThreadPerTaskExecutor();
    private static final Semaphore SLOTS = new Semaphore(MAX_PARALLEL);
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final Consumer<String> onSelect;
    @Getter
    private final SearchEditBox searchBox;

    private final Map<String, GifTexture> anims = new HashMap<>();
    private final Set<String> loading = new HashSet<>();
    private final Set<String> failed = new HashSet<>();
    private final Set<String> seen = new HashSet<>();
    private final List<Item> items = new ArrayList<>();

    private volatile boolean closed;
    private volatile long gen;
    private Set<String> visibleIds = Set.of();

    private Path tempDir;
    private String query = "";
    private String pendingQuery = "";
    private long searchAt = 0;
    private int page = 0;
    private boolean hasMore = true;
    private boolean fetching;
    private boolean error;
    private boolean requested;
    private boolean open;

    private double scroll;
    private double targetScroll;
    private long lastFrame;

    private int px;
    private int py;
    private int pw;
    private int ph;
    private int gridW;
    private int gridH;
    private int cellW;

    public GifPickerWidget(int x, int y, int width, int height, Consumer<String> onSelect) {
        super(x, y, width, height, Component.literal("GIF"));
        this.onSelect = onSelect;

        this.searchBox = new SearchEditBox(
                Minecraft.getInstance().font, 0, 0, 100, SEARCH_H, Component.literal("Search"));
        this.searchBox.setHint(Component.literal("Search KLIPY..."));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(text -> {
            pendingQuery = text;
            searchAt = System.currentTimeMillis() + SEARCH_DEBOUNCE_MS;
        });
        this.searchBox.setOnEnter(() -> {
            searchAt = 0;
            applyPendingQuery(searchBox.getValue());
        });
    }

    private static List<Item> toItems(GifsResponse r) {
        List<Item> out = new ArrayList<>();
        if (r == null || r.getData() == null || r.getData().getData() == null) return out;
        for (var g : r.getData().getData()) {
            out.add(new Item(g.getSlug(), g.getFile().getXs().getAnimated().getUrl()));
        }
        return out;
    }

    private static McAnimCodec.Animation readAndDelete(Path file) {
        try {
            return McAnimCodec.read(file);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
            }
        }
    }

    private void applyPendingQuery(String raw) {
        String q = raw.trim();
        if (q.equals(query) && requested) return;
        startSearch(q);
    }

    private void startSearch(String q) {
        requested = true;
        query = q;
        gen++;
        items.clear();
        seen.clear();
        failed.clear();
        loading.clear();
        clearAnims();
        page = 0;
        hasMore = true;
        fetching = false;
        error = false;
        scroll = 0;
        targetScroll = 0;
        fetchNext();
    }

    private void fetchNext() {
        if (closed || fetching || !hasMore || error) return;
        fetching = true;
        int nextPage = page + 1;
        long g = gen;
        String q = query;

        CompletableFuture
                .supplyAsync(
                        () -> q.isEmpty() ? Gif.getInstance().trending(nextPage) : Gif.getInstance().search(q, nextPage),
                        IO
                )
                .whenCompleteAsync((r, ex) -> {
                    if (closed || g != gen) return;
                    fetching = false;
                    if (ex != null) {
                        error = true;
                        return;
                    }
                    try {
                        int added = 0;
                        for (Item it : toItems(r)) {
                            if (seen.add(it.id())) {
                                items.add(it);
                                added++;
                            }
                        }
                        page = nextPage;
                        if (added == 0) hasMore = false;
                    } catch (Exception e) {
                        error = true;
                    }
                }, Minecraft.getInstance());
    }

    private void load(Item item) {
        if (!loading.add(item.id())) return;
        long g = gen;
        Path dir = tempDir();
        String name = "g" + COUNTER.incrementAndGet();

        IO.execute(() -> {
            McAnimCodec.Animation result = null;
            boolean skipped = false;
            try {
                SLOTS.acquire();
                try {
                    if (closed || g != gen || !visibleIds.contains(item.id())) {
                        skipped = true;
                    } else {
                        boolean ok = Boolean.TRUE.equals(
                                Gif.getInstance().downloadFromUrl(item.animUrl(), name, dir.toString()).join());
                        if (ok) result = readAndDelete(dir.resolve(name + ".mcanim"));
                    }
                } finally {
                    SLOTS.release();
                }
            } catch (Exception ignored) {
            }

            McAnimCodec.Animation finalResult = result;
            boolean wasSkipped = skipped;
            Minecraft.getInstance().execute(() -> {
                if (closed || g != gen) return;
                loading.remove(item.id());
                if (wasSkipped) return;
                if (finalResult == null) failed.add(item.id());
                else if (!anims.containsKey(item.id())) anims.put(item.id(), GifTexture.standalone(finalResult));
            });
        });
    }

    private Path tempDir() {
        if (tempDir == null) {
            try {
                tempDir = Files.createTempDirectory("diamondchat-gifs");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return tempDir;
    }

    private void clearAnims() {
        anims.values().forEach(GifTexture::close);
        anims.clear();
    }

    private void deleteTempDir() {
        if (tempDir == null) return;
        try (Stream<Path> walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public void close() {
        closed = true;
        open = false;
        searchBox.setFocused(false);
        clearAnims();
        deleteTempDir();
    }

    private void layout() {
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        pw = Math.min(MAX_W, sw - 8);
        ph = Math.max(100, Math.min(MAX_H, getY() - 8));
        px = Math.max(4, getX() + getWidth() - pw);
        py = getY() - ICON_GAP - ph;

        gridW = pw - PAD * 2;
        cellW = (gridW - (COLS - 1) * GAP) / COLS;
        gridH = ph - gridTop() - FOOTER_H - 2;
    }

    private int gridTop() {
        return PAD + SEARCH_H + GAP;
    }

    private int gridX() {
        return px + PAD;
    }

    private int gridY() {
        return py + gridTop();
    }

    private int contentH() {
        int rows = (items.size() + COLS - 1) / COLS;
        return rows == 0 ? 0 : rows * (CELL_H + GAP) - GAP;
    }

    private int maxScroll() {
        return Math.max(0, contentH() - gridH);
    }

    private boolean overIcon(double mx, double my) {
        return mx >= getX() && mx < getX() + width && my >= getY() && my < getY() + height;
    }

    private boolean overPanel(double mx, double my) {
        return mx >= px && mx < px + pw && my >= py && my < getY();
    }

    private boolean overSearch(double mx, double my) {
        return mx >= px + PAD && mx < px + PAD + gridW && my >= py + PAD && my < py + PAD + SEARCH_H;
    }

    private int indexAt(double mx, double my) {
        int rx = (int) mx - gridX();
        int ry = (int) my - gridY();
        if (rx < 0 || ry < 0 || rx >= gridW || ry >= gridH) return -1;

        int col = rx / (cellW + GAP);
        if (col >= COLS || rx % (cellW + GAP) >= cellW) return -1;

        int cy = ry + (int) scroll;
        int rowH = CELL_H + GAP;
        if (cy % rowH >= CELL_H) return -1;

        int idx = (cy / rowH) * COLS + col;
        return idx < items.size() ? idx : -1;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layout();

        boolean hoverIcon = overIcon(mouseX, mouseY);
        if (hoverIcon && !open) {
            openPanel();
        } else if (open && !hoverIcon && !overPanel(mouseX, mouseY) && !searchBox.isFocused()) {
            open = false;
            lastFrame = 0;
        }

        renderIcon(g, hoverIcon);
        if (open) renderPopup(g, mouseX, mouseY, delta);
    }

    private void openPanel() {
        open = true;
        if (!requested) {
            startSearch("");
        } else if (error) {
            error = false;
            fetchNext();
        }
    }

    private void renderIcon(GuiGraphicsExtractor g, boolean hover) {
        g.fill(getX(), getY(), getX() + width, getY() + height, COL_ICON_BG);
        g.outline(getX(), getY(), width, height, open ? COL_PURPLE : hover ? COL_BORDER_HOVER : COL_BORDER);

        var font = Minecraft.getInstance().font;
        String label = "GIF";
        int color = open ? COL_TEXT_ACTIVE : hover ? COL_TEXT_HOVER : COL_MUTED;
        g.text(font, label, getX() + (width - font.width(label)) / 2, getY() + (height - 8) / 2 + 1, color, false);
    }

    private void renderPopup(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;

        if (searchAt != 0 && System.currentTimeMillis() >= searchAt) {
            searchAt = 0;
            applyPendingQuery(pendingQuery);
        }

        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0;
        lastFrame = now;

        double max = maxScroll();
        targetScroll = Math.clamp(targetScroll, 0.0, max);
        scroll += (targetScroll - scroll) * (1 - Math.exp(-dt * 18));
        if (Math.abs(targetScroll - scroll) < 0.4) scroll = targetScroll;
        scroll = Math.clamp(scroll, 0.0, max);

        int rowH = CELL_H + GAP;
        if (!error && hasMore && !fetching && scroll + gridH >= contentH() - 2 * rowH) fetchNext();

        g.fill(px, py, px + pw, py + ph, COL_BG);
        g.outline(px, py, pw, ph, COL_PURPLE);

        searchBox.setX(px + PAD);
        searchBox.setY(py + PAD);
        searchBox.setWidth(gridW);
        searchBox.extractRenderState(g, mouseX, mouseY, delta);

        int sc = (int) scroll;
        int firstRow = sc / rowH;
        int lastRow = (sc + gridH) / rowH;
        int first = firstRow * COLS;
        int last = Math.min(items.size(), (lastRow + 1) * COLS);

        Set<String> visible = new HashSet<>();
        for (int i = first; i < last; i++) visible.add(items.get(i).id());
        visibleIds = visible;
        anims.entrySet().removeIf(e -> {
            if (visible.contains(e.getKey())) return false;
            e.getValue().close();
            return true;
        });

        int hovered = indexAt(mouseX, mouseY);

        g.enableScissor(gridX(), gridY(), gridX() + gridW, gridY() + gridH);
        for (int i = first; i < last; i++) {
            Item item = items.get(i);
            int x = gridX() + (i % COLS) * (cellW + GAP);
            int y = gridY() + (i / COLS) * rowH - sc;

            g.fill(x, y, x + cellW, y + CELL_H, COL_ROW);

            GifTexture t = anims.get(item.id());
            if (t != null) {
                drawFit(g, t, x, y);
            } else {
                if (!failed.contains(item.id())) load(item);
                String s = failed.contains(item.id()) ? "-" : "...";
                g.text(font, s, x + (cellW - font.width(s)) / 2, y + (CELL_H - 8) / 2, COL_MUTED, false);
            }

            g.outline(x, y, cellW, CELL_H, i == hovered ? COL_PURPLE : COL_BORDER);
        }

        if (items.isEmpty()) {
            String status = fetching ? "Загрузка..." : error ? "Ошибка запроса" : "Ничего не найдено";
            g.text(font, status, gridX(), gridY() + 4, COL_MUTED, false);
        }
        g.disableScissor();

        if (max > 0) {
            int trackX = px + pw - 3;
            int thumbH = Math.max(12, (int) ((double) gridH * gridH / contentH()));
            int thumbY = gridY() + (int) (scroll * (gridH - thumbH) / max);
            g.fill(trackX, gridY(), trackX + 2, gridY() + gridH, COL_TRACK);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, COL_PURPLE);
        }

        int footerY = py + ph - FOOTER_H + 2;
        String left = fetching && !items.isEmpty() ? "Загрузка..." : query.isEmpty() ? "Популярное" : "Поиск";
        g.text(font, left, px + PAD, footerY, 0xFF434354, false);
    }

    private void drawFit(GuiGraphicsExtractor g, GifTexture t, int x, int y) {
        if (t.getWidth() <= 0 || t.getHeight() <= 0) return;
        double scale = Math.min((cellW - 2) / (double) t.getWidth(), (CELL_H - 2) / (double) t.getHeight());
        int w = Math.max(1, (int) (t.getWidth() * scale));
        int h = Math.max(1, (int) (t.getHeight() * scale));
        t.render(g, x + (cellW - w) / 2, y + (CELL_H - h) / 2, w, h, 1f);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        boolean inIcon = overIcon(mx, my);
        boolean inPanel = open && overPanel(mx, my);

        if (!inIcon && !inPanel) {
            if (searchBox.isFocused()) {
                searchBox.setFocused(false);
                open = false;
                lastFrame = 0;
            }
            return false;
        }

        if (inPanel && overSearch(mx, my)) {
            searchBox.setFocused(true);
            searchBox.mouseClicked(event, doubleClick);
            return true;
        }

        if (searchBox.isFocused()) searchBox.setFocused(false);
        if (inIcon) return true;

        if (event.button() == 0) {
            int idx = indexAt(mx, my);
            if (idx >= 0) onSelect.accept(items.get(idx).id());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!open || !(overPanel(mouseX, mouseY) || overIcon(mouseX, mouseY))) return false;
        targetScroll = Math.clamp(targetScroll - scrollY * SCROLL_STEP, 0.0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!searchBox.isFocused()) return false;

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            searchBox.setFocused(false);
            open = false;
            lastFrame = 0;
            return true;
        }

        searchBox.keyPressed(event);
        return true;
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput output) {
    }

    private record Item(String id, String animUrl) {
    }
}