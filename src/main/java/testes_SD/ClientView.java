package testes_SD;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;

public class ClientView {

    private final BufferedReader consoleReader;

    public ClientView() {
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void displayMessage(String message) {
        System.out.println(message);
    }

    public void displayError(String message) {
        System.err.println("!! " + message);
    }

    public void displayServerResponse(JsonNode response) {
        if (response == null) {
            displayError("Erro: Nenhuma resposta recebida do servidor.");
            return;
        }

        if (response.has("status") && response.get("status").asText().startsWith("2")) {
            System.out.println("Sucesso: " + response.get("mensagem").asText());
            
            if (response.has("usuario")) {
                if (response.get("usuario").isTextual()) {
                    System.out.println(" -> Nome de Usuário: " + response.get("usuario").asText());
                } else if (response.get("usuario").isObject()) {
                     System.out.println(" -> Dados: " + response.get("usuario").toString());
                }
            }
            
            if (response.has("usuarios")) {
                System.out.println(" -> Lista de Usuários:");
                for (JsonNode userNode : response.get("usuarios")) {
                    System.out.println("    - ID: " + userNode.get("id").asText() + ", Nome: " + userNode.get("nome").asText());
                }
            }
            
            if (response.has("filmes")) {
                System.out.println(" -> Lista de Filmes:");
                JsonNode filmesArray = response.get("filmes");
                
                if (filmesArray.isEmpty()) {
                    System.out.println("    (Nenhum filme cadastrado)");
                } else {
                    for (JsonNode filmeNode : filmesArray) {
                        System.out.println("    -----------------");
                        System.out.println("    ID: " + filmeNode.get("id").asText() + " | Título: " + filmeNode.get("titulo").asText());
                        System.out.println("      Dir: " + filmeNode.get("diretor").asText() + " | Ano: " + filmeNode.get("ano").asText());
                        System.out.println("      Nota: " + filmeNode.get("nota").asText() + " (" + filmeNode.get("qtd_avaliacoes").asText() + " votos)");
                        System.out.println("      Gêneros: " + filmeNode.get("genero").toString());
                        System.out.println("      Sinopse: " + filmeNode.get("sinopse").asText());
                    }
                    System.out.println("    -----------------");
                }
            }
            // TODO: Adicionar 'if' aqui para 'reviews', etc.

        } else {
            if (response.has("mensagem")) {
                displayError("Erro: " + response.get("mensagem").asText());
            } else {
                displayError("Erro: Resposta desconhecida do servidor.");
            }
        }
    }

    public String displayMainMenu(String role) throws IOException {
        System.out.println("\n--- VoteFlix® Menu Principal ---");
        
        if (role == null) {
            System.out.println("1. Cadastrar novo usuário");
            System.out.println("2. Fazer Login");
            System.out.println("0. Sair do programa");
            
        } else if ("admin".equals(role)) {
            System.out.println("1. Minha Conta");
            System.out.println("2. Gerenciar Usuários");
            System.out.println("3. Gerenciar Filmes");
            System.out.println("\n9. Fazer Logout");
            System.out.println("0. Sair do programa");
            
        } else {
            System.out.println("1. Ver meu cadastro");
            System.out.println("2. Atualizar minha senha");
            System.out.println("3. Listar todos os filmes");
            System.out.println("\n8. Excluir minha conta");
            System.out.println("9. Fazer Logout");
            System.out.println("0. Sair do programa");
        }
        System.out.print("Escolha uma opção: ");
        return consoleReader.readLine();
    }

    public String displayAccountMenu() throws IOException {
        System.out.println("\n--- Menu: Minha Conta ---");
        System.out.println("1. Ver meu cadastro");
        System.out.println("2. Atualizar minha senha");
        System.out.println("\n0. Voltar ao menu principal");
        System.out.print("Escolha uma opção: ");
        return consoleReader.readLine();
    }

    public String displayUserMenu() throws IOException {
        System.out.println("\n--- Menu: Gerenciar Usuários ---");
        System.out.println("1. Listar TODOS os usuários");
        System.out.println("2. Mudar senha de um usuário");
        System.out.println("3. Excluir um usuário");
        System.out.println("\n0. Voltar ao menu principal");
        System.out.print("Escolha uma opção: ");
        return consoleReader.readLine();
    }

    public String displayMovieMenu() throws IOException {
        System.out.println("\n--- Menu: Gerenciar Filmes ---");
        System.out.println("1. Listar todos os filmes");
        System.out.println("2. Criar Novo Filme");
        System.out.println("3. Editar dados de um filme");
        System.out.println("4. Excluir um filme");
        System.out.println("\n0. Voltar ao menu principal");
        System.out.print("Escolha uma opção: ");
        return consoleReader.readLine();
    }

    public String promptForIP() throws IOException {
        System.out.println("Qual o IP do servidor? ");
        return consoleReader.readLine();
    }

    public int promptForPort() throws IOException {
        while(true) {
            try {
                System.out.println("Qual a Porta do servidor? ");
                return Integer.parseInt(consoleReader.readLine());
            } catch (NumberFormatException e) {
                displayError("Por favor, digite apenas números.");
            }
        }
    }

    public Map<String, String> promptForLogin() throws IOException {
        Map<String, String> credentials = new HashMap<>();
        System.out.print("Login: ");
        credentials.put("user", consoleReader.readLine());
        System.out.print("Senha: ");
        credentials.put("pass", consoleReader.readLine());
        return credentials;
    }

    public Map<String, String> promptForNewUser() throws IOException {
        Map<String, String> credentials = new HashMap<>();
        System.out.print("Digite o nome de usuário (3-20 caracteres): ");
        credentials.put("user", consoleReader.readLine());
        System.out.print("Digite a senha (3-20 caracteres): ");
        credentials.put("pass", consoleReader.readLine());
        return credentials;
    }

    public String promptForNewPassword() throws IOException {
        System.out.print("Digite sua NOVA senha (3-20 caracteres): ");
        return consoleReader.readLine();
    }

    public boolean promptForConfirmation(String message) throws IOException {
        System.out.print(message + " ");
        String input = consoleReader.readLine();
        return input != null && input.trim().equalsIgnoreCase("s");
    }

    public Map<String, String> promptForAdminEditUser() throws IOException {
        Map<String, String> data = new HashMap<>();
        System.out.println("\n--- Mudar Senha (Admin) ---");
        System.out.print("Digite o ID do usuário que deseja alterar: ");
        data.put("id", consoleReader.readLine());
        System.out.print("Digite a NOVA senha para este usuário (3-20 caracteres): ");
        data.put("newPassword", consoleReader.readLine());
        return data;
    }

    public Map<String, Object> promptForMovieEditDetails() throws IOException {
        Map<String, Object> movieData = new HashMap<>();
        System.out.println("\n--- Editar Filme ---");
        
        System.out.print("Digite o ID do filme que deseja editar: ");
        movieData.put("id", consoleReader.readLine());
        
        System.out.print("Novo Título: ");
        movieData.put("titulo", consoleReader.readLine());
        
        System.out.print("Novo Diretor: ");
        movieData.put("diretor", consoleReader.readLine());
        
        System.out.print("Novo Ano: ");
        movieData.put("ano", consoleReader.readLine());
        
        System.out.print("Nova Sinopse: ");
        movieData.put("sinopse", consoleReader.readLine());
        
        System.out.print("Novos Gêneros (separados por vírgula, ex: Acao,Drama): ");
        List<String> generos = Arrays.asList(consoleReader.readLine().trim().split("\\s*,\\s*"));
        movieData.put("genero", generos);

        return movieData;
    }
    
    public Map<String, Object> promptForNewMovieDetails() throws IOException {
        Map<String, Object> movieData = new HashMap<>();
        System.out.println("\n--- Criar Novo Filme ---");
        
        System.out.print("Título: ");
        movieData.put("titulo", consoleReader.readLine());
        
        System.out.print("Diretor: ");
        movieData.put("diretor", consoleReader.readLine());
        
        System.out.print("Ano (ex: 2010): ");
        movieData.put("ano", consoleReader.readLine());
        
        System.out.print("Sinopse: ");
        movieData.put("sinopse", consoleReader.readLine());
        
        System.out.print("Gêneros (separados por vírgula, ex: Acao,Drama): ");
        List<String> generos = Arrays.asList(consoleReader.readLine().trim().split("\\s*,\\s*"));
        movieData.put("genero", generos);

        return movieData;
    }
    
    public String promptForId(String resourceName, String acao) throws IOException {
        System.out.println("\n--- " + acao + " " + resourceName + " ---");
        System.out.print("Digite o ID do " + resourceName + ": ");
        return consoleReader.readLine();
    }
}