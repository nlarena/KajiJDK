package javax.xml.crypto.dsig.spec;

import java.security.spec.PSSParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.RSAPSSParameterSpec -- los parametros de RSASSA-PSS.
 *
 * <p>Envuelve un {@link PSSParameterSpec} de {@code java.security.spec}, que es donde esos parametros
 * ya estaban definidos. El envoltorio existe solo para <b>tiparlos</b> como parametros de un algoritmo
 * de firma de XML-DSig; no agrega nada.
 *
 * <p>PSS es el esquema de relleno moderno para RSA, y a diferencia del clasico --PKCS#1 v1.5-- tiene
 * cosas que configurar: el resumen, la funcion de generacion de mascara y el largo de la sal. Por eso
 * es el unico algoritmo de firma de la lista con parametros de verdad.
 *
 * <p>Llego en Java 17. Que sea la unica clase reciente del paquete se nota en el estilo: no valida
 * nada y no copia, porque {@code PSSParameterSpec} ya es inmutable.
 */
public final class RSAPSSParameterSpec implements SignatureMethodParameterSpec {

    /** Los parametros de PSS. */
    private final PSSParameterSpec spec;

    /**
     * @param spec los parametros de PSS
     * @throws NullPointerException si es null
     */
    public RSAPSSParameterSpec(PSSParameterSpec spec) {
        if (spec == null) {
            throw new NullPointerException("spec cannot be null");
        }
        this.spec = spec;
    }

    /** Los parametros de PSS. */
    public PSSParameterSpec getPSSParameterSpec() {
        return this.spec;
    }
}
