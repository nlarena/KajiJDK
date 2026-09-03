package java.nio.charset.spi;

import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * KajiLibrary's java.nio.charset.spi.CharsetProvider -- codificaciones que no trae la plataforma.
 *
 * <p>Dos metodos, y son dos preguntas distintas que conviene no confundir:
 * {@link #charsetForName} resuelve <b>una</b> por nombre, y {@link #charsets} enumera las que este
 * proveedor ofrece. La resolucion no se implementa recorriendo la enumeracion, y por eso son dos: un
 * proveedor puede reconocer nombres que no enumera.
 *
 * <p>Eso pasa de verdad y no es un detalle. Una codificacion tiene un nombre canonico y una lista de
 * <b>alias</b> --{@code UTF-8} tambien se llama {@code unicode-1-1-utf-8}, y hay decenas de nombres
 * historicos-- y {@code charsetForName} tiene que reconocerlos todos aunque enumere una sola. Al
 * reves seria absurdo: enumerar cada alias como si fuera una codificacion distinta le mostraria a
 * quien pregunta veinte entradas que son la misma.
 *
 * <p>La busqueda no distingue mayusculas, y la enumeracion no puede repetir la misma codificacion.
 */
public abstract class CharsetProvider {

    /** Para las subclases. */
    protected CharsetProvider() {
    }

    /**
     * Las codificaciones de este proveedor.
     *
     * <p>Una por codificacion, no una por alias; ver la nota de la clase.
     */
    public abstract Iterator<Charset> charsets();

    /**
     * La codificacion con ese nombre o alias.
     *
     * @param charsetName sin distinguir mayusculas de minusculas
     * @return null si este proveedor no la conoce, que no es un error: se le pregunta al que sigue
     */
    public abstract Charset charsetForName(String charsetName);
}
