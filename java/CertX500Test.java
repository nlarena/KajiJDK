import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

/**
 * Los miembros de java.security.cert que dependen de un nombre X.500.
 *
 * <p>Todo lo que se prueba aca son metodos <b>concretos</b> de clases abstractas, asi que las
 * subclases de prueba solo llenan lo abstracto y dejan que la clase base haga su trabajo. Esa es la
 * unica forma de mirar estos metodos sin un proveedor de criptografia: no hace falta ninguno,
 * porque ninguno de ellos verifica nada -- caminan el DER hasta un campo y comparan nombres.
 *
 * <p>Las expectativas salieron de preguntarle al JDK 25 con este mismo esqueleto de subclases, no de
 * leer la documentacion. Las tres que no son obvias y por las que vale la pena tener la prueba:
 * getCAName() NO siempre coincide con getCA().getName(), getRevokedCertificate(cert) ni siquiera
 * llama al que toma la serie cuando los emisores no coinciden, y getIssuerNames() devuelve mezclados
 * los String y los byte[] tal como se pusieron.
 */
public class CertX500Test {

    // Un certificado de verdad, hecho con keytool: CN=Juan Perez, OU=Ventas, O=Acme, C=AR.
    // Esta en hexadecimal y no en un archivo para que la prueba no dependa de nada de afuera.
    private static final String CERT_HEX =
        "3082036130820249a003020102020900ca128bc07835ef95300d06092a864886f70d01010c0500"
        + "3042310b3009060355040613024152310d300b060355040a130441636d65310f300d060355040b"
        + "130656656e746173311330110603550403130a4a75616e20506572657a301e170d323630393033"
        + "3038303334375a170d3336303833313038303334375a3042310b3009060355040613024152310d"
        + "300b060355040a130441636d65310f300d060355040b130656656e746173311330110603550403"
        + "130a4a75616e20506572657a30820122300d06092a864886f70d01010105000382010f00308201"
        + "0a0282010100d5871d23c7c3ebeb823bcc854c5fa648b52e250b3307fa556e4af4434289833ebb"
        + "3ab68a5cb8674f3ec897dfabf5486e308c5e0e59f6078bfe6253ed51524670e4e022bb174357a9"
        + "4ddc620832155a6f9d12fe109675160cfdbbd1e516bc07fe1449b95fe10b981780401a8f1c607d"
        + "c809a30b9861cb569d6936d19417c71a9221927ee020033f726dfad68a558f20350266676ea949"
        + "612d54e1bc95450ac1a998886a9571cfc7bdaef0bb03ad276baae6004005fa60317a286c615ebe"
        + "63ed5cb7d262b8337140488013a7d5c966e79adde271fa29fe376f65face5030d6320791d2db12"
        + "fd1746c72a6650af50cfdd3b33323557f24a705a0640e79c2fccb0670203010001a35a3058301d"
        + "0603551d0e041604149cd37acdf971bef73b43b91754f4e825cdbbb3ba30370603551d11043030"
        + "2e820861636d652e636f6d810a6a4061636d652e636f6d87040a000001861068747470733a2f2f"
        + "61636d652e636f6d300d06092a864886f70d01010c050003820101007ef2eb978b5d91e4b9c7ab"
        + "4ab3b1ad953ef2cec88e25f14d9b1312951adee32f3e42350bd75856fd55ad01f4a6192b35e5b8"
        + "719415e51b5d183798379ac9953321de2d1dad2b4cef4d7735bfb237c0f77d1f832bcf717c1525"
        + "3a452b9cba59d087bbf77218a2594d94ae0fbf4e636b41ff4bb918e61692a3e1e2091a67cb5384"
        + "7f966792c8c6c6454bbe687134219107658c9bd4913720dd0917d5e1a82a3307e662b3b416fb7f"
        + "1e4da646d743a5adf5573aea554c09bc03e33c8e941c1a3393d85e41214e26e94120f42d6a36fa"
        + "624f64bc56693a8d87aea69d7798860d929d8046c4362fffd980c6435cad2d6e20deb751a6418b"
        + "b1c933990a0c62b675496c";

    private static final String DN = "CN=Juan Perez,OU=Ventas,O=Acme,C=AR";

    private static byte[] fromHex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) (Character.digit(s.charAt(2 * i), 16) * 16
                + Character.digit(s.charAt(2 * i + 1), 16));
        }
        return b;
    }

    // -- un constructor de DER de tres lineas, solo para armar la CRL de prueba
    private static byte[] tlv(int tag, byte[] body) {
        byte[] len;
        int n = body.length;
        if (n < 0x80) {
            len = new byte[] {(byte) n};
        } else if (n < 0x100) {
            len = new byte[] {(byte) 0x81, (byte) n};
        } else {
            len = new byte[] {(byte) 0x82, (byte) (n >> 8), (byte) n};
        }
        byte[] r = new byte[1 + len.length + n];
        r[0] = (byte) tag;
        System.arraycopy(len, 0, r, 1, len.length);
        System.arraycopy(body, 0, r, 1 + len.length, n);
        return r;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static byte[] intDer(int v) {
        return tlv(0x02, new byte[] {(byte) v});
    }

    private static byte[] utcTime() {
        return tlv(0x17, "260101000000Z".getBytes());
    }

    private static byte[] algId() {
        byte[] oid = tlv(0x06, new byte[] {0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7,
            0x0d, 0x01, 0x01, 0x0b});
        return tlv(0x30, concat(oid, tlv(0x05, new byte[0])));
    }

    /**
     * El valor de la extension PrivateKeyUsagePeriod, envuelto en su OCTET STRING.
     *
     * <p>Los dos GeneralizedTime son opcionales y van con etiqueta de contexto: [0] y [1].
     */
    private static byte[] privateKeyUsagePeriod(String from, String to) {
        byte[] body = new byte[0];
        if (from != null) {
            body = concat(body, tlv(0x80, from.getBytes()));
        }
        if (to != null) {
            body = concat(body, tlv(0x81, to.getBytes()));
        }
        return tlv(0x04, tlv(0x30, body));
    }

    private static Cert certWithPeriod(String from, String to) {
        Cert c = new Cert(null);
        c.fixedIssuer = new X500Principal("CN=E");
        c.fixedSubject = new X500Principal("CN=S");
        c.exts.put("2.5.29.16", privateKeyUsagePeriod(from, to));
        return c;
    }


    // El SubjectPublicKeyInfo del mismo certificado de arriba. Hace falta entero porque los
    // certificados que arma esta prueba tienen que ser validos de verdad: el JDK aplica las
    // restricciones de nombres sobre el certificado CODIFICADO, asi que un stub que solo contestara
    // getSubjectX500Principal() daria distinto en las dos VMs.
    private static final String SPKI_HEX =
        "30820122300d06092a864886f70d01010105000382010f003082010a0282010100d5871d23c7c3"
        + "ebeb823bcc854c5fa648b52e250b3307fa556e4af4434289833ebb3ab68a5cb8674f3ec897dfab"
        + "f5486e308c5e0e59f6078bfe6253ed51524670e4e022bb174357a94ddc620832155a6f9d12fe10"
        + "9675160cfdbbd1e516bc07fe1449b95fe10b981780401a8f1c607dc809a30b9861cb569d6936d1"
        + "9417c71a9221927ee020033f726dfad68a558f20350266676ea949612d54e1bc95450ac1a99888"
        + "6a9571cfc7bdaef0bb03ad276baae6004005fa60317a286c615ebe63ed5cb7d262b83371404880"
        + "13a7d5c966e79adde271fa29fe376f65face5030d6320791d2db12fd1746c72a6650af50cfdd3b"
        + "33323557f24a705a0640e79c2fccb0670203010001";

    private static byte[] ia5(int tag, String s) {
        return tlv(tag, s.getBytes());
    }

    private static byte[] dirName(String dn) {
        // [4] es CONSTRUIDO: envuelve el `Name` entero.
        return tlv(0xa4, new X500Principal(dn).getEncoded());
    }

    private static byte[] ipName(byte[] b) {
        return tlv(0x87, b);
    }

    /** El valor de SubjectAltName, envuelto en su OCTET STRING. */
    private static byte[] sanValue(byte[][] names) {
        byte[] body = new byte[0];
        for (int i = 0; i < names.length; i++) {
            body = concat(body, names[i]);
        }
        return tlv(0x04, tlv(0x30, body));
    }

    /** El valor de NameConstraints, sin envolver. */
    private static byte[] ncValue(byte[][] permitted, byte[][] excluded) {
        byte[] body = new byte[0];
        if (permitted != null) {
            byte[] t = new byte[0];
            for (int i = 0; i < permitted.length; i++) {
                t = concat(t, tlv(0x30, permitted[i]));
            }
            body = concat(body, tlv(0xa0, t));
        }
        if (excluded != null) {
            byte[] t = new byte[0];
            for (int i = 0; i < excluded.length; i++) {
                t = concat(t, tlv(0x30, excluded[i]));
            }
            body = concat(body, tlv(0xa1, t));
        }
        return tlv(0x30, body);
    }

    private static byte[] oidDer(int[] arcs) {
        byte[] b = new byte[arcs.length];
        for (int i = 0; i < arcs.length; i++) {
            b[i] = (byte) arcs[i];
        }
        return tlv(0x06, b);
    }

    private static final byte[] OID_SAN_DER = oidDer(new int[] {0x55, 0x1d, 0x11});
    private static final byte[] OID_NC_DER = oidDer(new int[] {0x55, 0x1d, 0x1e});

    private static byte[] extension(byte[] oid, byte[] wrappedValue) {
        return tlv(0x30, concat(oid, wrappedValue));
    }

    /** Un certificado X.509 v3 valido, con el sujeto y las extensiones que se le pidan. */
    private static byte[] certDer(String subject, byte[] extensions) {
        byte[] tbs = concat(tlv(0xa0, tlv(0x02, new byte[] {2})), tlv(0x02, new byte[] {1}));
        tbs = concat(tbs, algId());
        tbs = concat(tbs, new X500Principal("CN=CA").getEncoded());
        tbs = concat(tbs, tlv(0x30, concat(tlv(0x17, "200101000000Z".getBytes()),
            tlv(0x17, "300101000000Z".getBytes()))));
        tbs = concat(tbs, new X500Principal(subject).getEncoded());
        tbs = concat(tbs, fromHex(SPKI_HEX));
        if (extensions != null) {
            tbs = concat(tbs, tlv(0xa3, tlv(0x30, extensions)));
        }
        return tlv(0x30, concat(concat(tlv(0x30, tbs), algId()),
            tlv(0x03, new byte[] {0, 0x11, 0x22, 0x33})));
    }

    /**
     * Un certificado de prueba coherente en las dos dimensiones: los bytes de getEncoded() y lo que
     * contestan getSubjectX500Principal() y getExtensionValue() dicen lo mismo.
     *
     * <p>Tiene que ser asi porque las dos VMs lo miran distinto -- el JDK reparsea la codificacion y
     * nosotros usamos el API publico --, y un stub incoherente daria dos respuestas distintas por un
     * motivo que no es el que se esta probando.
     */
    private static Cert certFor(String subject, byte[][] altNames, byte[] constraints) {
        byte[] exts = null;
        if (altNames != null) {
            exts = extension(OID_SAN_DER, sanValue(altNames));
        }
        if (constraints != null) {
            byte[] one = extension(OID_NC_DER, tlv(0x04, constraints));
            exts = exts == null ? one : concat(exts, one);
        }
        Cert c = new Cert(certDer(subject, exts));
        if (altNames != null) {
            c.exts.put("2.5.29.17", sanValue(altNames));
        }
        if (constraints != null) {
            c.exts.put("2.5.29.30", tlv(0x04, constraints));
        }
        return c;
    }

    /** Una CRL de ese emisor que revoca las series 5 y 9. */
    private static byte[] crlFor(byte[] issuer) {
        byte[] revoked = tlv(0x30, concat(intDer(5), utcTime()));
        revoked = concat(revoked, tlv(0x30, concat(intDer(9), utcTime())));
        byte[] tbs = concat(concat(concat(intDer(1), algId()), concat(issuer, utcTime())),
            concat(utcTime(), tlv(0x30, revoked)));
        return tlv(0x30, concat(concat(tlv(0x30, tbs), algId()),
            tlv(0x03, new byte[] {0, 0x11, 0x22, 0x33})));
    }

    // ==========================================================================================
    // subclases peladas: solo lo abstracto, para dejar hablar a la clase base
    // ==========================================================================================

    static class Cert extends X509Certificate {
        byte[] enc;
        X500Principal fixedIssuer;
        X500Principal fixedSubject;
        Map<String, byte[]> exts = new HashMap<String, byte[]>();
        BigInteger serial = BigInteger.ONE;

        Cert(byte[] e) {
            this.enc = e;
        }

        public X500Principal getIssuerX500Principal() {
            if (fixedIssuer != null) {
                return fixedIssuer;
            }
            return super.getIssuerX500Principal();
        }

        public X500Principal getSubjectX500Principal() {
            if (fixedSubject != null) {
                return fixedSubject;
            }
            return super.getSubjectX500Principal();
        }

        public void checkValidity() { }

        public void checkValidity(Date d) { }

        public int getVersion() { return 3; }

        public BigInteger getSerialNumber() { return serial; }

        public Principal getIssuerDN() { return null; }

        public Principal getSubjectDN() { return null; }

        public Date getNotBefore() { return null; }

        public Date getNotAfter() { return null; }

        public byte[] getTBSCertificate() { return null; }

        public byte[] getSignature() { return null; }

        public String getSigAlgName() { return null; }

        public String getSigAlgOID() { return null; }

        public byte[] getSigAlgParams() { return null; }

        public boolean[] getIssuerUniqueID() { return null; }

        public boolean[] getSubjectUniqueID() { return null; }

        public boolean[] getKeyUsage() { return null; }

        public int getBasicConstraints() { return -1; }

        public byte[] getEncoded() throws CertificateEncodingException { return enc; }

        public void verify(PublicKey k) { }

        public void verify(PublicKey k, String p) { }

        public String toString() { return "Cert"; }

        public PublicKey getPublicKey() { return null; }

        public boolean hasUnsupportedCriticalExtension() { return false; }

        public Set<String> getCriticalExtensionOIDs() { return null; }

        public Set<String> getNonCriticalExtensionOIDs() { return exts.keySet(); }

        public byte[] getExtensionValue(String oid) { return exts.get(oid); }
    }

    static class Entry extends X509CRLEntry {
        BigInteger serial;

        Entry(BigInteger s) { this.serial = s; }

        public byte[] getEncoded() { return null; }

        public BigInteger getSerialNumber() { return serial; }

        public Date getRevocationDate() { return null; }

        public boolean hasExtensions() { return false; }

        public String toString() { return "entrada"; }

        public boolean hasUnsupportedCriticalExtension() { return false; }

        public Set<String> getCriticalExtensionOIDs() { return null; }

        public Set<String> getNonCriticalExtensionOIDs() { return null; }

        public byte[] getExtensionValue(String oid) { return null; }
    }

    static class Crl extends X509CRL {
        byte[] enc;
        // Cuenta cuantas veces se delego en el que toma la serie: es como se ve que el chequeo de
        // emisor corta ANTES de buscar, y no despues.
        int delegations = 0;

        Crl(byte[] e) { this.enc = e; }

        public byte[] getEncoded() throws CRLException { return enc; }

        public void verify(PublicKey k) { }

        public void verify(PublicKey k, String p) { }

        public int getVersion() { return 2; }

        public Principal getIssuerDN() { return null; }

        public Date getThisUpdate() { return null; }

        public Date getNextUpdate() { return null; }

        public X509CRLEntry getRevokedCertificate(BigInteger serial) {
            delegations = delegations + 1;
            if (serial.intValue() == 5) {
                return new Entry(serial);
            }
            return null;
        }

        public Set<? extends X509CRLEntry> getRevokedCertificates() { return null; }

        public byte[] getTBSCertList() { return null; }

        public byte[] getSignature() { return null; }

        public String getSigAlgName() { return null; }

        public String getSigAlgOID() { return null; }

        public byte[] getSigAlgParams() { return null; }

        public String toString() { return "Crl"; }

        public boolean isRevoked(java.security.cert.Certificate c) { return false; }

        public boolean hasUnsupportedCriticalExtension() { return false; }

        public Set<String> getCriticalExtensionOIDs() { return null; }

        public Set<String> getNonCriticalExtensionOIDs() { return null; }

        public byte[] getExtensionValue(String oid) { return null; }
    }

    /** Una extension cualquiera: lo unico que importa aca es su id y su valor. */
    static class Ext implements java.security.cert.Extension {
        String id;
        byte[] value;

        Ext(String id, byte[] v) {
            this.id = id;
            this.value = v;
        }

        public String getId() { return id; }

        public boolean isCritical() { return false; }

        public byte[] getValue() { return value; }

        public void encode(java.io.OutputStream out) throws java.io.IOException {
            out.write(value);
        }
    }

    static class PrivKey implements java.security.PrivateKey {
        public String getAlgorithm() { return "KAJI"; }

        public String getFormat() { return "PKCS#8"; }

        public byte[] getEncoded() { return new byte[] {9}; }
    }

    static class PubKey implements PublicKey {
        public String getAlgorithm() { return "KAJI"; }

        public String getFormat() { return "X.509"; }

        public byte[] getEncoded() { return new byte[] {1, 2, 3}; }
    }

    /** Un almacen de prueba: dos entradas de certificado y una de clave, para ver cual se toma. */
    static class Spi extends KeyStoreSpi {
        Map<String, java.security.cert.Certificate> certs =
            new HashMap<String, java.security.cert.Certificate>();
        List<String> claves = new ArrayList<String>();

        public java.security.Key engineGetKey(String a, char[] p) { return null; }

        public java.security.cert.Certificate[] engineGetCertificateChain(String a) { return null; }

        public java.security.cert.Certificate engineGetCertificate(String a) {
            return certs.get(a);
        }

        public Date engineGetCreationDate(String a) { return null; }

        public void engineSetKeyEntry(String a, java.security.Key k, char[] p,
                java.security.cert.Certificate[] c) { }

        public void engineSetKeyEntry(String a, byte[] k, java.security.cert.Certificate[] c) { }

        public void engineSetCertificateEntry(String a, java.security.cert.Certificate c) { }

        public void engineDeleteEntry(String a) { }

        public Enumeration<String> engineAliases() {
            List<String> todos = new ArrayList<String>(certs.keySet());
            todos.addAll(claves);
            return java.util.Collections.enumeration(todos);
        }

        public boolean engineContainsAlias(String a) {
            return certs.containsKey(a) || claves.contains(a);
        }

        public int engineSize() { return certs.size() + claves.size(); }

        public boolean engineIsKeyEntry(String a) { return claves.contains(a); }

        public boolean engineIsCertificateEntry(String a) { return certs.containsKey(a); }

        public String engineGetCertificateAlias(java.security.cert.Certificate c) { return null; }

        public void engineStore(java.io.OutputStream o, char[] p) { }

        public void engineLoad(java.io.InputStream i, char[] p) { }
    }

    static class TestKeyStore extends KeyStore {
        TestKeyStore(Spi spi) {
            super(spi, null, "kaji");
        }
    }

    // ==========================================================================================

    public static int run() {
        int i = 0;
        byte[] der = fromHex(CERT_HEX);

        // -- el nombre sale del DER, no de getIssuerDN()
        Cert c = new Cert(der);
        if (!c.getIssuerX500Principal().getName().equals(DN)) { return i; } i++;
        if (!c.getSubjectX500Principal().getName().equals(DN)) { return i; } i++;
        // Se recuerda: la misma instancia dos veces. Hay codigo que compara con ==.
        if (c.getIssuerX500Principal() != c.getIssuerX500Principal()) { return i; } i++;
        if (c.getSubjectX500Principal() != c.getSubjectX500Principal()) { return i; } i++;
        // Emisor y sujeto son iguales porque el certificado es autofirmado, pero no la misma
        // instancia: son dos campos distintos del DER.
        if (!c.getIssuerX500Principal().equals(c.getSubjectX500Principal())) { return i; } i++;

        // -- los nombres alternativos: la clase base no los sabe y devuelve null, no vacio
        try {
            if (c.getSubjectAlternativeNames() != null) { return i; }
            i++;
            if (c.getIssuerAlternativeNames() != null) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- un DER que no se puede leer no puede devolver un nombre a medias
        boolean threw = false;
        try { new Cert(new byte[] {1, 2, 3}).getIssuerX500Principal(); }
        catch (RuntimeException e) { threw = "Could not parse issuer".equals(e.getMessage()); }
        if (!threw) { return i; } i++;
        threw = false;
        try { new Cert(new byte[] {1, 2, 3}).getSubjectX500Principal(); }
        catch (RuntimeException e) { threw = "Could not parse subject".equals(e.getMessage()); }
        if (!threw) { return i; } i++;
        threw = false;
        try { new Cert(null).getIssuerX500Principal(); }
        catch (RuntimeException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Un certificado truncado a la mitad tampoco: el campo esta cortado, no ausente.
        byte[] truncated = new byte[40];
        System.arraycopy(der, 0, truncated, 0, 40);
        threw = false;
        try { new Cert(truncated).getIssuerX500Principal(); }
        catch (RuntimeException e) { threw = true; }
        if (!threw) { return i; } i++;

        // -- la CRL: el emisor sale de TBSCertList.issuer
        X500Principal issuer = new X500Principal("CN=Emisor,O=Acme,C=AR");
        Crl crl = new Crl(crlFor(issuer.getEncoded()));
        if (!crl.getIssuerX500Principal().getName().equals("CN=Emisor,O=Acme,C=AR")) { return i; } i++;
        if (!crl.getIssuerX500Principal().equals(issuer)) { return i; } i++;
        if (crl.getIssuerX500Principal() != crl.getIssuerX500Principal()) { return i; } i++;
        threw = false;
        try { new Crl(new byte[] {1, 2, 3}).getIssuerX500Principal(); }
        catch (RuntimeException e) { threw = "Could not parse issuer".equals(e.getMessage()); }
        if (!threw) { return i; } i++;

        // -- getRevokedCertificate(cert): mismo emisor y serie revocada
        Cert revokedCert = new Cert(null);
        revokedCert.fixedIssuer = issuer;
        revokedCert.serial = BigInteger.valueOf(5);
        if (crl.getRevokedCertificate(revokedCert) == null) { return i; } i++;
        if (crl.delegations != 1) { return i; } i++;

        // -- mismo emisor, serie que no esta: null, pero SI se llego a buscar
        Cert cleanCert = new Cert(null);
        cleanCert.fixedIssuer = issuer;
        cleanCert.serial = BigInteger.valueOf(77);
        if (crl.getRevokedCertificate(cleanCert) != null) { return i; } i++;
        if (crl.delegations != 2) { return i; } i++;

        // -- OTRO emisor: null, y NO se llega a buscar. Esta es la parte que importa: la misma
        //    serie de dos CAs distintas son dos certificados distintos.
        Cert foreignCert = new Cert(null);
        foreignCert.fixedIssuer = new X500Principal("CN=Otro,O=Acme,C=AR");
        foreignCert.serial = BigInteger.valueOf(5);
        if (crl.getRevokedCertificate(foreignCert) != null) { return i; } i++;
        if (crl.delegations != 2) { return i; } i++;

        // -- el emisor se compara canonicamente, no por texto
        Cert sameNameOtherText = new Cert(null);
        sameNameOtherText.fixedIssuer = new X500Principal("cn=EMISOR,  o=acme, c=ar");
        sameNameOtherText.serial = BigInteger.valueOf(5);
        if (crl.getRevokedCertificate(sameNameOtherText) == null) { return i; } i++;

        threw = false;
        try { crl.getRevokedCertificate((X509Certificate) null); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;

        // -- una entrada suelta no sabe de que CA es: null significa "la de la CRL"
        if (new Entry(BigInteger.ONE).getCertificateIssuer() != null) { return i; } i++;

        // ======================================================================================
        // TrustAnchor
        // ======================================================================================
        PublicKey clave = new PubKey();
        X500Principal juan = new X500Principal("CN=Juan, O=Acme");
        TrustAnchor ta = new TrustAnchor(juan, clave, null);
        if (ta.getCA() != juan) { return i; } i++;
        // Desde un X500Principal, getCAName() es su forma RFC 2253: SIN el espacio.
        if (!ta.getCAName().equals("CN=Juan,O=Acme")) { return i; } i++;
        if (ta.getTrustedCert() != null) { return i; } i++;
        if (ta.getCAPublicKey() != clave) { return i; } i++;

        // Desde un String, getCAName() devuelve el texto ORIGINAL, con el espacio. No coincide con
        // getCA().getName(), y por eso comparar anclas por getCAName() esta mal.
        TrustAnchor ts = new TrustAnchor("CN=Juan, O=Acme", clave, null);
        if (!ts.getCAName().equals("CN=Juan, O=Acme")) { return i; } i++;
        if (!ts.getCA().getName().equals("CN=Juan,O=Acme")) { return i; } i++;
        if (!ts.getCA().equals(juan)) { return i; } i++;

        // Desde un certificado no se derivan: los dos accesores dan null.
        TrustAnchor tc = new TrustAnchor(c, null);
        if (tc.getCA() != null) { return i; } i++;
        if (tc.getCAName() != null) { return i; } i++;
        if (tc.getCAPublicKey() != null) { return i; } i++;
        if (tc.getTrustedCert() != c) { return i; } i++;

        // El orden de los chequeos importa: con los dos en null se queja la CLAVE, no el nombre.
        String queja = null;
        try { new TrustAnchor((String) null, null, null); }
        catch (NullPointerException e) { queja = e.getMessage(); }
        if (queja == null || queja.indexOf("pubKey") < 0) { return i; } i++;
        queja = null;
        try { new TrustAnchor((String) null, clave, null); }
        catch (NullPointerException e) { queja = e.getMessage(); }
        if (queja == null || queja.indexOf("caName") < 0) { return i; } i++;
        threw = false;
        try { new TrustAnchor("", clave, null); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Un nombre mal escrito se rechaza aca y no queda un ancla sin validar.
        threw = false;
        try { new TrustAnchor("no-hay-igual", clave, null); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new TrustAnchor((X500Principal) null, clave, null); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new TrustAnchor(juan, null, null); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;

        // Las restricciones de nombres se copian al entrar y al salir.
        byte[] nc = new byte[] {0x30, 0x00};
        TrustAnchor tn = new TrustAnchor(juan, clave, nc);
        nc[0] = 0x7f;
        if (tn.getNameConstraints()[0] != 0x30) { return i; } i++;
        tn.getNameConstraints()[0] = 0x7f;
        if (tn.getNameConstraints()[0] != 0x30) { return i; } i++;

        // ======================================================================================
        // X509CRLSelector
        // ======================================================================================
        X509CRLSelector s = new X509CRLSelector();
        // Sin criterio los dos accesores dan null, no una coleccion vacia.
        if (s.getIssuers() != null) { return i; } i++;
        if (s.getIssuerNames() != null) { return i; } i++;

        s.addIssuer(issuer);
        if (s.getIssuers().size() != 1) { return i; } i++;
        if (!s.getIssuers().iterator().next().equals(issuer)) { return i; } i++;
        // Un X500Principal se guarda como bytes en la vista cruda.
        if (!(s.getIssuerNames().iterator().next() instanceof byte[])) { return i; } i++;
        // La vista cruda es una copia con los byte[] clonados.
        byte[] rawBytes = (byte[]) s.getIssuerNames().iterator().next();
        rawBytes[0] = 0x7f;
        if (((byte[]) s.getIssuerNames().iterator().next())[0] != 0x30) { return i; } i++;
        // getIssuers() es inmutable; getIssuerNames() no.
        threw = false;
        try { s.getIssuers().clear(); }
        catch (UnsupportedOperationException e) { threw = true; }
        if (!threw) { return i; } i++;
        s.getIssuerNames().clear();
        if (s.getIssuerNames().size() != 1) { return i; } i++;

        // Un String se guarda TAL CUAL en la vista cruda, aunque el criterio si se canonice.
        X509CRLSelector s2 = new X509CRLSelector();
        try { s2.addIssuerName("CN=Emisor, O=Acme, C=AR"); }
        catch (Exception e) { return i; }
        if (!"CN=Emisor, O=Acme, C=AR".equals(s2.getIssuerNames().iterator().next())) { return i; } i++;
        if (!s2.getIssuers().iterator().next().equals(issuer)) { return i; } i++;

        // El criterio filtra por forma canonica: ese selector acepta esta CRL.
        if (!s2.match(crl)) { return i; } i++;
        X509CRLSelector s3 = new X509CRLSelector();
        try { s3.addIssuerName("CN=Ajeno"); }
        catch (Exception e) { return i; }
        if (s3.match(crl)) { return i; } i++;
        if (s3.match(null)) { return i; } i++;

        // Desde el DER del Name.
        X509CRLSelector s4 = new X509CRLSelector();
        try { s4.addIssuerName(issuer.getEncoded()); }
        catch (Exception e) { return i; }
        if (!s4.match(crl)) { return i; } i++;
        // Un DER que no es un Name se rechaza como IOException, no como IllegalArgumentException.
        threw = false;
        try { new X509CRLSelector().addIssuerName(new byte[] {1, 2, 3}); }
        catch (java.io.IOException e) { threw = true; }
        catch (Exception e) { threw = false; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new X509CRLSelector().addIssuerName("no-hay-igual"); }
        catch (java.io.IOException e) { threw = true; }
        catch (Exception e) { threw = false; }
        if (!threw) { return i; } i++;

        // setIssuers/setIssuerNames con null o vacio quitan el criterio: dejan los DOS en null.
        s4.setIssuers(null);
        if (s4.getIssuers() != null) { return i; } i++;
        if (s4.getIssuerNames() != null) { return i; } i++;
        List<X500Principal> single = new ArrayList<X500Principal>();
        single.add(issuer);
        s4.setIssuers(single);
        if (s4.getIssuerNames().size() != 1) { return i; } i++;
        s4.setIssuers(new ArrayList<X500Principal>());
        if (s4.getIssuers() != null) { return i; } i++;
        if (s4.getIssuerNames() != null) { return i; } i++;

        X509CRLSelector s5 = new X509CRLSelector();
        List<Object> mixed = new ArrayList<Object>();
        mixed.add("CN=Emisor, O=Acme, C=AR");
        mixed.add(new X500Principal("CN=Otro").getEncoded());
        try { s5.setIssuerNames(mixed); }
        catch (Exception e) { return i; }
        if (s5.getIssuers().size() != 2) { return i; } i++;
        if (!s5.match(crl)) { return i; } i++;
        try {
            s5.setIssuerNames(new ArrayList<Object>());
            if (s5.getIssuers() != null) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }
        // Un elemento que no es ni String ni byte[] se rechaza, y el selector queda como estaba.
        X509CRLSelector s6 = new X509CRLSelector();
        try { s6.addIssuerName("CN=Emisor, O=Acme, C=AR"); }
        catch (Exception e) { return i; }
        List<Object> bad = new ArrayList<Object>();
        bad.add(Integer.valueOf(1));
        threw = false;
        try { s6.setIssuerNames(bad); }
        catch (java.io.IOException e) { threw = true; }
        catch (Exception e) { threw = false; }
        if (!threw) { return i; } i++;
        if (s6.getIssuers().size() != 1) { return i; } i++;

        // clone() copia los conjuntos: cambiar la copia no cambia el original.
        X509CRLSelector s7 = (X509CRLSelector) s2.clone();
        if (s7.getIssuers().size() != 1) { return i; } i++;
        try { s7.addIssuerName("CN=Tercero"); }
        catch (Exception e) { return i; }
        if (s7.getIssuers().size() != 2) { return i; } i++;
        if (s2.getIssuers().size() != 1) { return i; } i++;
        if (s2.toString().indexOf("IssuerNames") < 0) { return i; } i++;

        // ======================================================================================
        // PKIXParameters(KeyStore)
        // ======================================================================================
        threw = false;
        try { new PKIXParameters((KeyStore) null); }
        catch (NullPointerException e) { threw = true; }
        catch (Exception e) { threw = false; }
        if (!threw) { return i; } i++;

        Spi spi = new Spi();
        spi.certs.put("ca", c);
        spi.claves.add("mia");
        TestKeyStore ks = new TestKeyStore(spi);
        try {
            ks.load(null, null);
            PKIXParameters pp = new PKIXParameters(ks);
            // Solo la entrada de certificado: la de clave privada es la identidad de uno, no una CA.
            if (pp.getTrustAnchors().size() != 1) { return i; }
            i++;
            if (pp.getTrustAnchors().iterator().next().getTrustedCert() != c) { return i; }
            i++;
            if (!pp.isRevocationEnabled()) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // Un almacen con solo claves privadas no da ninguna ancla, y eso tiene que fallar.
        Spi soloClaves = new Spi();
        soloClaves.claves.add("mia");
        TestKeyStore ks2 = new TestKeyStore(soloClaves);
        threw = false;
        try {
            ks2.load(null, null);
            new PKIXParameters(ks2);
        } catch (java.security.InvalidAlgorithmParameterException e) {
            threw = true;
        } catch (Exception e) {
            threw = false;
        }
        if (!threw) { return i; } i++;

        // Un almacen sin cargar no se puede recorrer.
        threw = false;
        try { new PKIXParameters(new TestKeyStore(new Spi())); }
        catch (java.security.KeyStoreException e) { threw = true; }
        catch (Exception e) { threw = false; }
        if (!threw) { return i; } i++;

        try {
            X509CertSelector blanco = new X509CertSelector();
            PKIXBuilderParameters bp = new PKIXBuilderParameters(ks, blanco);
            if (bp.getTrustAnchors().size() != 1) { return i; }
            i++;
            if (bp.getMaxPathLength() != 5) { return i; }
            i++;
            if (bp.getTargetCertConstraints() == null) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // ======================================================================================
        // X509CertSelector: emisor, sujeto y vigencia de la clave privada
        // ======================================================================================
        X509CertSelector x = new X509CertSelector();
        try {
            if (x.getIssuer() != null) { return i; }
            i++;
            if (x.getIssuerAsString() != null) { return i; }
            i++;
            if (x.getIssuerAsBytes() != null) { return i; }
            i++;
            if (x.getSubject() != null) { return i; }
            i++;
            if (x.getSubjectAsString() != null) { return i; }
            i++;
            if (x.getSubjectAsBytes() != null) { return i; }
            i++;
            if (x.getPrivateKeyValid() != null) { return i; }
            i++;
            if (x.match(null)) { return i; }
            i++;

            // A diferencia de TrustAnchor, aca el texto NO se conserva: lo que vuelve es la forma
            // canonica del nombre.
            x.setIssuer("cn=Juan,  o=Acme");
            if (!x.getIssuerAsString().equals("CN=Juan,O=Acme")) { return i; }
            i++;
            if (!x.getIssuer().equals(new X500Principal("CN=Juan,O=Acme"))) { return i; }
            i++;
            byte[] bytes = x.getIssuerAsBytes();
            bytes[0] = 0x7f;
            if (x.getIssuerAsBytes()[0] != 0x30) { return i; }
            i++;

            // Con un X500Principal se guarda la MISMA instancia.
            X500Principal pi = new X500Principal("CN=Otro, O=Acme");
            x.setIssuer(pi);
            if (x.getIssuer() != pi) { return i; }
            i++;
            x.setIssuer(pi.getEncoded());
            if (!x.getIssuerAsString().equals("CN=Otro,O=Acme")) { return i; }
            i++;

            x.setSubject("cn=Ana,  o=Acme");
            if (!x.getSubjectAsString().equals("CN=Ana,O=Acme")) { return i; }
            i++;

            // Null saca el criterio, por cualquiera de las tres puertas.
            x.setIssuer((X500Principal) null);
            if (x.getIssuer() != null || x.getIssuerAsString() != null) { return i; }
            i++;
            x.setSubject((String) null);
            if (x.getSubject() != null) { return i; }
            i++;
            x.setSubject((byte[]) null);
            if (x.getSubject() != null) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // Un nombre mal escrito y un DER que no es un nombre se rechazan como IOException.
        threw = false;
        try { new X509CertSelector().setIssuer("no-hay-igual"); }
        catch (java.io.IOException e) { threw = true; }
        catch (Exception e) { threw = false; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new X509CertSelector().setSubject(new byte[] {1, 2, 3}); }
        catch (java.io.IOException e) { threw = true; }
        catch (Exception e) { threw = false; }
        if (!threw) { return i; } i++;

        // -- match por nombre, comparando canonicamente
        Cert filterable = new Cert(null);
        filterable.fixedIssuer = new X500Principal("CN=Emisor,O=Acme,C=AR");
        filterable.fixedSubject = new X500Principal("CN=Sujeto,O=Acme,C=AR");
        X509CertSelector m = new X509CertSelector();
        try {
            m.setIssuer("cn=EMISOR,  o=acme, c=ar");
            if (!m.match(filterable)) { return i; }
            i++;
            m.setIssuer("CN=Ajeno");
            if (m.match(filterable)) { return i; }
            i++;
            m.setIssuer((X500Principal) null);
            m.setSubject(new X500Principal("CN=Sujeto,O=Acme,C=AR"));
            if (!m.match(filterable)) { return i; }
            i++;
            m.setSubject(new X500Principal("CN=Ajeno"));
            if (m.match(filterable)) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- privateKeyValid
        X509CertSelector v = new X509CertSelector();
        Date when = new Date(1800000000000L);
        v.setPrivateKeyValid(when);
        if (v.getPrivateKeyValid().getTime() != 1800000000000L) { return i; } i++;
        // La fecha se copia al entrar y al salir.
        when.setTime(0);
        if (v.getPrivateKeyValid().getTime() != 1800000000000L) { return i; } i++;
        v.getPrivateKeyValid().setTime(0);
        if (v.getPrivateKeyValid().getTime() != 1800000000000L) { return i; } i++;

        // Un certificado SIN la extension pasa: sin periodo declarado, la clave no se limito.
        if (!v.match(filterable)) { return i; } i++;
        if (!v.match(certWithPeriod("20200101000000Z", "20300101000000Z"))) { return i; } i++;
        if (v.match(certWithPeriod("20200101000000Z", "20210101000000Z"))) { return i; } i++;
        if (v.match(certWithPeriod("20900101000000Z", "20990101000000Z"))) { return i; } i++;
        // Los dos campos son opcionales por separado.
        if (!v.match(certWithPeriod("20200101000000Z", null))) { return i; } i++;
        if (v.match(certWithPeriod(null, "20210101000000Z"))) { return i; } i++;
        if (!v.match(certWithPeriod(null, null))) { return i; } i++;
        // Una extension presente pero ilegible no pasa: no se puede afirmar que la clave valia.
        Cert rota = new Cert(null);
        rota.fixedIssuer = new X500Principal("CN=E");
        rota.fixedSubject = new X500Principal("CN=S");
        rota.exts.put("2.5.29.16", new byte[] {1, 2, 3});
        if (v.match(rota)) { return i; } i++;
        // El borde exacto entra: 2027-01-15 08:00:00Z son 1800000000000 milisegundos.
        if (!v.match(certWithPeriod("20270115080000Z", "20270115080000Z"))) { return i; } i++;
        if (v.match(certWithPeriod(null, "20270115075959Z"))) { return i; } i++;
        v.setPrivateKeyValid(null);
        if (v.getPrivateKeyValid() != null) { return i; } i++;
        if (!v.match(certWithPeriod("20200101000000Z", "20210101000000Z"))) { return i; } i++;

        // -- clone copia las fechas y los nombres
        try {
            X509CertSelector k = new X509CertSelector();
            k.setIssuer("CN=Juan");
            k.setSubject("CN=Ana");
            k.setPrivateKeyValid(new Date(1800000000000L));
            X509CertSelector k2 = (X509CertSelector) k.clone();
            if (!k2.getIssuerAsString().equals("CN=Juan")) { return i; }
            i++;
            if (!k2.getSubjectAsString().equals("CN=Ana")) { return i; }
            i++;
            if (k2.getPrivateKeyValid().getTime() != 1800000000000L) { return i; }
            i++;
            k2.getPrivateKeyValid().setTime(0);
            if (k.getPrivateKeyValid().getTime() != 1800000000000L) { return i; }
            i++;
            if (k.toString().indexOf("Issuer") < 0) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // ======================================================================================
        // CertificateRevokedException
        // ======================================================================================
        Date published = new Date(1700000000000L);
        X500Principal ca = new X500Principal("CN=CA, O=Acme");
        Map<String, java.security.cert.Extension> exts =
            new HashMap<String, java.security.cert.Extension>();
        // InvalidityDate: un GeneralizedTime pelado. El valor de una extension viene SIN el OCTET
        // STRING de afuera, que es lo que promete Extension.getValue().
        exts.put("2.5.29.24", new Ext("2.5.29.24", tlv(0x18, "20231101000000Z".getBytes())));
        java.security.cert.CertificateRevokedException revoked =
            new java.security.cert.CertificateRevokedException(published,
                java.security.cert.CRLReason.KEY_COMPROMISE, ca, exts);

        if (revoked.getRevocationDate().getTime() != 1700000000000L) { return i; } i++;
        // La fecha se copia al entrar y al salir.
        published.setTime(0);
        if (revoked.getRevocationDate().getTime() != 1700000000000L) { return i; } i++;
        revoked.getRevocationDate().setTime(0);
        if (revoked.getRevocationDate().getTime() != 1700000000000L) { return i; } i++;
        if (revoked.getRevocationReason() != java.security.cert.CRLReason.KEY_COMPROMISE) { return i; } i++;
        // X500Principal es inmutable: se devuelve la misma instancia.
        if (revoked.getAuthorityName() != ca) { return i; } i++;
        // La fecha de invalidez es ANTERIOR a la de publicacion --dos semanas antes aca--, y esa
        // diferencia es justamente su sentido: una firma hecha en el medio es sospechosa aunque la
        // CRL de ese momento todavia no dijera nada.
        if (revoked.getInvalidityDate().getTime() != 1698796800000L) { return i; } i++;
        if (!revoked.getInvalidityDate().before(revoked.getRevocationDate())) { return i; } i++;
        revoked.getInvalidityDate().setTime(0);
        if (revoked.getInvalidityDate().getTime() != 1698796800000L) { return i; } i++;
        if (revoked.getExtensions().size() != 1) { return i; } i++;
        if (revoked.getExtensions().get("2.5.29.24") != exts.get("2.5.29.24")) { return i; } i++;
        threw = false;
        try { revoked.getExtensions().clear(); }
        catch (UnsupportedOperationException e) { threw = true; }
        if (!threw) { return i; } i++;
        // El mapa se copia al entrar: tocarlo despues no cambia la excepcion.
        exts.put("2.5.29.99", new Ext("2.5.29.99", new byte[] {1}));
        if (revoked.getExtensions().size() != 1) { return i; } i++;
        if (revoked.getMessage().indexOf("KEY_COMPROMISE") < 0) { return i; } i++;
        if (revoked.getMessage().indexOf("2.5.29.24") < 0) { return i; } i++;
        if (revoked.getMessage().indexOf("CN=CA, O=Acme") < 0) { return i; } i++;

        // Sin la extension, o con la extension ilegible: null, no una excepcion. Un dato accesorio
        // que no se entiende no deberia tapar el hecho principal.
        java.security.cert.CertificateRevokedException withoutExts =
            new java.security.cert.CertificateRevokedException(new Date(0),
                java.security.cert.CRLReason.UNSPECIFIED, ca,
                new HashMap<String, java.security.cert.Extension>());
        if (withoutExts.getInvalidityDate() != null) { return i; } i++;
        if (!withoutExts.getExtensions().isEmpty()) { return i; } i++;
        Map<String, java.security.cert.Extension> brokenExts =
            new HashMap<String, java.security.cert.Extension>();
        brokenExts.put("2.5.29.24", new Ext("2.5.29.24", new byte[] {1, 2, 3}));
        java.security.cert.CertificateRevokedException withBrokenExt =
            new java.security.cert.CertificateRevokedException(new Date(0),
                java.security.cert.CRLReason.UNSPECIFIED, ca, brokenExts);
        if (withBrokenExt.getInvalidityDate() != null) { return i; } i++;
        // Es una CertificateException: se puede atrapar por el tipo de arriba.
        if (!(revoked instanceof java.security.cert.CertificateException)) { return i; } i++;

        // Los cuatro argumentos son obligatorios.
        Map<String, java.security.cert.Extension> empty =
            new HashMap<String, java.security.cert.Extension>();
        threw = false;
        try {
            new java.security.cert.CertificateRevokedException(null,
                java.security.cert.CRLReason.UNSPECIFIED, ca, empty);
        } catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new java.security.cert.CertificateRevokedException(new Date(0), null, ca, empty); }
        catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new java.security.cert.CertificateRevokedException(new Date(0),
                java.security.cert.CRLReason.UNSPECIFIED, null, empty);
        } catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new java.security.cert.CertificateRevokedException(new Date(0),
                java.security.cert.CRLReason.UNSPECIFIED, ca, null);
        } catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // X500PrivateCredential
        // ======================================================================================
        java.security.PrivateKey privKey = new PrivKey();
        javax.security.auth.x500.X500PrivateCredential pair =
            new javax.security.auth.x500.X500PrivateCredential(c, privKey);
        if (pair.getCertificate() != c) { return i; } i++;
        if (pair.getPrivateKey() != privKey) { return i; } i++;
        if (pair.getAlias() != null) { return i; } i++;
        // El alias ya arranca en null y sin embargo isDestroyed() da false: exige los TRES.
        if (pair.isDestroyed()) { return i; } i++;
        pair.destroy();
        if (!pair.isDestroyed()) { return i; } i++;
        if (pair.getCertificate() != null) { return i; } i++;
        if (pair.getPrivateKey() != null) { return i; } i++;
        // Llamarlo dos veces no tiene que romper.
        pair.destroy();
        if (!pair.isDestroyed()) { return i; } i++;
        if (!(pair instanceof javax.security.auth.Destroyable)) { return i; } i++;

        javax.security.auth.x500.X500PrivateCredential withAlias =
            new javax.security.auth.x500.X500PrivateCredential(c, privKey, "mi-alias");
        if (!withAlias.getAlias().equals("mi-alias")) { return i; } i++;
        if (withAlias.isDestroyed()) { return i; } i++;

        // Se rechaza con IllegalArgumentException, no con NullPointerException.
        threw = false;
        try { new javax.security.auth.x500.X500PrivateCredential(null, privKey); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new javax.security.auth.x500.X500PrivateCredential(c, null); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new javax.security.auth.x500.X500PrivateCredential(c, privKey, null); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;


        // ======================================================================================
        // X509CertSelector: nombres alternativos
        // ======================================================================================
        X509CertSelector san = new X509CertSelector();
        if (san.getSubjectAlternativeNames() != null) { return i; } i++;
        if (san.getPathToNames() != null) { return i; } i++;
        if (san.getNameConstraints() != null) { return i; } i++;
        try {
            san.addSubjectAlternativeName(2, "acme.com");
            san.addSubjectAlternativeName(4, "CN=Juan,O=Acme");
            san.addSubjectAlternativeName(7, "10.0.0.1");
            java.util.Collection<List<?>> stored = san.getSubjectAlternativeNames();
            if (stored.size() != 3) { return i; }
            i++;
            // Lo que entro como texto sale como texto.
            boolean allStrings = true;
            java.util.Iterator<List<?>> sanIt = stored.iterator();
            while (sanIt.hasNext()) {
                if (!(sanIt.next().get(1) instanceof String)) { allStrings = false; }
            }
            if (!allStrings) { return i; }
            i++;
            // Es una copia: vaciarla no cambia el criterio.
            stored.clear();
            if (san.getSubjectAlternativeNames().size() != 3) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // Los tipos 0, 3 y 5 no tienen forma de texto acordada y se rechazan.
        int[] noTextForm = new int[] {0, 3, 5, -1, 9};
        int k = 0;
        while (k < noTextForm.length) {
            threw = false;
            try { new X509CertSelector().addSubjectAlternativeName(noTextForm[k], "a@b.com"); }
            catch (java.io.IOException e) { threw = true; }
            catch (Exception e) { threw = false; }
            if (!threw) { return i; }
            k = k + 1;
        }
        i++;
        // Un nombre DNS mal escrito se rechaza; por eso `CN=Juan Perez` no cuenta como uno.
        threw = false;
        try { new X509CertSelector().addSubjectAlternativeName(2, ""); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new X509CertSelector().addSubjectAlternativeName(2, "a_b.com"); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new X509CertSelector().addSubjectAlternativeName(2, "a..b"); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new X509CertSelector().addSubjectAlternativeName(8, "1"); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new X509CertSelector().addSubjectAlternativeName(6, "a.com"); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;

        // El DER va SIN la etiqueta de contexto, y lo que entro como bytes sale como bytes.
        try {
            X509CertSelector sanDer = new X509CertSelector();
            sanDer.addSubjectAlternativeName(2, ia5(0x16, "acme.com"));
            Object rawName = sanDer.getSubjectAlternativeNames().iterator().next().get(1);
            if (!(rawName instanceof byte[])) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }
        threw = false;
        try { new X509CertSelector().addSubjectAlternativeName(2, new byte[] {1, 2, 3}); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;

        // -- match contra el SubjectAltName del certificado
        byte[][] fourNames = new byte[][] {ia5(0x82, "acme.com"), ia5(0x81, "u@acme.com"),
            ipName(new byte[] {10, 0, 0, 1}), dirName("CN=Juan,O=Acme")};
        Cert withSan = certFor("CN=x", fourNames, null);
        try {
            X509CertSelector m1 = new X509CertSelector();
            m1.addSubjectAlternativeName(2, "acme.com");
            if (!m1.match(withSan)) { return i; }
            i++;
            // El nombre de host no distingue mayusculas.
            X509CertSelector byUpperCase = new X509CertSelector();
            byUpperCase.addSubjectAlternativeName(2, "ACME.COM");
            if (!byUpperCase.match(withSan)) { return i; }
            i++;
            X509CertSelector byMail = new X509CertSelector();
            byMail.addSubjectAlternativeName(1, "u@acme.com");
            if (!byMail.match(withSan)) { return i; }
            i++;
            X509CertSelector byIp = new X509CertSelector();
            byIp.addSubjectAlternativeName(7, "10.0.0.1");
            if (!byIp.match(withSan)) { return i; }
            i++;
            // El nombre X.500 se compara canonicamente, no por texto.
            X509CertSelector byDirName = new X509CertSelector();
            byDirName.addSubjectAlternativeName(4, "cn=JUAN,  o=acme");
            if (!byDirName.match(withSan)) { return i; }
            i++;
            // matchAll: el default exige TODOS; en false alcanza con uno.
            X509CertSelector byTwoNames = new X509CertSelector();
            byTwoNames.addSubjectAlternativeName(2, "acme.com");
            byTwoNames.addSubjectAlternativeName(2, "otro.com");
            if (!byTwoNames.getMatchAllSubjectAltNames()) { return i; }
            i++;
            if (byTwoNames.match(withSan)) { return i; }
            i++;
            byTwoNames.setMatchAllSubjectAltNames(false);
            if (!byTwoNames.match(withSan)) { return i; }
            i++;
            // Un certificado sin la extension no trae ningun nombre.
            if (m1.match(certFor("CN=x", null, null))) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- setSubjectAlternativeNames
        try {
            X509CertSelector sl = new X509CertSelector();
            List<List<?>> sanList = new ArrayList<List<?>>();
            List<Object> oneEntry = new ArrayList<Object>();
            oneEntry.add(Integer.valueOf(2));
            oneEntry.add("acme.com");
            sanList.add(oneEntry);
            sl.setSubjectAlternativeNames(sanList);
            if (sl.getSubjectAlternativeNames().size() != 1) { return i; }
            i++;
            if (!sl.match(withSan)) { return i; }
            i++;
            sl.setSubjectAlternativeNames(null);
            if (sl.getSubjectAlternativeNames() != null) { return i; }
            i++;
            sl.setSubjectAlternativeNames(new ArrayList<List<?>>());
            if (sl.getSubjectAlternativeNames() != null) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }
        threw = false;
        try {
            List<List<?>> badList = new ArrayList<List<?>>();
            List<Object> tooShort = new ArrayList<Object>();
            tooShort.add(Integer.valueOf(2));
            badList.add(tooShort);
            new X509CertSelector().setSubjectAlternativeNames(badList);
        } catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // setNameConstraints: los nombres del CERTIFICADO contra las restricciones del criterio
        // ======================================================================================
        byte[] acmeOnly = ncValue(new byte[][] {ia5(0x82, "acme.com")}, null);
        try {
            X509CertSelector nc1 = new X509CertSelector();
            nc1.setNameConstraints(acmeOnly);
            // Copia al salir.
            byte[] taken = nc1.getNameConstraints();
            taken[0] = 0x7f;
            if (nc1.getNameConstraints()[0] != acmeOnly[0]) { return i; }
            i++;
            // El corte va en el punto: `acme.com` abarca `www.acme.com` pero no `xacme.com`.
            if (!nc1.match(certFor("CN=x", new byte[][] {ia5(0x82, "www.acme.com")}, null))) { return i; }
            i++;
            if (!nc1.match(certFor("CN=x", new byte[][] {ia5(0x82, "acme.com")}, null))) { return i; }
            i++;
            if (nc1.match(certFor("CN=x", new byte[][] {ia5(0x82, "xacme.com")}, null))) { return i; }
            i++;
            if (nc1.match(certFor("CN=x", new byte[][] {ia5(0x82, "www.otro.com")}, null))) { return i; }
            i++;
            // Un nombre de OTRO tipo no esta restringido y pasa.
            if (!nc1.match(certFor("O=Acme",
                new byte[][] {ia5(0x82, "www.acme.com"), ia5(0x81, "u@zzz.com")}, null))) { return i; }
            i++;
            // La regla heredada: el CN se comprueba TAMBIEN como nombre DNS, pero solo si el
            // certificado no trae ningun dNSName propio.
            if (nc1.match(certFor("CN=www.otro.com", null, null))) { return i; }
            i++;
            if (!nc1.match(certFor("CN=www.acme.com", null, null))) { return i; }
            i++;
            // Con un dNSName propio el CN ya no se mira.
            if (!nc1.match(certFor("CN=www.otro.com",
                new byte[][] {ia5(0x82, "www.acme.com")}, null))) { return i; }
            i++;
            // Sin ningun dNSName propio, si.
            if (nc1.match(certFor("CN=www.otro.com",
                new byte[][] {ia5(0x81, "u@zzz.com")}, null))) { return i; }
            i++;
            // Un CN que no tiene forma de nombre DNS se saltea.
            if (!nc1.match(certFor("CN=Juan Perez", null, null))) { return i; }
            i++;
            // Un sujeto sin CN tampoco aporta ningun dNSName.
            if (!nc1.match(certFor("O=Acme", null, null))) { return i; }
            i++;
            // Si hay varios nombres del tipo restringido, tienen que caer TODOS.
            if (nc1.match(certFor("O=Acme",
                new byte[][] {ia5(0x82, "www.acme.com"), ia5(0x82, "www.otro.com")}, null))) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- excluidos
        try {
            X509CertSelector nc2 = new X509CertSelector();
            nc2.setNameConstraints(ncValue(null, new byte[][] {ia5(0x82, "otro.com")}));
            if (nc2.match(certFor("O=Acme", new byte[][] {ia5(0x82, "www.otro.com")}, null))) { return i; }
            i++;
            if (nc2.match(certFor("O=Acme", new byte[][] {ia5(0x82, "otro.com")}, null))) { return i; }
            i++;
            if (!nc2.match(certFor("O=Acme", new byte[][] {ia5(0x82, "www.acme.com")}, null))) { return i; }
            i++;
            if (!nc2.match(certFor("O=Acme", null, null))) { return i; }
            i++;
            // Permitido y excluido a la vez: el excluido manda.
            X509CertSelector nc3 = new X509CertSelector();
            nc3.setNameConstraints(ncValue(new byte[][] {ia5(0x82, "acme.com")},
                new byte[][] {ia5(0x82, "malo.acme.com")}));
            if (!nc3.match(certFor("O=Acme", new byte[][] {ia5(0x82, "www.acme.com")}, null))) { return i; }
            i++;
            if (nc3.match(certFor("O=Acme", new byte[][] {ia5(0x82, "malo.acme.com")}, null))) { return i; }
            i++;
            if (nc3.match(certFor("O=Acme", new byte[][] {ia5(0x82, "sub.malo.acme.com")}, null))) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- subarbol X.500: el prefijo del DER es el sufijo del texto
        try {
            X509CertSelector byDirSubtree = new X509CertSelector();
            byDirSubtree.setNameConstraints(ncValue(new byte[][] {dirName("O=Acme")}, null));
            if (!byDirSubtree.match(certFor("CN=Juan,O=Acme", null, null))) { return i; }
            i++;
            if (!byDirSubtree.match(certFor("O=Acme", null, null))) { return i; }
            i++;
            if (byDirSubtree.match(certFor("CN=Juan,O=Otro", null, null))) { return i; }
            i++;
            // La raiz del directorio contiene a todos.
            X509CertSelector byRootSubtree = new X509CertSelector();
            byRootSubtree.setNameConstraints(ncValue(new byte[][] {dirName("")}, null));
            if (!byRootSubtree.match(certFor("CN=Juan,O=Acme", null, null))) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- correo: buzon, host pelado y dominio con punto son tres cosas distintas
        try {
            X509CertSelector byMailHost = new X509CertSelector();
            byMailHost.setNameConstraints(ncValue(new byte[][] {ia5(0x81, "acme.com")}, null));
            if (!byMailHost.match(certFor("O=Acme", new byte[][] {ia5(0x81, "u@acme.com")}, null))) { return i; }
            i++;
            // Un host pelado NO abarca subdominios, al reves que en dNSName.
            if (byMailHost.match(certFor("O=Acme", new byte[][] {ia5(0x81, "u@sub.acme.com")}, null))) { return i; }
            i++;
            X509CertSelector byMailDomain = new X509CertSelector();
            byMailDomain.setNameConstraints(ncValue(new byte[][] {ia5(0x81, ".acme.com")}, null));
            if (!byMailDomain.match(certFor("O=Acme",
                new byte[][] {ia5(0x81, "u@sub.acme.com")}, null))) { return i; }
            i++;
            if (byMailDomain.match(certFor("O=Acme", new byte[][] {ia5(0x81, "u@acme.com")}, null))) { return i; }
            i++;
            X509CertSelector byMailbox = new X509CertSelector();
            byMailbox.setNameConstraints(ncValue(new byte[][] {ia5(0x81, "u@acme.com")}, null));
            if (!byMailbox.match(certFor("O=Acme", new byte[][] {ia5(0x81, "u@acme.com")}, null))) { return i; }
            i++;
            if (byMailbox.match(certFor("O=Acme", new byte[][] {ia5(0x81, "v@acme.com")}, null))) { return i; }
            i++;
            // Y el EMAILADDRESS del sujeto cuenta como rfc822Name si no hay ninguno propio.
            if (byMailHost.match(certFor("EMAILADDRESS=u@zzz.com,O=Acme", null, null))) { return i; }
            i++;
            if (!byMailHost.match(certFor("EMAILADDRESS=u@zzz.com,O=Acme",
                new byte[][] {ia5(0x81, "u@acme.com")}, null))) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- URI: solo se mira el host, y el host pelado no abarca subdominios
        try {
            X509CertSelector byUriHost = new X509CertSelector();
            byUriHost.setNameConstraints(ncValue(new byte[][] {ia5(0x86, "www.acme.com")}, null));
            if (!byUriHost.match(certFor("O=Acme",
                new byte[][] {ia5(0x86, "http://www.acme.com/a")}, null))) { return i; }
            i++;
            if (byUriHost.match(certFor("O=Acme",
                new byte[][] {ia5(0x86, "http://acme.com/a")}, null))) { return i; }
            i++;
            X509CertSelector byUriDomain = new X509CertSelector();
            byUriDomain.setNameConstraints(ncValue(new byte[][] {ia5(0x86, ".acme.com")}, null));
            if (!byUriDomain.match(certFor("O=Acme",
                new byte[][] {ia5(0x86, "http://www.acme.com/a")}, null))) { return i; }
            i++;
            if (byUriDomain.match(certFor("O=Acme",
                new byte[][] {ia5(0x86, "http://acme.com/a")}, null))) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // -- IP con mascara
        try {
            X509CertSelector byIpSubtree = new X509CertSelector();
            byIpSubtree.setNameConstraints(ncValue(new byte[][] {tlv(0x87,
                new byte[] {10, 1, 2, 0, (byte) 255, (byte) 255, (byte) 255, 0})}, null));
            if (!byIpSubtree.match(certFor("O=Acme",
                new byte[][] {ipName(new byte[] {10, 1, 2, 7})}, null))) { return i; }
            i++;
            if (byIpSubtree.match(certFor("O=Acme",
                new byte[][] {ipName(new byte[] {10, 1, 3, 7})}, null))) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        threw = false;
        try { new X509CertSelector().setNameConstraints(new byte[] {1, 2, 3}); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;
        try {
            X509CertSelector cleared = new X509CertSelector();
            cleared.setNameConstraints(acmeOnly);
            cleared.setNameConstraints(null);
            if (cleared.getNameConstraints() != null) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        // ======================================================================================
        // pathToNames: los nombres del CRITERIO contra las restricciones DEL CERTIFICADO
        // ======================================================================================
        Cert acmeCa = certFor("CN=CA Acme", null, acmeOnly);
        try {
            X509CertSelector pt = new X509CertSelector();
            pt.addPathToName(2, "www.acme.com");
            if (pt.getPathToNames().size() != 1) { return i; }
            i++;
            if (!pt.match(acmeCa)) { return i; }
            i++;
            X509CertSelector pt2 = new X509CertSelector();
            pt2.addPathToName(2, "www.otro.com");
            if (pt2.match(acmeCa)) { return i; }
            i++;
            // Un certificado sin la extension no restringe nada y pasa siempre.
            if (!pt2.match(certFor("CN=x", null, null))) { return i; }
            i++;
            // Un nombre de otro tipo no esta restringido por un subarbol de dNSName.
            X509CertSelector pt3 = new X509CertSelector();
            pt3.addPathToName(1, "u@zzz.com");
            if (!pt3.match(acmeCa)) { return i; }
            i++;
            pt.setPathToNames(null);
            if (pt.getPathToNames() != null) { return i; }
            i++;
            X509CertSelector pt4 = new X509CertSelector();
            pt4.addPathToName(2, ia5(0x16, "www.acme.com"));
            if (!pt4.match(acmeCa)) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }
        threw = false;
        try { new X509CertSelector().addPathToName(3, "x"); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // setSubjectPublicKey(byte[])
        // ======================================================================================
        byte[] spki = fromHex(SPKI_HEX);
        try {
            X509CertSelector pk = new X509CertSelector();
            pk.setSubjectPublicKey(spki);
            PublicKey readKey = pk.getSubjectPublicKey();
            if (!readKey.getAlgorithm().equals("RSA")) { return i; }
            i++;
            if (!readKey.getFormat().equals("X.509")) { return i; }
            i++;
            if (!java.util.Arrays.equals(readKey.getEncoded(), spki)) { return i; }
            i++;
            // getEncoded() devuelve una copia.
            readKey.getEncoded()[0] = 0x7f;
            if (readKey.getEncoded()[0] != spki[0]) { return i; }
            i++;
            pk.setSubjectPublicKey((byte[]) null);
            if (pk.getSubjectPublicKey() != null) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }
        threw = false;
        try { new X509CertSelector().setSubjectPublicKey(new byte[] {1, 2, 3}); }
        catch (java.io.IOException e) { threw = true; }
        if (!threw) { return i; } i++;

        // -- clone conserva las tres listas nuevas, y son copias
        try {
            X509CertSelector whole = new X509CertSelector();
            whole.addSubjectAlternativeName(2, "acme.com");
            whole.addPathToName(2, "www.acme.com");
            whole.setNameConstraints(acmeOnly);
            X509CertSelector cloned = (X509CertSelector) whole.clone();
            if (cloned.getSubjectAlternativeNames().size() != 1) { return i; }
            i++;
            if (cloned.getPathToNames().size() != 1) { return i; }
            i++;
            if (cloned.getNameConstraints() == null) { return i; }
            i++;
            cloned.addSubjectAlternativeName(2, "otro.com");
            if (whole.getSubjectAlternativeNames().size() != 1) { return i; }
            i++;
        } catch (Exception e) {
            return i;
        }

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
