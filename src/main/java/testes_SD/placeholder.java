package testes_SD;

import java.io.*;
import java.net.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EchoServerTCP_Thread_Client {
	

    public static void main(String[] args) {
    	System.out.println("a");
    	public String teste = login("joao","123");
        /*System.out.println("Qual o IP do servidor? ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String serverIP = br.readLine();

        System.out.println("Qual a Porta do servidor? ");
        br = new BufferedReader(new InputStreamReader(System.in));
        int serverPort = Integer.parseInt(br.readLine());

        System.out.println("Tentando conectar com host " + serverIP + " na porta " + serverPort);

        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            echoSocket = new Socket(serverIP, serverPort);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host " + serverIP + " nao encontrado!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Não foi possivel reservar I/O para conectar com " + serverIP);
            System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        System.out.println("Conectado. Digite (\"bye\" para sair)");
        System.out.print("Digite: ");
        while ((userInput = stdIn.readLine()) != null) {
            out.println(userInput);

            // end loop
            if (userInput.toUpperCase().equals("BYE"))
                break;

            System.out.println("Servidor retornou: " + in.readLine());
            System.out.print("Digite: ");
        }

        out.close();
        in.close();
        stdIn.close();
        echoSocket.close();
         */
    }
}
    
    
    /*x
    public String login(String login, String password) {
    	class myObject{
    		private String operacao = "LOGIN";
        	private String usuario = login;
        	private String senha = password;
    	}
    	private String outputJson;
    	myObject obj = new myObject();
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.writeValue(outputJson, obj);
		System.out.println("a");
		return outputJson;
    }
}
    /*
    public void logout() {
    	
    }
    
    public void createMovie() {
    	
    }
    
    public void createReview() {
    	
    }
    
    public void createUser() {
    	
    }
    
    public void listMovies() {
    	
    }
    
    public void selectMovieById() {
    	
    }
    
    public void updateMovie() {
    	
    }
    
    public void updateReview() {
    	
    }
    
    public void updateUser() {
    	
    }
    
    public void deleteMovie() {
    	
    }
    
    public void deleteReview() {
    	
    }
    
    public void deleteUser() {
    	
    }
    
    
}