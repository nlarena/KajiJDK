package javax.xml.crypto.dsig.keyinfo;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.PGPData -- informacion de clave del mundo PGP.
 *
 * <p>Lleva un identificador de clave PGP, un paquete de clave, o los dos, mas lo que la aplicacion
 * quiera agregar en {@link #getExternalElements}.
 *
 * <p>Es el rincon menos usado del paquete: XML-DSig salio cuando PGP y X.509 competian, y este
 * elemento existe por esa epoca. Casi ninguna implementacion lo maneja de verdad, y una que lo
 * encuentre normalmente lo ignora -- lo cual esta bien, porque un {@code KeyInfo} con contenido que
 * no se entiende no invalida la firma.
 *
 * <p>Los bytes son de los formatos de OpenPGP, no de XML: el elemento los lleva en base 64.
 */
public interface PGPData extends XMLStructure {

    /** El URI de tipo de este elemento. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#PGPData";

    /** El identificador de la clave PGP, o null. */
    byte[] getKeyId();

    /** El paquete de clave PGP, o null. */
    byte[] getKeyPacket();

    /** Lo que la aplicacion haya agregado. No modificable. */
    List<XMLStructure> getExternalElements();
}
