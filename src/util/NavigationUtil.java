package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utilitário para transição de ecrãs na interface gráfica desenvolvida em JavaFX.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class NavigationUtil {

    private NavigationUtil() {
    }

    /**
     * Carrega uma nova vista FXML e altera a cena da janela (Stage) fornecida.
     *
     * @param stage Janela atual do JavaFX
     * @param fxml Caminho relativo do ficheiro .fxml
     * @throws IOException Se o ficheiro FXML não for encontrado ou não puder ser carregado
     */
    public static void goTo(Stage stage, String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                NavigationUtil.class.getResource("view/fxml" + fxml)
        );

        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }
}
