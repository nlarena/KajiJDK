package javax.xml.crypto;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.AlgorithmMethod -- an algorithm named by URI, with its parameters.
 *
 * <p>Two methods, and together they are the whole way XML-DSig names cryptography: a <b>URI</b>
 * that identifies the algorithm and an optional {@link AlgorithmParameterSpec} with whatever that
 * algorithm needs.
 *
 * <p>Naming by URI and not by a short string --{@code "SHA-256"}-- is what allows anyone to define
 * a new algorithm without asking permission or clashing with anyone. The price is very long URIs
 * and the classic trap: two different URIs for the same algorithm, depending on which specification
 * it came from.
 */
public interface AlgorithmMethod {

    /** The URI that identifies the algorithm. */
    String getAlgorithm();

    /** Its parameters, or null if it takes none. */
    AlgorithmParameterSpec getParameterSpec();
}
