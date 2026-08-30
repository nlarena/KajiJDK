import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// Comportamiento de ResourceBundle con subclases DIRECTAS (sin ListResourceBundle, que no se
// puede subclasear con nuestro javac por el finding #281).
//
// Bnd     -> greet=hello, bye=goodbye
// Bnd_es  -> greet=hola
//
// Con locale "es" la cadena es Bnd_es -> Bnd: greet sale del hijo, bye del padre.
public class RbTest2 {

    public static int run() {
        int r = 0;

        ResourceBundle es = ResourceBundle.getBundle("Bnd", new Locale("es"));

        r = r + (es.getString("greet").equals("hola") ? 1 : 0);
        r = r + (es.getString("bye").equals("goodbye") ? 10 : 0);
        r = r + es.keySet().size() * 100;
        r = r + (es.containsKey("bye") ? 1000 : 0);
        r = r + (es.containsKey("nada") ? 7777 : 0);
        r = r + (es.getLocale().getLanguage().equals("es") ? 10000 : 0);
        r = r + (es.getBaseBundleName().equals("Bnd") ? 100000 : 0);

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

        ResourceBundle root = ResourceBundle.getBundle("Bnd", Locale.ROOT);
        r = r + (root.getString("greet").equals("hello") ? 100000000 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
