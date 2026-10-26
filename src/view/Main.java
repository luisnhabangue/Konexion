package view;

import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import java.io.IOException;
/**
 * Ponto de entrada da interface gráfica JavaFX da aplicação.
 *
 * @since 1.0
 */


public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        stage.initStyle(StageStyle.UNDECORATED);

        UserAgentBuilder.builder().themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();


        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-SemiBold.ttf"), 14);


        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/fxml/LoginView.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        if (getClass().getResource("/view/css/style.css") != null) {

            scene.getStylesheets().add(getClass().getResource("/view/css/style.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.setWidth(1366);
        stage.setHeight(760);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }


}
