package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignature -- una firma XML completa.
 *
 * <p>La estructura central del paquete. Tiene tres partes y conviene tenerlas claras:
 *
 * <ul>
 *   <li>el {@link SignedInfo} -- <b>lo unico que se firma</b>. Contiene las referencias a los datos y
 *       los algoritmos usados;
 *   <li>el {@link SignatureValue} -- la firma criptografica del {@code SignedInfo};
 *   <li>el {@link KeyInfo} -- opcional, y ver su nota: informacion, no autoridad.
 * </ul>
 *
 * <h2>La firma cubre el SignedInfo, no los datos</h2>
 *
 * <p>Es la parte que hay que entender de XML-DSig y la que produce todos los malentendidos: la firma
 * se calcula sobre el {@code SignedInfo}, y el {@code SignedInfo} contiene el <b>resumen</b> de cada
 * dato. Los datos en si no se firman directamente.
 *
 * <p>De ahi salen dos consecuencias. La buena: se puede firmar cualquier cosa, incluso algo externo
 * al documento. La peligrosa: {@link #validate} devuelve true si la firma del {@code SignedInfo}
 * cierra <b>y</b> todos los resumenes cierran, pero eso no dice nada sobre <b>que</b> se firmo. Una
 * firma valida sobre una referencia que apunta a otra cosa es una firma valida.
 *
 * <p>Por eso, despues de validar hay que mirar dos cosas mas: con que clave se valido
 * ({@link #getKeySelectorResult}) y que cubren las referencias.
 */
public interface XMLSignature extends XMLStructure {

    /** El espacio de nombres de XML-DSig. */
    static final String XMLNS = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Valida la firma.
     *
     * <p>Ver la nota de la clase: true no significa que se firmo lo que uno cree.
     *
     * @throws XMLSignatureException si la validacion no se pudo hacer
     */
    boolean validate(XMLValidateContext validateContext) throws XMLSignatureException;

    /** Lo que la firma dice sobre su clave, o null. */
    KeyInfo getKeyInfo();

    /** Lo que de verdad se firmo. */
    SignedInfo getSignedInfo();

    /** Los objetos que la firma lleva adentro. No modificable. */
    List<XMLObject> getObjects();

    /** El identificador del elemento, o null. */
    String getId();

    /** La firma criptografica. */
    SignatureValue getSignatureValue();

    /**
     * Calcula la firma y la deja en el contexto.
     *
     * @throws MarshalException si no se pudo escribir el XML
     * @throws XMLSignatureException si no se pudo firmar
     */
    void sign(XMLSignContext signContext) throws MarshalException, XMLSignatureException;

    /**
     * Con que clave se valido.
     *
     * <p>Es lo que hay que comparar contra la lista de confianza; ver la nota de la clase.
     *
     * @return null si todavia no se valido
     */
    KeySelectorResult getKeySelectorResult();

    /**
     * El valor de la firma.
     *
     * <p>Tiene su propio {@link #validate} porque una firma puede fallar de dos formas distintas: el
     * valor criptografico no cierra, o alguna referencia no cierra. Poder preguntarlas por separado
     * es la unica forma de saber cual fallo, y eso cambia el diagnostico -- lo primero es una clave
     * equivocada o un documento alterado; lo segundo, un dato alterado.
     */
    public static interface SignatureValue extends XMLStructure {

        /** El identificador del elemento, o null. */
        String getId();

        /** Los bytes de la firma. */
        byte[] getValue();

        /**
         * Si el valor criptografico cierra, sin mirar las referencias.
         *
         * @throws XMLSignatureException si la validacion no se pudo hacer
         */
        boolean validate(XMLValidateContext validateContext) throws XMLSignatureException;
    }
}
