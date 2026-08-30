import java.util.Enumeration;
import java.util.ResourceBundle;

// Bundle base: subclase DIRECTA de ResourceBundle, sin Object[][], para poder compilarlo
// con nuestro propio javac (ver finding #281).
public class Bnd extends ResourceBundle {
    protected Object handleGetObject(String key) {
        if (key.equals("greet")) {
            return "hello";
        }
        if (key.equals("bye")) {
            return "goodbye";
        }
        return null;
    }

    public Enumeration<String> getKeys() {
        String[] ks = new String[2];
        ks[0] = "greet";
        ks[1] = "bye";
        return new KeyEnum(ks);
    }
}
