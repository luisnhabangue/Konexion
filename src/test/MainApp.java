package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * Componente de apoio à aplicação, responsável pela funcionalidade indicada pelo nome da classe.
 *
 * @since 1.0
 */

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/view/fxml/registerView.fxml")
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("Ixitique");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}