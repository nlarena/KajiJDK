import java.util.*;
import java.util.function.*;

/**
 * Finding #554: adentro de una clase **local o anonima** no se inserta ningun boxing ni unboxing.
 *
 * `TransTypes` (`src/javac/transtypes.rs`) recorre los miembros de las clases de la unidad, pero
 * para cuando corre, `enter` ya convirtio cada anonima en un `StmtKind::LocalClass` adentro del cuerpo
 * del metodo, y `stmt` no tiene brazo para `LocalClass`: el cuerpo de la clase no se visita. El JDK
 * imprime `//554 [1] 41 41`; nosotros compilamos y la VM revienta ("expected a reference, found
 * Int(1)"); la HotSpot tira `VerifyError`. Una lambda con el mismo cuerpo anda.
 */
public class Finding554 {
    public static int run() {
        List<Integer> l = new ArrayList<>();
        Runnable r = new Runnable() { public void run() { l.add(1); } };      // argumento sin boxear
        r.run();
        Supplier<Integer> s = new Supplier<Integer>() { public Integer get() { return 41; } };  // return
        class L implements Supplier<Integer> { public Integer get() { return 41; } }            // local
        System.out.println("//554 " + l + " " + s.get() + " " + new L().get());
        return l.size();
    }

    public static void main(String[] args) { run(); }
}
