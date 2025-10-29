package testes_SD;

import java.io.*;
import java.net.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

public class EchoServerTCP_Thread_Client {

    private static String tokenJWT = null; // Armazena o token após o login
    private static String userRole = null; // Armazena o cargo (admin/user)
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Qual o IP do servidor? ");
        String serverIP = consoleReader.readLine();

        System.out.println("Qual a Porta do servidor? ");
        int serverPort = Integer.parseInt(consoleReader.readLine());

        System.out.println("Tentando conectar com host " + serverIP + " na porta " + serverPort);

        try (
                Socket echoSocket = new Socket(serverIP, serverPort);
                PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()))) {
            System.out.println("Conectado!");

            boolean running = true;
            while (running) {
                if (tokenJWT == null) {
                    System.out.println("\n--- VoteFlix® Menu (Deslogado) ---");
                    System.out.println("1. Cadastrar novo usuário");
                    System.out.println("2. Fazer Login");
                    System.out.println("0. Sair do programa");
                    System.out.print("Escolha uma opção: ");

                    String escolha = consoleReader.readLine();
                    switch (escolha) {
                        case "1": cadastrarUsuario(consoleReader, out, in); break;
                        case "2": fazerLogin(consoleReader, out, in); break;
                        case "0": running = false; break;
                        default: System.out.println("Opção inválida.");
                    }
                } else if ("admin".equals(userRole)) {
                    System.out.println("\n--- VoteFlix® Menu (ADMIN) ---");
                    System.out.println("1. Ver meu cadastro");
                    System.out.println("2. Atualizar minha senha");
                    System.out.println("3. Listar TODOS os usuários");
                    System.out.println("4. Mudar senha de um usuário");
                    System.out.println("5. Fazer Logout");
                    System.out.println("0. Sair do programa");
                    System.out.print("Escolha uma opção: ");

                    String escolha = consoleReader.readLine();
                    switch (escolha) {
                        case "1": verCadastro(out, in); break;
                        case "2": atualizarCadastro(consoleReader, out, in); break;
                        case "3": adminListarUsuarios(out, in); break;
                        case "4": adminEditarUsuario(consoleReader, out, in); break;
                        case "5":
                            if (fazerLogout(out, in)) {
                                running = false;
                            }
                            break;
                        case "0": running = false; break;
                        default: System.out.println("Opção inválida.");
                    }
                } else {
                    System.out.println("\n--- VoteFlix® Menu (Usuário) ---");
                    System.out.println("1. Ver meu cadastro");
                    System.out.println("2. Atualizar minha senha");
                    System.out.println("3. Excluir minha conta");
                    System.out.println("4. Fazer Logout");
                    System.out.println("0. Sair do programa");
                    System.out.print("Escolha uma opção: ");

                    String escolha = consoleReader.readLine();
                    switch (escolha) {
                        case "1":
                            verCadastro(out, in);
                            break;
                        case "2":
                            atualizarCadastro(consoleReader, out, in);
                            break;
                        case "3":
                            if (excluirConta(out, in)) {
                                running = false;
                            }
                            break;
                        case "4":
                            if (fazerLogout(out, in)) {
                                running = false;
                            }
                            break;
                        case "0":
                            running = false;
                            break;
                        default:
                            System.out.println("Opção inválida.");
                    }
                }
            }
            System.out.println("Desconectando...");

        } catch (UnknownHostException e) {
            System.err.println("Host " + serverIP + " nao encontrado!");
        } catch (IOException e) {
            System.err.println("Não foi possivel conectar/comunicar com " + serverIP);
        }
    }

    private static void cadastrarUsuario(BufferedReader reader, PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Digite o nome de usuário (3-20 caracteres alfanuméricos): ");
        String user = reader.readLine();
        System.out.print("Digite a senha (3-20 caracteres alfanuméricos): ");
        String pass = reader.readLine();


        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("nome", user);
        usuarioMap.put("senha", pass);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "CRIAR_USUARIO");
        requestMap.put("usuario", usuarioMap);
        
        String jsonRequest = mapper.writeValueAsString(requestMap);
        
        enviarEImprimir(jsonRequest, out, in);
    }

    private static void fazerLogin(BufferedReader reader, PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Login: ");
        String user = reader.readLine();
        System.out.print("Senha: ");
        String pass = reader.readLine();

        try {
            UserClass loginUser = new UserClass("LOGIN", user, pass);
            String jsonRequest = mapper.writeValueAsString(loginUser);

            System.out.println("\nCliente enviou: " + jsonRequest);
            out.println(jsonRequest);
            String jsonResponse = in.readLine();
            System.out.println("Servidor retornou: " + jsonResponse);

            JsonNode responseNode = mapper.readTree(jsonResponse);

            if (responseNode.has("status") && responseNode.get("status").asText().equals("200")) {
                
                if (responseNode.has("token")) {
                    tokenJWT = responseNode.get("token").asText();
                    
                    userRole = getRoleFromToken(tokenJWT); 
                    
                    System.out.println("Login bem-sucedido! Cargo: " + userRole);

                } else {
                    System.err.println("Erro de protocolo: Login OK, mas resposta não contém token.");
                }
            } else if (responseNode.has("mensagem")) {
                System.out.println("Falha no login: " + responseNode.get("mensagem").asText());
            } else {
                System.err.println("Erro: Resposta do servidor em formato JSON desconhecido.");
            }

        } catch (JsonProcessingException e) {
            System.err.println("Erro crítico: Falha ao processar a resposta do servidor. Resposta não é um JSON válido.");
            System.err.println("Resposta recebida: " + e.getOriginalMessage());
        }
    }
    
    private static String getRoleFromToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();

            String payloadJson = new String(decoder.decode(chunks[1]));
            
            JsonNode payloadNode = mapper.readTree(payloadJson);
            
            if (payloadNode.has("role")) {
                return payloadNode.get("role").asText();
            }

        } catch (Exception e) {
            System.err.println("Erro ao decodificar o token JWT: " + e.getMessage());
        }
        
        return "user";
    }

    private static void verCadastro(PrintWriter out, BufferedReader in) throws IOException {
        if (tokenInvalido()) return;

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("operacao", "LISTAR_PROPRIO_USUARIO");
        requestMap.put("token", tokenJWT);

        enviarEImprimir(mapper.writeValueAsString(requestMap), out, in);
    }


    private static void atualizarCadastro(BufferedReader reader, PrintWriter out, BufferedReader in) throws IOException {
        if (tokenInvalido()) return;

        System.out.print("Digite sua NOVA senha (3-20 caracteres): ");
        String novaSenha = reader.readLine();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "EDITAR_PROPRIO_USUARIO");
        requestMap.put("token", tokenJWT);

        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("senha", novaSenha);
        requestMap.put("usuario", usuarioMap);

        enviarEImprimir(mapper.writeValueAsString(requestMap), out, in);
    }


    private static boolean excluirConta(PrintWriter out, BufferedReader in) throws IOException {
        if (tokenInvalido()) return false;

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("operacao", "EXCLUIR_PROPRIO_USUARIO");
        requestMap.put("token", tokenJWT);
        
        String jsonResponse = enviarEImprimir(mapper.writeValueAsString(requestMap), out, in);
        
        try {
            JsonNode responseNode = mapper.readTree(jsonResponse);
            if (responseNode.has("status") && responseNode.get("status").asText().equals("200")) {
                System.out.println("Conta excluída. Encerrando.");
                tokenJWT = null;
                userRole = null; // Limpa o cargo
                return true;
            }
        } catch (JsonProcessingException e) {
             System.err.println("Erro ao excluir conta, resposta do servidor não é JSON.");
        }
        return false;
    }

    private static boolean fazerLogout(PrintWriter out, BufferedReader in) throws IOException {
        if (tokenInvalido()) return false;

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("operacao", "LOGOUT");
        requestMap.put("token", tokenJWT);
        
        String jsonResponse = enviarEImprimir(mapper.writeValueAsString(requestMap), out, in);

        try {
            JsonNode responseNode = mapper.readTree(jsonResponse);
            if (responseNode.has("status") && responseNode.get("status").asText().equals("200")) {
                System.out.println("Logout realizado. Encerrando.");
                tokenJWT = null;
                userRole = null; // Limpa o cargo
                return true;
            }
        } catch (JsonProcessingException e) {
             System.err.println("Erro no logout, resposta do servidor não é JSON.");
        }
        return false;
    }


    private static void adminListarUsuarios(PrintWriter out, BufferedReader in) throws IOException {
        if (tokenInvalido()) return;

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("operacao", "LISTAR_USUARIOS");
        requestMap.put("token", tokenJWT);

        enviarEImprimir(mapper.writeValueAsString(requestMap), out, in);
    }

    private static void adminEditarUsuario(BufferedReader reader, PrintWriter out, BufferedReader in) throws IOException {
        if (tokenInvalido()) return;

        System.out.print("Digite o ID do usuário que deseja alterar: ");
        String idUsuarioAlvo = reader.readLine();
        System.out.print("Digite a NOVA senha para este usuário (3-20 caracteres): ");
        String novaSenha = reader.readLine();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("operacao", "ADMIN_EDITAR_USUARIO");
        requestMap.put("token", tokenJWT);
        requestMap.put("id", idUsuarioAlvo);

        Map<String, String> usuarioMap = new HashMap<>();
        usuarioMap.put("senha", novaSenha);
        requestMap.put("usuario", usuarioMap);

        enviarEImprimir(mapper.writeValueAsString(requestMap), out, in);
    }

    private static boolean tokenInvalido() {
        if (tokenJWT == null) {
            System.out.println("Erro: Você precisa estar logado para fazer isso.");
            return true;
        }
        return false;
    }


    private static String enviarEImprimir(String jsonRequest, PrintWriter out, BufferedReader in) throws IOException {
        System.out.println("\nCliente enviou: " + jsonRequest);
        out.println(jsonRequest);
        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);
        return jsonResponse;
    }
}