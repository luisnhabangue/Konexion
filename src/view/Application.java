package view;

import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {

        // Configurar o tema MaterialFX
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        // Carregar as fontes
        Font.loadFont(
                getClass().getResourceAsStream("/fonts/Montserrat-Bold.ttf"),
                14
        );

        Font.loadFont(
                getClass().getResourceAsStream("/fonts/Inter_18pt-Medium.ttf"),
                14
        );

        Font.loadFont(
                getClass().getResourceAsStream("/fonts/Inter_18pt-Regular.ttf"),
                14
        );

        Font.loadFont(
                getClass().getResourceAsStream("/fonts/Inter_18pt-SemiBold.ttf"),
                14
        );

        // Localizar o LoginView.fxml dentro de src/view/fxml
        URL fxmlUrl = Paths.get(
                "src/view/fxml/LoginView.fxml"
        ).toUri().toURL();

        System.out.println("FXML encontrado em: " + fxmlUrl);

        if (!Paths.get("src/view/fxml/LoginView.fxml").toFile().exists()) {
            throw new IOException(
                    "Não foi possível encontrar LoginView.fxml em src/view/fxml."
            );
        }

        // Carregar a interface de login
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load());

        // Localizar e carregar o CSS
        URL cssUrl = Paths.get(
                "src/view/css/style.css"
        ).toFile().exists()
                ? Paths.get("src/view/css/style.css").toUri().toURL()
                : null;

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Configurar a janela principal
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
