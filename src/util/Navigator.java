package util;


import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import model.User;
import model.enums.Page;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Consumer;

public final class Navigator {
    private static StackPane content;
    private static Consumer<Page> onChange = p -> {};
    private static Long selectedGroupId;

    public static void init(StackPane contentArea, Consumer<Page> listener) {
        content = contentArea; onChange = listener;
    }

    public static <T> T go(Page page) {
        try {
            URL resource = Navigator.class.getResource("/view/fxml/" + page.file);
            if (resource == null) {
                throw new IllegalStateException("A tela não foi encontrada: " + page.file);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            content.getChildren().setAll(view);
            onChange.accept(page);
            return loader.getController();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static void setSelectedGroupId(Long groupId) {
        selectedGroupId = groupId;
    }

    public static Long getSelectedGroupId() {
        return selectedGroupId;
    }
}
