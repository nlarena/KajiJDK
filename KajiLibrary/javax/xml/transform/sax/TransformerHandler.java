package javax.xml.transform.sax;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * KajiLibrary's javax.xml.transform.sax.TransformerHandler -- transforma lo que le va llegando.
 *
 * <p>Es la pieza que hace que una transformacion se pueda poner <b>en el medio de una cadena SAX</b>:
 * recibe el documento por eventos y escribe el resultado en el {@link Result} que se le haya puesto.
 * Con eso, leer, transformar y escribir pasan a la vez y ninguno de los pasos intermedios existe en
 * memoria.
 *
 * <p>Implementa las <b>tres</b> interfaces de entrada de SAX --contenido, lexica y DTD-- y eso no es
 * exceso de celo: si solo recibiera contenido, los comentarios y las secciones CDATA del original
 * desaparecerian de la salida, y una hoja de estilo tiene toda la autoridad para decidir que hacer
 * con ellos. Recibir la DTD importa por las notaciones y las entidades no analizadas, que tambien
 * son parte del documento.
 *
 * <p>{@link #setResult} tiene que llamarse <b>antes</b> del primer evento: sin destino no hay adonde
 * escribir, y por eso lanza si el resultado no sirve en vez de esperar a fallar en el medio.
 *
 * <p>{@link #getTransformer} devuelve el transformador de adentro, y esta para poder ponerle
 * parametros y propiedades de salida antes de arrancar -- no para transformar algo por separado.
 */
public interface TransformerHandler extends ContentHandler, LexicalHandler, DTDHandler {

    /**
     * Adonde va el resultado.
     *
     * <p>Antes del primer evento; ver la nota de la clase.
     *
     * @throws IllegalArgumentException si ese destino no sirve para esta implementacion
     */
    void setResult(Result result) throws IllegalArgumentException;

    /**
     * De donde viene el documento.
     *
     * <p>Contra esto se resuelve lo relativo que aparezca durante la transformacion.
     */
    void setSystemId(String systemID);

    /** Ver {@link #setSystemId}. */
    String getSystemId();

    /** El transformador de adentro, para configurarlo. Ver la nota de la clase. */
    Transformer getTransformer();
}
