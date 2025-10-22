package testes_SD;

public class UserClass {
        public String operacao = "LOGIN"; // public so Jackson can see it
        public String usuario;
        public String senha;

        // Constructor for MyObject
        public UserClass(String operation,String user, String pass) {
        	this.operacao = operation;
            this.usuario = user;
            this.senha = pass;
        }
        public String getOperacao() {
			return operacao;
		}
        
		public String getUsuario() {
			return usuario;
		}


		public String getSenha() {
			return senha;
		}
		
		public String setOperacao(String newOperation) {
			operacao = newOperation;
			return operacao;
		}
        
		public String setUsuario(String newUser) {
			usuario = newUser;
			return usuario;
		}


		public String setSenha(String newPassword) {
			senha = newPassword;
			return senha;
		}
       
}
