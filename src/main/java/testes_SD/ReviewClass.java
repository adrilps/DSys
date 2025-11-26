package testes_SD;

public class ReviewClass {
    private String id_filme;
    private String nota;
    private String titulo;
    private String descricao;

    public ReviewClass() {}

    public String getId_filme() { return id_filme; }
    public void setId_filme(String id_filme) { this.id_filme = id_filme; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}