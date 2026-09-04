/**
 * `Class.forName` corre el inicializador estatico, y las otras dos formas no.
 *
 * <p>Es la diferencia entera entre las tres sobrecargas, y la razon por la que la de tres argumentos
 * existe. De esto viven los idiomas en los que una clase **se registra sola** al ser nombrada: el
 * driver de JDBC que se anota en el `DriverManager`, un proveedor de `spi` que se instala al
 * cargarse. Sin esto compilan, corren, y no hacen nada.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25.
 */
public class ClinitTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        Marca.visto = false;
        Class<?> c1 = Class.forName("Cargada1");
        ok("forName(String) inicializa", Marca.visto);
        ok("y devuelve la clase", c1 != null && "Cargada1".equals(c1.getName()));

        Marca.visto = false;
        Class.forName("Cargada2", false, ClinitTest.class.getClassLoader());
        ok("forName(name, false, loader) NO inicializa", !Marca.visto);

        Marca.visto = false;
        Class.forName("Cargada3", true, ClinitTest.class.getClassLoader());
        ok("forName(name, true, loader) si inicializa", Marca.visto);

        // Una clase que no existe sigue siendo un `ClassNotFoundException`, no un null.
        boolean tiro = false;
        try {
            Class.forName("no.existe.En.Ningun.Lado");
        } catch (ClassNotFoundException e) {
            tiro = true;
        }
        ok("una clase que no existe tira", tiro);

        boolean tiroTres = false;
        try {
            Class.forName("no.existe.Tampoco", true, ClinitTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            tiroTres = true;
        }
        ok("y la de tres argumentos tambien", tiroTres);

        // Inicializar dos veces corre el bloque una sola vez: es lo que hace que el registro de un
        // driver no se duplique.
        Marca.cuenta = 0;
        Class.forName("Contada");
        Class.forName("Contada");
        ok("el <clinit> corre una sola vez", Marca.cuenta == 1);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] a) throws Exception {
        System.out.println("ClinitTest " + ClinitTest.run());
    }
}

class Marca {
    static boolean visto = false;
    static int cuenta = 0;
}

class Cargada1 {
    static {
        Marca.visto = true;
    }
}

class Cargada2 {
    static {
        Marca.visto = true;
    }
}

class Cargada3 {
    static {
        Marca.visto = true;
    }
}

class Contada {
    static {
        Marca.cuenta = Marca.cuenta + 1;
    }
}
