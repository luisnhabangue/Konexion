package controller;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Group;
import util.UiFormatting;

import java.util.function.Consumer;

final class GroupCards {
    private GroupCards() {
    }

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

    private static Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }
}
