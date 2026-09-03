package java.security.cert;

import java.security.PublicKey;

// Un ancla de confianza: donde termina una cadena y empieza la fe.
//
// Es el objeto mas importante de toda la validacion, y conviene decir por que con precision. Una
// cadena de certificados no se valida "sola": cada certificado se verifica con la clave del
// siguiente, y esa recursion tiene que parar en algo que se acepte **sin verificar**. Eso es el
// ancla. Toda la seguridad de PKIX se apoya en que el conjunto de anclas sea el correcto; una CA
// de mas en esa lista puede firmar un certificado para cualquier nombre y la validacion va a decir
// que si.
//
// Un ancla no necesita ser un certificado. Alcanza con el nombre de la CA y su clave publica, y de
// hecho es lo mas honesto: lo unico que hace falta para cerrar la cadena es la clave. Que el JDK
// deje pasar un `X509Certificate` es una comodidad —se saca de ahi el nombre y la clave— y no
// significa que ese certificado se valide: **el ancla nunca se verifica**, ni se le mira la fecha.
//
// Los tres constructores dicen la misma cosa de tres formas, y el orden en que se les mira los
// argumentos importa: el que toma un `String` **parsea** ese nombre y lo rechaza si esta mal, asi
// que no queda un ancla con un nombre que nadie valido.
//
// Un detalle del JDK que se reproduce a proposito y que sorprende: `getCAName()` no siempre devuelve
// lo mismo que `getCA().getName()`. Cuando el ancla se creo desde un `String`, se guarda **ese
// texto tal cual** —espacios incluidos—; cuando se creo desde un `X500Principal`, se guarda su
// `getName()`, que es RFC 2253 sin espacios. Comparar anclas por `getCAName()` es entonces un error;
// hay que comparar por `getCA()`, que si tiene forma canonica.
//
// Diferencia de comportamiento anotada a proposito: el JDK **valida** el DER de `nameConstraints` y
// tira `IllegalArgumentException` si esta mal formado. Aca no hay parser de `NameConstraints`, asi
// que los bytes se guardan sin mirar. No es una decision de confianza —esta clase no aplica las
// restricciones, solo las transporta— pero es una diferencia observable y queda escrita.
public class TrustAnchor {

    private final X509Certificate trustedCert;
    private final PublicKey pubKey;
    private final byte[] ncBytes;
    private final javax.security.auth.x500.X500Principal caPrincipal;
    private final String caName;

    // Un ancla a partir del certificado autofirmado de la CA.
    //
    // `nameConstraints` puede ser null. Cuando no lo es, son las restricciones de nombres que se le
    // imponen **a esta ancla desde afuera**, sin que figuren en su certificado: sirve para acotar
    // una CA a un subdominio aunque ella no se haya acotado sola.
    public TrustAnchor(X509Certificate trustedCert, byte[] nameConstraints) {
        if (trustedCert == null) {
            throw new NullPointerException("the trustedCert parameter must be non-null");
        }
        this.trustedCert = trustedCert;
        this.pubKey = null;
        // No se derivan del certificado, y es del JDK: un ancla creada asi devuelve null en los dos
        // accesores. Quien quiera el nombre lo saca de `getTrustedCert().getSubjectX500Principal()`,
        // que es el sujeto —el ancla es la CA, no el emisor de su propio certificado—.
        this.caPrincipal = null;
        this.caName = null;
        this.ncBytes = copy(nameConstraints);
    }

    // Un ancla a partir del nombre de la CA y su clave publica, sin certificado.
    //
    // Es la forma mas honesta de las tres: lo unico que hace falta para cerrar una cadena es saber
    // a quien creerle y con que clave. El certificado de la CA no aporta nada mas —no se verifica ni
    // se le mira la fecha—.
    public TrustAnchor(javax.security.auth.x500.X500Principal caPrincipal, PublicKey pubKey,
            byte[] nameConstraints) {
        if (caPrincipal == null || pubKey == null) {
            throw new NullPointerException();
        }
        this.trustedCert = null;
        this.caPrincipal = caPrincipal;
        this.caName = caPrincipal.getName();
        this.pubKey = pubKey;
        this.ncBytes = copy(nameConstraints);
    }

    // Idem, con el nombre de la CA en RFC 2253.
    //
    // El nombre se parsea aca mismo y se guardan las dos formas. El orden de los chequeos es el del
    // JDK y se nota: con los dos argumentos en null, el que se queja es `pubKey`, no `caName`.
    public TrustAnchor(String caName, PublicKey pubKey, byte[] nameConstraints) {
        if (pubKey == null) {
            throw new NullPointerException("the pubKey parameter must be non-null");
        }
        if (caName == null) {
            throw new NullPointerException("the caName parameter must be non-null");
        }
        if (caName.length() == 0) {
            throw new IllegalArgumentException("the caName "
                + "parameter must be a non-empty String");
        }
        // Un nombre mal formado tira `IllegalArgumentException` desde aca: es el constructor de
        // `X500Principal` el que lo rechaza, y dejarlo pasar seria el ancla sin validar.
        this.caPrincipal = new javax.security.auth.x500.X500Principal(caName);
        this.pubKey = pubKey;
        // Se guarda el texto **original**, no el canonico. Ver el comentario de la clase.
        this.caName = caName;
        this.trustedCert = null;
        this.ncBytes = copy(nameConstraints);
    }

    private static byte[] copy(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    // El certificado de la CA de confianza, o null si el ancla se creo desde un nombre y una clave.
    // `final` en todos los accesores: una subclase que cambiara lo que devuelven correria el punto
    // en el que la cadena deja de verificarse.
    public final X509Certificate getTrustedCert() {
        return this.trustedCert;
    }

    // La clave publica de la CA, o null si el ancla se creo desde un certificado —ahi la clave sale
    // del certificado, no de aca—.
    public final PublicKey getCAPublicKey() {
        return this.pubKey;
    }

    // El nombre de la CA, o null si el ancla se creo desde un certificado.
    public final javax.security.auth.x500.X500Principal getCA() {
        return this.caPrincipal;
    }

    // El nombre de la CA como texto, o null si el ancla se creo desde un certificado.
    //
    // **No** es necesariamente `getCA().getName()`: ver el comentario de la clase. Para comparar
    // anclas hay que usar `getCA()`.
    public final String getCAName() {
        return this.caName;
    }

    // Copia de las restricciones de nombres en DER, o null si no hay.
    public final byte[] getNameConstraints() {
        return copy(this.ncBytes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        if (this.pubKey != null) {
            sb.append("  Trusted CA Public Key: " + this.pubKey.toString() + "\n");
            sb.append("  Trusted CA Issuer Name: " + String.valueOf(this.caName) + "\n");
        } else {
            sb.append("  Trusted CA cert: " + this.trustedCert.toString() + "\n");
        }
        if (this.ncBytes != null) {
            sb.append("  Name Constraints: " + this.ncBytes.length + " bytes\n");
        }
        return sb.toString();
    }
}
