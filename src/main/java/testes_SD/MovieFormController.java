package testes_SD;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class MovieFormController {

    @FXML private TextField titleField;
    @FXML private TextField directorField;
    @FXML private TextField yearField;
    @FXML private TextField genreField;
    @FXML private TextArea synopsisArea;
    @FXML private Label errorLabel;

    private ClientModel model;
    private boolean saveClicked = false; // Para saber se salvou com sucesso

    public void initialize() {
        this.model = Context.getInstance().getModel();
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (!validarCampos()) return;

        try {
            Map<String, Object> movieData = new HashMap<>();
            movieData.put("titulo", titleField.getText());
            movieData.put("diretor", directorField.getText());
            movieData.put("ano", yearField.getText());
            movieData.put("sinopse", synopsisArea.getText());

            // Converte string de gêneros para List
            List<String> generos = Arrays.asList(genreField.getText().trim().split("\\s*,\\s*"));
            movieData.put("genero", generos);

            // Envia ao servidor
            JsonNode response = model.adminCreateMovie(movieData);

            if (response.has("status") && response.get("status").asText().equals("201")) {
                saveClicked = true;
                closeWindow();
            } else {
                String msg = response.has("mensagem") ? response.get("mensagem").asText() : "Erro desconhecido";
                errorLabel.setText("Erro: " + msg);
            }

        } catch (IOException e) {
            errorLabel.setText("Erro de conexão: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private boolean validarCampos() {
        if (titleField.getText().isEmpty() || directorField.getText().isEmpty() ||
                yearField.getText().isEmpty() || genreField.getText().isEmpty()) {
            errorLabel.setText("Por favor, preencha todos os campos obrigatórios.");
            return false;
        }
        return true;
    }
}