package javax.xml.crypto.dsig;

import java.io.InputStream;
import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.SignedInfo -- lo unico que la firma cubre.
 *
 * <p>Ver la nota de {@link XMLSignature}: la firma criptografica se calcula sobre esto, y esto
 * contiene los resumenes de los datos. Todo lo que este adentro esta protegido; todo lo que no, no.
 *
 * <p>Lleva los dos algoritmos --como canonicalizar y como firmar-- adentro de lo firmado, y eso es
 * deliberado: si el algoritmo estuviera afuera, un atacante podria cambiarlo por uno debil sin
 * romper la firma.
 *
 * <p>{@link #getCanonicalizedData} devuelve exactamente los bytes que se firmaron. Es la unica forma
 * de depurar una firma que no valida: comparar esos bytes de los dos lados muestra en que difiere la
 * canonicalizacion, que es la causa mas comun.
 */
public interface SignedInfo extends XMLStructure {

    /** Como se convierte a bytes lo que se firma. */
    CanonicalizationMethod getCanonicalizationMethod();

    /** Con que algoritmo se firma. */
    SignatureMethod getSignatureMethod();

    /** Los datos cubiertos, uno por referencia. No modificable y nunca vacia. */
    List<Reference> getReferences();

    /** El identificador del elemento, o null. */
    String getId();

    /**
     * Los bytes que de verdad se firmaron.
     *
     * @return null si todavia no se calcularon
     */
    InputStream getCanonicalizedData();
}
