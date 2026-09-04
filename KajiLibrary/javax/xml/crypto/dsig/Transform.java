package javax.xml.crypto.dsig;

import java.io.OutputStream;
import java.security.spec.AlgorithmParameterSpec;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.Data;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.Transform -- un paso del camino entre el dato y su resumen.
 *
 * <p>Las transformaciones se encadenan: la salida de una es la entrada de la siguiente, y lo que sale
 * de la ultima es lo que se resume. Cada una puede convertir nodos en nodos o nodos en bytes, y
 * encadenar dos que no encajan es el error clasico al armar una firma a mano.
 *
 * <p>{@link #ENVELOPED} es la que casi siempre aparece: saca el elemento de la propia firma del
 * documento antes de resumirlo. Sin ella, firmar un documento que va a contener la firma es
 * imposible -- el resumen incluiria la firma que todavia no existe.
 *
 * <p>{@link #XSLT} y {@link #XPATH} son las peligrosas: ejecutan codigo o expresiones que eligio
 * quien firmo. Ver {@code XSLTTransformParameterSpec}.
 *
 * <p>La sobrecarga de {@link #transform(Data, XMLCryptoContext, OutputStream)} escribe ademas a un
 * flujo. Sirve para ver que salio de cada paso, que es como se depura una cadena que no cierra.
 */
public interface Transform extends XMLStructure, AlgorithmMethod {

    /** Decodifica base 64. */
    static final String BASE64 = "http://www.w3.org/2000/09/xmldsig#base64";

    /** Saca el elemento de la firma. Ver la nota de la clase. */
    static final String ENVELOPED = "http://www.w3.org/2000/09/xmldsig#enveloped-signature";

    /** Selecciona nodos con una expresion XPath, nodo por nodo. */
    static final String XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";

    /** Idem, por subarboles: mucho mas rapida. */
    static final String XPATH2 = "http://www.w3.org/2002/06/xmldsig-filter2";

    /** Aplica una hoja de estilo. Ver la nota de la clase. */
    static final String XSLT = "http://www.w3.org/TR/1999/REC-xslt-19991116";

    /** Los parametros de esta transformacion, o null. */
    AlgorithmParameterSpec getParameterSpec();

    /**
     * Aplica la transformacion.
     *
     * @throws TransformException si no se puede aplicar a esos datos
     */
    Data transform(Data data, XMLCryptoContext context) throws TransformException;

    /** Idem, escribiendo tambien a un flujo. Ver la nota de la clase. */
    Data transform(Data data, XMLCryptoContext context, OutputStream os) throws TransformException;
}
