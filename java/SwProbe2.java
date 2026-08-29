import java.io.StringWriter;
import java.io.Writer;

// El camino exacto del FilerDriver: el writer se recupera con el tipo estatico `Writer`, se
// baja con un cast, se escribe y se cierra.
public class SwProbe2 {
    public static int prueba() {
        StringWriter original = new StringWriter();
        Writer wide = original;
        StringWriter w = (StringWriter) wide;
        w.write("class Foo {}");
        w.close();
        String out = original.toString();
        if (out.equals("class Foo {}")) {
            return 0;
        }
        return out.length() + 1;
    }
    public static void main(String[] a) { System.out.println(SwProbe2.prueba()); }
}
