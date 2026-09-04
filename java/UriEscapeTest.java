import java.net.URI;

// Los constructores POR PARTES de `java.net.URI` escapan; el de un solo `String` no.
//
// Esa distincion es la razon de ser del par `getPath()`/`getRawPath()`, y mientras el escape no
// existio los dos metodos devolvian lo mismo -- o sea que `getRawPath()` contestaba mal, que es peor
// que no estar.
//
// Cada caso de aca esta tomado del JDK real: la prueba corre igual en la JVM real, donde ejercita
// `java.net.URI` del JDK, y en la VM de KajiJDK, donde ejercita la nuestra. Las dos tienen que dar
// -1; si una da otra cosa, es que difieren.
public class UriEscapeTest {

    private static int caso;

    public static int run() throws Exception {
        // ---- lo que se escapa en un camino ------------------------------------------------------
        // El espacio, que es el caso que aparece con cualquier nombre de archivo real.
        caso = 1;
        URI u = new URI("file", null, "/a b/c", null);
        if (!eq(u.toString(), "file:/a%20b/c")) {
            return caso;
        }
        // Y las dos mitades del par dejan de coincidir, que es el punto.
        if (!eq(u.getRawPath(), "/a%20b/c")) {
            return 2;
        }
        if (!eq(u.getPath(), "/a b/c")) {
            return 3;
        }

        // Un '%' literal se escapa a %25; si no, al releerlo se lo tomaria por un escape.
        caso = 4;
        u = new URI("file", null, "/a%20b", null);
        if (!eq(u.toString(), "file:/a%2520b")) {
            return caso;
        }
        if (!eq(u.getPath(), "/a%20b")) {
            return 5;
        }

        // Un '%' que NO es un escape valido tambien se escapa, y vuelve tal cual.
        caso = 6;
        u = new URI("file", null, "/a%zz", null);
        if (!eq(u.toString(), "file:/a%25zz")) {
            return caso;
        }
        if (!eq(u.getPath(), "/a%zz")) {
            return 7;
        }

        // '?' y '#' dentro de un camino SI se escapan: sin eso cortarian el camino.
        caso = 8;
        u = new URI("http", null, "h", -1, "/a?b#c", null, null);
        if (!eq(u.toString(), "http://h/a%3Fb%23c")) {
            return caso;
        }
        if (!eq(u.getPath(), "/a?b#c")) {
            return 9;
        }

        // Los "mark" del RFC 2396 NO se escapan, aunque el RFC 3986 los considere reservados.
        // Escapar de mas tambien cambia el valor.
        caso = 10;
        u = new URI("http", null, "h", -1, "/a!b~c*d'e(f)g-h_i.j", null, null);
        if (!eq(u.toString(), "http://h/a!b~c*d'e(f)g-h_i.j")) {
            return caso;
        }

        // Ni los separadores que un camino puede llevar adentro.
        caso = 11;
        u = new URI("http", null, "h", -1, "/a:b@c&d=e+f$g,h", null, null);
        if (!eq(u.toString(), "http://h/a:b@c&d=e+f$g,h")) {
            return caso;
        }

        // Un caracter de control se escapa.
        caso = 12;
        u = new URI("http", null, "h", -1, "/a\tb", null, null);
        if (!eq(u.toString(), "http://h/a%09b")) {
            return caso;
        }
        if (!eq(u.getPath(), "/a\tb")) {
            return 13;
        }

        // ---- el conjunto es POR COMPONENTE ------------------------------------------------------
        // En la consulta, '/' y '?' ya no separan nada y se dejan; el espacio se sigue escapando.
        caso = 14;
        u = new URI("http", null, "h", -1, "/p", "a/b?c", "x#y");
        if (!eq(u.toString(), "http://h/p?a/b?c#x%23y")) {
            return caso;
        }
        if (!eq(u.getRawQuery(), "a/b?c")) {
            return 15;
        }

        caso = 16;
        u = new URI("http", null, "h", -1, "/p", "a b&c=d", "fr ag");
        if (!eq(u.toString(), "http://h/p?a%20b&c=d#fr%20ag")) {
            return caso;
        }
        if (!eq(u.getQuery(), "a b&c=d")) {
            return 17;
        }
        if (!eq(u.getFragment(), "fr ag")) {
            return 18;
        }

        // ---- los no-ASCII NO se escapan al construir --------------------------------------------
        // Se guardan literales, y `toASCIIString()` es el unico que los codifica. Es lo que hace el
        // JDK, y es lo que distingue a los dos metodos.
        caso = 19;
        u = new URI("http", null, "h", -1, "/café", null, null);
        if (!eq(u.toString(), "http://h/café")) {
            return caso;
        }
        if (!eq(u.getRawPath(), "/café")) {
            return 20;
        }
        if (!eq(u.toASCIIString(), "http://h/caf%C3%A9")) {
            return 21;
        }

        // Un caracter suplementario son dos `char`, y hay que codificar el par entero.
        caso = 22;
        u = new URI("http", null, "h", -1, "/😀", null, null);
        if (!eq(u.toASCIIString(), "http://h/%F0%9F%98%80")) {
            return caso;
        }

        // ---- un camino nulo con autoridad da camino vacio, no nulo ------------------------------
        caso = 23;
        u = new URI("http", null, "h", -1, null, null, null);
        if (!eq(u.toString(), "http://h")) {
            return caso;
        }
        if (!eq(u.getRawPath(), "")) {
            return 24;
        }

        // ---- el constructor de un solo String NO escapa -----------------------------------------
        // Recibe el URI ya escapado, asi que un %XX que venga en el texto es un escape y se decodifica
        // al pedir `getPath()`. Es la otra mitad del contrato.
        caso = 25;
        u = new URI("file:/a%20b/c");
        if (!eq(u.getRawPath(), "/a%20b/c")) {
            return caso;
        }
        if (!eq(u.getPath(), "/a b/c")) {
            return 26;
        }

        // ---- resolve/normalize no vuelven a escapar --------------------------------------------
        // Trabajan sobre componentes que YA estan escapados; escaparlos de nuevo convertiria un
        // `%20` en `%2520` en cada paso.
        caso = 27;
        URI base = new URI("file", null, "/a b/c", null);
        // `resolve(String)` recibe una referencia YA escapada, igual que el constructor de un solo
        // String: pasarle "d e" con el espacio crudo es un error, y el JDK lo rechaza.
        URI rel = base.resolve("d%20e");
        if (!eq(rel.getRawPath(), "/a%20b/d%20e")) {
            return caso;
        }
        caso = 28;
        URI norm = new URI("file", null, "/a b/./c", null).normalize();
        if (!eq(norm.getRawPath(), "/a%20b/c")) {
            return caso;
        }

        return -1;
    }

    private static boolean eq(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
