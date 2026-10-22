package util;

import controller.CreateGroupController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Utilitário responsável pela exibição de janelas modais na aplicação JavaFX.
 *
 * @since 1.0
 */
public final class ModalDialogs {

    /**
     * Construtor privado para impedir a instanciação da classe utilitária.
     */
    private ModalDialogs() {
    }

    /**
     * Exibe a janela modal para a criação de um novo grupo e executa uma ação ao fechar.
     *
     * @param owner A janela proprietária (pai) sobre a qual a modal será exibida, podendo ser {@code null}.
     * @param afterClose Ação ({@link Runnable}) a ser executada após o fecho da janela modal.
     * @throws UncheckedIOException Se ocorrer uma falha de leitura ao carregar o ficheiro FXML.
     */
    public static void showCreateGroup(Window owner, Runnable afterClose) {
        try {
            FXMLLoader loader = new FXMLLoader(ModalDialogs.class.getResource("/view/fxml/modal-create-group.fxml"));
            Parent content = loader.load();
            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.setTitle("Criar grupo");
            dialog.setScene(new Scene(content));
            ((CreateGroupController) loader.getController()).setDialog(dialog);
            dialog.showAndWait();
            afterClose.run();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível abrir o formulário de criação do grupo.", e);
        }
    }
}