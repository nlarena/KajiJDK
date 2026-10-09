// Repro de #256 - el atributo `Exceptions` se ESCRIBIA pero no se LEIA de vuelta.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_256.java
//
// ANTES: un override que declaraba la misma excepcion chequeada que el metodo que implementa era
// rechazado cuando ese metodo venia de un archivo de clase del CLASSPATH:
//
//   error: `call` declara lanzar `Exception`, mas ancho que lo que permite `Callable` (8.4.8.3)
//
// `java.util.concurrent.Callable.call()` esta declarado `throws Exception` en la fuente Y en el
// class file emitido —`bin\jvm.exe -v` sobre `Callable.class` imprime el atributo, byte por byte
// igual que el JDK—, pero el chequeador veia una clausula `throws` vacia en el metodo heredado.
//
// `Local`/`Mine` son el control: la misma forma con la interfaz en ESTE archivo siempre compilo,
// lo que ubicaba la perdida al cruzar el borde del class file y no al escribirlo.
//
// AHORA: **compila entero**. `#104`/`#256` figuran cerrados en COMPILER_FINDINGS.md — el atributo
// ya se lee. Queda como REGRESION: cubre las dos mitades a la vez, la del classpath y la local.
import java.util.concurrent.Callable;

/**
 * El control del repro. `Local`/`Mine` reproducen la misma forma con la interfaz en ESTE archivo:
 * siempre compilaron, y por eso ubicaban la perdida del atributo al cruzar el borde del class
 * file y no al escribirlo. Se conservan porque una regresion podria romper una mitad sola.
 */
public class finding_256 implements Callable<String> {

    @Override
    public String call() throws Exception {
        return "x";
    }
}

interface Local {
    String call() throws Exception;
}

class Mine implements Local {
    public String call() throws Exception {
        return "x";
    }
}
