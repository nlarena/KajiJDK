import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;

public class SpfDbg2 {

    static class A implements Serializable {
        private static final ObjectStreamField[] campos = {
            new ObjectStreamField("uno", Integer.TYPE)
        };
        static int contador = 7;
    }

    static class B implements Serializable {
        private static final ObjectStreamField[] campos = {
            new ObjectStreamField("uno", Integer.TYPE)
        };
        static int contador = 7;
    }

    public static void main(String[] a) throws Exception {
        // Sin tocar la clase antes: si el `<clinit>` no corre, el campo se lee en su valor por
        // defecto en vez del que el inicializador le pone.
        Field f = A.class.getDeclaredField("campos");
        f.setAccessible(true);
        System.out.println("A.campos sin inicializar -> " + (f.get(null) == null ? "null" : "ok"));
        Field g = A.class.getDeclaredField("contador");
        g.setAccessible(true);
        System.out.println("A.contador sin inicializar -> " + g.getInt(null));

        // Ahora forzando la inicializacion por la via normal.
        System.out.println("toco B: " + B.contador);
        Field h = B.class.getDeclaredField("campos");
        h.setAccessible(true);
        System.out.println("B.campos despues de tocar -> " + (h.get(null) == null ? "null" : "ok"));
    }
}
