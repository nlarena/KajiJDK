package java.security.cert;

import java.math.BigInteger;
import java.security.DEREncodable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Date;
import java.util.Set;

// Una lista de revocacion X.509 (RFC 5280): que certificados de este emisor dejaron de valer.
//
// Una CRL es **un objeto firmado**, igual que un certificado, y por eso tiene `verify`. Eso no es
// un detalle: una CRL sin verificar es una lista que cualquiera pudo escribir, y aceptarla tiene el
// efecto opuesto al que se busca —un atacante que puede inyectar CRLs falsas puede revocar
// certificados legitimos, o entregar una CRL vieja que todavia no lista el certificado que le
// robaron—. El contrato de `verify` es el mismo que en `Certificate`: **no devuelve nada, lanza si
// falla**.
//
// Las dos fechas son el otro punto sensible. `thisUpdate` dice de cuando es la foto y `nextUpdate`
// cuando se promete la siguiente; una CRL cuyo `nextUpdate` ya paso es una CRL vencida y usarla es
// como no chequear nada. Que `getNextUpdate()` pueda devolver null —el campo es opcional— hace que
// ese chequeo sea facil de olvidar.
//
// `getRevokedCertificate(X509Certificate)` es el que hay que usar, y no el que toma solo la serie:
// **compara los emisores primero**. Dos CAs distintas pueden emitir la misma serie, asi que buscar
// una serie en la CRL equivocada es mas rapido y esta mal. El que toma un `BigInteger` sigue
// existiendo porque el JDK lo tiene, pero ahi el llamador es quien se hace cargo de haber
// comprobado el emisor.
public abstract class X509CRL extends CRL implements X509Extension, DEREncodable {

    // Se recuerda por lo mismo que en `X509Certificate`: parsear en cada llamada seria caro y el
    // JDK devuelve la misma instancia dos veces seguidas.
    private javax.security.auth.x500.X500Principal issuerX500;

    protected X509CRL() {
        super("X.509");
    }

    // El emisor de la CRL como nombre X.500.
    //
    // Se lee del DER —`TBSCertList.issuer`— y no de `getIssuerDN()`, por lo mismo que en
    // `X509Certificate`: pasar por el texto de un `Principal` cualquiera y re-parsearlo es la
    // confusion de nombres que este metodo existe para evitar. `RuntimeException` si el DER no se
    // puede leer, igual que el JDK.
    public javax.security.auth.x500.X500Principal getIssuerX500Principal() {
        if (this.issuerX500 == null) {
            try {
                this.issuerX500 = new javax.security.auth.x500.X500Principal(
                    DerReader.crlName(this.getEncoded()));
            } catch (CRLException e) {
                throw new RuntimeException("Could not parse issuer");
            } catch (java.io.IOException e) {
                throw new RuntimeException("Could not parse issuer");
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Could not parse issuer");
            }
        }
        return this.issuerX500;
    }

    // La entrada de este certificado, o null si esta CRL no lo revoca.
    //
    // El chequeo de emisor va **antes** de mirar la serie, y si no coincide se devuelve null sin
    // llegar a buscar: no es una optimizacion, es que la respuesta correcta ahi es "esta CRL no
    // habla de este certificado". Una CRL indirecta —que revoca certificados de varias CAs— no se
    // contempla, igual que en el JDK: para eso habria que mirar el `certificateIssuer` de cada
    // entrada, y ese campo es una extension que esta clase no decodifica.
    public X509CRLEntry getRevokedCertificate(X509Certificate certificate) {
        javax.security.auth.x500.X500Principal delCert = certificate.getIssuerX500Principal();
        if (!delCert.equals(this.getIssuerX500Principal())) {
            return null;
        }
        return this.getRevokedCertificate(certificate.getSerialNumber());
    }

    // Igualdad por codificacion, igual que en `Certificate`.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof X509CRL)) {
            return false;
        }
        try {
            byte[] a = this.getEncoded();
            byte[] b = ((X509CRL) other).getEncoded();
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
        } catch (CRLException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        try {
            byte[] a = this.getEncoded();
            int i = 0;
            while (i < a.length) {
                h = h * 31 + a[i];
                i = i + 1;
            }
        } catch (CRLException e) {
            return 0;
        }
        return h;
    }

    public abstract byte[] getEncoded() throws CRLException;

    // Verifica la firma de la CRL. Si no lanza, la firma vale.
    public abstract void verify(PublicKey key)
        throws CRLException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    public abstract void verify(PublicKey key, String sigProvider)
        throws CRLException, NoSuchAlgorithmException, InvalidKeyException,
               NoSuchProviderException, SignatureException;

    // Igual que en `Certificate`: la variante con `Provider` llego despues y su implementacion base
    // lanza para no obligar a las subclases que ya existian. Inventar aca una verificacion seria el
    // agujero.
    public void verify(PublicKey key, Provider sigProvider)
            throws CRLException, NoSuchAlgorithmException, InvalidKeyException,
                   SignatureException {
        throw new UnsupportedOperationException();
    }

    // La version: 1 o 2. Solo las v2 tienen extensiones, y por lo tanto solo ellas pueden ser
    // delta o indirectas.
    public abstract int getVersion();

    // Quien firmo la CRL. Ver la nota de la clase sobre por que no esta la variante moderna.
    public abstract Principal getIssuerDN();

    // De cuando es esta foto.
    public abstract Date getThisUpdate();

    // Cuando se promete la proxima, o null si no lo dice. Si ya paso, la CRL esta vencida.
    public abstract Date getNextUpdate();

    // La entrada de esa serie, o null si no esta revocada. El llamador tiene que haber comprobado
    // que el emisor de la CRL sea el del certificado: esta clase no puede hacerlo por el.
    public abstract X509CRLEntry getRevokedCertificate(BigInteger serialNumber);

    // Todas las entradas, o null si la CRL esta vacia. **Null y no un conjunto vacio**: es lo que
    // hace el JDK y confundirlos es un NPE esperando.
    public abstract Set<? extends X509CRLEntry> getRevokedCertificates();

    // La parte firmada de la CRL.
    public abstract byte[] getTBSCertList() throws CRLException;

    public abstract byte[] getSignature();

    public abstract String getSigAlgName();

    public abstract String getSigAlgOID();

    public abstract byte[] getSigAlgParams();
}
