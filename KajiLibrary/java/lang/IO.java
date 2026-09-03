package java.lang;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// La entrada/salida de linea para programas chicos (JEP 512, Java 25): `println`, `print` y
// `readln` sin tener que nombrar `System.out` ni armar un `BufferedReader` a mano.
//
// Existe para que el primer programa que alguien escribe no arranque explicando que es un
// `PrintStream`. De ahi salen sus tres decisiones de forma, que no son arbitrarias:
//
//   - No hay `printf`. Un lenguaje de formato cifrado no le sirve a quien recien empieza, y ademas
//     arrastra el problema del `Locale`. El JDK lo dejo afuera a proposito y aca tambien.
//   - Todo es `static` y la clase es `final` con constructor privado: no hay instancia que crear ni
//     estado que configurar.
//   - `readln` devuelve `null` en fin de entrada, no lanza. Es un `while ((s = readln()) != null)`.
//
// Cuidado con mezclar: a partir del primer `readln` el decodificador puede haber consumido de
// `System.in` mas bytes de los que devolvio (el buffer del `InputStreamReader`), asi que leer
// despues directo de `System.in` da resultados impredecibles. Es el contrato del JDK, no un limite
// nuestro: la clase esta pensada para ser la unica que toque la entrada estandar.
public final class IO {

    // No hay instancias. Se tira `Error` y no `UnsupportedOperationException` porque llegar aca es
    // imposible desde codigo compilado —no hay constructor visible— y solo puede pasar por
    // reflexion forzada, que es un error del programa, no una operacion no soportada.
    private IO() {
        throw new Error("no instances");
    }

    public static void println(Object obj) {
        System.out.println(obj);
    }

    public static void println() {
        System.out.println();
    }

    // `print` si vacia el buffer y `println` no. El motivo es el prompt: quien escribe
    // `print("nombre: ")` y despues `readln()` tiene que ver el prompt **antes** de que le pidan
    // tipear, y sin salto de linea el autoflush por linea de `System.out` no lo garantiza. Con
    // `println` ese autoflush ya alcanza.
    public static void print(Object obj) {
        java.io.PrintStream out = System.out;
        out.print(obj);
        out.flush();
    }

    // Devuelve la linea sin el separador, o `null` si la entrada se termino sin leer nada.
    //
    // El `IOException` se envuelve en `IOError` en vez de propagarse: el publico de esta clase no
    // deberia tener que escribir un `try`/`catch` para leer una linea, y una entrada estandar que
    // falla no es una condicion de la que un programa asi se pueda recuperar.
    public static String readln() {
        try {
            return IO.reader().readLine();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public static String readln(String prompt) {
        IO.print(prompt);
        return IO.readln();
    }

    // El lector cacheado. Se toca solo desde `reader()`, que esta sincronizado.
    private static BufferedReader br;

    // El decodificador se arma tarde, en la primera lectura, y no en un inicializador estatico: si
    // el programa nunca lee, no se paga el costo ni se roban bytes de `System.in`.
    //
    // La codificacion sale de `stdin.encoding` y cae en UTF-8 si la propiedad no esta o nombra algo
    // que no existe. Se usa la forma indulgente de `forName` justamente por eso: el valor viene de
    // la configuracion del entorno, y un nombre mal escrito ahi no deberia voltear al programa.
    static synchronized BufferedReader reader() {
        if (IO.br == null) {
            String enc = System.getProperty("stdin.encoding", "");
            Charset cs = Charset.forName(enc, StandardCharsets.UTF_8);
            IO.br = new BufferedReader(new InputStreamReader(System.in, cs));
        }
        return IO.br;
    }
}
