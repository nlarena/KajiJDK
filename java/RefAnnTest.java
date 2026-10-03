import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import jdk.jfr.DataAmount;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Las anotaciones de un MIEMBRO, por reflexion: el finding #505.
 *
 * <p>`java/RefAnn.java` ya era el repro, pero no es un `*Test.java` y por lo tanto **ninguna compuerta
 * lo miraba**. Esto es la mitad que lo cuida, y cubre las tres cosas que el #505 resulto ser:
 *
 * <ol>
 *   <li>el nativo de `Field.declaredAnnotations0()`, que no existia;
 *   <li>`getAnnotations()` de cualquier miembro, que devolvia vacio escrito a mano --tambien para un
 *       **metodo**, que si leia su `getDeclaredAnnotations()`--;
 *   <li>el elemento sin valor ni default, que se salteaba y dejaba la clase sintetica sin ese accesor:
 *       la llamada se iba al abstracto de la `@interface` y la VM panicaba.
 * </ol>
 *
 * <p>Todos los valores esperados salen de correr el mismo fuente en el JDK 25.
 *
 * <p>Devuelve -1 si todo coincide; si no, la cantidad de fallas.
 */
public class RefAnnTest {

    static int fallas = 0;

    static void ok(String que, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + que);
            fallas = fallas + 1;
        }
    }

    static class E extends Event {
        @Label("Ruta") String ruta;
        @Label("Bytes") @DataAmount long bytes;
        String sinAnotar;
    }

    @Label("EnClase") static class C { }

    @Label("EnMetodo") static void metodoAnotado() { }

    public static int run() throws Exception {
        fallas = 0;

        // ---- 1. un campo anotado: las cuatro preguntas contestan lo mismo que el JDK
        Field ruta = E.class.getDeclaredField("ruta");
        ok("campo: getAnnotations da 1", ruta.getAnnotations().length == 1);
        ok("campo: getDeclaredAnnotations da 1", ruta.getDeclaredAnnotations().length == 1);
        ok("campo: isAnnotationPresent", ruta.isAnnotationPresent(Label.class));
        Label l = ruta.getAnnotation(Label.class);
        ok("campo: getAnnotation no es null", l != null);
        ok("campo: y el valor es el escrito", l != null && "Ruta".equals(l.value()));
        ok("campo: getAnnotationsByType da 1", ruta.getAnnotationsByType(Label.class).length == 1);

        // ---- 2. un campo con DOS anotaciones, una de ellas sin valor explicito
        Field bytes = E.class.getDeclaredField("bytes");
        ok("dos anotaciones en un campo", bytes.getAnnotations().length == 2);
        // `@DataAmount` se usa sin valor: el suyo sale del `AnnotationDefault`. Es lo que destapo el
        // #560 --nuestro javac no lo emitia cuando el default es una referencia a una constante-- y
        // por eso en la biblioteca esta escrito como literal.
        DataAmount da = bytes.getAnnotation(DataAmount.class);
        ok("la anotacion sin valor explicito se encuentra", da != null);
        ok("y su default resuelve a BYTES", da != null && "BYTES".equals(da.value()));

        // ---- 3. un campo sin anotar contesta vacio, que es la otra mitad de la respuesta
        Field pelado = E.class.getDeclaredField("sinAnotar");
        ok("campo sin anotar: 0", pelado.getAnnotations().length == 0);
        ok("campo sin anotar: getAnnotation da null", pelado.getAnnotation(Label.class) == null);

        // ---- 4. un METODO: `getAnnotations()` tambien daba 0 aunque el declared diera 1
        Method m = RefAnnTest.class.getDeclaredMethod("metodoAnotado");
        ok("metodo: getAnnotations da 1", m.getAnnotations().length == 1);
        ok("metodo: getDeclaredAnnotations da 1", m.getDeclaredAnnotations().length == 1);
        Label lm = m.getAnnotation(Label.class);
        ok("metodo: el valor es el escrito", lm != null && "EnMetodo".equals(lm.value()));

        // ---- 5. y una CLASE, que ya andaba: el control que dice que no se rompio
        ok("clase: getAnnotations da 1", C.class.getAnnotations().length == 1);
        Label lc = C.class.getAnnotation(Label.class);
        ok("clase: el valor es el escrito", lc != null && "EnClase".equals(lc.value()));

        // ---- 6. el tipo de cada objeto de anotacion es la @interface, no la clase sintetica
        Annotation[] todas = ruta.getAnnotations();
        ok("annotationType es la @interface", todas.length == 1
                && todas[0].annotationType() == Label.class);
        ok("y es una instancia de ella", todas.length == 1 && todas[0] instanceof Label);

        System.out.println("TOTAL " + fallas);
        return fallas == 0 ? -1 : fallas;
    }
}
