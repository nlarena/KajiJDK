package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLObject -- contenido que viaja adentro de la firma.
 *
 * <p>Un contenedor libre: puede llevar cualquier XML. Es el mecanismo de extension de XML-DSig, y se
 * usa sobre todo para dos cosas: llevar los datos firmados adentro de la propia firma --la firma
 * <b>envolvente</b>-- y llevar propiedades sobre la firma, como el momento en que se hizo.
 *
 * <p>Vale insistir en algo: estar adentro de la firma <b>no</b> significa estar firmado. Un
 * {@code Object} solo queda cubierto si alguna {@link Reference} lo apunta. Es la confusion mas
 * frecuente del paquete, y produce firmas donde el dato interesante no esta protegido.
 */
public interface XMLObject extends XMLStructure {

    /** El URI de tipo de este elemento. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#Object";

    /** Lo que lleva adentro. No modificable. */
    List<XMLStructure> getContent();

    /** El identificador; es lo que una {@link Reference} apunta. */
    String getId();

    /** El tipo de contenido, o null. */
    String getMimeType();

    /** Como esta codificado, o null. */
    String getEncoding();
}
