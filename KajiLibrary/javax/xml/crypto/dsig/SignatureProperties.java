package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignatureProperties -- un grupo de
 * {@link SignatureProperty}.
 *
 * <p>Solo agrupa. Existe porque un {@link Reference} apunta a <b>un</b> elemento, y sin el habria que
 * poner una referencia por propiedad; con el, una sola referencia cubre todas.
 *
 * <p>Vale la misma advertencia que en {@link SignatureProperty}: agrupar no protege. Si nadie apunta
 * al grupo, las propiedades quedan afuera de la firma.
 */
public interface SignatureProperties extends XMLStructure {

    /** El URI de tipo de este elemento. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#SignatureProperties";

    /** El identificador; es lo que una {@link Reference} apunta. */
    String getId();

    /** Las propiedades. No modificable y nunca vacia. */
    List<SignatureProperty> getProperties();
}
