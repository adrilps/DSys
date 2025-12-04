package testes_SD;

import java.util.StringJoiner;

public class ReviewDBModel {

    private static final String CSV_DELIMITER = ";";

    private String id;
    private String id_filme;
    private String nome_usuario;
    private String nota;
    private String titulo;
    private String descricao;
    private String data;
    private String editado;

    public ReviewDBModel(String id, String id_filme, String nome_usuario, String nota, String titulo, String descricao, String data, String editado) {
        this.id = id;
        this.id_filme = id_filme;
        this.nome_usuario = nome_usuario;
        this.nota = nota;
        this.titulo = titulo;
        this.descricao = descricao;
        this.data = data;
        this.editado = editado;
    }

    public String getId() { return id; }
    public String getId_filme() { return id_filme; }
    public String getNome_usuario() { return nome_usuario; }
    public String getNota() { return nota; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getData() { return data; }
    public String getEditado() { return editado; }

    public void setNota(String nota) { this.nota = nota; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setEditado(String editado) { this.editado = editado; }

    public String paraCsvString() {
        StringJoiner joiner = new StringJoiner(CSV_DELIMITER);
        joiner.add(id);
        joiner.add(id_filme);
        joiner.add(nome_usuario);
        joiner.add(nota);
        joiner.add(titulo);
        joiner.add(descricao);
        joiner.add(data);
        joiner.add(editado);
        return joiner.toString();
    }

    public static ReviewDBModel deCsvString(String csvLine) {
        if (csvLine == null || csvLine.isEmpty()) return null;
        String[] parts = csvLine.split(CSV_DELIMITER);
        if (parts.length >= 7) {
            String editadoVal = (parts.length > 7) ? parts[7] : "false";
            return new ReviewDBModel(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], editadoVal);
        }
        return null;
    }
}