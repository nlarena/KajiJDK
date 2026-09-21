package javax.xml.crypto.dsig.spec;

import java.security.spec.PSSParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.RSAPSSParameterSpec -- the parameters of RSASSA-PSS.
 *
 * <p>It wraps a {@link PSSParameterSpec} from {@code java.security.spec}, which is where those
 * parameters were already defined. The wrapper exists only to <b>type</b> them as parameters of an
 * XML-DSig signature algorithm; it adds nothing.
 *
 * <p>PSS is the modern padding scheme for RSA, and unlike the classic one --PKCS#1 v1.5-- it has
 * things to configure: the digest, the mask generation function and the salt length. That is why it
 * is the only signature algorithm of the list with real parameters.
 *
 * <p>It arrived in Java 17. Being the only recent class of the package shows in the style: it
 * validates nothing and does not copy, because {@code PSSParameterSpec} is already immutable.
 */
public final class RSAPSSParameterSpec implements SignatureMethodParameterSpec {

    /** The PSS parameters. */
    private final PSSParameterSpec spec;

    /**
     * @param spec the PSS parameters
     * @throws NullPointerException if it is null
     */
    public RSAPSSParameterSpec(PSSParameterSpec spec) {
        if (spec == null) {
            throw new NullPointerException("spec cannot be null");
        }
        this.spec = spec;
    }

    /** The PSS parameters. */
    public PSSParameterSpec getPSSParameterSpec() {
        return this.spec;
    }
}
