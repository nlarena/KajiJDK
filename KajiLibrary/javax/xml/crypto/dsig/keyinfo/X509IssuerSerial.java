package javax.xml.crypto.dsig.keyinfo;

import java.math.BigInteger;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.X509IssuerSerial -- un certificado, nombrado por
 * emisor y numero de serie.
 *
 * <p>El par que identifica <b>univocamente</b> a un certificado X.509: el nombre distinguido de quien
 * lo emitio y el numero de serie que ese emisor le dio. Un numero de serie solo no alcanza --cada
 * emisor lleva su propia numeracion-- y por eso los dos van juntos.
 *
 * <p>Es la forma segura de referirse a un certificado adentro de un {@link KeyInfo}: no lo trae, lo
 * nombra. Quien valida lo busca en su propio almacen, con la misma inversion de confianza que
 * {@link KeyName}.
 *
 * <p>El nombre del emisor va en el formato de RFC 2253. Compararlo como texto es fragil --el mismo
 * nombre admite varias escrituras-- y por eso conviene parsearlo antes de buscar.
 */
public interface X509IssuerSerial extends XMLStructure {

    /** El nombre distinguido del emisor, en formato RFC 2253. */
    String getIssuerName();

    /** El numero de serie que ese emisor le dio. */
    BigInteger getSerialNumber();
}
