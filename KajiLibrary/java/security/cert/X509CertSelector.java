package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// Un criterio para elegir certificados X.509 de un `CertStore`.
//
// Es un objeto de criterios acumulables: se van poniendo condiciones y `match` devuelve true solo
// si el certificado cumple **todas**. Un selector recien creado no tiene ninguna condicion, asi que
// acepta cualquier certificado X.509 —y ese es el default correcto, porque un selector es un filtro
// de busqueda y no un chequeo de seguridad—.
//
// Que un certificado pase el selector **no dice nada sobre si es de fiar**. El selector no verifica
// firmas ni cadenas; solo compara campos. Es el paso previo a la validacion, no un sustituto.
//
// ===============================================================================================
// QUE CRITERIOS ESTAN Y CUALES NO
// ===============================================================================================
//
// La regla que se siguio es simple: un criterio se declara solo si `match` lo puede aplicar de
// verdad. Un setter cuyo criterio `match` ignorara seria peor que su ausencia, porque devolveria
// true para certificados que no lo cumplen —y aca eso significa elegir el certificado equivocado—.
//
// **Estan todos**, y `match` los aplica todos: certificado exacto, emisor, sujeto, numero de serie,
// vigencia en una fecha, vigencia de la clave privada, clave publica exacta, OID del algoritmo de la
// clave publica, KeyUsage, ExtendedKeyUsage, SubjectKeyIdentifier, AuthorityKeyIdentifier,
// BasicConstraints, politicas, nombres alternativos, restricciones de nombres y `pathToNames`.
//
// Los tres ultimos llegaron con `GeneralNameValue` y `NameConstraints`, que son las dos piezas que
// faltaban. Vale la pena decir en que se diferencian los dos que suenan parecido, porque miran cosas
// opuestas del certificado:
//
//   - **`setNameConstraints`** pone las restricciones **del llamador** y las aplica a los nombres
//     **del certificado**: "traeme uno cuyo sujeto y cuyos nombres alternativos caigan aca adentro".
//   - **`setPathToNames`** hace lo contrario: son nombres del llamador que se comprueban contra la
//     extension NameConstraints **del propio certificado**, o sea "traeme una CA que pueda emitir
//     para estos nombres". Un certificado sin esa extension no restringe nada y pasa siempre.
//
// `setMatchAllSubjectAltNames` gobierna los nombres alternativos y ahora **si** cambia el resultado:
// en true --el default-- el certificado tiene que traer todos los nombres pedidos; en false le
// alcanza con traer uno.
public class X509CertSelector implements CertSelector {

    private static final String OID_SUBJECT_KEY_ID = "2.5.29.14";
    private static final String OID_AUTHORITY_KEY_ID = "2.5.29.35";
    private static final String OID_CERT_POLICIES = "2.5.29.32";
    private static final String OID_PRIVATE_KEY_USAGE = "2.5.29.16";
    private static final String OID_SUBJECT_ALT_NAME = "2.5.29.17";
    private static final String OID_NAME_CONSTRAINTS = "2.5.29.30";
    // El "cualquier uso" de ExtendedKeyUsage: un certificado que lo lleva satisface cualquier
    // exigencia de uso extendido.
    private static final String OID_ANY_EXTENDED_KEY_USAGE = "2.5.29.37.0";

    private X509Certificate x509Cert;
    private BigInteger serialNumber;
    private byte[] subjectKeyID;
    private byte[] authorityKeyID;
    private Date certificateValid;
    private Date privateKeyValid;
    // Los nombres alternativos se guardan dos veces por lo mismo que los emisores de
    // `X509CRLSelector`: uno es lo que el llamador puso y el otro es con lo que se compara.
    private List<List<?>> subjectAlternativeNames;
    private List<GeneralNameValue> subjectAlternativeGeneralNames;
    private List<List<?>> pathToNames;
    private List<GeneralNameValue> pathToGeneralNames;
    private byte[] nameConstraintsBytes;
    private NameConstraints nameConstraints;
    private javax.security.auth.x500.X500Principal issuer;
    private javax.security.auth.x500.X500Principal subject;
    private String subjectPublicKeyAlgID;
    private PublicKey subjectPublicKey;
    private byte[] subjectPublicKeyBytes;
    private boolean[] keyUsage;
    private Set<String> keyPurposeSet;
    private boolean matchAllSubjectAltNames = true;
    // -1 es "no me importa"; -2 es "tiene que ser de entidad final"; >= 0 es el largo minimo.
    private int basicConstraints = -1;
    private Set<String> policySet;

    // Un selector sin ningun criterio: acepta cualquier certificado X.509.
    public X509CertSelector() {
    }

    // Exige **este** certificado exacto. Es el criterio mas fuerte de todos y hace redundantes a
    // los demas.
    public void setCertificate(X509Certificate cert) {
        this.x509Cert = cert;
    }

    public X509Certificate getCertificate() {
        return this.x509Cert;
    }

    // Exige este emisor. Null saca el criterio.
    //
    // Se guarda la **instancia** que se paso, no una copia: `X500Principal` es inmutable, y el JDK
    // hace lo mismo —`getIssuer()` devuelve el mismo objeto—.
    public void setIssuer(javax.security.auth.x500.X500Principal issuer) {
        this.issuer = issuer;
    }

    // Idem, con el nombre escrito en RFC 2253.
    //
    // El texto se parsea aca y **no se guarda**: lo que queda es el nombre, y `getIssuerAsString()`
    // devuelve su forma canonica, no lo que se escribio. Es distinto de `TrustAnchor`, que si
    // conserva el original; la diferencia es del JDK y conviene tenerla presente.
    public void setIssuer(String issuerDN) throws IOException {
        this.issuer = issuerDN == null ? null : principalOf(issuerDN);
    }

    // Idem, con el DER del `Name`.
    public void setIssuer(byte[] issuerDN) throws IOException {
        this.issuer = issuerDN == null ? null : principalOf(issuerDN);
    }

    public javax.security.auth.x500.X500Principal getIssuer() {
        return this.issuer;
    }

    // El emisor del criterio en RFC 2253, o null si no hay.
    public String getIssuerAsString() {
        return this.issuer == null ? null : this.issuer.getName();
    }

    // El emisor del criterio como el DER de su `Name`, o null si no hay. Es una copia.
    //
    // Declara `IOException` porque el JDK lo declara —ahi el nombre se guarda en otra forma y hay
    // que codificarlo—; aca se guarda ya codificado y nunca llega a lanzarse.
    public byte[] getIssuerAsBytes() throws IOException {
        return this.issuer == null ? null : this.issuer.getEncoded();
    }

    // Exige este sujeto. Null saca el criterio. Vale todo lo dicho para el emisor.
    public void setSubject(javax.security.auth.x500.X500Principal subject) {
        this.subject = subject;
    }

    public void setSubject(String subjectDN) throws IOException {
        this.subject = subjectDN == null ? null : principalOf(subjectDN);
    }

    public void setSubject(byte[] subjectDN) throws IOException {
        this.subject = subjectDN == null ? null : principalOf(subjectDN);
    }

    public javax.security.auth.x500.X500Principal getSubject() {
        return this.subject;
    }

    public String getSubjectAsString() {
        return this.subject == null ? null : this.subject.getName();
    }

    public byte[] getSubjectAsBytes() throws IOException {
        return this.subject == null ? null : this.subject.getEncoded();
    }

    // `X500Principal` rechaza con `IllegalArgumentException` y estos metodos prometen `IOException`.
    // Los mensajes son los del JDK: distingue el nombre mal escrito del DER que no es un nombre.
    private static javax.security.auth.x500.X500Principal principalOf(String dn)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(dn);
        } catch (IllegalArgumentException e) {
            throw new IOException("Incorrect AVA format", e);
        }
    }

    private static javax.security.auth.x500.X500Principal principalOf(byte[] dn)
            throws IOException {
        try {
            return new javax.security.auth.x500.X500Principal(dn);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid name", e);
        }
    }

    /**
     * Exige que el certificado traiga este nombre alternativo. Se pueden pedir varios; ver
     * {@link #setMatchAllSubjectAltNames} para si hacen falta todos o alcanza con uno.
     *
     * <p>El tipo es el numero del `CHOICE` de `GeneralName`: 1 rfc822Name, 2 dNSName,
     * 4 directoryName, 6 URI, 7 iPAddress, 8 registeredID. Los otros tres --0, 3 y 5-- no tienen
     * forma de texto acordada y se rechazan; para esos esta la sobrecarga que toma el DER.
     *
     * @throws IOException si el tipo no tiene forma de texto o el nombre no es valido para el
     */
    public void addSubjectAlternativeName(int type, String name) throws IOException {
        addAlternativeName(GeneralNameValue.ofString(type, name), type, name);
    }

    /**
     * Idem, con el DER del nombre.
     *
     * <p><b>Sin la etiqueta de contexto</b>: un IA5String pelado para los tipos de texto, un `Name`
     * para directoryName, un OCTET STRING para iPAddress. Es lo que espera el JDK y es facil de
     * equivocar, porque en un certificado el mismo nombre viaja **con** su etiqueta.
     */
    public void addSubjectAlternativeName(int type, byte[] name) throws IOException {
        addAlternativeName(GeneralNameValue.ofValueDer(type, name), type, copy(name));
    }

    private void addAlternativeName(GeneralNameValue parsed, int type, Object raw) {
        if (this.subjectAlternativeNames == null) {
            this.subjectAlternativeNames = new ArrayList<List<?>>();
            this.subjectAlternativeGeneralNames = new ArrayList<GeneralNameValue>();
        }
        List<Object> entry = new ArrayList<Object>();
        entry.add(Integer.valueOf(type));
        entry.add(raw);
        this.subjectAlternativeNames.add(Collections.unmodifiableList(entry));
        this.subjectAlternativeGeneralNames.add(parsed);
    }

    /**
     * Exige estos nombres alternativos. Cada elemento es una lista de dos: el tipo como
     * {@code Integer} y el nombre como {@code String} o {@code byte[]}. Null o vacio saca el
     * criterio.
     *
     * <p>Los dos conjuntos se arman completos antes de asignar ninguno: si un elemento del medio
     * esta mal, el selector queda como estaba y no a mitad de camino.
     *
     * @throws IOException si algun elemento no tiene la forma esperada
     */
    public void setSubjectAlternativeNames(java.util.Collection<List<?>> names) throws IOException {
        if (names == null || names.isEmpty()) {
            this.subjectAlternativeNames = null;
            this.subjectAlternativeGeneralNames = null;
            return;
        }
        List<List<?>> raw = new ArrayList<List<?>>();
        List<GeneralNameValue> parsed = new ArrayList<GeneralNameValue>();
        Iterator<List<?>> it = names.iterator();
        while (it.hasNext()) {
            List<?> entry = it.next();
            if (entry == null || entry.size() != 2) {
                throw new IOException("name list entry must be of size 2");
            }
            if (!(entry.get(0) instanceof Integer)) {
                throw new IOException("expected an Integer name type");
            }
            int type = ((Integer) entry.get(0)).intValue();
            Object value = entry.get(1);
            List<Object> entryCopy = new ArrayList<Object>();
            entryCopy.add(Integer.valueOf(type));
            if (value instanceof String) {
                parsed.add(GeneralNameValue.ofString(type, (String) value));
                entryCopy.add(value);
            } else if (value instanceof byte[]) {
                parsed.add(GeneralNameValue.ofValueDer(type, (byte[]) value));
                entryCopy.add(copy((byte[]) value));
            } else {
                throw new IOException("name not byte array or String");
            }
            raw.add(Collections.unmodifiableList(entryCopy));
        }
        this.subjectAlternativeNames = raw;
        this.subjectAlternativeGeneralNames = parsed;
    }

    /**
     * Los nombres alternativos del criterio, o null si no hay. Copia: los {@code byte[]} van
     * clonados, asi que tocar lo que sale de aca no cambia el criterio.
     */
    public java.util.Collection<List<?>> getSubjectAlternativeNames() {
        return copyOfNameList(this.subjectAlternativeNames);
    }

    /**
     * Exige que el certificado pueda emitir para estos nombres, segun **su propia** extension
     * NameConstraints. Null o vacio saca el criterio.
     *
     * <p>Es el criterio para buscar una CA, no un certificado de entidad final: la pregunta que
     * contesta es "¿esta CA tiene derecho a firmar algo llamado asi?". Un certificado sin la
     * extension no restringe nada y pasa siempre.
     *
     * @throws IOException si algun elemento no tiene la forma esperada
     */
    public void setPathToNames(java.util.Collection<List<?>> names) throws IOException {
        if (names == null || names.isEmpty()) {
            this.pathToNames = null;
            this.pathToGeneralNames = null;
            return;
        }
        X509CertSelector tmp = new X509CertSelector();
        tmp.setSubjectAlternativeNames(names);
        this.pathToNames = tmp.subjectAlternativeNames;
        this.pathToGeneralNames = tmp.subjectAlternativeGeneralNames;
    }

    /** Agrega un nombre al criterio de {@link #setPathToNames}. */
    public void addPathToName(int type, String name) throws IOException {
        addPath(GeneralNameValue.ofString(type, name), type, name);
    }

    /** Idem, con el DER del nombre y sin su etiqueta de contexto. */
    public void addPathToName(int type, byte[] name) throws IOException {
        addPath(GeneralNameValue.ofValueDer(type, name), type, copy(name));
    }

    private void addPath(GeneralNameValue parsed, int type, Object raw) {
        if (this.pathToNames == null) {
            this.pathToNames = new ArrayList<List<?>>();
            this.pathToGeneralNames = new ArrayList<GeneralNameValue>();
        }
        List<Object> entry = new ArrayList<Object>();
        entry.add(Integer.valueOf(type));
        entry.add(raw);
        this.pathToNames.add(Collections.unmodifiableList(entry));
        this.pathToGeneralNames.add(parsed);
    }

    /** Los nombres de {@link #setPathToNames}, o null si no hay. Copia. */
    public java.util.Collection<List<?>> getPathToNames() {
        return copyOfNameList(this.pathToNames);
    }

    /**
     * Exige que el sujeto y los nombres alternativos del certificado caigan adentro de estas
     * restricciones. El argumento es el DER del **valor** de la extension NameConstraints.
     *
     * <p>Se parsea aca: unas restricciones mal formadas son un error del llamador, y guardarlas sin
     * mirar dejaria un criterio que dice restringir y no restringe.
     *
     * @throws IOException si el DER no es una extension NameConstraints bien formada
     */
    public void setNameConstraints(byte[] bytes) throws IOException {
        if (bytes == null) {
            this.nameConstraintsBytes = null;
            this.nameConstraints = null;
            return;
        }
        this.nameConstraints = NameConstraints.of(bytes);
        this.nameConstraintsBytes = copy(bytes);
    }

    /** Copia del DER de las restricciones, o null si no hay. */
    public byte[] getNameConstraints() {
        return copy(this.nameConstraintsBytes);
    }

    private static java.util.Collection<List<?>> copyOfNameList(List<List<?>> names) {
        if (names == null) {
            return null;
        }
        List<List<?>> out = new ArrayList<List<?>>();
        int i = 0;
        while (i < names.size()) {
            List<?> entry = names.get(i);
            List<Object> one = new ArrayList<Object>();
            one.add(entry.get(0));
            Object value = entry.get(1);
            one.add(value instanceof byte[] ? copy((byte[]) value) : value);
            out.add(Collections.unmodifiableList(one));
            i = i + 1;
        }
        return out;
    }

    // Exige este numero de serie. Por si solo **no identifica** un certificado: la serie es unica
    // por emisor, asi que sin fijar tambien el emisor esto puede traer certificados de otras CAs.
    public void setSerialNumber(BigInteger serial) {
        this.serialNumber = serial;
    }

    public BigInteger getSerialNumber() {
        return this.serialNumber;
    }

    // El SubjectKeyIdentifier que tiene que tener, como el **DER del KeyIdentifier** —es decir, un
    // OCTET STRING completo con su etiqueta y su largo, no los bytes pelados del identificador—.
    // Es lo que dice el contrato del JDK y es facil de equivocar.
    public void setSubjectKeyIdentifier(byte[] subjectKeyID) {
        this.subjectKeyID = copy(subjectKeyID);
    }

    public byte[] getSubjectKeyIdentifier() {
        return copy(this.subjectKeyID);
    }

    // El AuthorityKeyIdentifier, con el mismo formato: el DER completo de la extension ya
    // desenvuelta del OCTET STRING exterior.
    public void setAuthorityKeyIdentifier(byte[] authorityKeyID) {
        this.authorityKeyID = copy(authorityKeyID);
    }

    public byte[] getAuthorityKeyIdentifier() {
        return copy(this.authorityKeyID);
    }

    // Exige que el certificado este vigente en esta fecha. Null saca el criterio.
    public void setCertificateValid(Date certValid) {
        if (certValid == null) {
            this.certificateValid = null;
        } else {
            this.certificateValid = new Date(certValid.getTime());
        }
    }

    public Date getCertificateValid() {
        if (this.certificateValid == null) {
            return null;
        }
        return new Date(this.certificateValid.getTime());
    }

    // Exige que la **clave privada** del certificado estuviera vigente en esta fecha, segun la
    // extension PrivateKeyUsagePeriod. Null saca el criterio.
    //
    // No es lo mismo que `setCertificateValid`, y la diferencia es el motivo por el que la extension
    // existe: una clave de firma deja de poder firmar antes de que su certificado venza, para que
    // las firmas viejas se puedan seguir verificando despues. El certificado sigue siendo valido;
    // la clave ya no puede producir firmas nuevas.
    //
    // Un certificado **sin** la extension pasa el criterio. Es del JDK y hay que decirlo porque
    // suena al reves: sin extension no hay periodo declarado, o sea que la clave no se limito.
    public void setPrivateKeyValid(Date privateKeyValid) {
        if (privateKeyValid == null) {
            this.privateKeyValid = null;
        } else {
            this.privateKeyValid = new Date(privateKeyValid.getTime());
        }
    }

    public Date getPrivateKeyValid() {
        if (this.privateKeyValid == null) {
            return null;
        }
        return new Date(this.privateKeyValid.getTime());
    }

    // El OID del algoritmo de la clave publica: "1.2.840.113549.1.1.1" para RSA. Se valida que sea
    // un OID bien formado en el momento de ponerlo, no cuando se usa.
    public void setSubjectPublicKeyAlgID(String oid) throws IOException {
        if (oid == null) {
            this.subjectPublicKeyAlgID = null;
        } else {
            DerReader.validateOid(oid);
            this.subjectPublicKeyAlgID = oid;
        }
    }

    public String getSubjectPublicKeyAlgID() {
        return this.subjectPublicKeyAlgID;
    }

    // Exige exactamente esta clave publica. Se compara por la codificacion, no por identidad: dos
    // objetos distintos con los mismos bytes son la misma clave.
    public void setSubjectPublicKey(PublicKey key) {
        if (key == null) {
            this.subjectPublicKey = null;
            this.subjectPublicKeyBytes = null;
        } else {
            this.subjectPublicKey = key;
            this.subjectPublicKeyBytes = key.getEncoded();
        }
    }

    /**
     * Idem, con la clave en su forma codificada -- un `SubjectPublicKeyInfo` de X.509.
     *
     * <p>El DER se parsea aca y no cuando se usa: una clave mal formada es un error del llamador y
     * hay que decirselo en el momento, no dejar un criterio que despues no coincide con nada.
     *
     * <p>Lo que devuelve {@link #getSubjectPublicKey()} despues de esto no es la misma clase que
     * devolveria el JDK; ver {@code EncodedPublicKey} para la diferencia y su motivo.
     *
     * @throws IOException si el DER no es un SubjectPublicKeyInfo bien formado
     */
    public void setSubjectPublicKey(byte[] key) throws IOException {
        if (key == null) {
            this.subjectPublicKey = null;
            this.subjectPublicKeyBytes = null;
            return;
        }
        this.subjectPublicKey = EncodedPublicKey.of(key);
        this.subjectPublicKeyBytes = copy(key);
    }

    public PublicKey getSubjectPublicKey() {
        return this.subjectPublicKey;
    }

    // Los bits de KeyUsage que el certificado tiene que tener **prendidos**. Un false en la
    // posicion i no exige nada; solo los true son condiciones.
    //
    // Y hay una asimetria del JDK que conviene saber: un certificado **sin** extension KeyUsage
    // pasa siempre, porque no restringe nada. El criterio filtra certificados que declaran usos y
    // no incluyen los pedidos, no certificados que no declaran nada.
    public void setKeyUsage(boolean[] keyUsage) {
        if (keyUsage == null) {
            this.keyUsage = null;
        } else {
            boolean[] c = new boolean[keyUsage.length];
            System.arraycopy(keyUsage, 0, c, 0, keyUsage.length);
            this.keyUsage = c;
        }
    }

    public boolean[] getKeyUsage() {
        if (this.keyUsage == null) {
            return null;
        }
        boolean[] c = new boolean[this.keyUsage.length];
        System.arraycopy(this.keyUsage, 0, c, 0, this.keyUsage.length);
        return c;
    }

    // Los OIDs de ExtendedKeyUsage que el certificado tiene que tener. Un conjunto **vacio se trata
    // como null**: quiere decir "sin criterio", no "sin ningun uso".
    //
    // Igual que con KeyUsage: un certificado sin la extension pasa. Y uno que lleva
    // anyExtendedKeyUsage (2.5.29.37.0) tambien, porque ese OID significa exactamente "sirvo para
    // todo".
    public void setExtendedKeyUsage(Set<String> keyPurposeSet) throws IOException {
        if (keyPurposeSet == null || keyPurposeSet.isEmpty()) {
            this.keyPurposeSet = null;
        } else {
            Set<String> copyOf = new HashSet<String>(keyPurposeSet);
            Iterator<String> it = copyOf.iterator();
            while (it.hasNext()) {
                DerReader.validateOid(it.next());
            }
            this.keyPurposeSet = Collections.unmodifiableSet(copyOf);
        }
    }

    public Set<String> getExtendedKeyUsage() {
        return this.keyPurposeSet;
    }

    // Si hay que exigir **todos** los nombres alternativos o alcanza con uno.
    //
    // Ver la nota de la clase: el criterio que esta bandera gobierna no esta implementado aca, asi
    // que hoy guarda el valor y no cambia lo que `match` devuelve. El default es true, igual que en
    // el JDK.
    public void setMatchAllSubjectAltNames(boolean matchAllNames) {
        this.matchAllSubjectAltNames = matchAllNames;
    }

    public boolean getMatchAllSubjectAltNames() {
        return this.matchAllSubjectAltNames;
    }

    // La restriccion de BasicConstraints. Los tres rangos significan cosas distintas:
    //
    //   -1  sin criterio (el default).
    //   -2  el certificado tiene que ser de **entidad final**, o sea no una CA.
    //   >=0 tiene que ser una CA cuyo largo maximo de cadena sea al menos este numero.
    //
    // El -2 es el que sirve para "no me traigas CAs", y confundirlo con -1 hace que el filtro no
    // filtre.
    public void setBasicConstraints(int minMaxPathLen) {
        if (minMaxPathLen < -2) {
            throw new IllegalArgumentException("basic constraints less than -2");
        }
        this.basicConstraints = minMaxPathLen;
    }

    public int getBasicConstraints() {
        return this.basicConstraints;
    }

    // Los OIDs de politica que el certificado tiene que declarar. Alcanza con que tenga **uno** de
    // los del conjunto.
    //
    // El conjunto vacio no es lo mismo que null: vacio exige que el certificado tenga la extension
    // de politicas, sin importar cual; null saca el criterio.
    public void setPolicy(Set<String> certPolicySet) throws IOException {
        if (certPolicySet == null) {
            this.policySet = null;
        } else {
            Set<String> copyOf = new HashSet<String>(certPolicySet);
            Iterator<String> it = copyOf.iterator();
            while (it.hasNext()) {
                DerReader.validateOid(it.next());
            }
            this.policySet = Collections.unmodifiableSet(copyOf);
        }
    }

    public Set<String> getPolicy() {
        return this.policySet;
    }

    // Si el certificado cumple **todos** los criterios puestos.
    //
    // Un objeto que no es un `X509Certificate` no cumple: no hay forma de preguntarle nada de lo
    // que este selector compara.
    @Override
    public boolean match(Certificate cert) {
        if (!(cert instanceof X509Certificate)) {
            return false;
        }
        X509Certificate xcert = (X509Certificate) cert;

        if (this.x509Cert != null && !this.x509Cert.equals(xcert)) {
            return false;
        }
        if (this.serialNumber != null && !this.serialNumber.equals(xcert.getSerialNumber())) {
            return false;
        }
        // Los nombres se comparan por `X500Principal`, o sea por forma canonica. Comparar los textos
        // aceptaria un certificado ajeno cuyo nombre se escribe distinto pero significa lo mismo, y
        // rechazaria el propio por un espacio de mas.
        if (this.issuer != null && !this.issuer.equals(xcert.getIssuerX500Principal())) {
            return false;
        }
        if (this.subject != null && !this.subject.equals(xcert.getSubjectX500Principal())) {
            return false;
        }
        if (!matchesPrivateKeyPeriod(xcert)) {
            return false;
        }
        if (!matchesAlternativeNames(xcert)) {
            return false;
        }
        if (!matchesNameConstraints(xcert)) {
            return false;
        }
        if (!matchesPathToNames(xcert)) {
            return false;
        }
        if (this.certificateValid != null) {
            try {
                xcert.checkValidity(this.certificateValid);
            } catch (CertificateException e) {
                return false;
            }
        }
        if (this.subjectPublicKeyBytes != null) {
            byte[] certKey = xcert.getPublicKey().getEncoded();
            if (!sameBytes(this.subjectPublicKeyBytes, certKey)) {
                return false;
            }
        }
        return matchesBasicConstraints(xcert)
            && matchesKeyUsage(xcert)
            && matchesExtendedKeyUsage(xcert)
            && matchesKeyId(xcert, OID_SUBJECT_KEY_ID, this.subjectKeyID)
            && matchesKeyId(xcert, OID_AUTHORITY_KEY_ID, this.authorityKeyID)
            && matchesKeyAlgId(xcert)
            && matchesPolicy(xcert);
    }

    // La extension PrivateKeyUsagePeriod, si esta, tiene que cubrir la fecha pedida.
    //
    //   PrivateKeyUsagePeriod ::= SEQUENCE {
    //       notBefore [0] GeneralizedTime OPTIONAL,
    //       notAfter  [1] GeneralizedTime OPTIONAL }
    //
    // Los dos campos son opcionales y pueden faltar los dos —un SEQUENCE vacio, que segun el RFC no
    // deberia pasar pero se codifica igual—; ahi el periodo no limita nada y el certificado pasa.
    // Una extension **presente pero ilegible** en cambio no pasa: no se puede afirmar que la clave
    // estuviera vigente si no se entiende que dice el certificado, y el lado seguro es rechazar.
    private boolean matchesPrivateKeyPeriod(X509Certificate xcert) {
        if (this.privateKeyValid == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(OID_PRIVATE_KEY_USAGE);
        if (ext == null) {
            return true;
        }
        long when = this.privateKeyValid.getTime();
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_SEQUENCE);
            DerReader inner = new DerReader(value, d.position(), len);
            while (inner.hasMore()) {
                int tag = inner.readTag();
                int n = inner.readLength();
                int from = inner.skip(n);
                if (tag == 0x80) {
                    if (when < DerReader.generalizedTime(value, from, n)) {
                        return false;
                    }
                } else if (tag == 0x81) {
                    if (when > DerReader.generalizedTime(value, from, n)) {
                        return false;
                    }
                } else {
                    throw new IOException("DER: campo inesperado en PrivateKeyUsagePeriod");
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Los nombres pedidos tienen que estar en el SubjectAltName del certificado.
    //
    // `matchAllSubjectAltNames` decide si hacen falta todos o alcanza con uno. El default es
    // **todos**, que es el lado restrictivo: un selector que trae un certificado por coincidir en
    // uno solo de tres nombres no es lo que quien puso los tres estaba pidiendo.
    private boolean matchesAlternativeNames(X509Certificate xcert) {
        if (this.subjectAlternativeGeneralNames == null) {
            return true;
        }
        List<GeneralNameValue> delCert;
        try {
            delCert = alternativeNamesOf(xcert);
        } catch (IOException e) {
            // Un SubjectAltName ilegible no se puede afirmar que traiga los nombres pedidos.
            return false;
        }
        int i = 0;
        while (i < this.subjectAlternativeGeneralNames.size()) {
            boolean esta = delCert.contains(this.subjectAlternativeGeneralNames.get(i));
            if (esta && !this.matchAllSubjectAltNames) {
                return true;
            }
            if (!esta && this.matchAllSubjectAltNames) {
                return false;
            }
            i = i + 1;
        }
        // Con matchAll no fallo ninguno; sin matchAll no acerto ninguno.
        return this.matchAllSubjectAltNames;
    }

    private static List<GeneralNameValue> alternativeNamesOf(X509Certificate xcert)
            throws IOException {
        List<GeneralNameValue> out = new ArrayList<GeneralNameValue>();
        byte[] ext = xcert.getExtensionValue(OID_SUBJECT_ALT_NAME);
        if (ext == null) {
            return out;
        }
        byte[] value = DerReader.unwrapOctetString(ext);
        DerReader d = new DerReader(value, 0, value.length);
        int len = d.expect(DerReader.TAG_SEQUENCE);
        DerReader list = new DerReader(value, d.position(), len);
        while (list.hasMore()) {
            int at = list.position();
            int[] one = list.nextTlv();
            out.add(GeneralNameValue.ofTagged(value, at, one[1]));
        }
        return out;
    }

    // Los nombres del certificado tienen que caer adentro de las restricciones del criterio.
    private boolean matchesNameConstraints(X509Certificate xcert) {
        if (this.nameConstraints == null) {
            return true;
        }
        return this.nameConstraints.verify(xcert);
    }

    // Al reves: los nombres del criterio tienen que caer adentro de las restricciones **del
    // certificado**. Un certificado sin la extension no restringe nada y pasa.
    private boolean matchesPathToNames(X509Certificate xcert) {
        if (this.pathToGeneralNames == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(OID_NAME_CONSTRAINTS);
        if (ext == null) {
            return true;
        }
        try {
            NameConstraints nc = NameConstraints.of(DerReader.unwrapOctetString(ext));
            return nc.verify(this.pathToGeneralNames);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean matchesBasicConstraints(X509Certificate xcert) {
        if (this.basicConstraints == -1) {
            return true;
        }
        int maxPathLen = xcert.getBasicConstraints();
        if (this.basicConstraints == -2) {
            // -1 del certificado es "no es CA", que es justo lo que se pide.
            return maxPathLen == -1;
        }
        return maxPathLen >= this.basicConstraints;
    }

    private boolean matchesKeyUsage(X509Certificate xcert) {
        if (this.keyUsage == null) {
            return true;
        }
        boolean[] certKeyUsage = xcert.getKeyUsage();
        // Sin extension no hay restriccion que violar: pasa.
        if (certKeyUsage == null) {
            return true;
        }
        int i = 0;
        while (i < this.keyUsage.length) {
            if (this.keyUsage[i] && (i >= certKeyUsage.length || !certKeyUsage[i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private boolean matchesExtendedKeyUsage(X509Certificate xcert) {
        if (this.keyPurposeSet == null || this.keyPurposeSet.isEmpty()) {
            return true;
        }
        List<String> usos;
        try {
            usos = xcert.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            return false;
        }
        // Sin extension, el certificado no restringe para que sirve: pasa.
        if (usos == null) {
            return true;
        }
        if (usos.contains(OID_ANY_EXTENDED_KEY_USAGE)) {
            return true;
        }
        return usos.containsAll(this.keyPurposeSet);
    }

    // Compara un identificador de clave contra el de la extension. La extension viene envuelta en
    // un OCTET STRING; lo que se compara es lo de adentro contra lo que se puso en el selector.
    private boolean matchesKeyId(X509Certificate xcert, String oid, byte[] expected) {
        if (expected == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(oid);
        if (ext == null) {
            return false;
        }
        try {
            return sameBytes(expected, DerReader.unwrapOctetString(ext));
        } catch (IOException e) {
            return false;
        }
    }

    // Saca el OID del algoritmo del `SubjectPublicKeyInfo` de la clave.
    //
    // La estructura es SEQUENCE { AlgorithmIdentifier SEQUENCE { OID, params OPTIONAL },
    // BIT STRING }. Solo hace falta llegar al primer OID, que son tres pasos de DER y ninguna
    // decision de confianza.
    private boolean matchesKeyAlgId(X509Certificate xcert) {
        if (this.subjectPublicKeyAlgID == null) {
            return true;
        }
        PublicKey k = xcert.getPublicKey();
        if (k == null) {
            return false;
        }
        byte[] enc = k.getEncoded();
        if (enc == null) {
            return false;
        }
        try {
            DerReader d = new DerReader(enc, 0, enc.length);
            int extLen = d.expect(DerReader.TAG_SEQUENCE);
            DerReader inner = new DerReader(enc, d.position(), extLen);
            int algLen = inner.expect(DerReader.TAG_SEQUENCE);
            DerReader alg = new DerReader(enc, inner.position(), algLen);
            int oidLen = alg.expect(DerReader.TAG_OID);
            int from = alg.skip(oidLen);
            return this.subjectPublicKeyAlgID.equals(alg.readOid(from, oidLen));
        } catch (IOException e) {
            return false;
        }
    }

    // Comprueba las politicas del certificado.
    //
    // La extension es SEQUENCE OF PolicyInformation, y cada PolicyInformation es un SEQUENCE cuyo
    // primer elemento es el OID de la politica. El resto de cada entrada —los calificadores— se
    // saltea sin mirar, que es lo correcto: aca solo interesa que OIDs declara.
    private boolean matchesPolicy(X509Certificate xcert) {
        if (this.policySet == null) {
            return true;
        }
        byte[] ext = xcert.getExtensionValue(OID_CERT_POLICIES);
        if (ext == null) {
            return false;
        }
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_SEQUENCE);
            DerReader inner = new DerReader(value, d.position(), len);
            List<String> oids = new ArrayList<String>();
            while (inner.hasMore()) {
                int infoLen = inner.expect(DerReader.TAG_SEQUENCE);
                DerReader info = new DerReader(value, inner.position(), infoLen);
                inner.skip(infoLen);
                int oidLen = info.expect(DerReader.TAG_OID);
                int from = info.skip(oidLen);
                oids.add(info.readOid(from, oidLen));
            }
            // El conjunto vacio pide solo que la extension exista con alguna politica adentro.
            if (this.policySet.isEmpty()) {
                return !oids.isEmpty();
            }
            Iterator<String> it = oids.iterator();
            while (it.hasNext()) {
                if (this.policySet.contains(it.next())) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    // Copia con la que el `CertStore` se puede quedar. Los arreglos y la fecha se copian; los
    // conjuntos ya son inmutables.
    @Override
    public Object clone() {
        try {
            X509CertSelector copyOf = (X509CertSelector) super.clone();
            copyOf.subjectKeyID = copy(this.subjectKeyID);
            copyOf.authorityKeyID = copy(this.authorityKeyID);
            copyOf.subjectPublicKeyBytes = copy(this.subjectPublicKeyBytes);
            copyOf.keyUsage = this.getKeyUsage();
            copyOf.certificateValid = this.getCertificateValid();
            copyOf.privateKeyValid = this.getPrivateKeyValid();
            copyOf.nameConstraintsBytes = copy(this.nameConstraintsBytes);
            // Las tres listas se copian: el store se queda con el clon, y un `add` posterior sobre
            // el original no tiene que cambiarle el criterio a mitad de una busqueda.
            if (this.subjectAlternativeNames != null) {
                copyOf.subjectAlternativeNames =
                    new ArrayList<List<?>>(this.subjectAlternativeNames);
                copyOf.subjectAlternativeGeneralNames =
                    new ArrayList<GeneralNameValue>(this.subjectAlternativeGeneralNames);
            }
            if (this.pathToNames != null) {
                copyOf.pathToNames = new ArrayList<List<?>>(this.pathToNames);
                copyOf.pathToGeneralNames =
                    new ArrayList<GeneralNameValue>(this.pathToGeneralNames);
            }
            return copyOf;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    // A KajiLibrary subset: el JDK imprime tambien los criterios que aca no existen, y usa su
    // volcado hexadecimal interno para los arreglos. El formato no esta especificado; se conservan
    // la estructura y los nombres de los campos que si existen.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X509CertSelector: [\n");
        if (this.x509Cert != null) {
            sb.append("  Certificate: " + this.x509Cert.toString() + "\n");
        }
        if (this.serialNumber != null) {
            sb.append("  Serial Number: " + this.serialNumber.toString() + "\n");
        }
        if (this.issuer != null) {
            sb.append("  Issuer: " + this.issuer.getName() + "\n");
        }
        if (this.subject != null) {
            sb.append("  Subject: " + this.subject.getName() + "\n");
        }
        if (this.certificateValid != null) {
            sb.append("  Certificate Valid: " + this.certificateValid.toString() + "\n");
        }
        if (this.privateKeyValid != null) {
            sb.append("  Private Key Valid: " + this.privateKeyValid.toString() + "\n");
        }
        if (this.subjectPublicKeyAlgID != null) {
            sb.append("  Subject Public Key AlgID: " + this.subjectPublicKeyAlgID + "\n");
        }
        if (this.subjectPublicKey != null) {
            sb.append("  Subject Public Key: " + this.subjectPublicKey.toString() + "\n");
        }
        if (this.keyUsage != null) {
            sb.append("  Key Usage: " + this.keyUsage.length + " bits\n");
        }
        if (this.keyPurposeSet != null) {
            sb.append("  Extended Key Usage: " + this.keyPurposeSet.toString() + "\n");
        }
        if (this.subjectAlternativeNames != null) {
            sb.append("  Subject Alternative Names: " + this.subjectAlternativeNames.size()
                + " names\n");
        }
        if (this.pathToNames != null) {
            sb.append("  Path to Names: " + this.pathToNames.size() + " names\n");
        }
        if (this.nameConstraintsBytes != null) {
            sb.append("  Name Constraints: " + this.nameConstraintsBytes.length + " bytes\n");
        }
        sb.append("  Match All Subject Alt Names: " + this.matchAllSubjectAltNames + "\n");
        sb.append("  Basic Constraints: " + this.basicConstraints + "\n");
        if (this.policySet != null) {
            sb.append("  Policy: " + this.policySet.toString() + "\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static byte[] copy(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        int i = 0;
        while (i < a.length) {
            if (a[i] != b[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
}
