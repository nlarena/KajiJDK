package javax.xml.crypto.dsig.keyinfo;

import java.math.BigInteger;
import javax.xml.crypto.XMLStructure;

/**
 * KajiLibrary's javax.xml.crypto.dsig.keyinfo.X509IssuerSerial -- a certificate, named by issuer
 * and serial number.
 *
 * <p>The pair that <b>uniquely</b> identifies an X.509 certificate: the distinguished name of
 * whoever issued it and the serial number that issuer gave it. A serial number alone is not enough
 * --each issuer keeps its own numbering-- and that is why both go together.
 *
 * <p>It is the safe way of referring to a certificate inside a {@link KeyInfo}: it does not bring
 * it, it names it. Whoever validates looks it up in their own store, with the same inversion of
 * trust as {@link KeyName}.
 *
 * <p>The issuer's name goes in RFC 2253 format. Comparing it as text is fragile --the same name
 * admits several spellings-- and that is why it is advisable to parse it before looking it up.
 */
public interface X509IssuerSerial extends XMLStructure {

    /** The issuer's distinguished name, in RFC 2253 format. */
    String getIssuerName();

    /** The serial number that issuer gave it. */
    BigInteger getSerialNumber();
}
