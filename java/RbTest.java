import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// La misma prueba que RbTest2, pero con ListResourceBundle — que hasta el arreglo de #281 no se
// podia subclasear con nuestro javac, porque getContents() devuelve Object[][].
public class RbTest {
    public static int run() {
        int r = 0;
        ResourceBundle es = ResourceBundle.getBundle("Msgs", new Locale("es"));
        r = r + (es.getString("greet").equals("hola") ? 1 : 0);
        r = r + (es.getString("bye").equals("goodbye") ? 10 : 0);
        r = r + es.keySet().size() * 100;
        r = r + (es.containsKey("bye") ? 1000 : 0);
        r = r + (es.containsKey("nada") ? 7777 : 0);
        r = r + (es.getLocale().getLanguage().equals("es") ? 10000 : 0);
        r = r + (es.getBaseBundleName().equals("Msgs") ? 100000 : 0);
        try {
            es.getString("nada");
            r = r + 7777;
        } catch (MissingResourceException e) {
            r = r + (e.getKey().equals("nada") ? 1000000 : 0);
        }
        try {
            ResourceBundle.getBundle("NoExiste", new Locale("es"));
            r = r + 7777;
        } catch (MissingResourceException e) {
            r = r + 10000000;
        }
        ResourceBundle root = ResourceBundle.getBundle("Msgs", Locale.ROOT);
        r = r + (root.getString("greet").equals("hello") ? 100000000 : 0);
        return r;
    }
    public static void main(String[] args) { System.out.println(run()); }
}
