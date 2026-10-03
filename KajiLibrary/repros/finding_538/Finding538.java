/**
 * #538 -- un {@code switch} sobre un enum se baja bien solo si el enum es de nivel superior y esta
 * en la misma unidad. Las otras tres formas fallan, y con mensajes que apuntan a otro lado.
 *
 * <p>El {@code switch} sobre enum se baja a un {@code $SwitchMap}: un {@code int[]} sintetico que
 * vive en una clase {@code Unidad$1} generada al vuelo, poblada en su {@code <clinit>} con
 * {@code Enum.values()} y {@code Enum.CONST.ordinal()}. De ahi salen los dos defectos.
 *
 * <p><b>1. El enum anidado.</b> Esas expresiones se escriben dentro de {@code Unidad$1}, que es
 * hermana de la unidad y <b>no</b> del enum, asi que el nombre simple no resuelve. El error era
 * <em>"no se encuentra el simbolo: E, ubicacion: clase Finding538$1"</em> -- sobre una clase que el
 * usuario no escribio y un tipo que esta a la vista en su archivo.
 *
 * <p><b>2. El enum del classpath.</b> La lista de constantes se armaba recorriendo las
 * declaraciones <b>de la unidad</b>, asi que un enum leido de un {@code .class} no figuraba: el
 * {@code switch} no se reconocia como de enum, no se bajaba, y el emisor cortaba con
 * <em>"no soporta un switch cuyo selector no es int"</em> -- un mensaje sobre el generador para un
 * problema del desugar.
 *
 * <p>El JDK 25 imprime las cuatro lineas.
 */
public class Finding538 {

    enum E { A, B, C }

    static class Interna {
        enum F { X, Y }
    }

    // --- fallaba: enum anidado ---
    static int anidado(E e) {
        switch (e) {
            case A: return 1;
            case B: return 2;
            default: return 3;
        }
    }

    // --- fallaba: enum anidado dos niveles ---
    static int doble(Interna.F f) {
        switch (f) {
            case X: return 10;
            default: return 20;
        }
    }

    // --- fallaba: enum leido del classpath ---
    static int delClasspath(Color538 c) {
        switch (c) {
            case ROJO: return 100;
            default: return 200;
        }
    }

    // --- andaba: el unico caso que el repro viejo cubria ---
    static int conFlecha(E e) {
        switch (e) {
            case A -> { return 1000; }
            default -> { return 2000; }
        }
    }

    public static int run() {
        System.out.println("//anidado " + anidado(E.B));
        System.out.println("//doble " + doble(Interna.F.Y));
        System.out.println("//classpath " + delClasspath(Color538.ROJO));
        System.out.println("//flecha " + conFlecha(E.A));
        return 0;
    }

    public static void main(String[] a) {
        run();
    }
}
