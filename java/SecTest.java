import java.security.AccessControlException;
import java.security.AllPermission;
import java.security.BasicPermission;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.Guard;
import java.security.GuardedObject;
import java.security.InvalidParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.SecurityPermission;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAGenParameterSpec;
import java.security.spec.DSAParameterSpec;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EdDSAParameterSpec;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAMultiPrimePrivateCrtKeySpec;
import java.security.spec.RSAOtherPrimeInfo;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.XECPrivateKeySpec;
import java.security.spec.XECPublicKeySpec;
import java.math.BigInteger;
import java.util.Set;

// Prueba de comportamiento de java.security. Corre igual en KajiJDK y en el JDK 25 real: todo lo
// que compara son valores fijados por una especificacion o por el contrato del API, nunca cosas
// que dependan del proveedor (nombres de proveedor, orden de la lista, algoritmos de mas).
//
// Los digests se comparan contra los vectores publicados: RFC 1321 apendice A.5 para MD5 y
// FIPS 180-4 para la familia SHA.
class PermisoDePrueba extends BasicPermission {

    PermisoDePrueba(String name) {
        super(name);
    }
}

class GuardiaQueNiega implements Guard {

    public void checkGuard(Object object) {
        throw new SecurityException("no");
    }
}

// Un certificado de mentira: no parsea nada y devuelve lo que se le configuro. Sirve para probar
// los metodos **concretos** de X509Certificate y los selectores, que son los unicos que esta
// biblioteca implementa; todo lo abstracto lo pone el que trae un certificado de verdad.
class CertDePrueba extends java.security.cert.X509Certificate {

    java.util.Map<String, byte[]> exts = new java.util.HashMap<String, byte[]>();
    java.math.BigInteger serie = java.math.BigInteger.ONE;
    boolean[] usos;
    int bc = -1;
    java.security.PublicKey clave;
    long desde = 0;
    long hasta = Long.MAX_VALUE;
    byte[] codificado = new byte[] {1, 2, 3};

    public byte[] getExtensionValue(String oid) {
        return this.exts.get(oid);
    }

    public boolean hasUnsupportedCriticalExtension() {
        return false;
    }

    java.util.Set<String> criticas;

    public java.util.Set<String> getCriticalExtensionOIDs() {
        return this.criticas;
    }

    public java.util.Set<String> getNonCriticalExtensionOIDs() {
        return null;
    }

    public void checkValidity() throws java.security.cert.CertificateExpiredException,
            java.security.cert.CertificateNotYetValidException {
        this.checkValidity(new java.util.Date());
    }

    public void checkValidity(java.util.Date d)
            throws java.security.cert.CertificateExpiredException,
                   java.security.cert.CertificateNotYetValidException {
        if (d.getTime() < this.desde) {
            throw new java.security.cert.CertificateNotYetValidException("todavia no");
        }
        if (d.getTime() > this.hasta) {
            throw new java.security.cert.CertificateExpiredException("ya fue");
        }
    }

    public int getVersion() {
        return 3;
    }

    public java.math.BigInteger getSerialNumber() {
        return this.serie;
    }

    public java.security.Principal getIssuerDN() {
        return null;
    }

    public java.security.Principal getSubjectDN() {
        return null;
    }

    public java.util.Date getNotBefore() {
        return new java.util.Date(this.desde);
    }

    public java.util.Date getNotAfter() {
        return new java.util.Date(this.hasta);
    }

    public byte[] getTBSCertificate() {
        return new byte[0];
    }

    public byte[] getSignature() {
        return new byte[0];
    }

    public String getSigAlgName() {
        return "SHA256withRSA";
    }

    public String getSigAlgOID() {
        return "1.2.840.113549.1.1.11";
    }

    public byte[] getSigAlgParams() {
        return null;
    }

    public boolean[] getIssuerUniqueID() {
        return null;
    }

    public boolean[] getSubjectUniqueID() {
        return null;
    }

    public boolean[] getKeyUsage() {
        return this.usos;
    }

    public int getBasicConstraints() {
        return this.bc;
    }

    public byte[] getEncoded() {
        return this.codificado;
    }

    public void verify(java.security.PublicKey k) {
    }

    public void verify(java.security.PublicKey k, String p) {
    }

    public String toString() {
        return "CertDePrueba";
    }

    public java.security.PublicKey getPublicKey() {
        return this.clave;
    }
}

// Una clave publica de mentira: solo importa su codificacion, que es lo unico que los selectores
// miran.
class ClaveDePrueba implements java.security.PublicKey {

    byte[] enc;

    ClaveDePrueba(byte[] e) {
        this.enc = e;
    }

    public String getAlgorithm() {
        return "RSA";
    }

    public String getFormat() {
        return "X.509";
    }

    public byte[] getEncoded() {
        return this.enc;
    }

    public String toString() {
        return "ClaveDePrueba";
    }
}

class CrlDePrueba extends java.security.cert.X509CRL {

    java.util.Map<String, byte[]> exts = new java.util.HashMap<String, byte[]>();
    java.util.Date thisU;
    java.util.Date nextU;

    public byte[] getExtensionValue(String oid) {
        return this.exts.get(oid);
    }

    public boolean hasUnsupportedCriticalExtension() {
        return false;
    }

    public java.util.Set<String> getCriticalExtensionOIDs() {
        return null;
    }

    public java.util.Set<String> getNonCriticalExtensionOIDs() {
        return null;
    }

    public byte[] getEncoded() {
        return new byte[] {7};
    }

    public void verify(java.security.PublicKey k) {
    }

    public void verify(java.security.PublicKey k, String p) {
    }

    public int getVersion() {
        return 2;
    }

    public java.security.Principal getIssuerDN() {
        return null;
    }

    public java.util.Date getThisUpdate() {
        return this.thisU;
    }

    public java.util.Date getNextUpdate() {
        return this.nextU;
    }

    public java.security.cert.X509CRLEntry getRevokedCertificate(java.math.BigInteger s) {
        return null;
    }

    public java.util.Set<? extends java.security.cert.X509CRLEntry> getRevokedCertificates() {
        return null;
    }

    public byte[] getTBSCertList() {
        return new byte[0];
    }

    public byte[] getSignature() {
        return new byte[0];
    }

    public String getSigAlgName() {
        return "SHA256withRSA";
    }

    public String getSigAlgOID() {
        return "1.2.840.113549.1.1.11";
    }

    public byte[] getSigAlgParams() {
        return null;
    }

    public boolean isRevoked(java.security.cert.Certificate c) {
        return false;
    }

    public String toString() {
        return "CrlDePrueba";
    }
}

class EntradaDePrueba extends java.security.cert.X509CRLEntry {

    java.util.Map<String, byte[]> exts = new java.util.HashMap<String, byte[]>();
    byte[] codificado = new byte[] {5, 5};

    public byte[] getExtensionValue(String oid) {
        return this.exts.get(oid);
    }

    public boolean hasUnsupportedCriticalExtension() {
        return false;
    }

    public java.util.Set<String> getCriticalExtensionOIDs() {
        return null;
    }

    public java.util.Set<String> getNonCriticalExtensionOIDs() {
        return null;
    }

    public byte[] getEncoded() {
        return this.codificado;
    }

    public java.math.BigInteger getSerialNumber() {
        return java.math.BigInteger.TEN;
    }

    public java.util.Date getRevocationDate() {
        return new java.util.Date(0);
    }

    public boolean hasExtensions() {
        return !this.exts.isEmpty();
    }

    public String toString() {
        return "EntradaDePrueba";
    }
}

// Una "firma" de mentira: la suma de los bytes. **No es criptografia y no lo pretende** — sirve
// unicamente para ejercitar la maquina de estados de `Signature`, que es lo que esta biblioteca si
// implementa. Lo que se comprueba con ella es cuando `Signature` deja pasar una operacion y cuando
// la rechaza, no que la firma valga nada.
class FirmaDePrueba extends java.security.Signature {

    int suma;
    boolean falloAlFirmar;

    FirmaDePrueba() {
        super("SumaDePrueba");
    }

    protected void engineInitVerify(java.security.PublicKey k) {
        this.suma = 0;
    }

    protected void engineInitSign(java.security.PrivateKey k) {
        this.suma = 0;
    }

    protected void engineUpdate(byte b) {
        this.suma = this.suma + (b & 0xff);
    }

    protected void engineUpdate(byte[] b, int off, int len) {
        for (int i = 0; i < len; i++) {
            this.suma = this.suma + (b[off + i] & 0xff);
        }
    }

    protected byte[] engineSign() throws java.security.SignatureException {
        if (this.falloAlFirmar) {
            throw new java.security.SignatureException("no");
        }
        byte[] r = new byte[] {(byte) (this.suma >> 8), (byte) this.suma};
        this.suma = 0;
        return r;
    }

    protected boolean engineVerify(byte[] sig) {
        boolean ok = sig.length == 2
            && sig[0] == (byte) (this.suma >> 8)
            && sig[1] == (byte) this.suma;
        this.suma = 0;
        return ok;
    }

    protected void engineSetParameter(String p, Object v)
            throws java.security.InvalidParameterException {
        throw new java.security.InvalidParameterException("no");
    }

    protected Object engineGetParameter(String p)
            throws java.security.InvalidParameterException {
        throw new java.security.InvalidParameterException("no");
    }

    // Las tres constantes de estado son `protected`, asi que solo se ven desde una subclase.
    int[] constantes() {
        return new int[] {UNINITIALIZED, SIGN, VERIFY};
    }

    // El campo `state` tambien es protegido y es parte del contrato con las subclases.
    int estado() {
        return this.state;
    }
}

// Un almacen de mentira en memoria: solo certificados de confianza, que es lo unico que se puede
// guardar sin contraseña. Sirve para probar la fachada de `KeyStore`, que es lo que esta biblioteca
// implementa; el formato en disco no existe aca.
class AlmacenSpiDePrueba extends java.security.KeyStoreSpi {

    java.util.Map<String, java.security.cert.Certificate> certs =
        new java.util.LinkedHashMap<String, java.security.cert.Certificate>();
    boolean cargado;

    public java.security.Key engineGetKey(String alias, char[] password) {
        return null;
    }

    public java.security.cert.Certificate[] engineGetCertificateChain(String alias) {
        return null;
    }

    public java.security.cert.Certificate engineGetCertificate(String alias) {
        return this.certs.get(alias);
    }

    public java.util.Date engineGetCreationDate(String alias) {
        return new java.util.Date(0);
    }

    public void engineSetKeyEntry(String alias, java.security.Key key, char[] password,
                                  java.security.cert.Certificate[] chain)
            throws java.security.KeyStoreException {
        throw new java.security.KeyStoreException("solo certificados");
    }

    public void engineSetKeyEntry(String alias, byte[] key,
                                  java.security.cert.Certificate[] chain)
            throws java.security.KeyStoreException {
        throw new java.security.KeyStoreException("solo certificados");
    }

    public void engineSetCertificateEntry(String alias, java.security.cert.Certificate cert) {
        this.certs.put(alias, cert);
    }

    public void engineDeleteEntry(String alias) {
        this.certs.remove(alias);
    }

    public java.util.Enumeration<String> engineAliases() {
        return java.util.Collections.enumeration(this.certs.keySet());
    }

    public boolean engineContainsAlias(String alias) {
        return this.certs.containsKey(alias);
    }

    public int engineSize() {
        return this.certs.size();
    }

    public boolean engineIsKeyEntry(String alias) {
        return false;
    }

    public boolean engineIsCertificateEntry(String alias) {
        return this.certs.containsKey(alias);
    }

    // Se recorre por claves y no por `entrySet()` a proposito: nombrar `java.util.Map.Entry` en
    // este archivo, que tambien nombra `java.security.KeyStore.Entry`, dispara el bug #350 del
    // compilador —dos tipos anidados con el mismo nombre simple se pisan—. Ver COMPILER_FINDINGS.md.
    public String engineGetCertificateAlias(java.security.cert.Certificate cert) {
        java.util.Iterator<String> it = this.certs.keySet().iterator();
        while (it.hasNext()) {
            String alias = it.next();
            if (this.certs.get(alias).equals(cert)) {
                return alias;
            }
        }
        return null;
    }

    public void engineStore(java.io.OutputStream s, char[] password) {
    }

    public void engineLoad(java.io.InputStream s, char[] password) {
        this.cargado = true;
    }
}

// Claves privadas y publicas de mentira: solo importa el nombre del algoritmo, que es lo que
// `PrivateKeyEntry` compara.
class ClavePrivadaDePrueba implements java.security.PrivateKey {

    public String getAlgorithm() {
        return "RSA";
    }

    public String getFormat() {
        return "PKCS#8";
    }

    public byte[] getEncoded() {
        return new byte[] {1};
    }
}

class ClaveDePruebaEC implements java.security.PublicKey {

    byte[] enc;

    ClaveDePruebaEC(byte[] e) {
        this.enc = e;
    }

    public String getAlgorithm() {
        return "EC";
    }

    public String getFormat() {
        return "X.509";
    }

    public byte[] getEncoded() {
        return this.enc;
    }
}

// Subclase minima para poder construir un `KeyStore` sobre el SPI de prueba: el constructor de
// `KeyStore` es protegido y no hay proveedor registrado del que sacar uno.
class AlmacenDePrueba extends java.security.KeyStore {

    AlmacenDePrueba(java.security.KeyStoreSpi spi) {
        super(spi, null, "prueba");
    }
}

public class SecTest {

    static String hex(byte[] b) {
        String d = "0123456789abcdef";
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            s.append(d.charAt(v >> 4));
            s.append(d.charAt(v & 15));
        }
        return s.toString();
    }

    static byte[] ascii(String s) {
        byte[] b = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }

    static String md(String alg, String msg) {
        try {
            MessageDigest m = MessageDigest.getInstance(alg);
            return hex(m.digest(ascii(msg)));
        } catch (NoSuchAlgorithmException e) {
            return "NOALG";
        }
    }

    public static int run() {
        int n = 0;

        // ---- vectores de MD5 (RFC 1321 A.5) --------------------------------------------------
        if (!md("MD5", "").equals("d41d8cd98f00b204e9800998ecf8427e")) {
            return n;
        }
        n++;
        if (!md("MD5", "a").equals("0cc175b9c0f1b6a831c399e269772661")) {
            return n;
        }
        n++;
        if (!md("MD5", "abc").equals("900150983cd24fb0d6963f7d28e17f72")) {
            return n;
        }
        n++;
        if (!md("MD5", "message digest").equals("f96b697d7cb7938d525a2f31aaf161d0")) {
            return n;
        }
        n++;
        if (!md("MD5", "abcdefghijklmnopqrstuvwxyz")
                .equals("c3fcd3d76192e4007dfb496cca67e13b")) {
            return n;
        }
        n++;
        if (!md("MD5", "12345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890")
                .equals("57edf4a22be3c955ac49da2e2107b67a")) {
            return n;
        }
        n++;

        // ---- vectores de SHA-1 (FIPS 180-4) ---------------------------------------------------
        if (!md("SHA-1", "").equals("da39a3ee5e6b4b0d3255bfef95601890afd80709")) {
            return n;
        }
        n++;
        if (!md("SHA-1", "abc").equals("a9993e364706816aba3e25717850c26c9cd0d89d")) {
            return n;
        }
        n++;
        if (!md("SHA-1", "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
                .equals("84983e441c3bd26ebaae4aa1f95129e5e54670f1")) {
            return n;
        }
        n++;

        // ---- vectores de SHA-224 / SHA-256 ----------------------------------------------------
        if (!md("SHA-224", "abc")
                .equals("23097d223405d8228642a477bda255b32aadbce4bda0b3f7e36c9da7")) {
            return n;
        }
        n++;
        if (!md("SHA-224", "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
                .equals("75388b16512776cc5dba5da1fd890150b0c6455cb4f58b1952522525")) {
            return n;
        }
        n++;
        if (!md("SHA-256", "")
                .equals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")) {
            return n;
        }
        n++;
        if (!md("SHA-256", "abc")
                .equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")) {
            return n;
        }
        n++;
        if (!md("SHA-256", "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
                .equals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1")) {
            return n;
        }
        n++;

        // ---- vectores de SHA-384 / SHA-512 ----------------------------------------------------
        if (!md("SHA-384", "abc")
                .equals("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed"
                        + "8086072ba1e7cc2358baeca134c825a7")) {
            return n;
        }
        n++;
        if (!md("SHA-512", "")
                .equals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce"
                        + "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e")) {
            return n;
        }
        n++;
        if (!md("SHA-512", "abc")
                .equals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                        + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f")) {
            return n;
        }
        n++;
        // Este pasa los 112 bytes de relleno de SHA-512 y obliga a un bloque extra solo para el
        // contador de largo: es el caso que rompe una implementacion con el padding mal hecho.
        if (!md("SHA-512", "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmn"
                + "hijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu")
                .equals("8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018"
                        + "501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909")) {
            return n;
        }
        n++;

        // ---- multiples bloques y update incremental -------------------------------------------
        // Un millon de 'a' es el tercer vector de FIPS 180-4; se alimenta de a pedazos de largo
        // irregular para que el buffer interno tenga que partir bloques.
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            byte[] chunk = new byte[1000];
            for (int i = 0; i < 1000; i++) {
                chunk[i] = (byte) 'a';
            }
            for (int i = 0; i < 1000; i++) {
                m.update(chunk);
            }
            if (!hex(m.digest()).equals(
                    "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0")) {
                return n;
            }
        } catch (NoSuchAlgorithmException e) {
            return n;
        }
        n++;

        // update(byte) de a uno tiene que dar lo mismo que update(byte[]).
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-1");
            byte[] datos = ascii("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq");
            for (int i = 0; i < datos.length; i++) {
                m.update(datos[i]);
            }
            if (!hex(m.digest()).equals("84983e441c3bd26ebaae4aa1f95129e5e54670f1")) {
                return n;
            }
        } catch (NoSuchAlgorithmException e) {
            return n;
        }
        n++;

        // digest() deja el objeto reseteado: el segundo uso no arrastra el primero.
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.digest(ascii("abc"));
            if (!hex(m.digest(ascii("abc")))
                    .equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")) {
                return n;
            }
        } catch (NoSuchAlgorithmException e) {
            return n;
        }
        n++;

        // reset() en la mitad tira lo consumido.
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(ascii("basura"));
            m.reset();
            if (!hex(m.digest(ascii("abc")))
                    .equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")) {
                return n;
            }
        } catch (NoSuchAlgorithmException e) {
            return n;
        }
        n++;

        // ---- largos y clone -------------------------------------------------------------------
        try {
            if (MessageDigest.getInstance("MD5").getDigestLength() != 16) {
                return n;
            }
            n++;
            if (MessageDigest.getInstance("SHA-1").getDigestLength() != 20) {
                return n;
            }
            n++;
            if (MessageDigest.getInstance("SHA-224").getDigestLength() != 28) {
                return n;
            }
            n++;
            if (MessageDigest.getInstance("SHA-256").getDigestLength() != 32) {
                return n;
            }
            n++;
            if (MessageDigest.getInstance("SHA-384").getDigestLength() != 48) {
                return n;
            }
            n++;
            if (MessageDigest.getInstance("SHA-512").getDigestLength() != 64) {
                return n;
            }
            n++;
        } catch (NoSuchAlgorithmException e) {
            return n;
        }

        // Clonar a mitad de camino y seguir por dos ramas distintas.
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(ascii("ab"));
            MessageDigest copia = (MessageDigest) m.clone();
            m.update(ascii("c"));
            copia.update(ascii("c"));
            String a = hex(m.digest());
            String b = hex(copia.digest());
            if (!a.equals(b)) {
                return n;
            }
            n++;
            if (!a.equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")) {
                return n;
            }
            n++;
        } catch (Exception e) {
            return n;
        }

        // ---- getAlgorithm, alias, y algoritmo inexistente --------------------------------------
        try {
            if (!MessageDigest.getInstance("SHA-256").getAlgorithm().equals("SHA-256")) {
                return n;
            }
            n++;
        } catch (NoSuchAlgorithmException e) {
            return n;
        }
        // "SHA" es alias historico de SHA-1 en todo proveedor que ofrezca SHA-1.
        if (!md("SHA", "abc").equals("a9993e364706816aba3e25717850c26c9cd0d89d")) {
            return n;
        }
        n++;
        boolean tiro = false;
        try {
            MessageDigest.getInstance("NO-EXISTE-ESTE-ALGORITMO");
        } catch (NoSuchAlgorithmException e) {
            tiro = true;
        }
        if (!tiro) {
            return n;
        }
        n++;

        // ---- digest(byte[], int, int) ----------------------------------------------------------
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(ascii("abc"));
            byte[] out = new byte[40];
            int escritos = m.digest(out, 4, 32);
            if (escritos != 32) {
                return n;
            }
            n++;
            byte[] rec = new byte[32];
            System.arraycopy(out, 4, rec, 0, 32);
            if (!hex(rec)
                    .equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")) {
                return n;
            }
            n++;
        } catch (Exception e) {
            return n;
        }
        // Un buffer sin lugar para el digest completo tiene que fallar, no truncar.
        boolean fallo = false;
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(ascii("abc"));
            m.digest(new byte[16], 0, 16);
        } catch (DigestException e) {
            fallo = true;
        } catch (NoSuchAlgorithmException e) {
            return n;
        }
        if (!fallo) {
            return n;
        }
        n++;

        // ---- isEqual ---------------------------------------------------------------------------
        byte[] x = new byte[] {1, 2, 3};
        byte[] y = new byte[] {1, 2, 3};
        byte[] z = new byte[] {1, 2, 4};
        if (!MessageDigest.isEqual(x, y)) {
            return n;
        }
        n++;
        if (MessageDigest.isEqual(x, z)) {
            return n;
        }
        n++;
        if (MessageDigest.isEqual(x, new byte[] {1, 2})) {
            return n;
        }
        n++;
        if (MessageDigest.isEqual(null, x)) {
            return n;
        }
        n++;
        if (!MessageDigest.isEqual(new byte[0], new byte[0])) {
            return n;
        }
        n++;

        // ---- Security --------------------------------------------------------------------------
        Set<String> algs = Security.getAlgorithms("MessageDigest");
        if (!algs.contains("SHA-256") || !algs.contains("SHA-1") || !algs.contains("MD5")) {
            return n;
        }
        n++;
        if (Security.getProviders().length < 1) {
            return n;
        }
        n++;
        if (Security.getProvider("NO-HAY-PROVEEDOR-ASI") != null) {
            return n;
        }
        n++;
        Security.setProperty("kaji.prueba", "valor");
        if (!"valor".equals(Security.getProperty("kaji.prueba"))) {
            return n;
        }
        n++;
        if (Security.getProviders("MessageDigest.SHA-256") == null) {
            return n;
        }
        n++;
        if (Security.getProviders("MessageDigest.NO-EXISTE-ASI") != null) {
            return n;
        }
        n++;

        // ---- modelo de permisos ------------------------------------------------------------------
        Permission todo = new AllPermission();
        if (!todo.implies(new SecurityPermission("getPolicy"))) {
            return n;
        }
        n++;
        if (!todo.getActions().equals("<all actions>")) {
            return n;
        }
        n++;
        SecurityPermission sp = new SecurityPermission("getProperty.*");
        if (!sp.implies(new SecurityPermission("getProperty.foo"))) {
            return n;
        }
        n++;
        if (sp.implies(new SecurityPermission("getProperty"))) {
            return n;
        }
        n++;
        if (sp.implies(new PermisoDePrueba("getProperty.foo"))) {
            return n;
        }
        n++;
        if (!sp.getActions().equals("")) {
            return n;
        }
        n++;

        Permissions ps = new Permissions();
        ps.add(new SecurityPermission("getProperty.*"));
        ps.add(new PermisoDePrueba("a.b"));
        if (!ps.implies(new SecurityPermission("getProperty.x"))) {
            return n;
        }
        n++;
        if (!ps.implies(new PermisoDePrueba("a.b"))) {
            return n;
        }
        n++;
        if (ps.implies(new PermisoDePrueba("a.c"))) {
            return n;
        }
        n++;
        ps.setReadOnly();
        if (!ps.isReadOnly()) {
            return n;
        }
        n++;
        boolean cerrada = false;
        try {
            ps.add(new PermisoDePrueba("z"));
        } catch (SecurityException e) {
            cerrada = true;
        }
        if (!cerrada) {
            return n;
        }
        n++;

        PermissionCollection pc = new SecurityPermission("x").newPermissionCollection();
        pc.add(new SecurityPermission("a.*"));
        if (!pc.implies(new SecurityPermission("a.b.c"))) {
            return n;
        }
        n++;
        boolean mezcla = false;
        try {
            pc.add(new PermisoDePrueba("a.b"));
        } catch (IllegalArgumentException e) {
            mezcla = true;
        }
        if (!mezcla) {
            return n;
        }
        n++;

        // El caso especial de "exitVM": el nombre viejo y el comodin nuevo son el mismo permiso.
        SecurityPermission comodinSalida = new SecurityPermission("exitVM.*");
        if (!comodinSalida.implies(new SecurityPermission("exitVM"))) {
            return n;
        }
        n++;

        // ---- GuardedObject ------------------------------------------------------------------------
        // Sin guardia, el objeto sale.
        Object secreto = new Object();
        try {
            if (new GuardedObject(secreto, null).getObject() != secreto) {
                return n;
            }
        } catch (SecurityException e) {
            return n;
        }
        n++;
        // Con un `Permission` de guardia **no sale**, ni siquiera con `AllPermission`. Desde el
        // JDK 24 no hay `SecurityManager` a quien preguntarle si el permiso esta concedido, y
        // `Permission.checkGuard` niega en vez de dejar pasar. Es contraintuitivo y es lo que hace
        // el JDK 25; esta comprobacion esta justamente para que no se "arregle" al reves.
        boolean sinControl = false;
        try {
            new GuardedObject(secreto, new AllPermission()).getObject();
        } catch (SecurityException e) {
            sinControl = true;
        }
        if (!sinControl) {
            return n;
        }
        n++;
        GuardedObject cerrado = new GuardedObject(secreto, new GuardiaQueNiega());
        boolean negado = false;
        try {
            cerrado.getObject();
        } catch (SecurityException e) {
            negado = true;
        }
        if (!negado) {
            return n;
        }
        n++;

        // ---- jerarquia de excepciones ---------------------------------------------------------------
        if (!(new NoSuchAlgorithmException("x") instanceof GeneralSecurityException)) {
            return n;
        }
        n++;
        if (!(new java.security.InvalidKeyException("x") instanceof KeyException)) {
            return n;
        }
        n++;
        if (!(new java.security.ProviderException("x") instanceof RuntimeException)) {
            return n;
        }
        n++;
        if (!(new InvalidParameterException("x") instanceof IllegalArgumentException)) {
            return n;
        }
        n++;
        // Por Object: el compilador rechaza el instanceof directo justamente porque los tipos no
        // se relacionan, que es lo que esta comprobacion quiere dejar asentado.
        Object ipe = new InvalidParameterException("x");
        if (ipe instanceof GeneralSecurityException) {
            return n;
        }
        n++;
        if (!(new java.security.UnrecoverableKeyException("x")
                instanceof java.security.UnrecoverableEntryException)) {
            return n;
        }
        n++;
        Exception envuelta = new IllegalStateException("adentro");
        PrivilegedActionException pae = new PrivilegedActionException(envuelta);
        if (pae.getException() != envuelta || pae.getCause() != envuelta) {
            return n;
        }
        n++;
        AccessControlException ace = new AccessControlException("no", sp);
        if (ace.getPermission() != sp) {
            return n;
        }
        n++;
        if (new AccessControlException("no").getPermission() != null) {
            return n;
        }
        n++;

        // ---- specs de clave ----------------------------------------------------------------------
        byte[] material = new byte[] {9, 8, 7};
        X509EncodedKeySpec pub = new X509EncodedKeySpec(material);
        if (!pub.getFormat().equals("X.509")) {
            return n;
        }
        n++;
        // La spec copia: mutar el arreglo original no la cambia.
        material[0] = 0;
        if (pub.getEncoded()[0] != 9) {
            return n;
        }
        n++;
        PKCS8EncodedKeySpec priv = new PKCS8EncodedKeySpec(new byte[] {1}, "RSA");
        if (!priv.getFormat().equals("PKCS#8") || !priv.getAlgorithm().equals("RSA")) {
            return n;
        }
        n++;

        KeyPair kp = new KeyPair(null, null);
        if (kp.getPublic() != null || kp.getPrivate() != null) {
            return n;
        }
        n++;

        // ---- java.security.spec: curvas elipticas ------------------------------------------------
        // POINT_INFINITY no tiene coordenadas afines, y solo es igual a si mismo.
        if (ECPoint.POINT_INFINITY.getAffineX() != null
                || ECPoint.POINT_INFINITY.getAffineY() != null) {
            return n;
        }
        n++;
        ECPoint w1 = new ECPoint(BigInteger.ONE, BigInteger.valueOf(2));
        ECPoint w2 = new ECPoint(BigInteger.ONE, BigInteger.valueOf(2));
        if (!w1.equals(w2) || w1.hashCode() != w2.hashCode()) {
            return n;
        }
        n++;
        if (w1.equals(ECPoint.POINT_INFINITY) || ECPoint.POINT_INFINITY.equals(w1)) {
            return n;
        }
        n++;
        if (!ECPoint.POINT_INFINITY.equals(ECPoint.POINT_INFINITY)) {
            return n;
        }
        n++;
        if (!npe(() -> new ECPoint(null, BigInteger.ONE))) {
            return n;
        }
        n++;

        // GF(23): 23 son cinco bits.
        ECFieldFp fp = new ECFieldFp(BigInteger.valueOf(23));
        if (fp.getFieldSize() != 5 || !fp.getP().equals(BigInteger.valueOf(23))) {
            return n;
        }
        n++;
        if (!fp.equals(new ECFieldFp(BigInteger.valueOf(23)))) {
            return n;
        }
        n++;
        if (!iae(() -> new ECFieldFp(BigInteger.ZERO))) {
            return n;
        }
        n++;

        // GF(2^4) por trinomio x^4 + x + 1: los dos constructores describen lo mismo, asi que el
        // que recibe indices tiene que producir el mismo polinomio que el que recibe bits, y al
        // reves. 0b10011 = 19.
        ECFieldF2m f2mKs = new ECFieldF2m(4, new int[] {1});
        if (!f2mKs.getReductionPolynomial().equals(BigInteger.valueOf(19))) {
            return n;
        }
        n++;
        ECFieldF2m f2mRp = new ECFieldF2m(4, BigInteger.valueOf(19));
        int[] medios = f2mRp.getMidTermsOfReductionPolynomial();
        if (medios.length != 1 || medios[0] != 1) {
            return n;
        }
        n++;
        if (!f2mRp.equals(f2mKs) || f2mRp.getFieldSize() != 4 || f2mRp.getM() != 4) {
            return n;
        }
        n++;
        // El cuerpo sin base no es igual al que si la tiene, aunque el m coincida.
        ECFieldF2m f2mSolo = new ECFieldF2m(4);
        if (f2mSolo.getReductionPolynomial() != null
                || f2mSolo.getMidTermsOfReductionPolynomial() != null
                || f2mSolo.equals(f2mKs)) {
            return n;
        }
        n++;
        // Un pentanomio x^8 + x^4 + x^3 + x + 1: los indices salen en orden descendente.
        int[] penta = new ECFieldF2m(8, new BigInteger("100011011", 2))
            .getMidTermsOfReductionPolynomial();
        if (penta.length != 3 || penta[0] != 4 || penta[1] != 3 || penta[2] != 1) {
            return n;
        }
        n++;
        if (!iae(() -> new ECFieldF2m(0))) {
            return n;
        }
        n++;
        // Indice fuera de rango, largo que no es 1 ni 3, y orden no descendente: los tres se
        // rechazan.
        if (!iae(() -> new ECFieldF2m(4, new int[] {5}))
                || !iae(() -> new ECFieldF2m(4, new int[] {1, 2}))
                || !iae(() -> new ECFieldF2m(4, new int[] {2, 1, 3}))) {
            return n;
        }
        n++;
        // 7 = 0b111: no tiene el termino de grado 4, asi que no es un polinomio de reduccion valido
        // para m=4.
        if (!iae(() -> new ECFieldF2m(4, BigInteger.valueOf(7)))) {
            return n;
        }
        n++;

        EllipticCurve curva = new EllipticCurve(fp, BigInteger.ONE, BigInteger.valueOf(4));
        if (curva.getSeed() != null || curva.getField() != fp) {
            return n;
        }
        n++;
        // La semilla se copia y no participa de equals: dos curvas con los mismos a, b y cuerpo son
        // la misma curva aunque una diga como se genero.
        byte[] semilla = new byte[] {1, 2, 3};
        EllipticCurve conSemilla =
            new EllipticCurve(fp, BigInteger.ONE, BigInteger.valueOf(4), semilla);
        semilla[0] = 99;
        if (conSemilla.getSeed()[0] != 1 || !conSemilla.equals(curva)) {
            return n;
        }
        n++;
        // Un coeficiente que no entra en el cuerpo se rechaza.
        if (!iae(() -> new EllipticCurve(fp, BigInteger.valueOf(99), BigInteger.ONE))) {
            return n;
        }
        n++;
        if (!npe(() -> new EllipticCurve(null, BigInteger.ONE, BigInteger.ONE))) {
            return n;
        }
        n++;

        ECParameterSpec dominio = new ECParameterSpec(curva, w1, BigInteger.valueOf(19), 1);
        if (dominio.getCofactor() != 1 || !dominio.getOrder().equals(BigInteger.valueOf(19))
                || dominio.getCurve() != curva || dominio.getGenerator() != w1) {
            return n;
        }
        n++;
        if (!iae(() -> new ECParameterSpec(curva, w1, BigInteger.valueOf(19), 0))
                || !iae(() -> new ECParameterSpec(curva, w1, BigInteger.ZERO, 1))) {
            return n;
        }
        n++;
        // El infinito como clave publica significaria d = 0: no es una clave.
        if (!iae(() -> new ECPublicKeySpec(ECPoint.POINT_INFINITY, dominio))) {
            return n;
        }
        n++;
        if (new ECPublicKeySpec(w1, dominio).getW() != w1
                || !new ECPrivateKeySpec(BigInteger.TEN, dominio).getS().equals(BigInteger.TEN)) {
            return n;
        }
        n++;

        // ---- java.security.spec: nombres ---------------------------------------------------------
        if (!NamedParameterSpec.X25519.getName().equals("X25519")
                || !NamedParameterSpec.X448.getName().equals("X448")
                || !NamedParameterSpec.ED25519.getName().equals("Ed25519")
                || !NamedParameterSpec.ED448.getName().equals("Ed448")) {
            return n;
        }
        n++;
        if (!NamedParameterSpec.ML_DSA_44.getName().equals("ML-DSA-44")
                || !NamedParameterSpec.ML_DSA_65.getName().equals("ML-DSA-65")
                || !NamedParameterSpec.ML_DSA_87.getName().equals("ML-DSA-87")) {
            return n;
        }
        n++;
        if (!NamedParameterSpec.ML_KEM_512.getName().equals("ML-KEM-512")
                || !NamedParameterSpec.ML_KEM_768.getName().equals("ML-KEM-768")
                || !NamedParameterSpec.ML_KEM_1024.getName().equals("ML-KEM-1024")) {
            return n;
        }
        n++;
        if (!npe(() -> new NamedParameterSpec(null))) {
            return n;
        }
        n++;
        ECGenParameterSpec gen = new ECGenParameterSpec("secp256r1");
        if (!gen.getName().equals("secp256r1") || !(gen instanceof NamedParameterSpec)) {
            return n;
        }
        n++;

        // ---- java.security.spec: RSA -------------------------------------------------------------
        if (!RSAKeyGenParameterSpec.F0.equals(BigInteger.valueOf(3))
                || !RSAKeyGenParameterSpec.F4.equals(BigInteger.valueOf(65537))) {
            return n;
        }
        n++;
        // Las specs basicas de RSA no validan nada, ni siquiera null: es el contrato real.
        if (new RSAPublicKeySpec(null, null).getModulus() != null) {
            return n;
        }
        n++;
        RSAPrivateCrtKeySpec crt = new RSAPrivateCrtKeySpec(
            BigInteger.valueOf(3233), BigInteger.valueOf(17), BigInteger.valueOf(413),
            BigInteger.valueOf(61), BigInteger.valueOf(53), BigInteger.valueOf(53),
            BigInteger.valueOf(49), BigInteger.valueOf(38));
        if (!crt.getModulus().equals(BigInteger.valueOf(3233))
                || !crt.getPrivateExponent().equals(BigInteger.valueOf(413))
                || !crt.getPublicExponent().equals(BigInteger.valueOf(17))
                || !crt.getPrimeP().equals(BigInteger.valueOf(61))
                || !crt.getPrimeQ().equals(BigInteger.valueOf(53))
                || !crt.getPrimeExponentP().equals(BigInteger.valueOf(53))
                || !crt.getPrimeExponentQ().equals(BigInteger.valueOf(49))
                || !crt.getCrtCoefficient().equals(BigInteger.valueOf(38))
                || crt.getParams() != null) {
            return n;
        }
        n++;
        if (!(crt instanceof RSAPrivateKeySpec)) {
            return n;
        }
        n++;
        RSAOtherPrimeInfo opi = new RSAOtherPrimeInfo(
            BigInteger.valueOf(7), BigInteger.valueOf(5), BigInteger.valueOf(3));
        if (!opi.getPrime().equals(BigInteger.valueOf(7))
                || !opi.getExponent().equals(BigInteger.valueOf(5))
                || !opi.getCrtCoefficient().equals(BigInteger.valueOf(3))) {
            return n;
        }
        n++;
        if (!npe(() -> new RSAOtherPrimeInfo(null, BigInteger.ONE, BigInteger.ONE))) {
            return n;
        }
        n++;
        // Multi-primo: el arreglo vacio se rechaza (seria "multi-primo sin primos extra"), pero
        // null se acepta y significa que no hay.
        if (!iae(() -> multi(new RSAOtherPrimeInfo[0]))) {
            return n;
        }
        n++;
        if (multi(null).getOtherPrimeInfo() != null) {
            return n;
        }
        n++;
        RSAMultiPrimePrivateCrtKeySpec mp = multi(new RSAOtherPrimeInfo[] {opi});
        RSAOtherPrimeInfo[] sacado = mp.getOtherPrimeInfo();
        sacado[0] = null;
        if (mp.getOtherPrimeInfo()[0] != opi) {
            return n;
        }
        n++;
        // Multi-primo hereda de la spec basica, **no** de la de dos primos: dejar que pasara por
        // una haria creer que p y q son toda la factorizacion. Por Object porque el compilador
        // rechaza el instanceof directo, que es justamente lo que se quiere dejar asentado.
        Object mpObj = mp;
        if (!(mp instanceof RSAPrivateKeySpec) || mpObj instanceof RSAPrivateCrtKeySpec) {
            return n;
        }
        n++;

        // ---- java.security.spec: DSA -------------------------------------------------------------
        DSAParameterSpec dsa = new DSAParameterSpec(
            BigInteger.valueOf(23), BigInteger.valueOf(11), BigInteger.valueOf(4));
        if (!dsa.getP().equals(BigInteger.valueOf(23))
                || !dsa.getQ().equals(BigInteger.valueOf(11))
                || !dsa.getG().equals(BigInteger.valueOf(4))) {
            return n;
        }
        n++;
        // Es a la vez descriptor y parametros de una clave: los dos tipos tienen que dar.
        if (!(dsa instanceof AlgorithmParameterSpec)
                || !(dsa instanceof java.security.interfaces.DSAParams)) {
            return n;
        }
        n++;
        DSAPublicKeySpec dpub = new DSAPublicKeySpec(BigInteger.valueOf(8),
            BigInteger.valueOf(23), BigInteger.valueOf(11), BigInteger.valueOf(4));
        DSAPrivateKeySpec dprv = new DSAPrivateKeySpec(BigInteger.valueOf(3),
            BigInteger.valueOf(23), BigInteger.valueOf(11), BigInteger.valueOf(4));
        if (!dpub.getY().equals(BigInteger.valueOf(8))
                || !dprv.getX().equals(BigInteger.valueOf(3))
                || !dpub.getG().equals(dprv.getG())) {
            return n;
        }
        n++;
        // Sin largo de semilla explicito se toma el de q.
        if (new DSAGenParameterSpec(1024, 160).getSeedLength() != 160
                || new DSAGenParameterSpec(2048, 256, 300).getSeedLength() != 300
                || new DSAGenParameterSpec(3072, 256).getPrimePLength() != 3072) {
            return n;
        }
        n++;
        // Solo las combinaciones de FIPS 186-3 son legales.
        if (!iae(() -> new DSAGenParameterSpec(1024, 224))
                || !iae(() -> new DSAGenParameterSpec(999, 160))
                || !iae(() -> new DSAGenParameterSpec(3072, 224))) {
            return n;
        }
        n++;
        // Una semilla mas corta que q le pondria techo a la entropia del dominio entero.
        if (!iae(() -> new DSAGenParameterSpec(2048, 256, 100))) {
            return n;
        }
        n++;

        // ---- java.security.spec: PSS y MGF1 ------------------------------------------------------
        if (!MGF1ParameterSpec.SHA1.getDigestAlgorithm().equals("SHA-1")
                || !MGF1ParameterSpec.SHA512_224.getDigestAlgorithm().equals("SHA-512/224")
                || !MGF1ParameterSpec.SHA512_256.getDigestAlgorithm().equals("SHA-512/256")
                || !MGF1ParameterSpec.SHA3_384.getDigestAlgorithm().equals("SHA3-384")) {
            return n;
        }
        n++;
        if (!MGF1ParameterSpec.SHA256.toString()
                .equals("MGF1ParameterSpec[hashAlgorithm=SHA-256]")) {
            return n;
        }
        n++;
        if (PSSParameterSpec.TRAILER_FIELD_BC != 1) {
            return n;
        }
        n++;
        PSSParameterSpec def = PSSParameterSpec.DEFAULT;
        if (!def.getDigestAlgorithm().equals("SHA-1")
                || !def.getMGFAlgorithm().equals("MGF1")
                || def.getSaltLength() != 20
                || def.getTrailerField() != 1
                || def.getMGFParameters() != MGF1ParameterSpec.SHA1) {
            return n;
        }
        n++;
        // El campo maskGenAlgorithm imprime la spec del MGF, no su nombre. Es raro y es el
        // contrato: hay logs que se comparan contra esto.
        if (!def.toString().equals("PSSParameterSpec[hashAlgorithm=SHA-1, "
                + "maskGenAlgorithm=MGF1ParameterSpec[hashAlgorithm=SHA-1], "
                + "saltLength=20, trailerField=1]")) {
            return n;
        }
        n++;
        if (!new PSSParameterSpec("SHA-256", "MGF1", null, 32, 1).toString()
                .equals("PSSParameterSpec[hashAlgorithm=SHA-256, maskGenAlgorithm=null, "
                    + "saltLength=32, trailerField=1]")) {
            return n;
        }
        n++;
        PSSParameterSpec soloSal = new PSSParameterSpec(32);
        if (!soloSal.getDigestAlgorithm().equals("SHA-1")
                || !soloSal.getMGFAlgorithm().equals("MGF1")
                || soloSal.getSaltLength() != 32
                || soloSal.getTrailerField() != 1) {
            return n;
        }
        n++;
        if (!iae(() -> new PSSParameterSpec(-1))
                || !iae(() -> new PSSParameterSpec("SHA-1", "MGF1", null, 20, -1))) {
            return n;
        }
        n++;
        if (!npe(() -> new PSSParameterSpec(null, "MGF1", null, 20, 1))
                || !npe(() -> new PSSParameterSpec("SHA-1", null, null, 20, 1))) {
            return n;
        }
        n++;

        // ---- java.security.spec: Edwards y Montgomery --------------------------------------------
        EdDSAParameterSpec sinCtx = new EdDSAParameterSpec(true);
        if (!sinCtx.isPrehash() || sinCtx.getContext().isPresent()) {
            return n;
        }
        n++;
        // Un contexto de cero bytes es un contexto: presente y vacio, no ausente.
        if (!new EdDSAParameterSpec(false, new byte[0]).getContext().isPresent()) {
            return n;
        }
        n++;
        byte[] ctx = new byte[] {7, 7};
        EdDSAParameterSpec conCtx = new EdDSAParameterSpec(false, ctx);
        ctx[0] = 0;
        if (conCtx.getContext().get()[0] != 7 || conCtx.isPrehash()) {
            return n;
        }
        n++;
        // El largo del contexto se codifica en un byte: 255 entra, 256 no.
        if (new EdDSAParameterSpec(true, new byte[255]).getContext().get().length != 255) {
            return n;
        }
        n++;
        if (!iae(() -> new EdDSAParameterSpec(true, new byte[256]))) {
            return n;
        }
        n++;
        if (!npe(() -> new EdDSAParameterSpec(true, null))) {
            return n;
        }
        n++;
        EdECPoint edp = new EdECPoint(true, BigInteger.valueOf(5));
        if (!edp.isXOdd() || !edp.getY().equals(BigInteger.valueOf(5))) {
            return n;
        }
        n++;
        if (!npe(() -> new EdECPoint(false, null))) {
            return n;
        }
        n++;
        if (new EdECPublicKeySpec(NamedParameterSpec.ED25519, edp).getPoint() != edp) {
            return n;
        }
        n++;
        byte[] semillaEd = new byte[] {4, 5};
        EdECPrivateKeySpec edPriv =
            new EdECPrivateKeySpec(NamedParameterSpec.ED25519, semillaEd);
        semillaEd[0] = 0;
        if (edPriv.getBytes()[0] != 4
                || edPriv.getParams() != NamedParameterSpec.ED25519) {
            return n;
        }
        n++;
        byte[] esc = new byte[] {6};
        XECPrivateKeySpec xPriv = new XECPrivateKeySpec(NamedParameterSpec.X25519, esc);
        esc[0] = 0;
        if (xPriv.getScalar()[0] != 6) {
            return n;
        }
        n++;
        if (!new XECPublicKeySpec(NamedParameterSpec.X448, BigInteger.TEN)
                .getU().equals(BigInteger.TEN)) {
            return n;
        }
        n++;
        if (!npe(() -> new XECPrivateKeySpec(null, new byte[1]))
                || !npe(() -> new EdECPublicKeySpec(null, edp))) {
            return n;
        }
        n++;

        // ---- java.security.cert: jerarquia de excepciones ----------------------------------------
        if (!(new java.security.cert.CRLException("x") instanceof GeneralSecurityException)
                || !(new java.security.cert.CertStoreException("x")
                        instanceof GeneralSecurityException)
                || !(new java.security.cert.CertPathBuilderException("x")
                        instanceof GeneralSecurityException)) {
            return n;
        }
        n++;
        if (!(new java.security.cert.CertificateExpiredException("x")
                    instanceof java.security.cert.CertificateException)
                || !(new java.security.cert.CertificateNotYetValidException("x")
                    instanceof java.security.cert.CertificateException)
                || !(new java.security.cert.CertificateParsingException("x")
                    instanceof java.security.cert.CertificateException)) {
            return n;
        }
        n++;
        // Los constructores que envuelven una causa la conservan.
        Exception adentro = new IllegalStateException("q");
        if (new java.security.cert.CRLException(adentro).getCause() != adentro) {
            return n;
        }
        n++;

        // ---- java.security.cert: CertPathValidatorException ---------------------------------------
        java.security.cert.CertPathValidatorException cpve =
            new java.security.cert.CertPathValidatorException("m");
        if (cpve.getIndex() != -1 || cpve.getCertPath() != null
                || cpve.getReason()
                    != java.security.cert.CertPathValidatorException.BasicReason.UNSPECIFIED) {
            return n;
        }
        n++;
        // Un indice que no sea -1 sin camino no señala nada, y se rechaza.
        if (!iae(() -> new java.security.cert.CertPathValidatorException("m", null, null, 3))
                || !iae(() -> new java.security.cert.CertPathValidatorException(
                        "m", null, null, -2))) {
            return n;
        }
        n++;
        if (!npe(() -> new java.security.cert.CertPathValidatorException(
                "m", null, null, -1, null))) {
            return n;
        }
        n++;
        // Las dos familias de razones son tipos distintos que implementan la misma interfaz: es lo
        // que permite extender la lista sin tocar la excepcion.
        if (!(java.security.cert.PKIXReason.NO_TRUST_ANCHOR
                    instanceof java.security.cert.CertPathValidatorException.Reason)
                || !(java.security.cert.CertPathValidatorException.BasicReason.REVOKED
                    instanceof java.security.cert.CertPathValidatorException.Reason)) {
            return n;
        }
        n++;

        // ---- java.security.cert: CRLReason -------------------------------------------------------
        // El orden es el codigo del RFC 5280 y no se puede tocar: se codifica como ordinal.
        java.security.cert.CRLReason[] razones = java.security.cert.CRLReason.values();
        if (razones.length != 11
                || razones[0] != java.security.cert.CRLReason.UNSPECIFIED
                || razones[1] != java.security.cert.CRLReason.KEY_COMPROMISE
                || razones[6] != java.security.cert.CRLReason.CERTIFICATE_HOLD
                || razones[7] != java.security.cert.CRLReason.UNUSED
                || razones[10] != java.security.cert.CRLReason.AA_COMPROMISE) {
            return n;
        }
        n++;
        if (java.security.cert.PKIXReason.values().length != 8
                || java.security.cert.CertPathValidatorException.BasicReason.values().length != 7) {
            return n;
        }
        n++;

        // ---- java.security.cert: PolicyQualifierInfo (DER de verdad) -----------------------------
        // SEQUENCE { OID 1.3.6.1.5.5.7.2.1, IA5String "http://ex.com" }
        java.security.cert.PolicyQualifierInfo pqi;
        try {
            pqi = new java.security.cert.PolicyQualifierInfo(hexBytes(
                "30190608" + "2B06010505070201" + "160D" + "687474703A2F2F65782E636F6D"));
        } catch (java.io.IOException e) {
            return n;
        }
        if (!pqi.getPolicyQualifierId().equals("1.3.6.1.5.5.7.2.1")) {
            return n;
        }
        n++;
        if (!hex(pqi.getPolicyQualifier()).equals("160d687474703a2f2f65782e636f6d")) {
            return n;
        }
        n++;
        // Sin calificador: arreglo **vacio**, no null.
        try {
            byte[] q = new java.security.cert.PolicyQualifierInfo(
                hexBytes("300A06082B06010505070201")).getPolicyQualifier();
            if (q == null || q.length != 0) {
                return n;
            }
        } catch (java.io.IOException e) {
            return n;
        }
        n++;
        // El primer sub-identificador junta dos arcos: 0x55 = 85 = 40*2 + 5 -> "2.5".
        if (!oidDe("30050603551D20").equals("2.5.29.32")) {
            return n;
        }
        n++;
        // Componentes de varios bytes: 1.2.840.113549.
        if (!oidDe("300806062A864886F70D").equals("1.2.840.113549")) {
            return n;
        }
        n++;
        // Arco inicial 0, y un componente grande en el medio.
        if (!oidDe("300C060A0992268993F22C640119")
                .equals("0.9.2342.19200300.100.1.25")) {
            return n;
        }
        n++;
        // Largo en forma larga: 0x81 0x05.
        if (!oidDe("308105" + "0603551D20").equals("2.5.29.32")) {
            return n;
        }
        n++;
        // Lo que no es un PolicyQualifierInfo exacto se rechaza: no-SEQUENCE, cola de mas,
        // demasiado corto, y primer elemento que no es un OID.
        if (!ioe("0403060102") || !ioe("30050603551D2000") || !ioe("3000")
                || !ioe("3003020105")) {
            return n;
        }
        n++;

        // ---- java.security.cert: getExtendedKeyUsage ---------------------------------------------
        CertDePrueba c = new CertDePrueba();
        // Sin extension: null, que significa "no restringe para que sirve".
        try {
            if (c.getExtendedKeyUsage() != null) {
                return n;
            }
        } catch (java.security.cert.CertificateParsingException e) {
            return n;
        }
        n++;
        c.exts.put("2.5.29.37", hexBytes(
            "0416" + "3014" + "06082B06010505070301" + "06082B06010505070302"));
        try {
            java.util.List<String> eku = c.getExtendedKeyUsage();
            if (eku.size() != 2 || !eku.get(0).equals("1.3.6.1.5.5.7.3.1")
                    || !eku.get(1).equals("1.3.6.1.5.5.7.3.2")) {
                return n;
            }
        } catch (java.security.cert.CertificateParsingException e) {
            return n;
        }
        n++;
        // SEQUENCE vacio: lista vacia, que significa "no sirve para nada". Es lo opuesto de null.
        c.exts.put("2.5.29.37", hexBytes("0402" + "3000"));
        try {
            if (!c.getExtendedKeyUsage().isEmpty()) {
                return n;
            }
        } catch (java.security.cert.CertificateParsingException e) {
            return n;
        }
        n++;
        // Extension mal formada: excepcion, no una lista inventada.
        c.exts.put("2.5.29.37", hexBytes("0403" + "020105"));
        boolean lanzo = false;
        try {
            c.getExtendedKeyUsage();
        } catch (java.security.cert.CertificateParsingException e) {
            lanzo = true;
        }
        if (!lanzo) {
            return n;
        }
        n++;
        c.exts.remove("2.5.29.37");

        // ---- java.security.cert: getRevocationReason ---------------------------------------------
        EntradaDePrueba ent = new EntradaDePrueba();
        // Sin extensiones no hay razon.
        if (ent.getRevocationReason() != null) {
            return n;
        }
        n++;
        ent.exts.put("2.5.29.21", hexBytes("0403" + "0A0101"));
        if (ent.getRevocationReason() != java.security.cert.CRLReason.KEY_COMPROMISE) {
            return n;
        }
        n++;
        ent.exts.put("2.5.29.21", hexBytes("0403" + "0A010A"));
        if (ent.getRevocationReason() != java.security.cert.CRLReason.AA_COMPROMISE) {
            return n;
        }
        n++;
        // Un codigo que no esta en la lista da UNSPECIFIED y no una excepcion: no entender la razon
        // no cambia que este revocado.
        ent.exts.put("2.5.29.21", hexBytes("0403" + "0A0163"));
        if (ent.getRevocationReason() != java.security.cert.CRLReason.UNSPECIFIED) {
            return n;
        }
        n++;
        // Una extension mal formada da null, tambien sin lanzar.
        ent.exts.put("2.5.29.21", hexBytes("0403" + "020101"));
        if (ent.getRevocationReason() != null) {
            return n;
        }
        n++;
        // Igualdad por codificacion.
        EntradaDePrueba ent2 = new EntradaDePrueba();
        if (!ent.equals(ent2) || ent.hashCode() != ent2.hashCode()) {
            return n;
        }
        n++;
        ent2.codificado = new byte[] {9};
        if (ent.equals(ent2)) {
            return n;
        }
        n++;

        // ---- java.security.cert: TrustAnchor y resultados PKIX -----------------------------------
        CertDePrueba raiz = new CertDePrueba();
        java.security.cert.TrustAnchor ta = new java.security.cert.TrustAnchor(raiz, null);
        if (ta.getTrustedCert() != raiz || ta.getCAPublicKey() != null
                || ta.getNameConstraints() != null) {
            return n;
        }
        n++;
        // Las restricciones se copian. Se usa un NameConstraints valido —un SEQUENCE vacio— y no
        // bytes cualesquiera a proposito: el JDK **valida** el DER en el constructor y esta
        // biblioteca no, que es la unica diferencia de comportamiento anotada en `TrustAnchor`. La
        // comprobacion se queda en lo que las dos hacen igual.
        byte[] nc = hexBytes("3000");
        java.security.cert.TrustAnchor ta2 = new java.security.cert.TrustAnchor(raiz, nc);
        nc[0] = 9;
        byte[] ncLeido = ta2.getNameConstraints();
        if (ncLeido.length != 2 || (ncLeido[0] & 0xff) != 0x30) {
            return n;
        }
        n++;
        ncLeido[0] = 0;
        if ((ta2.getNameConstraints()[0] & 0xff) != 0x30) {
            return n;
        }
        n++;
        if (!npe(() -> new java.security.cert.TrustAnchor(
                (java.security.cert.X509Certificate) null, null))) {
            return n;
        }
        n++;
        ClaveDePrueba pk = new ClaveDePrueba(new byte[] {1});
        java.security.cert.PKIXCertPathValidatorResult vr =
            new java.security.cert.PKIXCertPathValidatorResult(ta, null, pk);
        if (vr.getTrustAnchor() != ta || vr.getPolicyTree() != null || vr.getPublicKey() != pk) {
            return n;
        }
        n++;
        if (!npe(() -> new java.security.cert.PKIXCertPathValidatorResult(ta, null, null))
                || !npe(() -> new java.security.cert.PKIXCertPathValidatorResult(null, null, pk))) {
            return n;
        }
        n++;
        if (vr.clone() == vr || !(vr.clone()
                instanceof java.security.cert.PKIXCertPathValidatorResult)) {
            return n;
        }
        n++;

        // ---- java.security.cert: PKIXParameters --------------------------------------------------
        Set<java.security.cert.TrustAnchor> anclas =
            new java.util.HashSet<java.security.cert.TrustAnchor>();
        anclas.add(ta);
        java.security.cert.PKIXParameters pp;
        try {
            pp = new java.security.cert.PKIXParameters(anclas);
        } catch (java.security.InvalidAlgorithmParameterException e) {
            return n;
        }
        // Los defaults, que son la politica de seguridad de la validacion.
        if (!pp.isRevocationEnabled() || !pp.getPolicyQualifiersRejected()
                || pp.isExplicitPolicyRequired() || pp.isPolicyMappingInhibited()
                || pp.isAnyPolicyInhibited() || pp.getDate() != null
                || pp.getSigProvider() != null || pp.getTargetCertConstraints() != null) {
            return n;
        }
        n++;
        if (!pp.getInitialPolicies().isEmpty() || !pp.getCertStores().isEmpty()
                || !pp.getCertPathCheckers().isEmpty() || pp.getTrustAnchors().size() != 1) {
            return n;
        }
        n++;
        // Las anclas salen inmutables: el validador se queda con ellas y nadie las puede cambiar.
        try {
            pp.getTrustAnchors().clear();
            return n;
        } catch (UnsupportedOperationException e) {
            n++;
        }
        // Sin anclas no hay donde terminar la cadena.
        if (!iape(() -> new java.security.cert.PKIXParameters(
                new java.util.HashSet<java.security.cert.TrustAnchor>()))) {
            return n;
        }
        n++;
        if (!npeL(() -> new java.security.cert.PKIXParameters(
                (Set<java.security.cert.TrustAnchor>) null))) {
            return n;
        }
        n++;
        // La fecha se copia en las dos direcciones.
        java.util.Date f = new java.util.Date(1000);
        pp.setDate(f);
        f.setTime(5000);
        if (pp.getDate().getTime() != 1000) {
            return n;
        }
        n++;
        pp.getDate().setTime(7000);
        if (pp.getDate().getTime() != 1000) {
            return n;
        }
        n++;
        pp.setInitialPolicies(null);
        pp.setCertStores(null);
        if (!pp.getInitialPolicies().isEmpty() || !pp.getCertStores().isEmpty()) {
            return n;
        }
        n++;
        if (!(pp.clone() instanceof java.security.cert.PKIXParameters)) {
            return n;
        }
        n++;
        java.security.cert.PKIXBuilderParameters bp;
        try {
            bp = new java.security.cert.PKIXBuilderParameters(anclas, null);
        } catch (java.security.InvalidAlgorithmParameterException e) {
            return n;
        }
        // El default de 5 es lo que impide que la busqueda se vaya al infinito con CAs cruzadas.
        if (bp.getMaxPathLength() != 5) {
            return n;
        }
        n++;
        bp.setMaxPathLength(-1);
        if (bp.getMaxPathLength() != -1) {
            return n;
        }
        n++;
        // -1 es "sin limite" y es el unico negativo aceptado.
        if (!iae(() -> bp.setMaxPathLength(-2))) {
            return n;
        }
        n++;
        if (!(bp instanceof java.security.cert.PKIXParameters) || !bp.isRevocationEnabled()) {
            return n;
        }
        n++;

        // ---- java.security.cert: parametros de CertStore ------------------------------------------
        // La coleccion **no** se copia: es lo que permite que el store crezca despues de creado.
        java.util.List<String> col = new java.util.ArrayList<String>();
        java.security.cert.CollectionCertStoreParameters ccsp =
            new java.security.cert.CollectionCertStoreParameters(col);
        col.add("x");
        if (ccsp.getCollection().size() != 1) {
            return n;
        }
        n++;
        if (!new java.security.cert.CollectionCertStoreParameters().getCollection().isEmpty()) {
            return n;
        }
        n++;
        if (!npe(() -> new java.security.cert.CollectionCertStoreParameters(null))) {
            return n;
        }
        n++;
        java.security.cert.LDAPCertStoreParameters ldap =
            new java.security.cert.LDAPCertStoreParameters();
        if (!ldap.getServerName().equals("localhost") || ldap.getPort() != 389
                || new java.security.cert.LDAPCertStoreParameters("h").getPort() != 389
                || new java.security.cert.LDAPCertStoreParameters("h", 1).getPort() != 1) {
            return n;
        }
        n++;
        if (!ldap.toString().equals(
                "LDAPCertStoreParameters: [\n  serverName: localhost\n  port: 389\n]")) {
            return n;
        }
        n++;
        java.net.URI u = java.net.URI.create("http://a/b");
        java.security.cert.URICertStoreParameters ucsp =
            new java.security.cert.URICertStoreParameters(u);
        // Clonar da una instancia **nueva** aunque la clase sea inmutable, y eso es observable.
        if (ucsp.getURI() != u || ucsp.clone() == ucsp || !ucsp.clone().equals(ucsp)
                || !ucsp.equals(new java.security.cert.URICertStoreParameters(u))
                || !ucsp.toString().equals("URICertStoreParameters: http://a/b")) {
            return n;
        }
        n++;

        // ---- java.security.cert: los tipos por default -------------------------------------------
        if (!java.security.cert.CertPathBuilder.getDefaultType().equals("PKIX")
                || !java.security.cert.CertPathValidator.getDefaultType().equals("PKIX")
                || !java.security.cert.CertStore.getDefaultType().equals("LDAP")) {
            return n;
        }
        n++;

        // ---- java.security.cert: X509CertSelector -------------------------------------------------
        java.security.cert.X509CertSelector sel = new java.security.cert.X509CertSelector();
        // Un selector sin criterios acepta cualquier certificado X.509, y nada que no lo sea.
        if (!sel.match(c) || sel.match(null)) {
            return n;
        }
        n++;
        if (sel.getBasicConstraints() != -1 || !sel.getMatchAllSubjectAltNames()
                || sel.getKeyUsage() != null || sel.getExtendedKeyUsage() != null
                || sel.getPolicy() != null || sel.getCertificate() != null
                || sel.getSerialNumber() != null || sel.getCertificateValid() != null
                || sel.getSubjectPublicKey() != null || sel.getSubjectPublicKeyAlgID() != null) {
            return n;
        }
        n++;
        sel.setSerialNumber(java.math.BigInteger.ONE);
        if (!sel.match(c)) {
            return n;
        }
        n++;
        sel.setSerialNumber(java.math.BigInteger.valueOf(2));
        if (sel.match(c)) {
            return n;
        }
        n++;
        sel.setSerialNumber(null);
        // -2 exige entidad final; el certificado dice -1, o sea que no es CA.
        sel.setBasicConstraints(-2);
        if (!sel.match(c)) {
            return n;
        }
        n++;
        sel.setBasicConstraints(0);
        if (sel.match(c)) {
            return n;
        }
        n++;
        c.bc = 3;
        if (!sel.match(c)) {
            return n;
        }
        n++;
        sel.setBasicConstraints(4);
        if (sel.match(c)) {
            return n;
        }
        n++;
        if (!iae(() -> sel.setBasicConstraints(-3))) {
            return n;
        }
        n++;
        sel.setBasicConstraints(-1);
        c.bc = -1;
        // KeyUsage: un certificado **sin** la extension pasa igual.
        sel.setKeyUsage(new boolean[] {false, false, false, false, false, true});
        if (!sel.match(c)) {
            return n;
        }
        n++;
        c.usos = new boolean[] {true, false, false, false, false, false};
        if (sel.match(c)) {
            return n;
        }
        n++;
        c.usos = new boolean[] {true, false, false, false, false, true};
        if (!sel.match(c)) {
            return n;
        }
        n++;
        // El getter copia.
        boolean[] leidos = sel.getKeyUsage();
        leidos[5] = false;
        if (!sel.getKeyUsage()[5]) {
            return n;
        }
        n++;
        sel.setKeyUsage(null);
        c.usos = null;
        // ExtendedKeyUsage: sin extension pasa; con anyExtendedKeyUsage tambien.
        try {
            sel.setExtendedKeyUsage(new java.util.HashSet<String>(
                java.util.Arrays.asList("1.3.6.1.5.5.7.3.1")));
        } catch (java.io.IOException e) {
            return n;
        }
        if (!sel.match(c)) {
            return n;
        }
        n++;
        c.exts.put("2.5.29.37", hexBytes("040C" + "300A" + "06082B06010505070302"));
        if (sel.match(c)) {
            return n;
        }
        n++;
        c.exts.put("2.5.29.37", hexBytes("040C" + "300A" + "06082B06010505070301"));
        if (!sel.match(c)) {
            return n;
        }
        n++;
        // 2.5.29.37.0 es anyExtendedKeyUsage: sirve para todo. Su OID son cuatro bytes —55 1D 25
        // 00— porque el ultimo arco es 0 y se codifica igual que cualquier otro.
        c.exts.put("2.5.29.37", hexBytes("0408" + "3006" + "0604551D2500"));
        if (!sel.match(c)) {
            return n;
        }
        n++;
        // Un conjunto vacio se trata como "sin criterio".
        try {
            sel.setExtendedKeyUsage(new java.util.HashSet<String>());
        } catch (java.io.IOException e) {
            return n;
        }
        if (sel.getExtendedKeyUsage() != null) {
            return n;
        }
        n++;
        // Los OIDs se validan al ponerlos, con las reglas de la codificacion DER.
        if (!ioeSet(sel, "no-es-oid") || !ioeSet(sel, "1") || !ioeSet(sel, "3.1.1")
                || !ioeSet(sel, "1.40")) {
            return n;
        }
        n++;
        c.exts.remove("2.5.29.37");
        // SubjectKeyIdentifier: lo que se compara es el DER de adentro del OCTET STRING exterior.
        sel.setSubjectKeyIdentifier(hexBytes("0403AABBCC"));
        if (sel.match(c)) {
            return n;
        }
        n++;
        c.exts.put("2.5.29.14", hexBytes("0405" + "0403AABBCC"));
        if (!sel.match(c)) {
            return n;
        }
        n++;
        c.exts.put("2.5.29.14", hexBytes("0405" + "0403AABBCD"));
        if (sel.match(c)) {
            return n;
        }
        n++;
        // El setter copia.
        byte[] skid = hexBytes("0403AABBCD");
        sel.setSubjectKeyIdentifier(skid);
        skid[2] = 0;
        if (!sel.match(c)) {
            return n;
        }
        n++;
        sel.setSubjectKeyIdentifier(null);
        c.exts.remove("2.5.29.14");
        // Vigencia: el selector delega en checkValidity del certificado.
        c.desde = 1000;
        c.hasta = 2000;
        sel.setCertificateValid(new java.util.Date(1500));
        if (!sel.match(c)) {
            return n;
        }
        n++;
        sel.setCertificateValid(new java.util.Date(500));
        if (sel.match(c)) {
            return n;
        }
        n++;
        sel.setCertificateValid(new java.util.Date(3000));
        if (sel.match(c)) {
            return n;
        }
        n++;
        sel.setCertificateValid(null);
        c.desde = 0;
        c.hasta = Long.MAX_VALUE;
        // Clave publica exacta: se compara por codificacion.
        c.clave = new ClaveDePrueba(hexBytes("300D" + "300906052B0E03021A0500" + "0300"));
        sel.setSubjectPublicKey(new ClaveDePrueba(hexBytes(
            "300D" + "300906052B0E03021A0500" + "0300")));
        if (!sel.match(c)) {
            return n;
        }
        n++;
        sel.setSubjectPublicKey(new ClaveDePrueba(hexBytes("3000")));
        if (sel.match(c)) {
            return n;
        }
        n++;
        // El cast desambigua: en el JDK real hay tambien una sobrecarga que recibe byte[], que aca
        // no esta porque construir una clave desde su DER necesitaria una KeyFactory registrada.
        sel.setSubjectPublicKey((java.security.PublicKey) null);
        // El OID del algoritmo sale del SubjectPublicKeyInfo: 1.3.14.3.2.26 es SHA-1.
        try {
            sel.setSubjectPublicKeyAlgID("1.3.14.3.2.26");
        } catch (java.io.IOException e) {
            return n;
        }
        if (!sel.match(c)) {
            return n;
        }
        n++;
        try {
            sel.setSubjectPublicKeyAlgID("1.2.840.113549.1.1.1");
        } catch (java.io.IOException e) {
            return n;
        }
        if (sel.match(c)) {
            return n;
        }
        n++;
        try {
            sel.setSubjectPublicKeyAlgID(null);
        } catch (java.io.IOException e) {
            return n;
        }
        // Politicas: el conjunto vacio pide que la extension exista con algo adentro.
        try {
            sel.setPolicy(new java.util.HashSet<String>());
        } catch (java.io.IOException e) {
            return n;
        }
        if (sel.match(c)) {
            return n;
        }
        n++;
        // SEQUENCE { PolicyInformation SEQUENCE { OID 2.5.29.32.0 } }, o sea anyPolicy.
        c.exts.put("2.5.29.32", hexBytes("040A" + "3008" + "3006" + "0604551D2000"));
        if (!sel.match(c)) {
            return n;
        }
        n++;
        try {
            sel.setPolicy(new java.util.HashSet<String>(
                java.util.Arrays.asList("2.5.29.32.0")));
        } catch (java.io.IOException e) {
            return n;
        }
        if (!sel.match(c)) {
            return n;
        }
        n++;
        try {
            sel.setPolicy(new java.util.HashSet<String>(java.util.Arrays.asList("1.2.3")));
        } catch (java.io.IOException e) {
            return n;
        }
        if (sel.match(c)) {
            return n;
        }
        n++;
        try {
            sel.setPolicy(null);
        } catch (java.io.IOException e) {
            return n;
        }
        c.exts.remove("2.5.29.32");
        // El certificado exacto hace redundante a todo lo demas. La comparacion es **por la
        // codificacion**, no por identidad: por eso el otro certificado tiene que codificar
        // distinto para que no coincida.
        CertDePrueba otro = new CertDePrueba();
        otro.codificado = new byte[] {9, 9, 9};
        sel.setCertificate(c);
        if (!sel.match(c) || sel.match(otro)) {
            return n;
        }
        n++;
        // Y uno distinto que codifica igual **si** coincide, que es lo que dice el contrato de
        // `Certificate.equals`.
        CertDePrueba gemelo = new CertDePrueba();
        if (!sel.match(gemelo)) {
            return n;
        }
        n++;
        sel.setCertificate(null);
        if (!(sel.clone() instanceof java.security.cert.X509CertSelector)) {
            return n;
        }
        n++;

        // ---- java.security.cert: X509CRLSelector --------------------------------------------------
        java.security.cert.X509CRLSelector csel = new java.security.cert.X509CRLSelector();
        CrlDePrueba crl = new CrlDePrueba();
        if (!csel.match(crl) || csel.match(null)) {
            return n;
        }
        n++;
        if (csel.getMinCRL() != null || csel.getMaxCRL() != null
                || csel.getDateAndTime() != null || csel.getCertificateChecking() != null) {
            return n;
        }
        n++;
        csel.setMinCRLNumber(java.math.BigInteger.valueOf(5));
        // Sin extension de numero de CRL no se puede satisfacer un criterio sobre el numero.
        if (csel.match(crl)) {
            return n;
        }
        n++;
        crl.exts.put("2.5.29.20", hexBytes("0403" + "02010A"));
        if (!csel.match(crl)) {
            return n;
        }
        n++;
        crl.exts.put("2.5.29.20", hexBytes("0403" + "020103"));
        if (csel.match(crl)) {
            return n;
        }
        n++;
        csel.setMinCRLNumber(null);
        csel.setMaxCRLNumber(java.math.BigInteger.valueOf(5));
        if (!csel.match(crl)) {
            return n;
        }
        n++;
        crl.exts.put("2.5.29.20", hexBytes("0403" + "02010A"));
        if (csel.match(crl)) {
            return n;
        }
        n++;
        csel.setMaxCRLNumber(null);
        crl.exts.clear();
        // Fecha: los dos extremos entran, y sin nextUpdate no se puede afirmar nada.
        csel.setDateAndTime(new java.util.Date(5000));
        crl.thisU = new java.util.Date(1000);
        crl.nextU = new java.util.Date(9000);
        if (!csel.match(crl)) {
            return n;
        }
        n++;
        crl.thisU = new java.util.Date(5000);
        if (!csel.match(crl)) {
            return n;
        }
        n++;
        crl.nextU = new java.util.Date(5000);
        if (!csel.match(crl)) {
            return n;
        }
        n++;
        crl.thisU = new java.util.Date(6000);
        crl.nextU = new java.util.Date(9000);
        if (csel.match(crl)) {
            return n;
        }
        n++;
        crl.thisU = new java.util.Date(1000);
        crl.nextU = new java.util.Date(4000);
        if (csel.match(crl)) {
            return n;
        }
        n++;
        crl.nextU = null;
        if (csel.match(crl)) {
            return n;
        }
        n++;
        // La fecha se copia en las dos direcciones.
        csel.getDateAndTime().setTime(1);
        if (csel.getDateAndTime().getTime() != 5000) {
            return n;
        }
        n++;
        // `certificateChecking` no es un criterio: no cambia lo que match devuelve.
        csel.setDateAndTime(null);
        csel.setCertificateChecking(c);
        if (!csel.match(crl) || csel.getCertificateChecking() != c) {
            return n;
        }
        n++;
        if (!(csel.clone() instanceof java.security.cert.X509CRLSelector)) {
            return n;
        }
        n++;

        // ---- java.security.Signature: la maquina de estados -------------------------------------
        FirmaDePrueba fs = new FirmaDePrueba();
        if (!fs.getAlgorithm().equals("SumaDePrueba") || fs.getProvider() != null) {
            return n;
        }
        n++;
        if (!fs.toString().equals("Signature object: SumaDePrueba<not initialized>")) {
            return n;
        }
        n++;
        // Sin inicializar no se puede alimentar ni cerrar: son SignatureException, no silencio.
        if (!se(() -> fs.update((byte) 1)) || !se(() -> fs.sign())
                || !se(() -> fs.verify(new byte[2]))) {
            return n;
        }
        n++;
        try {
            fs.initSign(null);
        } catch (java.security.InvalidKeyException e) {
            return n;
        }
        if (!fs.toString().equals("Signature object: SumaDePrueba<initialized for signing>")) {
            return n;
        }
        n++;
        // Firmar estando en modo firma anda; verificar en modo firma no.
        if (!se(() -> fs.verify(new byte[2]))) {
            return n;
        }
        n++;
        byte[] firma;
        try {
            fs.update(new byte[] {1, 2, 3});
            firma = fs.sign();
        } catch (java.security.SignatureException e) {
            return n;
        }
        if (firma.length != 2 || firma[1] != 6) {
            return n;
        }
        n++;
        // El objeto queda listo para otra operacion con la misma clave.
        try {
            fs.update((byte) 10);
            byte[] otra = fs.sign();
            if (otra[1] != 10) {
                return n;
            }
        } catch (java.security.SignatureException e) {
            return n;
        }
        n++;
        // Validacion de argumentos de update: los tres casos son IllegalArgumentException, no
        // SignatureException, porque son errores del llamador.
        if (!iaeL(() -> upd(fs, null, 0, 0)) || !iaeL(() -> upd(fs, new byte[3], -1, 1))
                || !iaeL(() -> upd(fs, new byte[3], 0, 4))) {
            return n;
        }
        n++;
        // Idem para sign en un buffer dado.
        if (!iaeL(() -> fs.sign(null, 0, 2)) || !iaeL(() -> fs.sign(new byte[2], -1, 2))
                || !iaeL(() -> fs.sign(new byte[2], 0, 3))) {
            return n;
        }
        n++;
        // El default de SignatureSpi no trunca: si el buffer no alcanza para la firma entera,
        // lanza en vez de devolver algo que parece una firma.
        if (!se(() -> fs.sign(new byte[1], 0, 1))) {
            return n;
        }
        n++;
        try {
            byte[] dest = new byte[5];
            fs.update((byte) 7);
            int puestos = fs.sign(dest, 1, 4);
            if (puestos != 2 || dest[2] != 7 || dest[0] != 0) {
                return n;
            }
        } catch (java.security.SignatureException e) {
            return n;
        }
        n++;
        // update(ByteBuffer) consume el buffer entero.
        try {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(new byte[] {4, 5});
            fs.update(bb);
            if (bb.remaining() != 0) {
                return n;
            }
            byte[] r = fs.sign();
            if (r[1] != 9) {
                return n;
            }
        } catch (java.security.SignatureException e) {
            return n;
        }
        n++;
        if (!npeL(() -> updBb(fs, null))) {
            return n;
        }
        n++;
        // Verificacion: devuelve false, **no lanza**, cuando la firma no vale.
        try {
            fs.initVerify((java.security.PublicKey) null);
            fs.update(new byte[] {1, 2, 3});
            if (!fs.verify(new byte[] {0, 6})) {
                return n;
            }
            fs.update(new byte[] {1, 2, 3});
            if (fs.verify(new byte[] {0, 7})) {
                return n;
            }
        } catch (Exception e) {
            return n;
        }
        n++;
        if (!fs.toString().equals("Signature object: SumaDePrueba<initialized for verifying>")) {
            return n;
        }
        n++;
        // En modo verificacion no se puede firmar.
        if (!se(() -> fs.sign())) {
            return n;
        }
        n++;
        // El default de engineVerify(byte[],int,int) recorta y delega.
        try {
            fs.update(new byte[] {1, 2, 3});
            if (!fs.verify(new byte[] {9, 0, 6, 9}, 1, 2)) {
                return n;
            }
        } catch (java.security.SignatureException e) {
            return n;
        }
        n++;
        // initVerify(Certificate) comprueba KeyUsage, pero **solo si la extension viene marcada
        // como critica**. Con el bit 0 (digitalSignature) apagado y la extension critica, la clave
        // se rechaza.
        CertDePrueba certFirma = new CertDePrueba();
        certFirma.clave = new ClaveDePrueba(new byte[] {1});
        certFirma.usos = new boolean[] {false, true, true};
        certFirma.criticas = new java.util.HashSet<String>(
            java.util.Arrays.asList("2.5.29.15"));
        if (!ike(() -> fs.initVerify(certFirma))) {
            return n;
        }
        n++;
        // El mismo KeyUsage, pero **no critico**, no rechaza nada: una extension no critica es una
        // recomendacion que quien no la entienda puede ignorar.
        certFirma.criticas = null;
        try {
            fs.initVerify(certFirma);
        } catch (java.security.InvalidKeyException e) {
            return n;
        }
        n++;
        // Con el bit 0 prendido pasa, critica o no.
        certFirma.criticas = new java.util.HashSet<String>(
            java.util.Arrays.asList("2.5.29.15"));
        certFirma.usos = new boolean[] {true, false, false};
        try {
            fs.initVerify(certFirma);
        } catch (java.security.InvalidKeyException e) {
            return n;
        }
        n++;
        // Un certificado sin extension KeyUsage no restringe nada y pasa.
        certFirma.usos = null;
        try {
            fs.initVerify(certFirma);
        } catch (java.security.InvalidKeyException e) {
            return n;
        }
        n++;
        // Los parametros modernos y `getParameters` tiran UnsupportedOperationException por default:
        // el SPI base no los soporta y no finge que si.
        if (!uoe(() -> fs.setParameter(
                    (java.security.spec.AlgorithmParameterSpec) NamedParameterSpec.X25519))
                || !uoe(() -> fs.getParameters())) {
            return n;
        }
        n++;
        // Los viejos, por nombre, llegan al SPI y este los rechaza.
        if (!ipe(() -> fs.setParameter("x", "y")) || !ipe(() -> fs.getParameter("x"))) {
            return n;
        }
        n++;
        // No es Cloneable, asi que no se clona: copiar una firma a medio calcular sin que el
        // proveedor lo haya pensado daria dos objetos compartiendo el estado.
        if (!cnse(() -> fs.clone())) {
            return n;
        }
        n++;
        // Los tres estados son constantes del contrato con las subclases, y `state` refleja en cual
        // esta. Se leen desde la subclase porque son `protected`.
        int[] ctes = fs.constantes();
        if (ctes[0] != 0 || ctes[1] != 2 || ctes[2] != 3 || fs.estado() != ctes[2]) {
            return n;
        }
        n++;

        // ---- java.security.KeyStore ---------------------------------------------------------------
        if (!java.security.KeyStore.getDefaultType().equals("pkcs12")) {
            return n;
        }
        n++;
        AlmacenSpiDePrueba spi = new AlmacenSpiDePrueba();
        java.security.KeyStore ks = new AlmacenDePrueba(spi);
        if (!ks.getType().equals("prueba") || ks.getProvider() != null) {
            return n;
        }
        n++;
        // Casi todo `KeyStore` declara excepciones chequeadas; se envuelve el bloque entero en vez
        // de repetir un try por linea. Cualquier excepcion que llegue aca es una falla de la
        // comprobacion en curso.
        try {
        // Sin `load` el almacen no responde nada: no es que este vacio, es que no se sabe.
        if (!kse(() -> ks.size()) || !kse(() -> ks.aliases())
                || !kse(() -> ks.containsAlias("x"))
                || !kse(() -> ks.getCertificate("x"))) {
            return n;
        }
        n++;
        try {
            ks.load(null, null);
        } catch (Exception e) {
            return n;
        }
        if (ks.size() != 0) {
            return n;
        }
        n++;
        CertDePrueba raizAlmacen = new CertDePrueba();
        raizAlmacen.codificado = new byte[] {4, 4};
        ks.setCertificateEntry("raiz", raizAlmacen);
        if (ks.size() != 1 || !ks.containsAlias("raiz") || !ks.isCertificateEntry("raiz")
                || ks.isKeyEntry("raiz") || ks.getCertificate("raiz") != raizAlmacen) {
            return n;
        }
        n++;
        // El alias se encuentra comparando por codificacion, no por identidad.
        CertDePrueba mismoContenido = new CertDePrueba();
        mismoContenido.codificado = new byte[] {4, 4};
        if (!"raiz".equals(ks.getCertificateAlias(mismoContenido))) {
            return n;
        }
        n++;
        if (!ks.aliases().hasMoreElements()) {
            return n;
        }
        n++;
        // Entradas tipadas: sin proteccion se puede sacar un certificado de confianza, porque no
        // hay nada secreto que descifrar.
        java.security.KeyStore.Entry ent1 = ks.getEntry("raiz", null);
        if (!(ent1 instanceof java.security.KeyStore.TrustedCertificateEntry)
                || ((java.security.KeyStore.TrustedCertificateEntry) ent1).getTrustedCertificate()
                    != raizAlmacen) {
            return n;
        }
        n++;
        if (ks.getEntry("no-existe", null) != null) {
            return n;
        }
        n++;
        if (!ks.entryInstanceOf("raiz", java.security.KeyStore.TrustedCertificateEntry.class)
                || ks.entryInstanceOf("raiz", java.security.KeyStore.PrivateKeyEntry.class)) {
            return n;
        }
        n++;
        CertDePrueba otroCert = new CertDePrueba();
        otroCert.codificado = new byte[] {8, 8};
        ks.setEntry("otro", new java.security.KeyStore.TrustedCertificateEntry(otroCert), null);
        if (ks.getCertificate("otro") != otroCert || ks.size() != 2) {
            return n;
        }
        n++;
        ks.deleteEntry("otro");
        if (ks.size() != 1) {
            return n;
        }
        n++;
        // Una clave privada sin cadena no se guarda: sin el certificado que la publica no serviria
        // para nada. Y es IllegalArgumentException, no KeyStoreException: le falto un argumento al
        // llamador, no fallo el almacen.
        if (!iaeL(() -> ks.setKeyEntry("k", new ClavePrivadaDePrueba(), new char[0], null))) {
            return n;
        }
        n++;
        // Los atributos de una entrada sin atributos son un conjunto vacio, no null.
        if (!ks.getAttributes("raiz").isEmpty()) {
            return n;
        }
        n++;
        // `setEntry` con entrada null es NPE, y se chequea **antes** que si el almacen esta
        // cargado: es un error del llamador, no del almacen.
        if (!npeL(() -> ks.setEntry("x", null, null))
                || !npeL(() -> ks.entryInstanceOf(null,
                    java.security.KeyStore.TrustedCertificateEntry.class))) {
            return n;
        }
        n++;

        // Las entradas tipadas validan lo suyo.
        ClavePrivadaDePrueba cpk = new ClavePrivadaDePrueba();
        CertDePrueba certRsa = new CertDePrueba();
        certRsa.clave = new ClaveDePrueba(new byte[] {1});
        java.security.KeyStore.PrivateKeyEntry pke =
            new java.security.KeyStore.PrivateKeyEntry(cpk,
                new java.security.cert.Certificate[] {certRsa});
        if (pke.getPrivateKey() != cpk || pke.getCertificate() != certRsa
                || pke.getCertificateChain().length != 1
                || !pke.getAttributes().isEmpty()) {
            return n;
        }
        n++;
        // La cadena se copia.
        java.security.cert.Certificate[] cad = pke.getCertificateChain();
        cad[0] = null;
        if (pke.getCertificateChain()[0] != certRsa) {
            return n;
        }
        n++;
        if (!npe(() -> new java.security.KeyStore.PrivateKeyEntry(null,
                    new java.security.cert.Certificate[] {certRsa}))
                || !npe(() -> new java.security.KeyStore.PrivateKeyEntry(cpk, null))) {
            return n;
        }
        n++;
        if (!iae(() -> new java.security.KeyStore.PrivateKeyEntry(cpk,
                new java.security.cert.Certificate[0]))) {
            return n;
        }
        n++;
        // El algoritmo de la privada tiene que coincidir con el de la publica del certificado: no
        // se comprueba que sean el par, pero si que no sean de familias distintas.
        CertDePrueba certOtroAlg = new CertDePrueba();
        certOtroAlg.clave = new ClaveDePruebaEC(new byte[] {1});
        if (!iae(() -> new java.security.KeyStore.PrivateKeyEntry(cpk,
                new java.security.cert.Certificate[] {certOtroAlg}))) {
            return n;
        }
        n++;
        java.security.KeyStore.TrustedCertificateEntry tce =
            new java.security.KeyStore.TrustedCertificateEntry(raizAlmacen);
        if (tce.getTrustedCertificate() != raizAlmacen || !tce.getAttributes().isEmpty()) {
            return n;
        }
        n++;
        if (!npe(() -> new java.security.KeyStore.TrustedCertificateEntry(null))) {
            return n;
        }
        n++;
        // Un Builder sobre un almacen ya cargado devuelve siempre el mismo.
        java.security.KeyStore.ProtectionParameter prot =
            new java.security.KeyStore.ProtectionParameter() { };
        java.security.KeyStore.Builder b =
            java.security.KeyStore.Builder.newInstance(ks, prot);
        if (b.getKeyStore() != ks || b.getProtectionParameter("raiz") != prot) {
            return n;
        }
        n++;
        if (!npe(() -> java.security.KeyStore.Builder.newInstance(
                    (java.security.KeyStore) null, prot))
                || !npeL(() -> b.getProtectionParameter(null))) {
            return n;
        }
        n++;
        } catch (Exception e) {
            return n;
        }

        // ---- java.security.DrbgParameters ---------------------------------------------------------
        java.security.DrbgParameters.Instantiation inst =
            java.security.DrbgParameters.instantiation(128,
                java.security.DrbgParameters.Capability.PR_AND_RESEED, null);
        if (inst.getStrength() != 128
                || inst.getCapability()
                    != java.security.DrbgParameters.Capability.PR_AND_RESEED
                || inst.getPersonalizationString() != null) {
            return n;
        }
        n++;
        if (!inst.toString().equals("128,pr_and_reseed,null")) {
            return n;
        }
        n++;
        if (!java.security.DrbgParameters.instantiation(-1,
                java.security.DrbgParameters.Capability.NONE, new byte[] {1})
                .toString().equals("-1,none,[1]")) {
            return n;
        }
        n++;
        // Las tres capacidades y lo que implican: PR obliga a poder resembrar.
        if (!java.security.DrbgParameters.Capability.PR_AND_RESEED.supportsReseeding()
                || !java.security.DrbgParameters.Capability.PR_AND_RESEED
                    .supportsPredictionResistance()
                || !java.security.DrbgParameters.Capability.RESEED_ONLY.supportsReseeding()
                || java.security.DrbgParameters.Capability.RESEED_ONLY
                    .supportsPredictionResistance()
                || java.security.DrbgParameters.Capability.NONE.supportsReseeding()
                || java.security.DrbgParameters.Capability.NONE.supportsPredictionResistance()) {
            return n;
        }
        n++;
        if (!java.security.DrbgParameters.Capability.RESEED_ONLY.toString()
                    .equals("reseed_only")
                || !java.security.DrbgParameters.Capability.NONE.toString().equals("none")) {
            return n;
        }
        n++;
        // -1 es "la que el proveedor prefiera"; cualquier otro negativo no significa nada.
        if (!iae(() -> java.security.DrbgParameters.instantiation(-2,
                    java.security.DrbgParameters.Capability.NONE, null))
                || !iae(() -> java.security.DrbgParameters.nextBytes(-2, false, null))) {
            return n;
        }
        n++;
        if (!npe(() -> java.security.DrbgParameters.instantiation(1, null, null))) {
            return n;
        }
        n++;
        // Los arreglos se copian al entrar y al salir.
        byte[] extra = new byte[] {3};
        java.security.DrbgParameters.NextBytes nb =
            java.security.DrbgParameters.nextBytes(256, true, extra);
        extra[0] = 0;
        if (nb.getStrength() != 256 || !nb.getPredictionResistance()
                || nb.getAdditionalInput()[0] != 3) {
            return n;
        }
        n++;
        nb.getAdditionalInput()[0] = 0;
        if (nb.getAdditionalInput()[0] != 3) {
            return n;
        }
        n++;
        java.security.DrbgParameters.Reseed rs =
            java.security.DrbgParameters.reseed(false, null);
        if (rs.getPredictionResistance() || rs.getAdditionalInput() != null) {
            return n;
        }
        n++;
        // Los tres son parametros de SecureRandom, aunque el generador no exista aca.
        if (!(inst instanceof java.security.SecureRandomParameters)
                || !(nb instanceof java.security.SecureRandomParameters)
                || !(rs instanceof java.security.SecureRandomParameters)) {
            return n;
        }
        n++;

        return -1;
    }

    // Envoltorios para poder pasar llamadas con excepciones chequeadas a `iae`/`npe`.
    static void upd(java.security.Signature s, byte[] b, int off, int len)
            throws java.security.SignatureException {
        s.update(b, off, len);
    }

    static void updBb(java.security.Signature s, java.nio.ByteBuffer b)
            throws java.security.SignatureException {
        s.update(b);
    }

    // True si el bloque tira SignatureException.
    static boolean se(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (java.security.SignatureException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean kse(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (java.security.KeyStoreException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean ike(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (java.security.InvalidKeyException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean uoe(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean ipe(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (InvalidParameterException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean cnse(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (CloneNotSupportedException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // Bytes a partir de un string hexadecimal.
    static byte[] hexBytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return b;
    }

    // El OID que sale de un PolicyQualifierInfo dado en hexadecimal, o "" si no parsea.
    static String oidDe(String h) {
        try {
            return new java.security.cert.PolicyQualifierInfo(hexBytes(h)).getPolicyQualifierId();
        } catch (java.io.IOException e) {
            return "";
        }
    }

    // True si construir el PolicyQualifierInfo tira IOException.
    static boolean ioe(String h) {
        try {
            new java.security.cert.PolicyQualifierInfo(hexBytes(h));
            return false;
        } catch (java.io.IOException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // True si poner ese OID como ExtendedKeyUsage tira IOException.
    static boolean ioeSet(java.security.cert.X509CertSelector s, String oid) {
        try {
            s.setExtendedKeyUsage(new java.util.HashSet<String>(java.util.Arrays.asList(oid)));
            return false;
        } catch (java.io.IOException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // Como `iae`, pero para InvalidAlgorithmParameterException, que es chequeada.
    static boolean iape(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (java.security.InvalidAlgorithmParameterException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // Como `iae`, pero para bloques que declaran excepciones chequeadas.
    static boolean iaeL(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // Como `npe`, pero para bloques que declaran excepciones chequeadas.
    static boolean npeL(Lanzador r) {
        try {
            r.run();
            return false;
        } catch (NullPointerException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    interface Lanzador {
        void run() throws Exception;
    }

    // Multi-primo con valores fijos: lo unico que varia entre las comprobaciones es la lista de
    // primos extra.
    static RSAMultiPrimePrivateCrtKeySpec multi(RSAOtherPrimeInfo[] extra) {
        return new RSAMultiPrimePrivateCrtKeySpec(
            BigInteger.valueOf(105), BigInteger.valueOf(5), BigInteger.valueOf(29),
            BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.ONE,
            BigInteger.ONE, BigInteger.valueOf(2), extra);
    }

    // Devuelve true si el bloque tira NullPointerException. Solo se mira el tipo, nunca el mensaje:
    // los mensajes no son parte de ninguna especificacion.
    static boolean npe(Runnable r) {
        try {
            r.run();
            return false;
        } catch (NullPointerException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // Idem para IllegalArgumentException. Sirve tambien para InvalidParameterException, que hereda
    // de ella: es el caso del contexto de EdDSA pasado de largo.
    static boolean iae(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
