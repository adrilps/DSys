package testes_SD;

import java.util.List;


public class MovieClass {
    private String titulo;
    private String diretor;
    private String ano;
    private List<String> genero;
    private String sinopse;


     
    public MovieClass() {
    }


    public String getTitulo() { return titulo; }
    public String getDiretor() { return diretor; }
    public String getAno() { return ano; }
    public List<String> getGenero() { return genero; }
    public String getSinopse() { return sinopse; }

    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDiretor(String diretor) { this.diretor = diretor; }
    public void setAno(String ano) { this.ano = ano; }
    public void setGenero(List<String> genero) { this.genero = genero; }
    public void setSinopse(String sinopse) { this.sinopse = sinopse; }
}