package util;


import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import model.enums.Page;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

public final class Navigator {
    private static StackPane content;
    private static Consumer<Page> onChange = p -> {};

    public static void init(StackPane contentArea, Consumer<Page> listener) {
        content = contentArea; onChange = listener;
    }

    public static <T> T go(Page page) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/ui/" + page.file));
            Node view = loader.load();
            content.getChildren().setAll(view);
            onChange.accept(page);
            return loader.getController();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}

