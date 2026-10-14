package app.ui; // change to match your project's package

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * All UI motion lives here so controllers stay free of styling code.
 * Usage: call UiAnimations.install(scene) once after creating the Scene.
 */
public final class UiAnimations {

    private static final Interpolator EASE = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

    private UiAnimations() {}

    public static void install(Scene scene) {
        Parent root = scene.getRoot();
        root.setOpacity(0); // avoid a flash before the intro plays
        installButtonEffects(scene);

        Platform.runLater(() -> {
            // Window intro: fade in, then cards rise in one after another.
            Timeline fade = new Timeline(new KeyFrame(Duration.millis(350),
                    new KeyValue(root.opacityProperty(), 1, EASE)));
            fade.play();
            int i = 0;
            for (Node n : root.lookupAll(".panel")) {
                play(n, 0, 18, 380, 120 + i * 90);
                i++;
            }

            for (Node n : root.lookupAll(".tab-pane")) {
                if (n instanceof TabPane) hookTabs((TabPane) n);
            }
            for (Node n : root.lookupAll(".table-view")) {
                if (n instanceof TableView) hookTable((TableView<?>) n);
            }
            for (Node n : root.lookupAll(".message")) {
                if (n instanceof Labeled) hookMessage((Labeled) n);
            }
        });
    }

    // ---------- Buttons: grow on hover, squash on press ----------
    private static void installButtonEffects(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, e -> {
            if (e.getTarget() instanceof Button && !((Button) e.getTarget()).isDisabled()) {
                scale((Button) e.getTarget(), 1.04, 120);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, e -> {
            if (e.getTarget() instanceof Button) scale((Button) e.getTarget(), 1.0, 140);
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Button b = findButton(e.getTarget());
            if (b != null && !b.isDisabled()) scale(b, 0.96, 80);
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            Button b = findButton(e.getTarget());
            if (b != null) scale(b, b.isHover() ? 1.04 : 1.0, 120);
        });
    }

    private static Button findButton(Object target) {
        Node n = target instanceof Node ? (Node) target : null;
        while (n != null && !(n instanceof Button)) n = n.getParent();
        return (Button) n;
    }

    private static void scale(Node n, double to, int ms) {
        Object old = n.getProperties().get("uiScaleAnim");
        if (old instanceof Timeline) ((Timeline) old).stop();
        Timeline t = new Timeline(new KeyFrame(Duration.millis(ms),
                new KeyValue(n.scaleXProperty(), to, EASE),
                new KeyValue(n.scaleYProperty(), to, EASE)));
        n.getProperties().put("uiScaleAnim", t);
        t.play();
    }

    // ---------- Tabs: content fades and slides up on switch ----------
    private static void hookTabs(TabPane tp) {
        tp.getSelectionModel().selectedItemProperty().addListener((o, a, tab) -> {
            if (tab != null && tab.getContent() != null) play(tab.getContent(), 0, 12, 240, 0);
        });
    }

    // ---------- Tables: rows slide in one by one when data changes ----------
    private static <T> void hookTable(TableView<T> tv) {
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // no sideways scrolling
        ListChangeListener<T> listener = c -> staggerRows(tv);
        if (tv.getItems() != null) tv.getItems().addListener(listener);
        tv.itemsProperty().addListener((o, a, b) -> {
            if (a != null) a.removeListener(listener);
            if (b != null) b.addListener(listener);
            staggerRows(tv);
        });
    }

    private static void staggerRows(TableView<?> tv) {
        Platform.runLater(() -> {
            int i = 0;
            for (Node n : tv.lookupAll(".table-row-cell")) {
                if (n instanceof TableRow && !((TableRow<?>) n).isEmpty()) {
                    play(n, -14, 0, 260, Math.min(i, 12) * 40);
                    i++;
                }
            }
        });
    }

    // ---------- Messages: fade in, shake when it is an error ----------
    private static void hookMessage(Labeled label) {
        label.textProperty().addListener((o, a, b) -> Platform.runLater(() -> {
            if (b == null || b.isEmpty()) return;
            play(label, 0, -6, 220, 0);
            if (label.getStyleClass().contains("error")) shake(label);
        }));
    }

    private static void shake(Node n) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(n.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60), new KeyValue(n.translateXProperty(), -8)),
                new KeyFrame(Duration.millis(120), new KeyValue(n.translateXProperty(), 8)),
                new KeyFrame(Duration.millis(180), new KeyValue(n.translateXProperty(), -5)),
                new KeyFrame(Duration.millis(240), new KeyValue(n.translateXProperty(), 3)),
                new KeyFrame(Duration.millis(300), new KeyValue(n.translateXProperty(), 0)));
        t.play();
    }

    // ---------- Shared helper: fade in while sliding from an offset ----------
    private static void play(Node n, double dx, double dy, int ms, int delayMs) {
        n.setOpacity(0);
        List<KeyValue> end = new ArrayList<>();
        end.add(new KeyValue(n.opacityProperty(), 1, EASE));
        if (dx != 0) {
            n.setTranslateX(dx);
            end.add(new KeyValue(n.translateXProperty(), 0, EASE));
        }
        if (dy != 0) {
            n.setTranslateY(dy);
            end.add(new KeyValue(n.translateYProperty(), 0, EASE));
        }
        Timeline t = new Timeline(new KeyFrame(Duration.millis(ms), end.toArray(new KeyValue[0])));
        t.setDelay(Duration.millis(delayMs));
        t.play();
    }
}
