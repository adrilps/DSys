package testes_SD;

public class UserClass {
        public String operacao = "LOGIN"; // public so Jackson can see it
        public String usuario;
        public String senha;

        // Constructor for MyObject
        public UserClass(String user, String pass) {
            this.usuario = user;
            this.senha = pass;
        }

		public String getUsuario() {
			return usuario;
		}


		public String getSenha() {
			return senha;
		}
       
}
