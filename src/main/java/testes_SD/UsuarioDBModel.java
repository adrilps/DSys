package testes_SD;

public class UsuarioDBModel {
	private String id;
    private String nome;
    private String senha;
    private String role; // "ADMIN_ROLE" ou "USER_ROLE"

    public UsuarioDBModel(String id, String nome, String senha, String role) {
        this.id = id;
        this.nome = nome;
        this.senha = senha;
        this.role = role;
    }


    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getSenha() { return senha; }
    public String getRole() { return role; }

    public void setSenha(String senha) { this.senha = senha; }

    public String paraCsvString() {
        return String.join(";", id, nome, senha, role);
    }

    public static UsuarioDBModel deCsvString(String csvLine) {
        String[] parts = csvLine.split(";", 4);
        if (parts.length == 4) {
            return new UsuarioDBModel(parts[0], parts[1], parts[2], parts[3]);
        }
        return null;
    }
}
