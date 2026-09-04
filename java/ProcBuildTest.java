// Las tres clases que cerraron `java.lang`: `ProcessBuilder` (con su `Redirect`),
// `RuntimePermission` e `IO`.
//
// Se comprueba contra el `java` real corriendo lo mismo, asi que la prueba no toca nada que dependa
// del entorno de la maquina: el mapa de `environment()` arranca vacio en KajiJDK y lleno en el JDK
// real, asi que solo se miran claves que pone la propia prueba.
//
// Lo que mas se cuida son las tres trampas del contrato, que son justamente lo que un
// `ProcessBuilder` sin `start()` todavia puede equivocar:
//
//   - el constructor y `command(List)` **no copian** la lista; los varargs si.
//   - `DISCARD.type()` es `WRITE`, no un tipo propio: tirar a la nada es escribir al archivo nulo.
//   - `redirectInput` rechaza `WRITE` y `APPEND`, y `redirectOutput`/`redirectError` rechazan
//     `READ`. Una fuente no puede ser un destino.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.File;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcBuildTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // ---- comando ----

    static void comando() {
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "echo");
        List<String> c = pb.command();
        ok(c.size() == 3);
        ok(c.get(0).equals("cmd") && c.get(2).equals("echo"));

        // El constructor de lista se queda con **la lista**, no con una copia: tocarla despues
        // cambia el builder. Es del contrato del JDK y es la trampa mas facil de perder.
        List<String> viva = new ArrayList<String>();
        viva.add("uno");
        ProcessBuilder pb2 = new ProcessBuilder(viva);
        viva.add("dos");
        ok(pb2.command().size() == 2);
        ok(pb2.command() == viva);

        // `command(List)` tampoco copia.
        List<String> otra = new ArrayList<String>();
        otra.add("x");
        ok(pb2.command(otra) == pb2);
        ok(pb2.command() == otra);

        // Los varargs si copian: la lista resultante no es ninguna de las anteriores.
        pb2.command("a", "b");
        ok(pb2.command() != otra && pb2.command().size() == 2);

        List<String> nula = null;
        try {
            new ProcessBuilder(nula);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        try {
            pb2.command(nula);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
    }

    // ---- directorio y mezcla de error ----

    static void directorioYError() {
        ProcessBuilder pb = new ProcessBuilder("x");
        // null no es "sin configurar": significa heredar el directorio del proceso actual.
        ok(pb.directory() == null);
        File d = new File("_kaji_dir");
        ok(pb.directory(d) == pb);
        ok(pb.directory() == d);
        ok(pb.directory(null) == pb && pb.directory() == null);

        ok(!pb.redirectErrorStream());
        ok(pb.redirectErrorStream(true) == pb);
        ok(pb.redirectErrorStream());
        pb.redirectErrorStream(false);
        ok(!pb.redirectErrorStream());
    }

    // ---- las seis formas de Redirect ----

    static void formasDeRedirect() {
        File f = new File("_kaji_red.txt");

        ok(ProcessBuilder.Redirect.PIPE.type() == ProcessBuilder.Redirect.Type.PIPE);
        ok(ProcessBuilder.Redirect.PIPE.file() == null);
        ok(ProcessBuilder.Redirect.PIPE.toString().equals("PIPE"));

        ok(ProcessBuilder.Redirect.INHERIT.type() == ProcessBuilder.Redirect.Type.INHERIT);
        ok(ProcessBuilder.Redirect.INHERIT.file() == null);
        ok(ProcessBuilder.Redirect.INHERIT.toString().equals("INHERIT"));

        // La excepcion que confunde: DISCARD escribe, no tiene tipo propio, y su archivo es el
        // dispositivo nulo del sistema.
        ok(ProcessBuilder.Redirect.DISCARD.type() == ProcessBuilder.Redirect.Type.WRITE);
        ok(ProcessBuilder.Redirect.DISCARD.file() != null);
        // Y por eso mismo se imprime "WRITE", no "DISCARD": las constantes se muestran como su tipo.
        ok(ProcessBuilder.Redirect.DISCARD.toString().equals("WRITE"));

        ProcessBuilder.Redirect r = ProcessBuilder.Redirect.from(f);
        ok(r.type() == ProcessBuilder.Redirect.Type.READ);
        ok(r.file() == f);
        ok(r.toString().equals("redirect to read from file \"" + f + "\""));

        ProcessBuilder.Redirect w = ProcessBuilder.Redirect.to(f);
        ok(w.type() == ProcessBuilder.Redirect.Type.WRITE);
        ok(w.file() == f);
        ok(w.toString().equals("redirect to write to file \"" + f + "\""));

        ProcessBuilder.Redirect a = ProcessBuilder.Redirect.appendTo(f);
        ok(a.type() == ProcessBuilder.Redirect.Type.APPEND);
        ok(a.file() == f);
        ok(a.toString().equals("redirect to append to file \"" + f + "\""));

        // Igualdad por tipo + archivo. `to` y `appendTo` sobre el mismo archivo NO son iguales.
        ok(w.equals(ProcessBuilder.Redirect.to(new File("_kaji_red.txt"))));
        ok(!w.equals(a));
        ok(!w.equals(r));
        ok(w.hashCode() == f.hashCode());
        ok(ProcessBuilder.Redirect.PIPE.equals(ProcessBuilder.Redirect.PIPE));
        ok(!ProcessBuilder.Redirect.PIPE.equals(ProcessBuilder.Redirect.INHERIT));
        ok(!ProcessBuilder.Redirect.PIPE.equals("PIPE"));

        try {
            ProcessBuilder.Redirect.from(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        try {
            ProcessBuilder.Redirect.to(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        try {
            ProcessBuilder.Redirect.appendTo(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
    }

    // ---- las redirecciones del builder ----

    static void redireccionesDelBuilder() {
        ProcessBuilder pb = new ProcessBuilder("x");
        ok(pb.redirectInput() == ProcessBuilder.Redirect.PIPE);
        ok(pb.redirectOutput() == ProcessBuilder.Redirect.PIPE);
        ok(pb.redirectError() == ProcessBuilder.Redirect.PIPE);

        File f = new File("_kaji_red2.txt");
        ok(pb.redirectInput(f) == pb);
        ok(pb.redirectInput().type() == ProcessBuilder.Redirect.Type.READ);
        ok(pb.redirectInput().file().equals(f));

        ok(pb.redirectOutput(f) == pb);
        ok(pb.redirectOutput().type() == ProcessBuilder.Redirect.Type.WRITE);
        ok(pb.redirectError(ProcessBuilder.Redirect.appendTo(f)) == pb);
        ok(pb.redirectError().type() == ProcessBuilder.Redirect.Type.APPEND);

        // Una fuente no puede ser un destino ni al reves.
        try {
            pb.redirectInput(ProcessBuilder.Redirect.to(f));
            ok(false);
        } catch (IllegalArgumentException e) {
            ok(true);
        }
        try {
            pb.redirectInput(ProcessBuilder.Redirect.appendTo(f));
            ok(false);
        } catch (IllegalArgumentException e) {
            ok(true);
        }
        try {
            pb.redirectOutput(ProcessBuilder.Redirect.from(f));
            ok(false);
        } catch (IllegalArgumentException e) {
            ok(true);
        }
        try {
            pb.redirectError(ProcessBuilder.Redirect.from(f));
            ok(false);
        } catch (IllegalArgumentException e) {
            ok(true);
        }
        // Un rechazo no deja nada a medio setear.
        ok(pb.redirectInput().type() == ProcessBuilder.Redirect.Type.READ);

        // DISCARD es un destino valido, no una fuente.
        ok(pb.redirectOutput(ProcessBuilder.Redirect.DISCARD) == pb);
        ok(pb.redirectOutput() == ProcessBuilder.Redirect.DISCARD);
        try {
            pb.redirectInput(ProcessBuilder.Redirect.DISCARD);
            ok(false);
        } catch (IllegalArgumentException e) {
            ok(true);
        }

        ok(pb.inheritIO() == pb);
        ok(pb.redirectInput() == ProcessBuilder.Redirect.INHERIT);
        ok(pb.redirectOutput() == ProcessBuilder.Redirect.INHERIT);
        ok(pb.redirectError() == ProcessBuilder.Redirect.INHERIT);
    }

    // ---- el entorno ----

    static void entorno() {
        ProcessBuilder pb = new ProcessBuilder("x");
        Map<String, String> env = pb.environment();
        // El mismo mapa vivo cada vez, no una copia nueva.
        ok(env == pb.environment());
        // Dos builders nunca comparten entorno.
        ok(env != new ProcessBuilder("x").environment());

        env.put("KajiVar", "1");
        ok(env.get("KajiVar").equals("1"));
        // Contraintuitivo: aunque en Windows los nombres de variable no distingan mayusculas, ESTE
        // mapa si las distingue. La busqueda insensible es la de `System.getenv(String)`, que usa
        // otra estructura; el mapa de escritura del builder es un `HashMap` comun.
        ok(env.get("KAJIVAR") == null);
        ok(!env.containsKey("kajivar"));
        ok(env.containsKey("KajiVar"));
        env.put("KajiVar", "2");
        ok(env.get("KajiVar").equals("2"));
        ok(env.remove("KajiVar").equals("2"));
        ok(!env.containsKey("KajiVar"));

        // Ni nulos para escribir ni nulos para consultar.
        try {
            env.put(null, "x");
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        try {
            env.put("x", null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        try {
            env.get(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        try {
            env.containsKey(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
        try {
            env.remove(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }

        // El `=` es el separador del bloque de entorno: no puede estar en el nombre. Salvo en la
        // posicion 0, donde Windows lo usa para sus variables magicas de unidad.
        try {
            env.put("A=B", "x");
            ok(false);
        } catch (IllegalArgumentException e) {
            ok(true);
        }
        env.put("=C:", "C:\\");
        ok(env.get("=C:").equals("C:\\"));
        env.remove("=C:");
    }

    // ---- RuntimePermission ----

    static void permisos() {
        RuntimePermission p = new RuntimePermission("exitVM.0");
        ok(p.getName().equals("exitVM.0"));
        // Sin acciones: lo variable va en el nombre.
        ok(p.getActions().equals(""));
        ok(new RuntimePermission("exitVM.0", "loQueSea").getActions().equals(""));

        RuntimePermission comodin = new RuntimePermission("exitVM.*");
        ok(comodin.implies(p));
        ok(!p.implies(comodin));

        // El comodin reemplaza un segmento que existe, asi que no alcanza al nodo de arriba.
        ok(!new RuntimePermission("loadLibrary.*").implies(new RuntimePermission("loadLibrary")));

        // Menos con `exitVM`, que es la excepcion vieja: hasta 1.6 el permiso se llamaba asi a
        // secas y hoy se parsea como si fuera `"exitVM.*"`, para que las policy files viejas sigan
        // valiendo. Los dos se implican mutuamente.
        RuntimePermission viejo = new RuntimePermission("exitVM");
        ok(comodin.implies(viejo));
        ok(viejo.implies(comodin));
        ok(viejo.implies(p));
        // Pero siguen siendo permisos distintos: `equals` mira el nombre sin canonizar.
        ok(!viejo.equals(comodin));
        ok(new RuntimePermission("*").implies(p));
        ok(new RuntimePermission("*").implies(comodin));

        ok(p.equals(new RuntimePermission("exitVM.0")));
        ok(p.hashCode() == new RuntimePermission("exitVM.0").hashCode());
        ok(!p.equals(new RuntimePermission("exitVM.1")));

        // El tipo separa los espacios de nombres: un permiso de otra clase nunca queda implicado.
        ok(!new RuntimePermission("*").implies(new AllPermission()));

        try {
            new RuntimePermission("");
            ok(false);
        } catch (IllegalArgumentException e) {
            ok(true);
        }
        try {
            new RuntimePermission(null);
            ok(false);
        } catch (NullPointerException e) {
            ok(true);
        }
    }

    // ---- IO ----

    // Lo natural seria desviar `System.out` a un `ByteArrayOutputStream` y mirar los bytes, pero en
    // KajiJDK no se puede: `PrintStream.println`/`writeString` son intrinsecos de la VM que
    // escriben a la salida del proceso sin pasar por el `OutputStream` de abajo, asi que ningun
    // `PrintStream` captura nada. Un `ok(...)` sobre el buffer daria siempre falso y estaria
    // midiendo eso y no a `IO`.
    //
    // Entonces la comprobacion es la salida misma: estas cuatro lineas se imprimen igual en las dos
    // VMs y se comparan al correr la prueba de los dos lados. Lo que se verifica es que `IO`
    // delega en `System.out` sin agregar ni comerse nada — incluido el `null`, que se imprime como
    // "null" y no explota, y el `println()` pelado, que es una linea vacia.
    static void io() {
        IO.print("[io] a");
        IO.println("b");
        IO.println();
        IO.println(null);
    }

    public static int run() throws Exception {
        comando();
        directorioYError();
        formasDeRedirect();
        redireccionesDelBuilder();
        entorno();
        permisos();
        io();
        return primerFallo;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(run());
    }
}
