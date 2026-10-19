package util;

import controller.CreateGroupController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.UncheckedIOException;
/**
 * Utilitário partilhado que centraliza a funcionalidade indicada pelo nome da classe.
 *
 * @since 1.0
 */

public final class ModalDialogs {
    private ModalDialogs() {
    }

    public static void showCreateGroup(Window owner, Runnable afterClose) {
        try {
            FXMLLoader loader = new FXMLLoader(ModalDialogs.class.getResource("/view/fxml/modal-create-group.fxml"));
            Parent content = loader.load();
            Stage dialog = new Stage();
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
