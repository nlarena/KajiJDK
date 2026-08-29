import java.util.concurrent.Callable;

/**
 * The `Exceptions` attribute is WRITTEN but not READ BACK: an override that declares the same
 * checked exception as the method it implements is rejected when that method comes from a class
 * file on the classpath.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_256.java
 *
 * Expected: compiles. `java.util.concurrent.Callable.call()` is declared `throws Exception`, in
 * the source AND in the emitted class file -- `bin/jvm.exe -v` on `Callable.class` prints the
 * attribute, byte for byte what the JDK prints.
 *
 * Actual: `error: `call` declara lanzar `Exception`, mas ancho que lo que permite `Callable`
 * (§8.4.8.3)` -- the checker sees an empty throws clause on the inherited method.
 *
 * `Local`/`Mine` are the control: the same shape with the interface in THIS file compiles, so the
 * attribute is lost crossing the class-file boundary, not when it is written.
 *
 * Blast radius: no cross-file implementation of any throwing interface method can be written.
 * `Callable` is the one that matters most -- it is how every task in java.util.concurrent is
 * expressed.
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
