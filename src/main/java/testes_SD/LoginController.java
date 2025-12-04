package testes_SD;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class LoginController {

    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Label statusLabel;

    private ClientModel model;

    public void initialize() {
        this.model = Context.getInstance().getModel();
    }

    @FXML
    private void handleLoginButtonAction() {
        String ip = ipField.getText();
        int port = Integer.parseInt(portField.getText());
        String user = userField.getText();
        String pass = passField.getText();

        try {
            boolean conectou = model.connect(ip, port);

            if (!conectou) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Erro: Não foi possível conectar ao servidor.");
                return;
            }

            JsonNode response = model.login(user, pass);

            if (response != null && response.has("status") && response.get("status").asText().equals("200")) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Sucesso! Cargo: " + model.getUserRole());

                // TODO: Mudar para a próxima tela (Dashboard) aqui
                App.setRoot("dashboard");

            } else if (response != null && response.has("mensagem")) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText(response.get("mensagem").asText());
            } else {
                statusLabel.setText("Erro desconhecido do servidor.");
            }

        } catch (IOException e) {
            statusLabel.setText("Erro de conexão: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegisterButtonAction() {
        try {
            model.connect(ipField.getText(), Integer.parseInt(portField.getText()));
            JsonNode response = model.createUser(userField.getText(), passField.getText());
            if (response.has("status") && response.get("status").asText().equals("201")) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Usuário criado! Faça login.");
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText(response.get("mensagem").asText());
            }
        } catch (Exception e) {
            statusLabel.setText("Erro: " + e.getMessage());
        }

    }
}