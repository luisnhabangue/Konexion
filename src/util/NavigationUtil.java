package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationUtil {

    public static void goTo(Stage stage, String fxml) throws IOException {

        FXMLLoader loader =
                new FXMLLoader(
                        NavigationUtil.class.getResource("view/fxml" + fxml)
                );

        Parent root = loader.load();

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }
}
