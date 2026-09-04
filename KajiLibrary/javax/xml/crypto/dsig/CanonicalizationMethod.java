package javax.xml.crypto.dsig;

import java.security.spec.AlgorithmParameterSpec;

/**
 * KajiLibrary's javax.xml.crypto.dsig.CanonicalizationMethod -- como se convierte el XML en bytes.
 *
 * <p>Es la transformacion que hace posible firmar XML. El mismo documento se puede escribir de muchas
 * formas --orden de atributos, comillas, espacios, prefijos-- y todas significan lo mismo; un resumen
 * criptografico, en cambio, cambia con cada byte. La canonicalizacion elige <b>una</b> forma de
 * escribir, y es la que se resume.
 *
 * <h2>Inclusiva contra exclusiva</h2>
 *
 * <p>La <b>inclusiva</b> arrastra todas las declaraciones de espacio de nombres del contexto, aunque
 * el fragmento no las use. La <b>exclusiva</b> arrastra solo las que usa.
 *
 * <p>La diferencia decide si un fragmento firmado sobrevive a que lo muevan. Con la inclusiva, meter
 * el fragmento en otro documento cambia su contexto y rompe la firma; con la exclusiva, no. Por eso
 * todo lo que viaja adentro de un sobre --SOAP, sobre todo-- usa exclusiva.
 *
 * <p>Las variantes {@code WITH_COMMENTS} conservan los comentarios. Casi nunca se quieren: un
 * comentario no cambia el significado del documento y firmarlo hace que la firma se rompa por un
 * cambio irrelevante.
 */
public interface CanonicalizationMethod extends Transform {

    /** Canonicalizacion inclusiva, version 1.0. */
    static final String INCLUSIVE = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    /** Idem, conservando comentarios. */
    static final String INCLUSIVE_WITH_COMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments";

    /** Exclusiva. Ver la nota de la clase. */
    static final String EXCLUSIVE = "http://www.w3.org/2001/10/xml-exc-c14n#";

    /** Idem, conservando comentarios. */
    static final String EXCLUSIVE_WITH_COMMENTS = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    /** Inclusiva, version 1.1. */
    static final String INCLUSIVE_11 = "http://www.w3.org/2006/12/xml-c14n11";

    /** Idem, conservando comentarios. */
    static final String INCLUSIVE_11_WITH_COMMENTS = "http://www.w3.org/2006/12/xml-c14n11#WithComments";

    /** Los parametros, o null. Para la exclusiva, un {@code ExcC14NParameterSpec}. */
    AlgorithmParameterSpec getParameterSpec();
}
