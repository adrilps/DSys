package testes_SD;

import java.io.*;
import java.net.*;
import com.fasterxml.jackson.core.JsonProcessingException; // Import this exception
import com.fasterxml.jackson.databind.ObjectMapper;

public class EchoServerTCP_Thread_Client extends Thread{

    // Main method - entry point of the program
    public static void main(String[] args) throws IOException {
        System.out.println("Qual o IP do servidor? ");
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
        
        EchoServerTCP_Thread_Client client = new EchoServerTCP_Thread_Client();

        String token = login(br, in, out);
        

        System.out.println("Conectado. Digite (\"bye\" para sair)");
        System.out.print("Digite: ");

        out.close();
        in.close();
        echoSocket.close();
         
    }
   
    // --- CHANGE 2: The login method is now correctly placed INSIDE the class ---
    public static String login(BufferedReader br, BufferedReader in, PrintWriter out) throws IOException{
        
    	
    	System.out.println("Insira seu login ");
        br = new BufferedReader(new InputStreamReader(System.in));
        String userLogin = br.readLine();
        
        System.out.println("Insira sua senha ");
        br = new BufferedReader(new InputStreamReader(System.in));
        String userPassword = br.readLine();
        // --- CHANGE 3: The local class now has a constructor to accept values ---
        class MyObject {
            public String operacao = "LOGIN"; // public so Jackson can see it
            public String usuario;
            public String senha;

            // Constructor for MyObject
            public MyObject(String user, String pass) {
                this.usuario = userLogin;
                this.senha = userPassword;
            }
        }
        // --- CHANGE 4: Use writeValueAsString() to get a JSON string ---
        if (userLogin.length() > 20 || userLogin.length() < 3) {
        	return null;
        }
        
        if (userPassword.length() > 20 || userLogin.length() < 3) {
        	return null;
        }
        
        
        MyObject obj = new MyObject(userLogin, userPassword); // Create the object using the constructor
        ObjectMapper mapper = new ObjectMapper();
        
        System.out.println("Servidor retornou: " + in.readLine());
        try {
            String outputJson = mapper.writeValueAsString(obj);
            System.out.println("JSON created successfully.");
            out.print(outputJson);
            return outputJson;
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // Handle potential JSON processing errors
            return null; // Return null if there's an error
        }
        System.out.println("Servidor retornou: " + in.readLine());
    }
    /*
    public void logout() {
        // Method stub
    }
    
    public void createMovie() {
        // Method stub
    }
    
    // ... all your other methods should be here, inside the class ...

 // --- CHANGE 5: This is the ONLY closing brace for the class ---
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
    
    */
}
