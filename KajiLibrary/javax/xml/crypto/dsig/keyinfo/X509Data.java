package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.X509Data -- informacion de clave por certificado.
 *
 * <p>El elemento mas usado de {@link KeyInfo}. Su contenido es una lista <b>heterogenea</b>: puede
 * traer certificados, listas de revocacion, nombres de sujeto, pares emisor-serie, o los bytes de un
 * certificado en crudo. Por eso {@link #getContent} devuelve {@code List<?>} sin tipar -- es lo que
 * el estandar permite.
 *
 * <p>Es el que mas se acerca a ser una fuente de confianza legitima, y sigue sin serlo por si solo:
 * un certificado adentro de la firma vale lo que valga la <b>cadena</b> que lo lleva hasta un ancla
 * en la que uno confia. Validar la firma y no validar la cadena es tener una firma valida de
 * cualquiera.
 *
 * <p>{@link #RAW_X509_CERTIFICATE_TYPE} nombra la forma cruda --los bytes DER sin envolver-- que se
 * usa cuando el certificado va como contenido de otro elemento.
 */
public interface X509Data extends XMLStructure {

    /** El URI de tipo de este elemento. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#X509Data";

    /** El de un certificado en crudo. */
    static final String RAW_X509_CERTIFICATE_TYPE =
        "http://www.w3.org/2000/09/xmldsig#rawX509Certificate";

    /** Lo que trae adentro; heterogeneo. No modificable. Ver la nota de la clase. */
    List<?> getContent();
}
