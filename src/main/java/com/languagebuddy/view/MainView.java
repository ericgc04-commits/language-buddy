package com.languagebuddy.view;

import com.languagebuddy.controller.MainController;
import com.languagebuddy.model.QuizResult;
import com.languagebuddy.model.UserProgress;
import com.languagebuddy.model.Word;
import com.languagebuddy.service.ChatbotService.BotResponse;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.*;

/**
 * Premium JavaFX UI for Language Buddy.
 * Design: luxury editorial dark theme — charcoal/navy, gold accents,
 * glassmorphism cards, smooth micro-animations.
 */
public class MainView {

    // ── Controller ────────────────────────────────────────────────────────────
    private final MainController ctrl = new MainController();

    // ── Chat state ────────────────────────────────────────────────────────────
    private VBox chatMessages;
    private ScrollPane chatScroll;
    private TextField chatInput;
    private Button sendBtn;
    private Node typingIndicator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bot-thread"); t.setDaemon(true); return t;
    });

    // ── Vocab table ───────────────────────────────────────────────────────────
    private TableView<Word> vocabTable;
    private ObservableList<Word> wordList;
    private TextField searchField;
    private ComboBox<String> catFilter;
    private ComboBox<String> diffFilter;

    // ── Sidebar labels ────────────────────────────────────────────────────────
    private Label levelLabel;
    private Label xpLabel;
    private Arc xpArc;
    private Label wordCountBadge;
    private Label masteredBadge;

    // ── Panels ────────────────────────────────────────────────────────────────
    private VBox chatPanel, vocabPanel, statsPanel;
    private Button activeNavBtn;

    // ═════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═════════════════════════════════════════════════════════════════════════

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());

        Scene scene = new Scene(root, 1160, 740);
        applyGlobalStyle(scene);

        stage.setTitle("Language Buddy — Vocabulary Learning Chatbot");
        stage.setScene(scene);
        stage.setMinWidth(860); stage.setMinHeight(560);
        stage.show();

        Platform.runLater(() -> {
            addBotMessage(ctrl.getWelcomeMessage(), BotResponse.Kind.TEXT);
            refreshSidebar();
            switchPanel("chat");
        });
    }

    // ── Global CSS ────────────────────────────────────────────────────────────

    private void applyGlobalStyle(Scene scene) {
        scene.setFill(Color.web("#0c0c18"));
        // Inline CSS for full control without needing an external file
        scene.getRoot().setStyle("""
            -fx-font-family: 'Segoe UI', 'SF Pro Text', Helvetica, Arial, sans-serif;
            -fx-background-color: #0c0c18;
            """);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SIDEBAR
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildSidebar() {
        VBox bar = new VBox(0);
        bar.setPrefWidth(230);
        bar.setStyle("-fx-background-color: linear-gradient(to bottom, #12122a 0%, #0a0a1c 100%); -fx-border-color: transparent #1e1e3a transparent transparent; -fx-border-width: 0 1 0 0;");

        bar.getChildren().addAll(
            buildLogo(),
            buildXpRing(),
            buildNavSection(),
            buildQuickStatsSection(),
            buildSidebarFooter()
        );
        return bar;
    }

    private VBox buildLogo() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(28, 22, 22, 22));

        Label icon = new Label("📖");
        icon.setStyle("-fx-font-size: 34px;");

        Label title = new Label("Language Buddy");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e8e4ff;");

        Label sub = new Label("Vocabulary Learning Chatbot");
        sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #4a4a7a; letter-spacing: 0.5px;");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #1e1e3a; -fx-pref-height: 1;");
        VBox.setMargin(sep, new Insets(12, 0, 0, 0));

        box.getChildren().addAll(icon, title, sub, sep);
        return box;
    }

    /** Circular XP ring + level badge. */
    private VBox buildXpRing() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16, 20, 16, 20));
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12;");
        VBox.setMargin(box, new Insets(0, 12, 8, 12));

        // Arc progress ring (background + foreground)
        double radius = 34, cx = 46, cy = 46;
        Arc bgArc = new Arc(cx, cy, radius, radius, 90, -360);
        bgArc.setType(ArcType.OPEN);
        bgArc.setStroke(Color.web("#1e1e3a")); bgArc.setStrokeWidth(5);
        bgArc.setFill(Color.TRANSPARENT); bgArc.setStrokeLineCap(StrokeLineCap.ROUND);

        xpArc = new Arc(cx, cy, radius, radius, 90, 0);
        xpArc.setType(ArcType.OPEN);
        xpArc.setStroke(Color.web("#c9a84c")); xpArc.setStrokeWidth(5);
        xpArc.setFill(Color.TRANSPARENT); xpArc.setStrokeLineCap(StrokeLineCap.ROUND);

        levelLabel = new Label("🌱");
        levelLabel.setStyle("-fx-font-size: 22px;");
        levelLabel.setTranslateX(-2);

        StackPane ring = new StackPane(bgArc, xpArc, levelLabel);
        ring.setPrefSize(92, 92);

        xpLabel = new Label("0 XP");
        xpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7070aa; -fx-font-weight: bold;");

        Label levelText = new Label("Seedling");
        levelText.setId("sidebar-level-text");
        levelText.setStyle("-fx-font-size: 12px; -fx-text-fill: #c9a84c; -fx-font-weight: bold;");

        box.getChildren().addAll(ring, levelText, xpLabel);
        return box;
    }

    private VBox buildNavSection() {
        VBox nav = new VBox(2);
        nav.setPadding(new Insets(12, 0, 0, 0));

        Label navHeader = new Label("NAVIGATION");
        navHeader.setStyle("-fx-font-size: 9px; -fx-text-fill: #2e2e5a; -fx-font-weight: bold; -fx-padding: 0 0 6 20;");

        Button chatBtn  = navBtn("💬  Chat",         "chat");
        Button vocabBtn = navBtn("📚  Vocabulary",    "vocab");
        Button statsBtn = navBtn("📊  Statistics",    "stats");

        activeNavBtn = chatBtn;
        activateNavBtn(chatBtn);

        nav.getChildren().addAll(navHeader, chatBtn, vocabBtn, statsBtn);
        return nav;
    }

    private Button navBtn(String text, String panel) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setStyle(NAV_IDLE);
        b.setOnMouseEntered(e -> { if (b != activeNavBtn) b.setStyle(NAV_HOVER); });
        b.setOnMouseExited(e  -> { if (b != activeNavBtn) b.setStyle(NAV_IDLE); });
        b.setOnAction(e -> {
            if (activeNavBtn != null) { activeNavBtn.setStyle(NAV_IDLE); }
            activateNavBtn(b);
            switchPanel(panel);
            if ("vocab".equals(panel))  refreshVocabTable(null, null, null);
            if ("stats".equals(panel))  refreshStatsPanel();
        });
        return b;
    }

    private void activateNavBtn(Button b) {
        b.setStyle(NAV_ACTIVE);
        activeNavBtn = b;
    }

    private static final String NAV_IDLE   = "-fx-background-color: transparent; -fx-text-fill: #5555aa; -fx-font-size: 13px; -fx-padding: 11 20; -fx-background-radius: 0; -fx-cursor: hand; -fx-border-color: transparent;";
    private static final String NAV_HOVER  = "-fx-background-color: rgba(200,168,76,0.06); -fx-text-fill: #8888cc; -fx-font-size: 13px; -fx-padding: 11 20; -fx-background-radius: 0; -fx-cursor: hand; -fx-border-color: transparent;";
    private static final String NAV_ACTIVE = "-fx-background-color: rgba(201,168,76,0.12); -fx-text-fill: #c9a84c; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 11 20 11 17; -fx-background-radius: 0; -fx-cursor: hand; -fx-border-color: transparent transparent transparent #c9a84c; -fx-border-width: 0 0 0 3;";

    private VBox buildQuickStatsSection() {
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);

        VBox box = new VBox(6);
        box.setPadding(new Insets(16, 16, 12, 16));
        box.setStyle("-fx-border-color: #1a1a32 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Label header = new Label("QUICK STATS");
        header.setStyle("-fx-font-size: 9px; -fx-text-fill: #2e2e5a; -fx-font-weight: bold;");

        HBox words = statRow("📚", "Words learned");
        wordCountBadge = (Label) ((HBox)words).getChildren().get(2);

        HBox mastered = statRow("🏆", "Mastered");
        masteredBadge = (Label) ((HBox)mastered).getChildren().get(2);

        box.getChildren().addAll(header, words, mastered);

        VBox combined = new VBox();
        combined.getChildren().addAll(push, box);
        return combined;
    }

    private HBox statRow(String emoji, String label) {
        Label e = new Label(emoji); e.setStyle("-fx-font-size: 13px;");
        Label l = new Label(label); l.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a4a7a;"); HBox.setHgrow(l, Priority.ALWAYS);
        Label v = new Label("0");   v.setStyle("-fx-font-size: 11px; -fx-text-fill: #8888bb; -fx-font-weight: bold;");
        HBox row = new HBox(6, e, l, v); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildSidebarFooter() {
        VBox footer = new VBox(2);
        footer.setPadding(new Insets(10, 16, 18, 16));
        Label l1 = new Label("York St John University");
        Label l2 = new Label("Eric González Ceballos · 250158429");
        for (Label l : List.of(l1, l2))
            l.setStyle("-fx-font-size: 9px; -fx-text-fill: #25254a;");
        footer.getChildren().addAll(l1, l2);
        return footer;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CONTENT AREA
    // ═════════════════════════════════════════════════════════════════════════

    private StackPane buildContent() {
        StackPane stack = new StackPane();
        stack.setStyle("-fx-background-color: #0c0c18;");

        chatPanel  = buildChatPanel();
        vocabPanel = buildVocabPanel();
        statsPanel = buildStatsPanel();

        stack.getChildren().addAll(statsPanel, vocabPanel, chatPanel);
        return stack;
    }

    private void switchPanel(String name) {
        chatPanel.setVisible("chat".equals(name));
        vocabPanel.setVisible("vocab".equals(name));
        statsPanel.setVisible("stats".equals(name));
    }

    // ── Section header template ───────────────────────────────────────────────

    private HBox sectionHeader(String title, String sub, String accent) {
        VBox texts = new VBox(3);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e8e4ff;");
        Label s = new Label(sub);
        s.setStyle("-fx-font-size: 11px; -fx-text-fill: #3a3a6a;");
        texts.getChildren().addAll(t, s);

        // Coloured left accent bar
        Rectangle accentBar = new Rectangle(3, 28);
        accentBar.setArcWidth(3); accentBar.setArcHeight(3);
        accentBar.setFill(Color.web(accent));

        HBox header = new HBox(14, accentBar, texts);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 22, 14, 22));
        header.setStyle("-fx-background-color: #0f0f22; -fx-border-color: transparent transparent #18183a transparent; -fx-border-width: 0 0 1 0;");
        return header;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CHAT PANEL
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildChatPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0c0c18;");

        HBox header = sectionHeader("💬 Chat", "Ask me to define words, quiz you, or add vocabulary", "#c9a84c");

        // Messages
        chatMessages = new VBox(12);
        chatMessages.setPadding(new Insets(20, 20, 10, 20));
        chatScroll = new ScrollPane(chatMessages);
        chatScroll.setFitToWidth(true);
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        chatScroll.setStyle(chatScroll.getStyle() + "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Quick actions
        HBox chips = buildChips();
        chips.setPadding(new Insets(0, 20, 6, 20));

        // Input
        HBox inputBar = buildInputBar();

        panel.getChildren().addAll(header, chatScroll, chips, inputBar);
        return panel;
    }

    private HBox buildChips() {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        String[][] chips = {
            {"💡 Word of Day","word of the day"},
            {"🎯 Quiz Me","quiz me"},
            {"🃏 Flashcards","flashcards"},
            {"📊 My Stats","stats"},
            {"📚 All Words","list words"},
            {"❓ Help","help"}
        };
        for (String[] c : chips) {
            Button btn = new Button(c[0]);
            btn.setStyle("-fx-background-color: rgba(201,168,76,0.08); -fx-text-fill: #7070aa; -fx-font-size: 11px; -fx-padding: 5 12; -fx-background-radius: 20; -fx-border-color: rgba(201,168,76,0.2); -fx-border-radius: 20; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(201,168,76,0.18); -fx-text-fill: #c9a84c; -fx-font-size: 11px; -fx-padding: 5 12; -fx-background-radius: 20; -fx-border-color: #c9a84c; -fx-border-radius: 20; -fx-cursor: hand;"));
            btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-color: rgba(201,168,76,0.08); -fx-text-fill: #7070aa; -fx-font-size: 11px; -fx-padding: 5 12; -fx-background-radius: 20; -fx-border-color: rgba(201,168,76,0.2); -fx-border-radius: 20; -fx-cursor: hand;"));
            final String cmd = c[1];
            btn.setOnAction(e -> { chatInput.setText(cmd); handleSend(); });
            row.getChildren().add(btn);
        }
        return row;
    }

    private HBox buildInputBar() {
        chatInput = new TextField();
        chatInput.setPromptText("Ask me anything... (define, quiz me, add, word of the day...)");
        chatInput.setStyle("""
            -fx-background-color: #141428;
            -fx-text-fill: #d0ccff;
            -fx-prompt-text-fill: #2e2e5e;
            -fx-border-color: #252550;
            -fx-border-radius: 28;
            -fx-background-radius: 28;
            -fx-padding: 12 18;
            -fx-font-size: 13px;
            """);
        chatInput.setOnAction(e -> handleSend());
        chatInput.focusedProperty().addListener((obs, o, focused) ->
            chatInput.setStyle(chatInput.getStyle().replace(
                focused ? "#252550" : "#c9a84c",
                focused ? "#c9a84c" : "#252550"
            ))
        );
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        sendBtn = new Button("Send ➤");
        sendBtn.setStyle("""
            -fx-background-color: linear-gradient(to right, #c9a84c, #e6c56a);
            -fx-text-fill: #0a0a18;
            -fx-font-weight: bold;
            -fx-font-size: 13px;
            -fx-padding: 12 24;
            -fx-background-radius: 28;
            -fx-cursor: hand;
            -fx-border-color: transparent;
            """);
        sendBtn.setOnMouseEntered(e -> sendBtn.setOpacity(0.88));
        sendBtn.setOnMouseExited(e  -> sendBtn.setOpacity(1.0));
        sendBtn.setOnAction(e -> handleSend());

        HBox bar = new HBox(10, chatInput, sendBtn);
        bar.setPadding(new Insets(10, 20, 16, 20));
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: #0f0f22; -fx-border-color: #18183a transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    // ── Message send ──────────────────────────────────────────────────────────

    private void handleSend() {
        String input = chatInput.getText().trim();
        if (input.isEmpty()) return;
        chatInput.clear(); sendBtn.setDisable(true); chatInput.setDisable(true);

        addUserMessage(input);
        showTyping();

        executor.submit(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            BotResponse resp = ctrl.sendMessage(input);
            Platform.runLater(() -> {
                removeTyping();
                addBotMessage(resp.content, resp.kind);
                sendBtn.setDisable(false); chatInput.setDisable(false); chatInput.requestFocus();
                refreshSidebar();
            });
        });
    }

    // ── Chat bubble builders ──────────────────────────────────────────────────

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(520);
        bubble.setStyle("""
            -fx-background-color: linear-gradient(135deg, #2a2060, #1a1448);
            -fx-background-radius: 18 4 18 18;
            -fx-padding: 12 16;
            -fx-border-color: rgba(201,168,76,0.2);
            -fx-border-radius: 18 4 18 18;
            -fx-border-width: 1;
            """);

        Label msg = new Label(text);
        msg.setWrapText(true); msg.setMaxWidth(500);
        msg.setStyle("-fx-text-fill: #d8d0ff; -fx-font-size: 13px;");
        bubble.getChildren().add(msg);

        row.getChildren().add(bubble);
        fadeIn(row);
        chatMessages.getChildren().add(row);
        scrollBottom();
    }

    private void showTyping() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label avatar = botAvatar();

        HBox dots = new HBox(5);
        dots.setAlignment(Pos.CENTER);
        dots.setStyle("-fx-background-color: #151530; -fx-background-radius: 4 18 18 18; -fx-padding: 12 18; -fx-border-color: #1e1e3a; -fx-border-radius: 4 18 18 18; -fx-border-width: 1;");

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(3.5, Color.web("#4a4a8a"));
            final int idx = i;
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(idx * 200),       kf -> dot.setFill(Color.web("#4a4a8a"))),
                new KeyFrame(Duration.millis(idx * 200 + 250), kf -> dot.setFill(Color.web("#c9a84c"))),
                new KeyFrame(Duration.millis(idx * 200 + 500), kf -> dot.setFill(Color.web("#4a4a8a")))
            );
            tl.setCycleCount(Timeline.INDEFINITE); tl.play();
            dots.getChildren().add(dot);
        }

        row.getChildren().addAll(avatar, dots);
        typingIndicator = row;
        chatMessages.getChildren().add(row);
        scrollBottom();
    }

    private void removeTyping() {
        if (typingIndicator != null) { chatMessages.getChildren().remove(typingIndicator); typingIndicator = null; }
    }

    private void addBotMessage(String md, BotResponse.Kind kind) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        Label avatar = botAvatar();

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(580);
        String bubbleStyle = switch (kind) {
            case SUCCESS -> "-fx-background-color: #0d2010; -fx-border-color: #1a4a22;";
            case ERROR   -> "-fx-background-color: #200d0d; -fx-border-color: #4a1a1a;";
            default      -> "-fx-background-color: #151530; -fx-border-color: #1e1e3a;";
        };
        bubble.setStyle(bubbleStyle + "-fx-background-radius: 4 18 18 18; -fx-padding: 14 18; -fx-border-radius: 4 18 18 18; -fx-border-width: 1;");

        TextFlow flow = parseMarkdown(md);
        bubble.getChildren().add(flow);

        row.getChildren().addAll(avatar, bubble);
        fadeIn(row);
        chatMessages.getChildren().add(row);
        scrollBottom();
    }

    private Label botAvatar() {
        Label a = new Label("🤖");
        a.setStyle("-fx-font-size: 20px; -fx-min-width: 30;");
        return a;
    }

    /** Parses **bold** and _italic_ markdown into a TextFlow. */
    private TextFlow parseMarkdown(String text) {
        TextFlow flow = new TextFlow();
        flow.setMaxWidth(560);
        String[] lines = text.split("\n", -1);

        for (int li = 0; li < lines.length; li++) {
            if (li > 0) flow.getChildren().add(new Text("\n"));
            String line = lines[li];
            int i = 0;
            while (i < line.length()) {
                // Bold **text**
                if (i + 1 < line.length() && line.charAt(i) == '*' && line.charAt(i+1) == '*') {
                    int end = line.indexOf("**", i+2);
                    if (end >= 0) {
                        Text t = new Text(line.substring(i+2, end));
                        t.setStyle("-fx-font-weight: bold; -fx-fill: #e8e4ff; -fx-font-size: 13px;");
                        flow.getChildren().add(t); i = end + 2; continue;
                    }
                }
                // Italic _text_
                if (line.charAt(i) == '_') {
                    int end = line.indexOf('_', i+1);
                    if (end >= 0) {
                        Text t = new Text(line.substring(i+1, end));
                        t.setStyle("-fx-font-style: italic; -fx-fill: #8888bb; -fx-font-size: 13px;");
                        flow.getChildren().add(t); i = end + 1; continue;
                    }
                }
                // Normal span until next marker
                int next = line.length();
                for (int k = i; k < line.length()-1; k++) {
                    if ((line.charAt(k) == '*' && line.charAt(k+1) == '*') || line.charAt(k) == '_') { next = k; break; }
                }
                Text t = new Text(line.substring(i, next));
                // Dim lines starting with ─
                if (line.startsWith("─")) t.setStyle("-fx-fill: #2a2a5a; -fx-font-size: 11px;");
                else t.setStyle("-fx-fill: #9090c0; -fx-font-size: 13px;");
                flow.getChildren().add(t); i = next;
            }
        }
        return flow;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  VOCABULARY PANEL
    // ═════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private VBox buildVocabPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0c0c18;");

        HBox header = sectionHeader("📚 Vocabulary", "Browse, search, add and manage your word collection", "#6a84ff");

        // ── Toolbar ──
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(14, 20, 10, 20));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #0f0f22; -fx-border-color: transparent transparent #18183a transparent; -fx-border-width: 0 0 1 0;");

        searchField = field("🔍  Search words...", 200);
        searchField.textProperty().addListener((obs, o, n) ->
            refreshVocabTable(n, catFilter.getValue(), diffFilter.getValue()));

        catFilter = combo("All Categories");
        catFilter.setOnAction(e -> refreshVocabTable(searchField.getText(), catFilter.getValue(), diffFilter.getValue()));

        diffFilter = combo("All Levels");
        diffFilter.setItems(FXCollections.observableArrayList("All Levels","BEGINNER","INTERMEDIATE","ADVANCED"));
        diffFilter.setValue("All Levels");
        diffFilter.setOnAction(e -> refreshVocabTable(searchField.getText(), catFilter.getValue(), diffFilter.getValue()));

        Button clearBtn = secondaryBtn("✕ Clear");
        clearBtn.setOnAction(e -> { searchField.clear(); catFilter.setValue(null); diffFilter.setValue("All Levels"); refreshVocabTable(null,null,null); });

        Region space = new Region(); HBox.setHgrow(space, Priority.ALWAYS);

        Button addBtn = goldBtn("＋ Add Word");
        addBtn.setOnAction(e -> showAddDialog());

        toolbar.getChildren().addAll(searchField, catFilter, diffFilter, clearBtn, space, addBtn);

        // ── Table ──
        wordList = FXCollections.observableArrayList();
        vocabTable = new TableView<>(wordList);
        vocabTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vocabTable.setStyle("-fx-background-color: transparent; -fx-table-cell-border-color: #18183a; -fx-border-color: transparent;");
        VBox.setVgrow(vocabTable, Priority.ALWAYS);

        vocabTable.setRowFactory(tv -> {
            TableRow<Word> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color: #141428;"); });
            row.setOnMouseExited(e  -> row.setStyle("-fx-background-color: transparent;"));
            return row;
        });

        TableColumn<Word,String> termCol = tc("Word", "term", 140);
        termCol.setCellFactory(c -> styledCell("#d0ccff", true));

        TableColumn<Word,String> defCol = tc("Definition", "definition", 260);
        defCol.setCellFactory(c -> styledCell("#8080aa", false));

        TableColumn<Word,String> catCol = tc("Category", "category", 100);
        catCol.setCellFactory(c -> styledCell("#6060aa", false));

        TableColumn<Word,String> diffCol = tc("Level", "difficulty", 110);
        diffCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(switch (item) { case "BEGINNER" -> "🟢 Beginner"; case "INTERMEDIATE" -> "🟡 Intermediate"; default -> "🔴 Advanced"; });
                setStyle("-fx-text-fill: #6060aa; -fx-font-size: 12px; -fx-background-color: transparent;");
            }
        });

        TableColumn<Word,Double> masteryCol = new TableColumn<>("Mastery");
        masteryCol.setMinWidth(100);
        masteryCol.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getMasteryScore()).asObject());
        masteryCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setGraphic(null); return; }
                HBox hb = new HBox(6);
                hb.setAlignment(Pos.CENTER_LEFT);
                ProgressBar bar = new ProgressBar(v / 100.0);
                bar.setPrefWidth(55); bar.setPrefHeight(6);
                String col = v >= 80 ? "#4aaa6a" : v >= 50 ? "#c9a84c" : "#aa4a4a";
                bar.setStyle("-fx-accent: " + col + "; -fx-background-color: #1a1a32; -fx-background-radius: 3; -fx-pref-height: 6;");
                Label pct = new Label(String.format("%.0f%%", v));
                pct.setStyle("-fx-font-size: 10px; -fx-text-fill: " + col + ";");
                hb.getChildren().addAll(bar, pct);
                setGraphic(hb); setStyle("-fx-background-color: transparent;");
            }
        });

        TableColumn<Word,Void> actCol = new TableColumn<>("Actions");
        actCol.setMinWidth(90);
        actCol.setCellFactory(c -> new TableCell<>() {
            private final Button edit   = tinyBtn("✏", "#4a7aff");
            private final Button delete = tinyBtn("🗑", "#ff4a4a");
            private final HBox box = new HBox(4, edit, delete);
            { box.setAlignment(Pos.CENTER);
              edit.setOnAction(e -> { Word w = getTableRow().getItem(); if (w != null) showEditDialog(w); });
              delete.setOnAction(e -> { Word w = getTableRow().getItem(); if (w != null) confirmDelete(w); }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
                setStyle("-fx-background-color: transparent;");
            }
        });

        vocabTable.getColumns().addAll(termCol, defCol, catCol, diffCol, masteryCol, actCol);

        Label footer = new Label("0 words");
        footer.setStyle("-fx-font-size: 10px; -fx-text-fill: #2e2e5a; -fx-padding: 4 20;");
        wordList.addListener((ListChangeListener<Word>) c -> footer.setText(wordList.size() + " words shown"));

        panel.getChildren().addAll(header, toolbar, vocabTable, footer);
        return panel;
    }

    private void refreshVocabTable(String search, String cat, String diff) {
        List<Word> words;
        if (search != null && !search.isBlank()) words = ctrl.searchWords(search);
        else if (cat != null && !cat.equals("All Categories")) words = ctrl.getByCategory(cat);
        else words = ctrl.getAllWords();
        if (diff != null && !diff.equals("All Levels"))
            words = words.stream().filter(w -> w.getDifficulty().equals(diff)).toList();
        wordList.setAll(words);

        List<String> cats = new ArrayList<>();
        cats.add("All Categories"); cats.addAll(ctrl.getCategories());
        String sel = catFilter.getValue();
        catFilter.setItems(FXCollections.observableArrayList(cats));
        catFilter.setValue(sel != null && cats.contains(sel) ? sel : "All Categories");
    }

    // ── Add / Edit dialogs ────────────────────────────────────────────────────

    private void showAddDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add New Word");
        dlg.setHeaderText(null);
        GridPane grid = wordForm(null);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setStyle("-fx-background-color: #12122a;");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialogBtn(dlg, ButtonType.OK, "#c9a84c", "#0a0a18");
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                TextField t = (TextField) grid.lookup("#f-term");
                TextField d = (TextField) grid.lookup("#f-def");
                TextField e = (TextField) grid.lookup("#f-ex");
                TextField c = (TextField) grid.lookup("#f-cat");
                ComboBox<?> df = (ComboBox<?>) grid.lookup("#f-diff");
                boolean ok = ctrl.addWord(t.getText(), d.getText(), e.getText(), c.getText().isBlank() ? "General" : c.getText(), df.getValue() != null ? df.getValue().toString() : "BEGINNER");
                if (ok) { refreshVocabTable(null,null,null); refreshSidebar(); alert("✅ Word Added", "\"" + t.getText() + "\" added successfully!"); }
                else alert("⚠️ Error", "Could not add word. It may already exist.");
            }
        });
    }

    private void showEditDialog(Word word) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Edit: " + word.getTerm());
        GridPane grid = wordForm(word);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setStyle("-fx-background-color: #12122a;");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialogBtn(dlg, ButtonType.OK, "#4a7aff", "white");
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                TextField d = (TextField) grid.lookup("#f-def");
                TextField e = (TextField) grid.lookup("#f-ex");
                TextField c = (TextField) grid.lookup("#f-cat");
                ComboBox<?> df = (ComboBox<?>) grid.lookup("#f-diff");
                word.setDefinition(d.getText()); word.setExampleSentence(e.getText());
                word.setCategory(c.getText().isBlank() ? "General" : c.getText());
                if (df.getValue() != null) word.setDifficulty(df.getValue().toString());
                ctrl.updateWord(word);
                refreshVocabTable(searchField.getText(), catFilter.getValue(), diffFilter.getValue());
            }
        });
    }

    private GridPane wordForm(Word existing) {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(20));
        g.setStyle("-fx-background-color: #12122a;");

        String fs = "-fx-background-color: #1a1a38; -fx-text-fill: #d0ccff; -fx-border-color: #252550; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-pref-width: 340; -fx-font-size: 13px;";
        String ls = "-fx-text-fill: #5555aa; -fx-font-size: 12px;";

        TextField termF = new TextField(existing != null ? existing.getTerm() : ""); termF.setId("f-term"); termF.setStyle(fs); if (existing != null) { termF.setEditable(false); termF.setStyle(fs + "-fx-opacity:0.5;"); }
        TextField defF  = new TextField(existing != null ? existing.getDefinition() : ""); defF.setId("f-def"); defF.setStyle(fs);
        TextField exF   = new TextField(existing != null && existing.getExampleSentence() != null ? existing.getExampleSentence() : ""); exF.setId("f-ex"); exF.setStyle(fs); exF.setPromptText("Optional");
        TextField catF  = new TextField(existing != null ? existing.getCategory() : "General"); catF.setId("f-cat"); catF.setStyle(fs);
        ComboBox<String> diffF = new ComboBox<>(FXCollections.observableArrayList("BEGINNER","INTERMEDIATE","ADVANCED")); diffF.setId("f-diff"); diffF.setValue(existing != null ? existing.getDifficulty() : "BEGINNER"); diffF.setStyle("-fx-background-color: #1a1a38; -fx-text-fill: #d0ccff;");

        int r = 0;
        for (String[] row : new String[][]{{"Word:",""},{"Definition:",""},{"Example:",""},{"Category:",""},{"Difficulty:",""}}) {
            Label l = new Label(row[0]); l.setStyle(ls);
            g.add(l, 0, r++);
        }
        g.add(termF, 1, 0); g.add(defF, 1, 1); g.add(exF, 1, 2); g.add(catF, 1, 3); g.add(diffF, 1, 4);
        return g;
    }

    private void confirmDelete(Word word) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + word.getTerm() + "\"? This cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Delete Word"); a.setHeaderText(null);
        a.showAndWait().ifPresent(bt -> { if (bt == ButtonType.OK) { ctrl.deleteWord(word.getId()); refreshVocabTable(searchField.getText(), catFilter.getValue(), diffFilter.getValue()); refreshSidebar(); } });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  STATS PANEL
    // ═════════════════════════════════════════════════════════════════════════

    private VBox buildStatsPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0c0c18;");
        panel.getChildren().add(sectionHeader("📊 Statistics", "Track your vocabulary journey and progress", "#6aaa6a"));

        ScrollPane scroll = new ScrollPane();
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0c0c18; -fx-border-color: transparent;");
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 24, 28, 24));
        content.setId("stats-content");
        scroll.setContent(content);
        panel.getChildren().add(scroll);
        return panel;
    }

    private void refreshStatsPanel() {
        ScrollPane scroll = (ScrollPane) statsPanel.getChildren().get(1);
        VBox content = (VBox) scroll.getContent();
        content.getChildren().clear();

        UserProgress p = ctrl.getProgress();
        int total = ctrl.getTotalWordCount(), mastered = ctrl.getMasteredWordCount();

        // ── Level banner ──
        HBox banner = new HBox(20);
        banner.setStyle("-fx-background-color: linear-gradient(135deg, #1a1430, #120e28); -fx-background-radius: 14; -fx-padding: 20 24; -fx-border-color: rgba(201,168,76,0.2); -fx-border-radius: 14; -fx-border-width: 1;");
        banner.setAlignment(Pos.CENTER_LEFT);

        // Mini XP arc for stats
        double progress2 = p.getLevelProgress();
        Arc bgA = new Arc(30,30,24,24,90,-360); bgA.setType(ArcType.OPEN); bgA.setStroke(Color.web("#1e1e3a")); bgA.setStrokeWidth(4); bgA.setFill(Color.TRANSPARENT); bgA.setStrokeLineCap(StrokeLineCap.ROUND);
        Arc fgA = new Arc(30,30,24,24,90,(float)(-360 * progress2)); fgA.setType(ArcType.OPEN); fgA.setStroke(Color.web("#c9a84c")); fgA.setStrokeWidth(4); fgA.setFill(Color.TRANSPARENT); fgA.setStrokeLineCap(StrokeLineCap.ROUND);
        Label lvlIcon = new Label(levelEmoji(p.getLevelIndex())); lvlIcon.setStyle("-fx-font-size: 18px;");
        StackPane arc = new StackPane(bgA, fgA, lvlIcon); arc.setPrefSize(60,60);

        VBox lvlText = new VBox(4);
        Label lvl = new Label(p.getLevel()); lvl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #c9a84c;");
        Label xp  = new Label(p.getTotalXP() + " XP" + (p.getXPForNextLevel() > 0 ? "  ·  " + p.getXPForNextLevel() + " to next level" : " · MAX LEVEL")); xp.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a4a7a;");
        ProgressBar xpPb = new ProgressBar(progress2); xpPb.setPrefWidth(220); xpPb.setStyle("-fx-accent: #c9a84c; -fx-background-color: #1a1a32; -fx-background-radius: 4; -fx-pref-height: 5;");
        lvlText.getChildren().addAll(lvl, xp, xpPb);

        banner.getChildren().addAll(arc, lvlText);
        content.getChildren().add(banner);

        // ── KPI grid ──
        GridPane kpi = new GridPane();
        kpi.setHgap(14); kpi.setVgap(14);
        kpi.add(kpiCard("📚", "Total Words",  String.valueOf(total),   "#6a84ff"), 0, 0);
        kpi.add(kpiCard("🏆", "Mastered",     String.valueOf(mastered), "#c9a84c"), 1, 0);
        kpi.add(kpiCard("✏️", "Quizzes",      String.valueOf(p.getTotalQuizzesTaken()), "#6aaa6a"), 0, 1);
        kpi.add(kpiCard("🎯", "Accuracy",     String.format("%.0f%%", p.getOverallAccuracy()), "#aa6aff"), 1, 1);
        kpi.add(kpiCard("🔥", "Streak",       p.getCurrentStreak() + "d", "#ff8844"), 0, 2);
        kpi.add(kpiCard("⭐", "Best Streak",  p.getLongestStreak() + "d", "#ffcc44"), 1, 2);
        kpi.getChildren().forEach(n -> { GridPane.setHgrow(n, Priority.ALWAYS); ((Region)n).setMaxWidth(Double.MAX_VALUE); });
        content.getChildren().add(kpi);

        // ── Category bars ──
        Map<String,Integer> byCat = ctrl.byCategory();
        if (!byCat.isEmpty()) {
            content.getChildren().add(barSection("📁 Words by Category", byCat, total, "#6a84ff"));
        }

        // ── Difficulty bars ──
        Map<String,Integer> byDiff = ctrl.byDifficulty();
        Map<String,String> diffColors = Map.of("BEGINNER","#6aaa6a","INTERMEDIATE","#c9a84c","ADVANCED","#ff6a6a");
        content.getChildren().add(barSection("📈 Words by Difficulty", byDiff, total, "#c9a84c"));

        // ── Quiz history ──
        List<QuizResult> recent = ctrl.getRecentQuizzes();
        if (!recent.isEmpty()) {
            VBox histCard = card("🕐 Recent Quizzes");
            recent.forEach(r -> {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                String scoreColor = r.getScorePercent() >= 80 ? "#6aaa6a" : r.getScorePercent() >= 60 ? "#c9a84c" : "#ff6a6a";
                Label dt  = new Label(r.getFormattedDate()); dt.setStyle("-fx-font-size: 11px; -fx-text-fill: #3a3a6a; -fx-min-width: 100;");
                Label tp  = new Label(r.getQuizType()); tp.setStyle("-fx-font-size: 11px; -fx-text-fill: #5555aa;"); HBox.setHgrow(tp, Priority.ALWAYS);
                Label sc  = new Label(String.format("%.0f%%", r.getScorePercent()) + "  (" + r.getCorrectAnswers() + "/" + r.getTotalQuestions() + ")");
                sc.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
                row.getChildren().addAll(dt, tp, sc);
                histCard.getChildren().add(row);
            });
            content.getChildren().add(histCard);
        }
    }

    private VBox kpiCard(String emoji, String label, String value, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #111126; -fx-background-radius: 12; -fx-padding: 18 14; -fx-border-color: #1a1a38; -fx-border-radius: 12; -fx-border-width: 1;");

        Label e = new Label(emoji); e.setStyle("-fx-font-size: 22px;");
        Label v = new Label(value); v.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label l = new Label(label); l.setStyle("-fx-font-size: 10px; -fx-text-fill: #3a3a6a;");
        card.getChildren().addAll(e, v, l);
        return card;
    }

    private VBox barSection(String title, Map<String,Integer> data, int total, String color) {
        VBox card = card(title);
        data.forEach((k, v) -> {
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
            Label l = new Label(k); l.setStyle("-fx-font-size: 12px; -fx-text-fill: #6060aa; -fx-min-width: 120;");
            ProgressBar bar = new ProgressBar(total > 0 ? (double)v/total : 0);
            bar.setPrefWidth(180); bar.setStyle("-fx-accent: " + color + "; -fx-background-color: #1a1a38; -fx-background-radius: 3; -fx-pref-height: 6;");
            HBox.setHgrow(bar, Priority.ALWAYS);
            Label cnt = new Label(v + " words"); cnt.setStyle("-fx-font-size: 11px; -fx-text-fill: #3a3a6a;");
            row.getChildren().addAll(l, bar, cnt);
            card.getChildren().add(row);
        });
        return card;
    }

    private VBox card(String title) {
        VBox c = new VBox(10);
        c.setStyle("-fx-background-color: #111126; -fx-background-radius: 12; -fx-padding: 18 20; -fx-border-color: #1a1a38; -fx-border-radius: 12; -fx-border-width: 1;");
        Label t = new Label(title); t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #6060aa;");
        c.getChildren().add(t);
        return c;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SIDEBAR REFRESH
    // ═════════════════════════════════════════════════════════════════════════

    private void refreshSidebar() {
        UserProgress p = ctrl.getProgress();
        double lvlProg = p.getLevelProgress();

        // XP ring
        xpArc.setLength(-360 * lvlProg);
        xpLabel.setText(p.getTotalXP() + " XP");
        levelLabel.setText(levelEmoji(p.getLevelIndex()));

        // Level text in ring
        VBox ringParent = (VBox) xpArc.getParent().getParent().getParent();
        ringParent.getChildren().stream()
            .filter(n -> n instanceof Label && "sidebar-level-text".equals(n.getId()))
            .findFirst()
            .ifPresent(n -> ((Label)n).setText(p.getLevel().substring(p.getLevel().indexOf(' ')+1)));

        // Quick stats
        wordCountBadge.setText(String.valueOf(ctrl.getTotalWordCount()));
        masteredBadge.setText(String.valueOf(ctrl.getMasteredWordCount()));
    }

    private String levelEmoji(int idx) {
        return switch (idx) { case 0 -> "🌱"; case 1 -> "📚"; case 2 -> "🎓"; case 3 -> "⭐"; default -> "🏆"; };
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  WIDGET HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private TextField field(String prompt, double width) {
        TextField f = new TextField();
        f.setPromptText(prompt); f.setPrefWidth(width);
        f.setStyle("-fx-background-color: #141428; -fx-text-fill: #9090c0; -fx-prompt-text-fill: #2e2e5e; -fx-border-color: #252550; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-font-size: 12px;");
        return f;
    }

    private ComboBox<String> combo(String prompt) {
        ComboBox<String> c = new ComboBox<>();
        c.setPromptText(prompt);
        c.setStyle("-fx-background-color: #141428; -fx-text-fill: #9090c0; -fx-border-color: #252550; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 12px;");
        return c;
    }

    private <T> TableColumn<T,String> tc(String title, String prop, int minW) {
        TableColumn<T,String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setMinWidth(minW);
        col.setStyle("-fx-alignment: CENTER_LEFT;");
        return col;
    }

    private <T> TableCell<T,String> styledCell(String color, boolean bold) {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-background-color: transparent;" + (bold ? "-fx-font-weight:bold;" : ""));
            }
        };
    }

    private Button goldBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: linear-gradient(to right, #c9a84c, #e6c56a); -fx-text-fill: #0a0a18; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 18; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: transparent;");
        b.setOnMouseEntered(e -> b.setOpacity(0.85));
        b.setOnMouseExited(e  -> b.setOpacity(1.0));
        return b;
    }

    private Button secondaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #4a4a7a; -fx-font-size: 12px; -fx-padding: 8 14; -fx-background-radius: 8; -fx-border-color: #252550; -fx-border-radius: 8; -fx-cursor: hand;");
        return b;
    }

    private Button tinyBtn(String icon, String color) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-padding: 2 6; -fx-cursor: hand; -fx-border-color: transparent;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace("transparent;-fx-text-fill", color + "22;-fx-text-fill")));
        b.setOnMouseExited(e  -> b.setStyle(b.getStyle().replace(color + "22;-fx-text-fill", "transparent;-fx-text-fill")));
        return b;
    }

    private void styleDialogBtn(Dialog<?> dlg, ButtonType type, String bg, String fg) {
        Button btn = (Button) dlg.getDialogPane().lookupButton(type);
        if (btn != null) btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 16;");
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void fadeIn(Node n) {
        n.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), n);
        ft.setToValue(1); ft.play();
    }

    private void scrollBottom() {
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }
}
