package repros.finding_537;

import java.util.AbstractSet;

/**
 * Un metodo {@code static} de una interfaz se heredaba, y no se hereda nunca (JLS 9.4.1).
 *
 * <p>Dos sintomas, y ninguno nombra la causa:
 *
 * <ol>
 *   <li>Un {@code static} propio con menor visibilidad que uno homonimo de una superinterfaz se
 *       rechaza por <em>"reduce la visibilidad heredada"</em>. No hay tal herencia.
 *   <li>Con la misma visibilidad --el caso de {@code EnumSet.of}-- la llamada resuelve contra el
 *       heredado: {@code EnumSet.of(x)} elegia {@code Set.of} y devolvia {@code Set<E>} en vez de
 *       {@code EnumSet<E>}, o sea "tipo incompatible" en el destino, sin mencionar a {@code Set}
 *       en ningun lado.
 * </ol>
 *
 * <p>El JDK compila las dos formas. Lo que lo hacia dificil de ver es que el sintoma depende de
 * <strong>que la superinterfaz tenga un estatico con ese nombre</strong>: con un nombre cualquiera
 * anda, y {@code of} es justo el nombre que {@code Set}, {@code List} y {@code Map} usan desde
 * Java 9.
 */
public class Finding537 {

    enum Color { RED, GREEN }

    /** Hereda de una clase del classpath que trae `Set.of` por la interfaz. */
    abstract static class Caja<E> extends AbstractSet<E> {
        /** Mismo nombre que `Set.of`, y de paquete: antes daba "reduce la visibilidad". */
        static <E> Caja<E> of(E e) {
            return null;
        }
    }

    public static int run() {
        // El caso 2, con la biblioteca de verdad.
        java.util.EnumSet<Color> a = java.util.EnumSet.of(Color.RED);
        java.util.EnumSet<Color> b = java.util.EnumSet.of(Color.RED, Color.GREEN);
        System.out.println("//tamanos: " + a.size() + " " + b.size());
        return 0;
    }

    public static void main(String[] x) {
        run();
    }
}
