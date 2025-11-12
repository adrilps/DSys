package testes_SD;

import java.util.List;

/**
 * Data Transfer Object (DTO) para criação/edição de filmes.
 * Representa o objeto "filme" aninhado dentro das requisições JSON do cliente.
 * Usado pelo Jackson para desserializar as requisições.
 */
public class MovieClass {
    private String titulo;
    private String diretor;
    private String ano;
    private List<String> genero;
    private String sinopse;

    /**
     * Construtor vazio necessário para a desserialização do Jackson.
     */
    public MovieClass() {
    }

    // Getters
    public String getTitulo() { return titulo; }
    public String getDiretor() { return diretor; }
    public String getAno() { return ano; }
    public List<String> getGenero() { return genero; }
    public String getSinopse() { return sinopse; }

    // Setters (Necessários para o Jackson)
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDiretor(String diretor) { this.diretor = diretor; }
    public void setAno(String ano) { this.ano = ano; }
    public void setGenero(List<String> genero) { this.genero = genero; }
    public void setSinopse(String sinopse) { this.sinopse = sinopse; }
}