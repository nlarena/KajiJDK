package java.security.cert;

import java.io.IOException;
import java.security.PublicKey;

// Una clave publica leida de su `SubjectPublicKeyInfo`, sin proveedor de criptografia.
//
//   SubjectPublicKeyInfo ::= SEQUENCE {
//       algorithm        AlgorithmIdentifier,
//       subjectPublicKey BIT STRING }
//
// ===============================================================================================
// QUE PROMETE Y QUE NO
// ===============================================================================================
//
// `PublicKey` promete exactamente tres cosas -- `getAlgorithm()`, `getFormat()` y `getEncoded()` --
// y esta clase las cumple las tres leyendolas del DER. No promete nada mas, y por eso se puede
// escribir sin `KeyFactory`: **una clave publica no es una operacion criptografica**, es un dato
// con un nombre de algoritmo. Quien quiera verificar una firma necesita un `Signature`, que esta
// biblioteca no tiene, y para eso no le alcanzaria ninguna implementacion de `PublicKey`.
//
// **Diferencia anotada con el JDK**: alla `X509CertSelector.getSubjectPublicKey()` devuelve un
// `sun.security.rsa.RSAPublicKeyImpl`, que ademas implementa `java.security.interfaces.RSAPublicKey`
// y contesta `getModulus()`. Aca devuelve esta clase, que **no** implementa esa interfaz. Un
// llamador que castee a `RSAPublicKey` recibe `ClassCastException` en vez de la clave. Se eligio asi
// porque la alternativa era peor: descomponer el modulo y el exponente para contestar `getModulus()`
// dejaria una clave que parece usable para cifrar y no lo es.
//
// Para lo que la usa este paquete alcanza y sobra: `X509CertSelector.match` compara la codificacion
// de la clave del certificado contra la del criterio, byte a byte.
final class EncodedPublicKey implements PublicKey {

    private static final long serialVersionUID = 4718264291549890431L;

    // Los algoritmos que se saben nombrar. Un OID que no este en la tabla no es un error: se
    // devuelve el OID como nombre, que es lo que hace el JDK con un algoritmo que no conoce y es
    // mas util que un null.
    private static final String[][] NAMES = {
        {"1.2.840.113549.1.1.1", "RSA"},
        {"1.2.840.113549.1.1.10", "RSASSA-PSS"},
        {"1.2.840.10040.4.1", "DSA"},
        {"1.2.840.10046.2.1", "DiffieHellman"},
        {"1.2.840.113549.1.3.1", "DiffieHellman"},
        {"1.2.840.10045.2.1", "EC"},
        {"1.3.101.110", "XDH"},
        {"1.3.101.111", "XDH"},
        {"1.3.101.112", "Ed25519"},
        {"1.3.101.113", "Ed448"},
    };

    private final String algorithm;
    private final byte[] encoded;

    private EncodedPublicKey(String algorithm, byte[] encoded) {
        this.algorithm = algorithm;
        this.encoded = encoded;
    }

    /**
     * Lee la clave de su `SubjectPublicKeyInfo`.
     *
     * <p>Se camina la estructura entera --no solo hasta el OID-- para que un DER truncado se
     * rechace aca y no mas adelante, cuando ya no se sabria de donde salio.
     *
     * @throws IOException si el DER no es un SubjectPublicKeyInfo bien formado
     */
    static EncodedPublicKey of(byte[] der) throws IOException {
        DerReader outer = new DerReader(der, 0, der.length);
        int len = outer.expect(DerReader.TAG_SEQUENCE);
        DerReader info = new DerReader(der, outer.position(), len);
        int algLen = info.expect(DerReader.TAG_SEQUENCE);
        DerReader alg = new DerReader(der, info.position(), algLen);
        info.skip(algLen);
        int oidLen = alg.expect(DerReader.TAG_OID);
        int oidAt = alg.skip(oidLen);
        String oid = alg.readOid(oidAt, oidLen);
        // El BIT STRING de la clave: se comprueba que este y que cierre, aunque no se mire adentro.
        int bitsLen = info.expect(0x03);
        info.skip(bitsLen);
        if (info.hasMore()) {
            throw new IOException("DER: datos de mas despues del SubjectPublicKeyInfo");
        }
        byte[] copy = new byte[der.length];
        System.arraycopy(der, 0, copy, 0, der.length);
        return new EncodedPublicKey(nameOf(oid), copy);
    }

    private static String nameOf(String oid) {
        int i = 0;
        while (i < NAMES.length) {
            if (NAMES[i][0].equals(oid)) {
                return NAMES[i][1];
            }
            i = i + 1;
        }
        return oid;
    }

    @Override
    public String getAlgorithm() {
        return this.algorithm;
    }

    /** Siempre {@code "X.509"}: es el nombre del formato SubjectPublicKeyInfo. */
    @Override
    public String getFormat() {
        return "X.509";
    }

    @Override
    public byte[] getEncoded() {
        byte[] c = new byte[this.encoded.length];
        System.arraycopy(this.encoded, 0, c, 0, this.encoded.length);
        return c;
    }

    @Override
    public String toString() {
        return this.algorithm + " public key, " + this.encoded.length + " encoded bytes";
    }
}
