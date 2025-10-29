package testes_SD;

import java.net.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EchoServerTCP_Thread_Server extends Thread {

    protected Socket clientSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UsuarioService usuarioService;
    private final AuthService authService;
    private String nomeUsuarioLogado = null;
    private String roleUsuarioLogado = null;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;

        System.out.println("Qual porta o servidor deve usar? ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int porta = Integer.parseInt(br.readLine());

        System.out.println("Servidor carregado na porta " + porta);

        final UsuarioService usuarioService = new UsuarioService();
        final AuthService authService = new AuthService();

        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Aguardando conexao....\n ");
            try {
                while (true) {
                    Socket newClientSocket = serverSocket.accept();
                    
                    String clientIpAddress = newClientSocket.getInetAddress().getHostAddress();
                    
                    System.out.println("Accept ativado. Novo cliente conectado de: " + clientIpAddress + "\n");
                    
                    new EchoServerTCP_Thread_Server(newClientSocket, usuarioService, authService);
                }
            } catch (IOException e) {
                System.err.println("Accept falhou!");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Nao foi possivel ouvir a porta " + porta);
            System.exit(1);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Nao foi possivel fechar a porta " + porta);
                System.exit(1);
            }
        }
    }

    private EchoServerTCP_Thread_Server(Socket clientSoc, UsuarioService uService, AuthService aService) {
        this.clientSocket = clientSoc;
        this.usuarioService = uService;
        this.authService = aService;
        start();
    }

    @Override
    public void run() {
        System.out.println("Nova thread de comunicacao iniciada.\n");

        try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                System.out.println("Servidor recebeu: " + inputLine);
                String operacao = "";
                JsonNode requestNode = null;
                boolean operacaoEncerra = false;

                try {
                    requestNode = objectMapper.readTree(inputLine);
                    if (requestNode.has("operacao")) {
                        operacao = requestNode.get("operacao").asText().toUpperCase();
                    } else {
                        sendJsonError(out, "422", "Erro: Chaves faltantes ou invalidas (sem 'operacao')");
                        continue;
                    }

                    boolean precisaAutenticar = !operacao.equals("LOGIN") && !operacao.equals("CRIAR_USUARIO");

                    if (precisaAutenticar) {
                        this.nomeUsuarioLogado = null;
                        this.roleUsuarioLogado = null;

                        if (!requestNode.has("token")) {
                            sendJsonError(out, "422", "Erro: Chaves faltantes ou invalidas (sem 'token')");
                            continue;
                        }
                        String token = requestNode.get("token").asText();
                        DecodedJWT jwtValido = authService.validarToken(token);

                        if (jwtValido == null) {
                            sendJsonError(out, "401", "Erro: Token inválido");
                            continue;
                        }
                        this.nomeUsuarioLogado = jwtValido.getClaim("nome").asString();
                        this.roleUsuarioLogado = jwtValido.getClaim("role").asString();
                    }

                    switch (operacao) {
                        case "LOGIN":
                            replyLogin(inputLine, out);
                            break;
                        case "CRIAR_USUARIO":
                            replyCreateUser(inputLine, out);
                            break;
                        case "LOGOUT":
                            replyLogout(out);
                            operacaoEncerra = true;
                            break;
                        case "LISTAR_PROPRIO_USUARIO":
                            replyListarProprioUsuario(out);
                            break;
                        case "EDITAR_PROPRIO_USUARIO":
                            replyEditarProprioUsuario(inputLine, out);
                            break;
                        case "EXCLUIR_PROPRIO_USUARIO":
                            replyExcluirProprioUsuario(out);
                            operacaoEncerra = true;
                            break;

                        // --- NOVAS OPERAÇÕES DE ADMIN ---
                        case "LISTAR_USUARIOS":
                            replyListarUsuarios(out);
                            break;
                        case "ADMIN_EDITAR_USUARIO":
                            replyAdminEditarUsuario(inputLine, out);
                            break;

                        default:
                            sendJsonError(out, "400", "Erro: Operação não encontrada ou inválida");
                    }

                    if (operacaoEncerra) {
                        break;
                    }

                } catch (JsonProcessingException e) {
                    sendJsonError(out, "400", "Erro: JSON mal formatado");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJsonError(out, "500", "Erro: Falha interna do servidor");
                }
            }
            System.out.println("Cliente " + (this.nomeUsuarioLogado != null ? this.nomeUsuarioLogado : "[NAO LOGADO]") + " desconectado.");

        } catch (IOException e) {
            System.err.println("Problema com Servidor de Comunicacao! " + e.getMessage());
        }
    }


    private void sendJsonError(PrintWriter out, String status, String mensagem) {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("status", status);
            response.put("mensagem", mensagem);
            String jsonResponse = objectMapper.writeValueAsString(response);
            System.out.println("Servidor enviou: " + jsonResponse);
            out.println(jsonResponse);
        } catch (JsonProcessingException e) {
            out.println("{\"status\":\"500\", \"mensagem\":\"Erro ao gerar JSON de erro\"}");
        }
    }

    private void sendJsonSuccess(PrintWriter out, String status, String mensagem, Map<String, Object> data) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("mensagem", mensagem);
            if (data != null) {
                response.putAll(data);
            }
            String jsonResponse = objectMapper.writeValueAsString(response);
            System.out.println("Servidor enviou: " + jsonResponse);
            out.println(jsonResponse);
        } catch (JsonProcessingException e) {
            sendJsonError(out, "500", "Erro ao gerar JSON de sucesso");
        }
    }


    public void replyLogin(String recievedJson, PrintWriter out) {
        try {
            UserClass obj = objectMapper.readValue(recievedJson, UserClass.class);
            UsuarioDBModel usuarioAutenticado = usuarioService.autenticar(obj.getUsuario(), obj.getSenha());

            if (usuarioAutenticado != null) {
            	//autenticação funciona
                String tokenGerado = authService.gerarToken(
                        usuarioAutenticado.getId(),
                        usuarioAutenticado.getNome(),
                        usuarioAutenticado.getRole());

                Map<String, Object> data = new HashMap<>();
                data.put("token", tokenGerado);
                
                
                sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", data);

            } else {
                sendJsonError(out, "403", "Erro: sem permissão (usuário ou senha inválidos)");
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "422", "Erro: Chaves faltantes ou invalidas no JSON de login");
        }
    }

    public void replyCreateUser(String recievedJson, PrintWriter out) {
        try {

            JsonNode requestNode = objectMapper.readTree(recievedJson);
            
            if (!requestNode.has("usuario") || 
                !requestNode.get("usuario").has("nome") || 
                !requestNode.get("usuario").has("senha")) 
            {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'usuario.nome' e 'usuario.senha')");
                return;
            }
            
            String nome = requestNode.get("usuario").get("nome").asText();
            String senha = requestNode.get("usuario").get("senha").asText();

            String resultado = usuarioService.criarUsuario(nome, senha);
            
            switch (resultado) {
                case "201": sendJsonSuccess(out, "201", "Sucesso: Recurso cadastrado", null); break;
                case "405": sendJsonError(out, "405", "Erro: Campos inválidos"); break;
                case "409": sendJsonError(out, "409", "Erro: Recurso ja existe"); break;
                default: sendJsonError(out, "500", "Erro: Falha interna do servidor");
            }
        } catch (JsonProcessingException e) {

            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyLogout(PrintWriter out) {
        this.nomeUsuarioLogado = null;
        this.roleUsuarioLogado = null;
        sendJsonSuccess(out, "200", "Sucesso: Operação realizada com sucesso", null);
    }

    public void replyListarProprioUsuario(PrintWriter out) {

        UsuarioDBModel user = usuarioService.buscarUsuarioPorNome(this.nomeUsuarioLogado);
        
        if (user == null) {

            sendJsonError(out, "404", "Erro: Recurso inexistente");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("usuario", user.getNome());

        sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", data);
    }

    public void replyEditarProprioUsuario(String recievedJson, PrintWriter out) {
        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("usuario") || !requestNode.get("usuario").has("senha")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'usuario.senha')");
                return;
            }

            String novaSenha = requestNode.get("usuario").get("senha").asText();
            String resultado = usuarioService.atualizarSenhaPropria(this.nomeUsuarioLogado, novaSenha);
            switch (resultado) {
                case "200": sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null); break;
                case "403": sendJsonError(out, "403", "Erro: sem permissão (admin não pode ser editado)"); break;
                case "405": sendJsonError(out, "405", "Erro: Campos inválidos"); break;
                case "404": sendJsonError(out, "404", "Erro: Recurso inexistente"); break;
                default: sendJsonError(out, "500", "Erro: Falha interna do servidor");
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }

    public void replyExcluirProprioUsuario(PrintWriter out) {
        String resultado = usuarioService.excluirUsuarioProprio(this.nomeUsuarioLogado);
        switch (resultado) {
            case "200":
                sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null);
                this.nomeUsuarioLogado = null;
                this.roleUsuarioLogado = null;
                break;
            case "403": sendJsonError(out, "403", "Erro: sem permissão (admin não pode ser excluído)"); break;
            case "404": sendJsonError(out, "404", "Erro: Recurso inexistente"); break;
            default: sendJsonError(out, "500", "Erro: Falha interna do servidor");
        }
    }

    public void replyListarUsuarios(PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }

        List<UsuarioDBModel> todosUsuarios = usuarioService.listarTodosUsuarios();

        List<Map<String, String>> usuariosFormatados = new ArrayList<>();
        for (UsuarioDBModel user : todosUsuarios) {
            Map<String, String> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("nome", user.getNome());
            usuariosFormatados.add(userData);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("usuarios", usuariosFormatados);
        sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", data);
    }

    public void replyAdminEditarUsuario(String recievedJson, PrintWriter out) {
        if (!"admin".equals(this.roleUsuarioLogado)) {
            sendJsonError(out, "403", "Erro: sem permissão");
            return;
        }

        try {
            JsonNode requestNode = objectMapper.readTree(recievedJson);
            if (!requestNode.has("id") || !requestNode.has("usuario") || !requestNode.get("usuario").has("senha")) {
                sendJsonError(out, "422", "Erro: Chaves faltantes (esperado 'id' e 'usuario.senha')");
                return;
            }
            String idAlvo = requestNode.get("id").asText();
            String novaSenha = requestNode.get("usuario").get("senha").asText();

            String resultado = usuarioService.adminEditarSenhaPorId(idAlvo, novaSenha);

            switch (resultado) {
                case "200": sendJsonSuccess(out, "200", "Sucesso: operação realizada com sucesso", null); break;
                case "403": sendJsonError(out, "403", "Erro: sem permissão (não pode alterar admin)"); break;
                case "404": sendJsonError(out, "404", "Erro: Recurso inexistente (ID do usuário)"); break;
                case "405": sendJsonError(out, "405", "Erro: Campos inválidos (nova senha)"); break;
                default: sendJsonError(out, "500", "Erro: Falha interna do servidor");
            }
        } catch (JsonProcessingException e) {
            sendJsonError(out, "400", "Erro: JSON mal formatado");
        }
    }
}