package main_project;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class server_side {

	public static void main(String args[]) throws IOException {
		
		 ServerSocket echoServer = null;     // cria o socket do servidor
	        String line;                        // string para conter informações transferidas
	        String verificacao;                 // string psrs encerramento do servidor
	        DataInputStream is;                 // cria um duto de entrada
	        PrintStream os;                     // cria um duto de saída
	        Socket clientSocket = null;  
	}
}
