package util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import model.User;
import model.enums.Page;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Utilitário responsável pela navegação e transição de ecrãs na aplicação.
 *
 * @since 1.0
 */
public final class Navigator {

    /** Painel contentor principal onde as vistas das páginas são apresentadas. */
    private static StackPane content;

    /** Callback invocado após a alteração bem-sucedida da página ativa. */
    private static Consumer<Page> onChange = p -> {};

    /** Identificador do grupo atualmente selecionado no contexto de navegação. */
    private static Long selectedGroupId;
    private static Page currentPage;

    /**
     * Inicializa o navegador com o painel de conteúdo e o ouvinte de navegação.
     *
     * @param contentArea O {@link StackPane} onde as vistas das páginas serão carregadas.
     * @param listener O ouvinte a ser notificado quando houver mudança de página.
     */
    public static void init(StackPane contentArea, Consumer<Page> listener) {
        content = contentArea; onChange = listener;
    }

    /**
     * Transita para a página especificada, atualizando o painel principal e notificando os ouvintes.
     *
     * @param <T> O tipo do controlador associado à nova vista.
     * @param page A página {@link Page} para a qual se pretende navegar.
     * @return O controlador da vista carregada.
     * @throws IllegalStateException Se o ficheiro FXML associado à página não for encontrado.
     * @throws UncheckedIOException Se ocorrer um erro durante o carregamento do FXML.
     */
    public static <T> T go(Page page) {
        try {
            currentPage = page;
            URL resource = Navigator.class.getResource("/view/fxml/" + page.file);
            if (resource == null) {
                throw new IllegalStateException("A tela não foi encontrada: " + page.file);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            content.getChildren().setAll(view);
            onChange.accept(page);
            return loader.getController();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static Page getCurrentPage() {
        return currentPage;
    }

    /**
     * Define o identificador do grupo selecionado no contexto atual.
     *
     * @param groupId O ID do grupo selecionado.
     */
    public static void setSelectedGroupId(Long groupId) {
        selectedGroupId = groupId;
    }

    /**
     * Obtém o identificador do grupo atualmente selecionado.
     *
     * @return O ID do grupo, ou {@code null} se nenhum estiver selecionado.
     */
    public static Long getSelectedGroupId() {
        return selectedGroupId;
    }
}