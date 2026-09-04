package javax.xml.crypto;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.AlgorithmMethod -- un algoritmo nombrado por URI, con sus
 * parametros.
 *
 * <p>Dos metodos, y juntos son toda la forma en que XML-DSig nombra criptografia: un <b>URI</b> que
 * identifica el algoritmo y un {@link AlgorithmParameterSpec} opcional con lo que ese algoritmo
 * necesite.
 *
 * <p>Que se nombre por URI y no por una cadena corta --{@code "SHA-256"}-- es lo que permite que
 * cualquiera defina un algoritmo nuevo sin pedir permiso ni chocar con nadie. El precio son URIs
 * larguisimos y la trampa clasica: dos URIs distintos para el mismo algoritmo, segun de que
 * especificacion salio.
 */
public interface AlgorithmMethod {

    /** El URI que identifica el algoritmo. */
    String getAlgorithm();

    /** Sus parametros, o null si no lleva. */
    AlgorithmParameterSpec getParameterSpec();
}
