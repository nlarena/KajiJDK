package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.Manifest -- una lista de referencias con validacion propia.
 *
 * <p>Un grupo de {@link Reference} que la firma cubre <b>como conjunto</b>: el {@code SignedInfo}
 * tiene una sola referencia al manifiesto, y el manifiesto tiene el resto.
 *
 * <p>La diferencia que lo justifica es de <b>politica de fallo</b>. Una referencia del
 * {@code SignedInfo} que no cierra invalida la firma entera. Una referencia de un manifiesto no: la
 * biblioteca la valida y reporta, y quien usa el API decide que hacer.
 *
 * <p>Eso sirve cuando se firman muchos archivos y falten algunos es aceptable -- un paquete de
 * documentos donde cada uno se verifica por separado. Y es una trampa si nadie mira los resultados:
 * la firma valida y las referencias del manifiesto pueden estar todas rotas.
 */
public interface Manifest extends XMLStructure {

    /** El URI de tipo de este elemento. */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#Manifest";

    /** El identificador; es lo que la referencia del {@code SignedInfo} apunta. */
    String getId();

    /** Las referencias del manifiesto. No modificable y nunca vacia. */
    List<Reference> getReferences();
}
