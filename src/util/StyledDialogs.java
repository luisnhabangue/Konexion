package util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Utilitário para apresentação de caixas de diálogo estilizadas com a folha de estilos global da aplicação.
 *
 * @since 1.0
 */
public final class StyledDialogs {

    /**
     * Construtor privado para impedir a instanciação da classe utilitária.
     */
    private StyledDialogs() {
    }

    /**
     * Aplica o tema CSS centralizado ao diálogo e exibe-o de forma modal aguardando a resposta.
     *
     * @param <R> O tipo do resultado produzido pelo diálogo.
     * @param dialog A instância de {@link Dialog} a ser exibida.
     * @return Um {@link Optional} contendo a resposta ou seleção do utilizador.
     * @throws IllegalStateException Se o ficheiro de estilos {@code app.css} não for encontrado.
     */
    public static <R> Optional<R> showAndWait(Dialog<R> dialog) {
        dialog.initStyle(StageStyle.UNDECORATED);
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