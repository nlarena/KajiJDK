import java.util.Enumeration;
import java.util.ResourceBundle;

public class Bnd_es extends ResourceBundle {
    protected Object handleGetObject(String key) {
        if (key.equals("greet")) {
            return "hola";
        }
        return null;
    }

    public Enumeration<String> getKeys() {
        String[] ks = new String[1];
        ks[0] = "greet";
        return new KeyEnum(ks);
    }
}
