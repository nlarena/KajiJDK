import java.lang.annotation.AnnotationFormatError;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.Documented;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.annotation.Native;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Prueba de comportamiento de java.lang.annotation. Corre igual en la VM real y en la nuestra:
// run() devuelve -1 si todo pasa, o el indice de la primera comprobacion que fallo.
//
// Los mensajes que se comparan son los que el JDK **especifica** en el codigo de las excepciones,
// no formatos casuales: por eso valen como prueba y no como transcripcion de lo que salio.
public class AnnoTest {

    // El campo existe solo para mirar como sale del compilador: @Native es @Retention(SOURCE), asi
    // que el `.class` no tiene que traer ninguna anotacion sobre el.
    @Native
    static final int MAGIC = 42;

    static int marca;

    public static int run() {
        marca = 0;

        // --- AnnotationFormatError: los tres constructores delegan y nada mas. ---

        AnnotationFormatError e0 = new AnnotationFormatError("roto");
        if (!"roto".equals(e0.getMessage())) return marca;
        marca++;                                                    // 1

        if (e0.getCause() != null) return marca;
        marca++;                                                    // 2

        Throwable causa = new IllegalStateException("abajo");
        AnnotationFormatError e1 = new AnnotationFormatError("arriba", causa);
        if (!"arriba".equals(e1.getMessage())) return marca;
        marca++;                                                    // 3

        if (e1.getCause() != causa) return marca;
        marca++;                                                    // 4

        // El constructor de una sola causa toma el toString() de la causa como mensaje; es la
        // convencion de Throwable y lo que distingue "envolver" de "describir".
        AnnotationFormatError e2 = new AnnotationFormatError(causa);
        if (!causa.toString().equals(e2.getMessage())) return marca;
        marca++;                                                    // 5

        if (e2.getCause() != causa) return marca;
        marca++;                                                    // 6

        // Error y no Exception: no se atrapa con `catch (Exception)`.
        if (!(e0 instanceof Error)) return marca;
        marca++;                                                    // 7

        boolean atrapado = false;
        try {
            throw e0;
        } catch (Exception ex) {
            atrapado = true;
        } catch (Error err) {
            atrapado = false;
        }
        if (atrapado) return marca;
        marca++;                                                    // 8

        // --- AnnotationTypeMismatchException ---

        // El JDK documenta que los dos argumentos pueden ser null; el mensaje sale con el literal
        // "null" en lugar de explotar.
        AnnotationTypeMismatchException m0 = new AnnotationTypeMismatchException(null, "int");
        if (!"Incorrectly typed data found for annotation element null (Found data of type int)"
                .equals(m0.getMessage())) return marca;
        marca++;                                                    // 9

        if (m0.element() != null) return marca;
        marca++;                                                    // 10

        if (!"int".equals(m0.foundType())) return marca;
        marca++;                                                    // 11

        AnnotationTypeMismatchException m1 = new AnnotationTypeMismatchException(null, null);
        if (!"Incorrectly typed data found for annotation element null (Found data of type null)"
                .equals(m1.getMessage())) return marca;
        marca++;                                                    // 12

        if (m1.foundType() != null) return marca;
        marca++;                                                    // 13

        // Con un Method de verdad se compara identidad, no texto: el toString() de un Method es un
        // formato de la implementacion y no algo que esta prueba deba fijar.
        Method met;
        try {
            met = AnnoTest.class.getDeclaredMethod("run", new Class[0]);
        } catch (Throwable t) {
            return marca;
        }
        AnnotationTypeMismatchException m2 = new AnnotationTypeMismatchException(met, "String");
        if (m2.element() != met) return marca;
        marca++;                                                    // 14

        // Aun sin fijar el formato del Method, el mensaje tiene que envolverlo con el prefijo y el
        // sufijo que el JDK especifica.
        String msg2 = m2.getMessage();
        if (msg2 == null) return marca;
        marca++;                                                    // 15

        if (!msg2.startsWith("Incorrectly typed data found for annotation element ")) return marca;
        marca++;                                                    // 16

        if (!msg2.endsWith(" (Found data of type String)")) return marca;
        marca++;                                                    // 17

        if (!(m2 instanceof RuntimeException)) return marca;
        marca++;                                                    // 18

        // --- IncompleteAnnotationException ---

        IncompleteAnnotationException i0 =
                new IncompleteAnnotationException(Documented.class, "value");
        if (!"java.lang.annotation.Documented missing element value".equals(i0.getMessage()))
            return marca;
        marca++;                                                    // 19

        if (i0.annotationType() != Documented.class) return marca;
        marca++;                                                    // 20

        if (!"value".equals(i0.elementName())) return marca;
        marca++;                                                    // 21

        if (!(i0 instanceof RuntimeException)) return marca;
        marca++;                                                    // 22

        // Los dos argumentos se desreferencian en el super(), asi que null falla temprano.
        boolean npe = false;
        try {
            new IncompleteAnnotationException(null, "value");
        } catch (NullPointerException npeEx) {
            npe = true;
        } catch (Throwable t) {
            npe = false;
        }
        if (!npe) return marca;
        marca++;                                                    // 23

        npe = false;
        try {
            new IncompleteAnnotationException(Documented.class, null);
        } catch (NullPointerException npeEx) {
            npe = true;
        } catch (Throwable t) {
            npe = false;
        }
        if (!npe) return marca;
        marca++;                                                    // 24

        // --- @Native ---

        // Es un tipo anotacion de verdad: ACC_ANNOTATION puesto y superinterfaz Annotation. Las dos
        // mitades hacen falta y en su momento fallaron por separado (ver COMPILER_FINDINGS).
        if (!Native.class.isAnnotation()) return marca;
        marca++;                                                    // 25

        if (!java.lang.annotation.Annotation.class.isAssignableFrom(Native.class)) return marca;
        marca++;                                                    // 26

        // Retention SOURCE: el compilador no la tiene que emitir al `.class`, asi que el campo
        // llega a la reflexion sin ninguna anotacion encima.
        //
        // OJO: en NUESTRA VM estas dos comprobaciones son debiles y pasan por dos razones que no
        // son la que se quiere probar -- `Field.getDeclaredAnnotations()` devuelve el arreglo vacio
        // sin mirar el `.class` (subconjunto declarado en java/lang/reflect/Field.java), y ademas
        // el finding #327 hace que ninguna anotacion resuelta desde el classpath se emita. Valen
        // como prueba contra el JDK real. Que @Native efectivamente NO se emite se verifico aparte,
        // sobre los bytes: el campo `b` de un `@Native static final int b` sale del compilador sin
        // atributo `RuntimeVisibleAnnotations`, mientras que uno con una anotacion RUNTIME del
        // mismo archivo si lo lleva.
        Field campo;
        try {
            campo = AnnoTest.class.getDeclaredField("MAGIC");
        } catch (Throwable t) {
            return marca;
        }
        if (campo.getDeclaredAnnotations().length != 0) return marca;
        marca++;                                                    // 27

        if (campo.getAnnotations().length != 0) return marca;
        marca++;                                                    // 28

        // Y el campo sigue siendo un campo normal: la anotacion no le cambio el valor.
        if (MAGIC != 42) return marca;
        marca++;                                                    // 29

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
