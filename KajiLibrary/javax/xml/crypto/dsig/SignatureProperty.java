package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignatureProperty -- un dato <b>sobre</b> la firma.
 *
 * <p>No sobre lo firmado: sobre el acto de firmar. El caso tipico es el momento en que se firmo, o
 * con que dispositivo.
 *
 * <p>{@link #getTarget} es obligatorio y dice a que firma se refiere la propiedad, apuntando por URI
 * al elemento de la firma. Hace falta porque las propiedades viven adentro de un {@code Object}, y un
 * {@code Object} puede estar en un documento con varias firmas.
 *
 * <p>Para que la propiedad este protegida hay que apuntarle una {@link Reference}. Una marca de
 * tiempo no firmada la puede cambiar cualquiera, que es justamente lo contrario de para lo que se la
 * pone.
 */
public interface SignatureProperty extends XMLStructure {

    /** A que firma se refiere. Obligatorio. */
    String getTarget();

    /** El identificador del elemento, o null. */
    String getId();

    /** El contenido de la propiedad. No modificable y nunca vacio. */
    List<XMLStructure> getContent();
}
