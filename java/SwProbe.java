import java.io.StringWriter;

// Comprueba que StringWriter acumula: es lo que el Filer entrega a un procesador de anotaciones.
public class SwProbe {
    public static int prueba() {
        StringWriter w = new StringWriter();
        w.write("class Foo {}");
        String out = w.toString();
        if (out.equals("class Foo {}")) {
            return 0;
        }
        return out.length() + 1;
    }
    public static void main(String[] a) { System.out.println(SwProbe.prueba()); }
}
