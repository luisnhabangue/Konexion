package util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.util.Optional;
/**
 * Utilitário partilhado que centraliza a funcionalidade indicada pelo nome da classe.
 *
 * @since 1.0
 */

public final class StyledDialogs {
    private StyledDialogs() {
    }

    public static <R> Optional<R> showAndWait(Dialog<R> dialog) {
        var stylesheet = StyledDialogs.class.getResource("/view/fxml/app.css");
        if (stylesheet == null) {
            throw new IllegalStateException("A folha de estilos dos diálogos não foi encontrada.");
        }
        var pane = dialog.getDialogPane();
        pane.getStylesheets().add(stylesheet.toExternalForm());
        pane.getStyleClass().add("app-dialog");
        pane.setMinWidth(360);
        pane.setMaxWidth(620);
        dialog.setResizable(true);
        return dialog.showAndWait();
    }
}
