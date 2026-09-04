package javax.print;

import java.io.IOException;

/**
 * KajiLibrary's javax.print.MultiDoc -- varios documentos en un solo trabajo.
 *
 * <p>Es una lista enlazada y no una coleccion, y eso llama la atencion. La razon es que los documentos
 * pueden llegar de a poco: {@link #next} puede <b>bloquear</b> esperando el siguiente, y la cantidad
 * puede no saberse de antemano. Con una {@code List} habria que tenerlos todos antes de empezar.
 *
 * <p>{@link #next} devuelve null cuando no hay mas.
 *
 * <p>Igual que {@link Doc}, los dos metodos tienen que devolver siempre lo mismo: recorrerlo dos veces
 * tiene que dar los mismos objetos.
 */
public interface MultiDoc {

    /**
     * El documento actual.
     *
     * @throws IOException si no se pudo obtener
     */
    Doc getDoc() throws IOException;

    /**
     * El resto, o null si este era el ultimo. Puede bloquear.
     *
     * @throws IOException si no se pudo obtener
     */
    MultiDoc next() throws IOException;
}
