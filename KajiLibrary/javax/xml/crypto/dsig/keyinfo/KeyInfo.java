package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.KeyInfo -- lo que la firma dice sobre su clave.
 *
 * <p>Un contenedor de estructuras heterogeneas: puede traer un nombre de clave, la clave publica en
 * limpio, una cadena de certificados, un puntero a donde buscarla, o nada. Por eso
 * {@link #getContent} devuelve {@code XMLStructure} y no algo mas preciso.
 *
 * <p><b>Es informacion, no autoridad.</b> Lo escribio quien firmo, asi que una firma falsificada trae
 * su propia clave y valida perfecto contra ella. Sirve para <b>elegir</b> entre claves que uno ya
 * conoce, nunca como fuente de la clave. Ver la nota de {@code KeySelector}, que es donde esa
 * decision se toma.
 *
 * <p>Es opcional: una firma sin {@code KeyInfo} es perfectamente valida y significa que quien valida
 * ya sabe cual es la clave. Es, de hecho, la forma mas segura de firmar.
 */
public interface KeyInfo extends XMLStructure {

    /** Lo que trae adentro. No modificable. */
    List<XMLStructure> getContent();

    /** El identificador del elemento, o null. */
    String getId();

    /**
     * Lo escribe adentro de esa estructura.
     *
     * @throws MarshalException si no se puede escribir ahi
     */
    void marshal(XMLStructure parent, XMLCryptoContext context) throws MarshalException;
}
