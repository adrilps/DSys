package testes_SD;

public class Context {
    private final static Context instance = new Context();
    private ClientModel model;

    public static Context getInstance() {
        return instance;
    }

    private Context() {
        model = new ClientModel();
    }

    public ClientModel getModel() {
        return model;
    }
}