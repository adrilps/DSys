package testes_SD;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;

public class ClientController {

    private ClientModel model;
    private ClientView view;

    public ClientController() {
        this.model = new ClientModel();
        this.view = new ClientView();
    }

    public static void main(String[] args) {
        ClientController controller = new ClientController();
        controller.start();
    }

    public void start() {
        try {
            String ip = view.promptForIP();
            int port = view.promptForPort();
            if (!model.connect(ip, port)) {
                view.displayError("Falha ao conectar.");
                return;
            }
            view.displayMessage("Conectado!");

            boolean running = true;
            while (running) {
                String role = model.getUserRole();
                String choice = view.displayMainMenu(role);

                if (role == null) {
                    switch (choice) {
                        case "1": handleCreateUser(); break;
                        case "2": handleLogin(); break;
                        case "0": running = false; break;
                        default: view.displayError("Opção inválida.");
                    }
                } else if ("admin".equals(role)) {
                    switch (choice) {
                        case "1": handleAccountMenuLoop(); break;
                        case "2": handleUserMenuLoop(); break;
                        case "3": handleMovieMenuLoop(); break;
                        case "9": if (handleLogout()) running = false; break;
                        case "0": running = false; break;
                        default: view.displayError("Opção inválida.");
                    }
                } else {
                    switch (choice) {
                        case "1": handleViewProfile(); break;
                        case "2": handleUpdatePassword(); break;
                        case "3": handleListarFilmes(); break;
                        case "4": handleCriarReview(); break; // Ajuste os números conforme necessário
                        case "5": handleListarMinhasReviews(); break;
                        case "7": handleExcluirReview(); break;
                        case "6": handleEditarReview(); break;
                        case "8": if (handleDeleteAccount()) running = false; break;
                        case "9": if (handleLogout()) running = false; break;
                        case "0": running = false; break;
                        default: view.displayError("Opção inválida.");
                    }
                }
            }

        } catch (IOException e) {
            view.displayError("Erro fatal de comunicação: " + e.getMessage());
        } finally {
            view.displayMessage("Desconectando...");
            model.disconnect();
        }
    }
    
    private void handleAccountMenuLoop() throws IOException {
        boolean inSubMenu = true;
        while (inSubMenu) {
            String choice = view.displayAccountMenu();
            switch (choice) {
                case "1": handleViewProfile(); break;
                case "2": handleUpdatePassword(); break;
                case "0": inSubMenu = false; break;
                default: view.displayError("Opção inválida.");
            }
        }
    }
    
    private void handleUserMenuLoop() throws IOException {
        boolean inSubMenu = true;
        while (inSubMenu) {
            String choice = view.displayUserMenu();
            switch (choice) {
                case "1": handleAdminListUsers(); break;
                case "2": handleAdminEditUser(); break;
                case "3": handleAdminExcluirUsuario(); break;
                case "0": inSubMenu = false; break;
                default: view.displayError("Opção inválida.");
            }
        }
    }
    
    private void handleMovieMenuLoop() throws IOException {
        boolean inSubMenu = true;
        while (inSubMenu) {
            String choice = view.displayMovieMenu();
            switch (choice) {
                case "1": handleListarFilmes(); break;
                case "2": handleAdminCreateMovie(); break;
                case "3": handleAdminEditMovie(); break;
                case "4": handleAdminExcluirFilme(); break;
                case "0": inSubMenu = false; break;
                default: view.displayError("Opção inválida.");
            }
        }
    }

    private void handleCreateUser() throws IOException {
        Map<String, String> credentials = view.promptForNewUser();
        JsonNode response = model.createUser(credentials.get("user"), credentials.get("pass"));
        view.displayServerResponse(response);
    }

    private void handleLogin() throws IOException {
        Map<String, String> credentials = view.promptForLogin();
        JsonNode response = model.login(credentials.get("user"), credentials.get("pass"));

        if (model.getUserRole() != null) {
            view.displayMessage("Login bem-sucedido! Cargo: " + model.getUserRole());
        } else if (response.has("mensagem")) {
            view.displayError("Falha no login: " + response.get("mensagem").asText());
        } else {
            view.displayError("Falha no login: resposta inválida do servidor.");
        }
    }

    private void handleViewProfile() throws IOException {
        JsonNode response = model.fetchProfile();
        view.displayServerResponse(response);
    }

    private void handleUpdatePassword() throws IOException {
        String newPassword = view.promptForNewPassword();
        JsonNode response = model.updatePassword(newPassword);
        view.displayServerResponse(response);
    }

    private boolean handleLogout() throws IOException {
        JsonNode response = model.logout();
        view.displayServerResponse(response);
        return response.has("status") && response.get("status").asText().equals("200");
    }

    private boolean handleDeleteAccount() throws IOException {
        if (!view.promptForConfirmation("Tem certeza que deseja excluir sua conta? (s/n)")) {
            view.displayMessage("Operação cancelada.");
            return false;
        }
        JsonNode response = model.deleteAccount();
        view.displayServerResponse(response);
        return response.has("status") && response.get("status").asText().equals("200");
    }

    private void handleListarFilmes() throws IOException {
        JsonNode response = model.listarFilmes();
        view.displayServerResponse(response);
    }

    private void handleAdminListUsers() throws IOException {
        JsonNode response = model.adminListUsers();
        view.displayServerResponse(response);
    }

    private void handleAdminEditUser() throws IOException {
        Map<String, String> data = view.promptForAdminEditUser();
        JsonNode response = model.adminEditUser(data.get("id"), data.get("newPassword"));
        view.displayServerResponse(response);
    }

    private void handleAdminCreateMovie() throws IOException {
        Map<String, Object> movieData = view.promptForNewMovieDetails();
        JsonNode response = model.adminCreateMovie(movieData);
        view.displayServerResponse(response);
    }
    
    private void handleAdminEditMovie() throws IOException {
        Map<String, Object> movieData = view.promptForMovieEditDetails();
        JsonNode response = model.adminEditMovie(movieData);
        view.displayServerResponse(response);
    }

    private void handleAdminExcluirFilme() throws IOException {
        String idFilme = view.promptForId("Filme", "Excluir");
        
        if (!view.promptForConfirmation("Tem certeza que deseja excluir o filme ID " + idFilme + "? (s/n)")) {
            view.displayMessage("Operação cancelada.");
            return;
        }
        
        JsonNode response = model.adminExcluirFilme(idFilme);
        view.displayServerResponse(response);
    }
    
    private void handleAdminExcluirUsuario() throws IOException {
        String idUsuario = view.promptForId("Usuário", "Excluir");
        
        if (!view.promptForConfirmation("Tem certeza que deseja excluir o usuário ID " + idUsuario + "? (s/n)")) {
            view.displayMessage("Operação cancelada.");
            return;
        }
        
        JsonNode response = model.adminExcluirUsuario(idUsuario);
        view.displayServerResponse(response);
    }

    private void handleCriarReview() throws IOException {
        String idFilme = view.promptForId("Filme", "Avaliar");
        Map<String, String> reviewData = view.promptForReview(idFilme);
        JsonNode response = model.createReview(idFilme, reviewData);
        view.displayServerResponse(response);
    }

    private void handleListarMinhasReviews() throws IOException {
        JsonNode response = model.listarMinhasReviews();
        view.displayServerResponse(response);
    }

    private void handleEditarReview() throws IOException {
        Map<String, String> reviewData = view.promptForReviewEdit();
        JsonNode response = model.editReview(reviewData);
        view.displayServerResponse(response);}

    private void handleExcluirReview() throws IOException {
        // 1. Pede o ID
        String idReview = view.promptForId("Review", "Excluir");

        // 2. Confirmação
        if (!view.promptForConfirmation("Tem certeza que deseja excluir esta review? (s/n)")) {
            view.displayMessage("Operação cancelada.");
            return;
        }

        // 3. Envia requisição
        JsonNode response = model.deleteReview(idReview);
        view.displayServerResponse(response);
    }
}