package view;

import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.fxml.FXMLLoader;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;


import java.io.IOException;


public class Application extends javafx.application.Application {



    @Override
    public void start(Stage stage) throws IOException {
        UserAgentBuilder.builder().themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        Font.loadFont(getClass().getResourceAsStream("/fonts/Montserrat-Bold.ttf"),14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Inter_18pt-SemiBold.ttf"), 14);


        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/fxml/LoginView.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        if (getClass().getResource("/view/css/style.css") != null) {

            scene.getStylesheets().add(getClass().getResource("/view/css/style.css").toExternalForm());
        }
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }



}
