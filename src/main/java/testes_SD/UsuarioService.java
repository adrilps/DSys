package testes_SD;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class UsuarioService {

    private static final String USERS_FILE_PATH = "C:\\Users\\paulo\\eclipse-workspace\\DSys\\SDistribuidos\\Usuários.txt";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public UsuarioService() {
        inicializarUserDatabase();
    }

    private void inicializarUserDatabase() {
        writeLock.lock();
        try {
            File file = new File(USERS_FILE_PATH);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            if (file.length() == 0) {
                List<UsuarioDBModel> usuarios = new ArrayList<>();
                // --- MUDANÇA AQUI ---
                usuarios.add(new UsuarioDBModel("1", "admin", "admin", "admin")); // Antes era "ADMIN_ROLE"
                salvarTodosUsuarios(usuarios);
                System.out.println("Arquivo 'Usuários.txt' criado e 'admin' cadastrado.");
            }
        } catch (IOException e) {
            System.err.println("Erro crítico ao inicializar o 'Usuários.txt': " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    private List<UsuarioDBModel> lerTodosUsuarios() throws IOException {
        return Files.lines(Paths.get(USERS_FILE_PATH))
                .map(UsuarioDBModel::deCsvString)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void salvarTodosUsuarios(List<UsuarioDBModel> usuarios) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE_PATH, false))) {
            for (UsuarioDBModel user : usuarios) {
                writer.println(user.paraCsvString());
            }
        }
    }

    public String criarUsuario(String nome, String senha) {
        if (nome == null || senha == null ||
                !nome.matches("[a-zA-Z0-9]{3,20}") ||
                !senha.matches("[a-zA-Z0-9]{3,20}")) {
            return "405";
        }
        writeLock.lock();
        try {
            List<UsuarioDBModel> usuarios = lerTodosUsuarios();
            if (usuarios.stream().anyMatch(u -> u.getNome().equalsIgnoreCase(nome))) {
                return "409";
            }
            int maxId = usuarios.stream().mapToInt(u -> Integer.parseInt(u.getId())).max().orElse(0);
            String novoId = String.valueOf(maxId + 1);
            
            // --- MUDANÇA AQUI ---
            usuarios.add(new UsuarioDBModel(novoId, nome, senha, "user")); // Antes era "USER_ROLE"
            
            salvarTodosUsuarios(usuarios);
            System.out.println("Usuário criado: " + nome);
            return "201";
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }

    public UsuarioDBModel autenticar(String nome, String senha) {
        readLock.lock();
        try {
            List<UsuarioDBModel> usuarios = lerTodosUsuarios();
            Optional<UsuarioDBModel> usuario = usuarios.stream()
                    .filter(u -> u.getNome().equals(nome) && u.getSenha().equals(senha))
                    .findFirst();
            return usuario.orElse(null); 
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            readLock.unlock();
        }
    }

    public UsuarioDBModel buscarUsuarioPorNome(String nome) {
        readLock.lock();
        try {
            List<UsuarioDBModel> usuarios = lerTodosUsuarios();
            return usuarios.stream()
                    .filter(u -> u.getNome().equals(nome))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            readLock.unlock();
        }
    }

    public String atualizarSenhaPropria(String nome, String novaSenha) {
        if (novaSenha == null || !novaSenha.matches("[a-zA-Z0-9]{3,20}")) {
            return "405";
        }
        if ("admin".equalsIgnoreCase(nome)) {
            return "403";
        }
        writeLock.lock();
        try {
            List<UsuarioDBModel> usuarios = lerTodosUsuarios();
            boolean encontrou = false;
            for (UsuarioDBModel user : usuarios) {
                if (user.getNome().equals(nome)) {
                    user.setSenha(novaSenha);
                    encontrou = true;
                    break;
                }
            }
            if (encontrou) {
                salvarTodosUsuarios(usuarios);
                System.out.println("Senha atualizada para: " + nome);
                return "200";
            } else {
                return "404";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }

    public String excluirUsuarioProprio(String nome) {
        if ("admin".equalsIgnoreCase(nome)) {
            return "403";
        }
        writeLock.lock();
        try {
            List<UsuarioDBModel> usuarios = lerTodosUsuarios();
            boolean removeu = usuarios.removeIf(u -> u.getNome().equals(nome));
            if (removeu) {
                salvarTodosUsuarios(usuarios);
                System.out.println("Usuário excluído: " + nome);
                return "200";
            } else {
                return "404";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }

    public List<UsuarioDBModel> listarTodosUsuarios() {
        readLock.lock();
        try {
            return new ArrayList<>(lerTodosUsuarios());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            readLock.unlock();
        }
    }

    public String adminEditarSenhaPorId(String idUsuarioAlvo, String novaSenha) {
        if (novaSenha == null || !novaSenha.matches("[a-zA-Z0-9]{3,20}")) {
            return "405";
        }
        
        if ("1".equals(idUsuarioAlvo)) {
             return "403";
        }

        writeLock.lock();
        try {
            List<UsuarioDBModel> usuarios = lerTodosUsuarios();
            boolean encontrou = false;
            String nomeUsuario = "";

            for (UsuarioDBModel user : usuarios) {
                if (user.getId().equals(idUsuarioAlvo)) {
                    user.setSenha(novaSenha);
                    nomeUsuario = user.getNome();
                    encontrou = true;
                    break;
                }
            }

            if (encontrou) {
                salvarTodosUsuarios(usuarios);
                System.out.println("Admin atualizou senha do usuário: " + nomeUsuario + " (ID: " + idUsuarioAlvo + ")");
                return "200";
            } else {
                return "404";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }
    
    public String adminExcluirUsuarioPorId(String idUsuarioAlvo) {
        if ("1".equals(idUsuarioAlvo)) {
            System.err.println("Tentativa ilegal de excluir o 'admin' (ID: 1).");
            return "403";
        }

        writeLock.lock();
        try {
            List<UsuarioDBModel> usuarios = lerTodosUsuarios();
            boolean removeu = usuarios.removeIf(u -> u.getId().equals(idUsuarioAlvo));

            if (removeu) {
                salvarTodosUsuarios(usuarios);
                System.out.println("Admin excluiu o usuário ID: " + idUsuarioAlvo);
                return "200";
            } else {
                return "404";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "500";
        } finally {
            writeLock.unlock();
        }
    }
}