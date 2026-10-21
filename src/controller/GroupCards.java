package controller;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Group;
import util.UiFormatting;

import java.util.function.Consumer;

/**
 * Fábrica utilitária para a criação dinâmica de componentes visuais (cartões) representativos de grupos de Xitique.
 *

 * @since 1.0
 */
final class GroupCards {

    /** Construtor privado para impedir a instanciação direta desta classe utilitária. */
    private GroupCards() {
    }

    /**
     * Cria e devolve um painel {@link VBox} formatado como cartão de grupo para ser inserido na interface JavaFX.
     *
     * @param group       Entidade do grupo a ser apresentado.
     * @param members     Total de membros participantes no grupo.
     * @param cycleNumber Número do ciclo ativo ou mais recente (0 se não houver).
     * @param paid        Quantidade de quotas pagas no ciclo atual.
     * @param onOpen      Callback invocado quando o utilizador clica sobre o cartão.
     * @return Componente {@link VBox} estilizado como cartão do grupo.
     */
    static VBox create(Group group, int members, int cycleNumber, int paid, Consumer<Group> onOpen) {
        VBox card = new VBox(10);
        card.getStyleClass().add("group-card");
        HBox top = new HBox();
        Label avatar = new Label(group.getGroupName().substring(0, 1).toUpperCase());
        avatar.getStyleClass().addAll("avatar", "avatar-lg");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        top.getChildren().addAll(avatar, spacer);

        Label name = new Label(group.getGroupName());
        name.getStyleClass().add("group-name");
        HBox metadata = new HBox(12,
                styledLabel(members + (members == 1 ? " membro" : " membros"), "group-meta"),
                styledLabel(cycleNumber == 0 ? "Sem ciclos" : "Ciclo " + cycleNumber, "group-meta"));
        Label progressText = new Label(cycleNumber == 0 ? "Ainda sem ciclo aberto"
                : paid + " de " + members + " contribuições");
        progressText.getStyleClass().add("progress-value");
        ProgressBar progress = new ProgressBar(members == 0 ? 0 : (double) paid / members);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("xt-progress");
        Label amount = new Label("Quota: " + UiFormatting.money(group.getAmountPerUser()));
        amount.getStyleClass().add("group-footer-text");
        card.getChildren().addAll(top, name, metadata, progressText, progress, amount);
        card.setOnMouseClicked(event -> onOpen.accept(group));
        card.setOnMouseEntered(event -> card.setStyle("-fx-cursor: hand;"));
        return card;
    }

    /**
     * Cria um rótulo com texto e classe de estilo CSS especificada.
     *
     * @param text  Texto do rótulo.
     * @param style Nome da classe CSS de estilo.
     * @return Rótulo {@link Label} instanciado.
     */
    private static Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }
}