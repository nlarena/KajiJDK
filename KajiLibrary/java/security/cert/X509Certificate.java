package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.DEREncodable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Un certificado X.509 v3 (RFC 5280).
//
// ===============================================================================================
// POR QUE DECLARARLA ENTERA ES HONESTO
// ===============================================================================================
//
// Esta clase es **abstracta**, y eso cambia todo. No parsea un certificado ni verifica una firma:
// declara **que se le puede preguntar** a algo que ya es un certificado. Es un contrato, y un
// contrato completo es exactamente lo que hace falta para que el resto del paquete —los
// selectores, los validadores, las anclas de confianza— pueda escribirse sin inventar nada.
//
// Esta biblioteca **no trae ninguna subclase**: no hay parser de DER de certificados ni RSA ni
// ECDSA. Quien quiera un certificado de verdad tiene que traer una implementacion. Lo que esta
// clase promete es solo la forma.
//
// ===============================================================================================
// LOS NOMBRES X.500
// ===============================================================================================
//
// `getSubjectX500Principal()` y `getIssuerX500Principal()` estuvieron un tiempo afuera, con el
// argumento de que comparar nombres X.500 mal es como se falsifica una cadena y que no habia donde
// hacerlo bien. Ahora `javax.security.auth.x500.X500Principal` existe —con su decodificador de DER,
// su parser de RFC 2253 y su forma canonica, todos probados contra el JDK—, asi que el argumento se
// cayo: la parte riesgosa vive en un solo lugar y estos dos metodos solo la usan.
//
// Lo que hacen es lo mismo que hace el JDK: parsear los bytes de `getEncoded()` hasta el campo que
// corresponde y construir un `X500Principal` con ese tramo. **No** se usa `getIssuerDN()`: pasar por
// el `toString` de un `Principal` cualquiera y re-parsearlo es justamente la confusion de nombres
// que se queria evitar. Si el DER no se puede leer se lanza `RuntimeException`, que es lo que hace
// el JDK —el metodo no declara excepciones y no hay forma honesta de devolver un nombre igual—.
//
// `getSubjectAlternativeNames()` y `getIssuerAlternativeNames()` estan, pero devolviendo `null`
// como la clase base del JDK: son metodos concretos cuyo contrato es "las subclases lo saben, yo
// no". Decodificar `GeneralName` sigue sin estar, y por eso ninguna subclase de aca los sobrescribe.
//
// `getExtendedKeyUsage()` decodifica de verdad: su extension es un SEQUENCE OF OID y nada mas, sin
// ninguna decision de confianza adentro. Ver `DerReader` para donde se puso exactamente el limite.
public abstract class X509Certificate extends Certificate implements X509Extension, DEREncodable {

    private static final String OID_EXTENDED_KEY_USAGE = "2.5.29.37";

    // Se recuerdan porque parsear el certificado entero en cada llamada seria caro y porque el JDK
    // devuelve la misma instancia dos veces seguidas, cosa que hay codigo que compara con `==`.
    private javax.security.auth.x500.X500Principal issuerX500;
    private javax.security.auth.x500.X500Principal subjectX500;

    protected X509Certificate() {
        super("X.509");
    }

    // Comprueba que el certificado este vigente **ahora**. Sin valor de retorno: si no lanza, esta
    // vigente. Es el mismo contrato que `verify` y el mismo riesgo de tragarselo con un catch vacio.
    public abstract void checkValidity()
        throws CertificateExpiredException, CertificateNotYetValidException;

    // Idem, pero en una fecha dada. Sirve para verificar una firma vieja: la pregunta correcta ahi
    // no es si el certificado vale hoy sino si valia cuando se firmo.
    public abstract void checkValidity(Date date)
        throws CertificateExpiredException, CertificateNotYetValidException;

    // La version: 1, 2 o 3. Un certificado v1 no tiene extensiones, asi que no tiene ni
    // BasicConstraints ni KeyUsage; tratarlo como CA porque "no dice que no" es un error clasico.
    public abstract int getVersion();

    // El numero de serie. Unico **por emisor**, no en absoluto: el par (emisor, serie) es lo que
    // identifica un certificado, y por eso las CRLs y los selectores siempre piden los dos.
    public abstract BigInteger getSerialNumber();

    // El emisor como `Principal`.
    //
    // Este metodo esta desaconsejado en el JDK a favor de `getIssuerX500Principal()`, y con razon:
    // el `Principal` que devuelve es de una clase interna y comparar dos nombres por su `toString`
    // no es confiable. Aca es el unico que hay, porque el reemplazo necesita un tipo que esta
    // biblioteca no tiene; ver el comentario de la clase.
    public abstract Principal getIssuerDN();

    // El sujeto como `Principal`. Vale lo mismo que para `getIssuerDN()`.
    public abstract Principal getSubjectDN();

    // El emisor como nombre X.500, que es la forma con la que **si** se puede comparar.
    //
    // Es el reemplazo de `getIssuerDN()` y la diferencia no es cosmetica: dos nombres X.500 iguales
    // pueden escribirse distinto —mayusculas, espacios, orden de escape— y solo la forma canonica de
    // `X500Principal` los da por iguales. Encadenar un certificado con su emisor comparando textos
    // es exactamente el error que deja pasar un certificado ajeno.
    public javax.security.auth.x500.X500Principal getIssuerX500Principal() {
        if (this.issuerX500 == null) {
            this.issuerX500 = name(false);
        }
        return this.issuerX500;
    }

    // El sujeto como nombre X.500. Vale lo mismo que para `getIssuerX500Principal()`.
    public javax.security.auth.x500.X500Principal getSubjectX500Principal() {
        if (this.subjectX500 == null) {
            this.subjectX500 = name(true);
        }
        return this.subjectX500;
    }

    private javax.security.auth.x500.X500Principal name(boolean subject) {
        try {
            return new javax.security.auth.x500.X500Principal(
                DerReader.certificateName(this.getEncoded(), subject));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(subject ? "Could not parse subject" : "Could not parse issuer");
        } catch (IOException e) {
            throw new RuntimeException(subject ? "Could not parse subject" : "Could not parse issuer");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(subject ? "Could not parse subject" : "Could not parse issuer");
        }
    }

    // Los nombres alternativos del sujeto (extension SubjectAltName), o null si no hay.
    //
    // Devuelve null y no lanza: es un metodo **concreto** de la clase base cuyo contrato es "la
    // subclase que sepa decodificar `GeneralName` que lo sobrescriba". El JDK hace exactamente esto
    // mismo. Devolver una lista vacia estaria mal: vacio y ausente son cosas distintas —vacio
    // significaria que el certificado no sirve para ningun nombre—.
    //
    // Para un certificado de servidor **este es el metodo que importa**, no el CN del sujeto: desde
    // el RFC 6125 el nombre del host se busca aca y el CN quedo como respaldo historico.
    public java.util.Collection<List<?>> getSubjectAlternativeNames()
            throws CertificateParsingException {
        return null;
    }

    // Los nombres alternativos del emisor. Mismo contrato que el de arriba.
    public java.util.Collection<List<?>> getIssuerAlternativeNames()
            throws CertificateParsingException {
        return null;
    }

    // Desde cuando vale.
    public abstract Date getNotBefore();

    // Hasta cuando vale.
    public abstract Date getNotAfter();

    // La parte firmada del certificado: todo menos la firma misma. Es sobre estos bytes que hay que
    // verificar, y por eso el metodo existe en vez de dejar que cada quien recorte el DER.
    public abstract byte[] getTBSCertificate() throws CertificateEncodingException;

    // Los bits de la firma.
    public abstract byte[] getSignature();

    // El nombre del algoritmo de firma: "SHA256withRSA".
    public abstract String getSigAlgName();

    // El OID del algoritmo de firma. Es el dato **autoritativo**: el nombre depende de que tabla de
    // OIDs tenga la implementacion y puede ser nulo o raro para algoritmos que no conoce.
    public abstract String getSigAlgOID();

    // Los parametros del algoritmo de firma en DER, o null si no lleva. Para RSASSA-PSS no es
    // opcional: ahi es donde vive el hash y el largo de sal.
    public abstract byte[] getSigAlgParams();

    // El identificador unico del emisor (v2+), o null. Practicamente no se usa.
    public abstract boolean[] getIssuerUniqueID();

    // El identificador unico del sujeto (v2+), o null.
    public abstract boolean[] getSubjectUniqueID();

    // La extension KeyUsage como bits, o null si no esta.
    //
    // El orden de los bits es el del RFC: 0 digitalSignature, 1 nonRepudiation, 2 keyEncipherment,
    // 3 dataEncipherment, 4 keyAgreement, 5 keyCertSign, 6 cRLSign, 7 encipherOnly,
    // 8 decipherOnly. El que importa para una cadena es el 5: sin el, el certificado no puede
    // firmar otros certificados aunque BasicConstraints diga que es CA.
    public abstract boolean[] getKeyUsage();

    // La restriccion de largo de cadena de BasicConstraints, o -1 si el certificado **no es una
    // CA**.
    //
    // El valor de retorno mezcla dos cosas y hay que leerlo con cuidado: -1 significa "no es CA";
    // `Integer.MAX_VALUE` significa "es CA sin limite de largo"; cualquier otro numero es el limite.
    // Tomar el -1 por "es CA con largo cero" es exactamente al reves de lo que dice.
    public abstract int getBasicConstraints();

    // Los OIDs de ExtendedKeyUsage, o null si la extension no esta.
    //
    // La distincion entre null y lista vacia importa: null es "el certificado no restringe para que
    // sirve", vacio es "no sirve para nada". Son opuestos.
    //
    // Se decodifica de verdad porque la extension es un SEQUENCE OF OBJECT IDENTIFIER y nada mas.
    public List<String> getExtendedKeyUsage() throws CertificateParsingException {
        byte[] ext = this.getExtensionValue(OID_EXTENDED_KEY_USAGE);
        if (ext == null) {
            return null;
        }
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_SEQUENCE);
            int end = d.position() + len;
            if (end > value.length) {
                throw new IOException("DER truncado: SEQUENCE incompleto");
            }
            List<String> oids = new ArrayList<String>();
            DerReader inner = new DerReader(value, d.position(), len);
            while (inner.hasMore()) {
                int oidLen = inner.expect(DerReader.TAG_OID);
                int from = inner.skip(oidLen);
                oids.add(inner.readOid(from, oidLen));
            }
            return java.util.Collections.unmodifiableList(oids);
        } catch (IOException e) {
            throw new CertificateParsingException(e.toString(), e);
        }
    }
}
