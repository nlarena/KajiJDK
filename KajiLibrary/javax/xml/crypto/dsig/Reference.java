package javax.xml.crypto.dsig;

import java.io.InputStream;
import java.util.List;
import javax.xml.crypto.Data;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.Reference -- un dato cubierto por la firma.
 *
 * <p>Apunta a algo por URI, dice que transformaciones aplicarle, y guarda el resumen del resultado.
 * Validar una referencia es rehacer ese camino y comparar el resumen.
 *
 * <h2>Los dos resumenes</h2>
 *
 * <p>{@link #getDigestValue} es el que <b>dice el documento</b> y {@link #getCalculatedDigestValue}
 * el que se obtuvo al validar. Que sean dos metodos distintos es lo que permite diagnosticar: si no
 * coinciden, el dato cambio despues de firmarse.
 *
 * <p>{@link #getDigestInputStream} y {@link #getDereferencedData} sirven para lo mismo un nivel mas
 * abajo: muestran que datos se resolvieron y que bytes entraron al resumen. Sin ellos, una referencia
 * que no valida es una pared.
 *
 * <p>Los tres devuelven algo util solo <b>despues</b> de validar, y null antes.
 */
public interface Reference extends URIReference, XMLStructure {

    /** Las transformaciones, en orden de aplicacion. No modificable. */
    List<Transform> getTransforms();

    /** Con que algoritmo se resume. */
    DigestMethod getDigestMethod();

    /** El identificador del elemento, o null. */
    String getId();

    /** El resumen que dice el documento. */
    byte[] getDigestValue();

    /**
     * El resumen que se calculo al validar.
     *
     * @return null si todavia no se valido
     */
    byte[] getCalculatedDigestValue();

    /**
     * Si esta referencia cierra.
     *
     * @throws XMLSignatureException si no se pudo resolver o transformar
     */
    boolean validate(XMLValidateContext validateContext) throws XMLSignatureException;

    /**
     * Los datos que el URI resolvio.
     *
     * @return null si todavia no se resolvio
     */
    Data getDereferencedData();

    /**
     * Los bytes que entraron al resumen.
     *
     * @return null si todavia no se calculo
     */
    InputStream getDigestInputStream();
}
