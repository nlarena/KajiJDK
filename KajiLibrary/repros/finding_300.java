// Repro de #300 - un tipo CALIFICADO en un `catch` no resolvia, y el handler se descartaba.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_300.java
//   bin\run-headless.exe KajiLibrary\repros\finding_300.class calificadoEnCatch
//
// ANTES el archivo NO compilaba, con un error que apunta al lugar equivocado:
//
//   error: excepcion chequeada `IOException` sin capturar ni declarar en `throws`
//       tira();
//       ^
//
// El `try` de arriba SI la atrapaba. Lo que pasaba es que el tipo del `catch`, escrito calificado,
// no resolvia a nada, asi que el handler no entraba en el conjunto de excepciones manejadas y el
// `throws` del metodo llamado quedaba sin cubrir.
//
// CAUSA: `check.rs::resolve_exc` tenia su propia resolucion de nombres, de dos lineas:
//
//   table.resolve_type(scope, name).or_else(|| table.external(name))
//
// Las dos claves son por nombre **simple**. Un `java.io.IOException` no esta en ninguna de las dos
// --los externos se clavean por el ultimo segmento-- asi que devolvia None. Y `None` ahi no es un
// error: es "este catch no atrapa nada".
//
// Lo peculiar del sintoma, y lo que lo hacia dificil de leer: solo aparece cuando el `throws` y el
// `catch` se escriben con **formas distintas** del mismo nombre. Con los dos calificados tampoco
// resolvia ninguno, pero entonces el `throws` del metodo que encierra tampoco cubria nada y el
// error salia igual; con los dos simples andaba todo. O sea que el error dependia del estilo de
// escritura y no de la semantica.
//
// AHORA: `resolve_exc` va por la resolucion **general** de tipos (`attribute::resolve_rtype`), que
// es la que ya sabe de nombres calificados, anidados y del paquete propio. Una resolucion menos que
// mantener aparte.
//
// El error era **espurio**: el bytecode del handler lo emite el codegen, que resuelve por otro
// camino, asi que lo que se rechazaba era fuente valido. No habia riesgo de una excepcion tragada
// en tiempo de ejecucion.
//
// `calificadoEnCatch` -> 1, `simpleEnCatch` -> 1, `anidadoEnCatch` -> 1, `noAtrapaDeMas` -> 2.
import java.io.IOException;

public class finding_300 {

    static void tira() throws IOException {
        throw new IOException("x");
    }

    static void tiraOtra() throws IllegalStateException {
        throw new IllegalStateException("y");
    }

    // El caso del finding: `throws` simple, `catch` calificado.
    public static int calificadoEnCatch() {
        try {
            tira();
        } catch (java.io.IOException e) {
            return 1;
        }
        return 0;
    }

    // Control: los dos simples, que siempre anduvo.
    public static int simpleEnCatch() {
        try {
            tira();
        } catch (IOException e) {
            return 1;
        }
        return 0;
    }

    // Un tipo ANIDADO calificado en el catch, que es el otro nombre que la resolucion propia no
    // sabia armar.
    public static int anidadoEnCatch() {
        try {
            throw new Caja.Falla();
        } catch (Caja.Falla e) {
            return 1;
        }
    }

    // Y el control que importa del otro lado: un catch calificado tiene que seguir sin atrapar lo
    // que no le corresponde. Una `IllegalStateException` no la agarra un
    // `catch (NumberFormatException)`, aunque las dos sean RuntimeException.
    //
    // El interno va con una **no chequeada** a proposito: atrapar una chequeada que el `try` no
    // puede lanzar es un error de compilacion en Java (y el javac real lo rechaza), asi que el
    // control tenia que escribirse con una que no lo sea.
    public static int noAtrapaDeMas() {
        try {
            try {
                tiraOtra();
            } catch (java.lang.NumberFormatException e) {
                return 0;
            }
        } catch (IllegalStateException e) {
            return 2;
        }
        return 0;
    }
}

class Caja {

    static final class Falla extends RuntimeException {
    }
}
