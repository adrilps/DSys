package testes_SD;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.util.Optional;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ChoiceDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TabPane mainTabPane;
    @FXML private Tab tabUsuarios;

    @FXML private ListView<String> moviesListView;
    @FXML private ListView<String> usersListView;
    @FXML private TextArea logArea;

    @FXML private TextField movieIdField;
    @FXML private TextField userIdField;
    @FXML private PasswordField newPasswordField;

    @FXML private Button btnCriarFilme;
    @FXML private Button btnExcluirFilme;

    @FXML private ListView<String> reviewsListView;
    @FXML private Label reviewsHeaderLabel;
    @FXML private Button btnAvaliarFilme;

    private String selectedMovieId = null;

    private ClientModel model;

    public void initialize() {
        this.model = Context.getInstance().getModel();
        String role = model.getUserRole();

        welcomeLabel.setText("Bem-vindo! (" + role + ")");

        // Configuração de Permissões
        if (!"admin".equals(role)) {
            // Remove a aba de usuários se não for admin
            mainTabPane.getTabs().remove(tabUsuarios);

            // Desabilita botões de admin na aba de filmes
            btnCriarFilme.setDisable(true);
            btnCriarFilme.setVisible(false);
            btnExcluirFilme.setDisable(true);
            btnExcluirFilme.setVisible(false);
        } else {
            btnCriarFilme.setDisable(false);
            btnExcluirFilme.setDisable(false);
        }

        log("Login efetuado com sucesso.");
        handleListarFilmes();
    }

    // --- MÉTODOS DE FILMES ---

    @FXML
    private void handleListarFilmes() {
        try {
            // 1. Busca os dados do servidor (Isso pode demorar, bloqueando a UI se não cuidar)
            JsonNode response = model.listarFilmes();

            // 2. Usa Platform.runLater para garantir que a atualização visual seja segura
            javafx.application.Platform.runLater(() -> {

                // Limpa a lista visual ANTES de adicionar novos itens
                moviesListView.getItems().clear();

                if (response.has("filmes")) {
                    JsonNode filmesArray = response.get("filmes");

                    if (filmesArray.isEmpty()) {
                        moviesListView.getItems().add("Nenhum filme encontrado.");
                    } else {
                        for (JsonNode filme : filmesArray) {
                            // Formato bonito para a lista
                            String item = String.format("[%s] %s (%s) - ⭐ %s (%s votos)",
                                    filme.get("id").asText(),
                                    filme.get("titulo").asText(),
                                    filme.get("ano").asText(),
                                    filme.get("nota").asText(),
                                    filme.get("qtd_avaliacoes").asText());

                            moviesListView.getItems().add(item);
                        }
                    }
                    log("Lista de filmes atualizada com sucesso.");
                } else {
                    logError(response.path("mensagem").asText("Erro ao listar filmes."));
                }
            });

        } catch (IOException e) {
            logError("Erro de conexão ao atualizar lista: " + e.getMessage());
        }
    }

    @FXML
    private void handleCriarFilme() {
        try {
            // 1. Carregar o FXML do formulário
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/movie_form.fxml"));
            Parent page = loader.load();

            // 2. Criar o Palco (Stage) da janela pop-up
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Cadastrar Novo Filme");
            dialogStage.initModality(Modality.WINDOW_MODAL); // Bloqueia a janela de trás
            dialogStage.initOwner(btnCriarFilme.getScene().getWindow());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // 3. Mostrar e esperar fechar
            dialogStage.showAndWait();

            // 4. Verificar se salvou e atualizar a lista
            MovieFormController controller = loader.getController();
            if (controller.isSaveClicked()) {
                log("Filme criado com sucesso!");
                handleListarFilmes(); // Atualiza a lista principal
            }

        } catch (IOException e) {
            logError("Erro ao abrir formulário: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- MÉTODOS DE USUÁRIOS ---

    @FXML
    private void handleListarUsuarios() {
        try {
            JsonNode response = model.adminListUsers();
            usersListView.getItems().clear();

            if (response.has("usuarios")) {
                for (JsonNode user : response.get("usuarios")) {
                    usersListView.getItems().add("[" + user.get("id").asText() + "] " + user.get("nome").asText());
                }
                log("Usuários listados.");
            } else {
                logError(response.path("mensagem").asText("Erro ao listar."));
            }
        } catch (IOException e) {
            logError(e.getMessage());
        }
    }

    @FXML
    private void handleExcluirUsuario() {
        String id = userIdField.getText();
        if (id.isEmpty()) return;

        try {
            JsonNode response = model.adminExcluirUsuario(id);
            log(response.path("mensagem").asText());
            handleListarUsuarios();
        } catch (IOException e) {
            logError(e.getMessage());
        }
    }

    // --- MÉTODOS DE PERFIL ---

    @FXML
    private void handleUpdatePassword() {
        String newPass = newPasswordField.getText();
        if (newPass.isEmpty()) return;

        try {
            JsonNode response = model.updatePassword(newPass);
            log(response.path("mensagem").asText());
            newPasswordField.clear();
        } catch (IOException e) {
            logError(e.getMessage());
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        model.logout();
        App.setRoot("login"); // Volta para a tela de login
    }

    @FXML
    private void handleDeleteAccount() throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Excluir Conta");
        alert.setHeaderText("Tem certeza?");
        alert.setContentText("Essa ação não pode ser desfeita.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            model.deleteAccount();
            App.setRoot("login");
        }
    }

    @FXML
    private void handleFilmeSelecionado() {
        String selectedItem = moviesListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        // Usa o novo helper para ser mais seguro
        String extractedId = extrairIdDoItem(selectedItem);

        if (extractedId != null) {
            this.selectedMovieId = extractedId;

            reviewsHeaderLabel.setText("📝 Reviews do filme ID: " + selectedMovieId);
            btnAvaliarFilme.setDisable(false);

            // Libera botão de excluir filme apenas se for admin
            if ("admin".equals(model.getUserRole())) {
                btnExcluirFilme.setDisable(false);
            }

            carregarReviews(this.selectedMovieId);
        }
    }

    private void carregarReviews(String idFilme) {
        try {
            reviewsListView.getItems().clear();

            // 1. Busca os dados no servidor
            JsonNode response = model.getMovieDetails(idFilme);

            // 2. Verifica sucesso
            if (response.has("status") && response.get("status").asText().equals("200")) {

                // 3. Processa a lista de reviews
                if (response.has("reviews")) {
                    JsonNode reviewsArray = response.get("reviews");

                    if (reviewsArray.isEmpty()) {
                        reviewsListView.getItems().add("Nenhuma avaliação para este filme ainda.");
                    } else {
                        for (JsonNode review : reviewsArray) {
                            // Formato: "[ID] Nota/5 - Título (por Usuário)"
                            // Ex: "[10] 5/5 - Adorei! (por Joao)"
                            String item = String.format("[%s] %s/5 - %s (por %s)\n      %s",
                                    review.get("id").asText(),
                                    review.get("nota").asText(),
                                    review.get("titulo").asText(),
                                    review.get("nome_usuario").asText(),
                                    review.get("descricao").asText()
                            );
                            reviewsListView.getItems().add(item);
                        }
                    }
                }
            } else {
                logError(response.path("mensagem").asText("Erro ao carregar detalhes."));
            }

        } catch (IOException e) {
            logError("Erro de conexão ao buscar reviews: " + e.getMessage());
        }
    }

    @FXML
    private void handleCriarReview() {
        if (selectedMovieId == null) return;

        try {
            // 1. Pedir Nota
            List<String> notas = new ArrayList<>();
            notas.add("5"); notas.add("4"); notas.add("3"); notas.add("2"); notas.add("1");
            ChoiceDialog<String> dialogNota = new ChoiceDialog<>("5", notas);
            dialogNota.setTitle("Avaliar Filme");
            dialogNota.setHeaderText("Dê uma nota para o filme (1-5)");
            dialogNota.setContentText("Nota:");

            Optional<String> resultNota = dialogNota.showAndWait();
            if (resultNota.isEmpty()) return;

            // 2. Pedir Título
            TextInputDialog dialogTitulo = new TextInputDialog();
            dialogTitulo.setTitle("Avaliar Filme");
            dialogTitulo.setHeaderText("Título da sua avaliação");
            dialogTitulo.setContentText("Título:");
            Optional<String> resultTitulo = dialogTitulo.showAndWait();
            if (resultTitulo.isEmpty()) return;

            // 3. Pedir Descrição
            TextInputDialog dialogDesc = new TextInputDialog();
            dialogDesc.setTitle("Avaliar Filme");
            dialogDesc.setHeaderText("Escreva sua opinião detalhada");
            dialogDesc.setContentText("Descrição:");
            Optional<String> resultDesc = dialogDesc.showAndWait();
            if (resultDesc.isEmpty()) return;

            // 4. Enviar para o Model
            java.util.Map<String, String> reviewData = new java.util.HashMap<>();
            reviewData.put("nota", resultNota.get());
            reviewData.put("titulo", resultTitulo.get());
            reviewData.put("descricao", resultDesc.get());

            JsonNode response = model.createReview(selectedMovieId, reviewData);

            if (response.has("status") && response.get("status").asText().equals("201")) {
                log("Review criada com sucesso!");
                handleListarFilmes(); // Atualiza a lista para mostrar a nova média
            } else {
                logError(response.path("mensagem").asText("Erro ao criar review."));
            }

        } catch (IOException e) {
            logError(e.getMessage());
        }
    }

    @FXML
    private void handleExcluirFilme() {
        // Usa o ID selecionado na lista, em vez de digitar no TextField
        if (selectedMovieId == null) {
            logError("Selecione um filme na lista para excluir.");
            return;
        }

        try {
            JsonNode response = model.adminExcluirFilme(selectedMovieId);
            log(response.path("mensagem").asText());
            handleListarFilmes(); // Atualiza a lista
            selectedMovieId = null; // Reseta seleção
            btnExcluirFilme.setDisable(true);
        } catch (IOException e) {
            logError(e.getMessage());
        }
    }

    @FXML
    private void handleExcluirReview() {
        // 1. Pega o item selecionado na lista de reviews (parte de baixo)
        String selectedItem = reviewsListView.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            logError("Selecione uma review na lista para excluir.");
            return;
        }

        // 2. Extrai o ID da string "[10] 5/5 - Título..."
        String idReview = extrairIdDoItem(selectedItem);

        if (idReview == null) {
            logError("Não foi possível identificar o ID da review.");
            return;
        }

        // 3. Chama o Model
        try {
            JsonNode response = model.deleteReview(idReview);

            if (response.has("status") && response.get("status").asText().equals("200")) {
                log("Review excluída com sucesso.");

                // 4. ATUALIZAÇÃO DUPLA (Importante!)

                // Recarrega a lista de reviews para remover o item excluído
                if (this.selectedMovieId != null) {
                    carregarReviews(this.selectedMovieId);
                }

                // Recarrega a lista de filmes para atualizar a Nota Média (que mudou)
                handleListarFilmes();

            } else {
                logError(response.path("mensagem").asText("Erro ao excluir review."));
            }

        } catch (IOException e) {
            logError("Erro de conexão: " + e.getMessage());
        }
    }

    private String extrairIdDoItem(String itemTexto) {
        try {
            int start = itemTexto.indexOf('[');
            int end = itemTexto.indexOf(']');
            if (start != -1 && end > start) {
                return itemTexto.substring(start + 1, end);
            }
        } catch (Exception e) {
            // Ignora erro de parse
        }
        return null;
    }

    // --- HELPERS ---

    private void log(String msg) {
        logArea.appendText("INFO: " + msg + "\n");
    }

    private void logError(String msg) {
        logArea.appendText("ERRO: " + msg + "\n");
    }
}