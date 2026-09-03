package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

// Una entrada de una CRL X.509: un certificado revocado, cuando, y —si lo dice— por que.
//
// La identidad es el **numero de serie**, no el certificado: la CRL no incluye los certificados que
// revoca, solo sus series. Por eso hace falta saber ademas quien es el emisor, y por eso
// `getCertificateIssuer()` existe: en una CRL indirecta —una que revoca certificados de varias
// CAs— cada entrada puede tener un emisor distinto del de la CRL.
//
public abstract class X509CRLEntry implements X509Extension {

    private static final String OID_CRL_REASON = "2.5.29.21";

    public X509CRLEntry() {
    }

    // El emisor del certificado que esta entrada revoca, o null si es el mismo que el de la CRL.
    //
    // Devuelve null y no lanza: es un metodo **concreto** de la clase base cuyo contrato es "la
    // subclase que sepa decodificar la extension `certificateIssuer` que lo sobrescriba", y el JDK
    // hace exactamente esto mismo. Null no significa "no se": significa "el de la CRL", que es el
    // caso de casi todas. Solo una CRL indirecta —una que revoca certificados de varias CAs— lleva
    // ese campo, y ahi decodificarlo pide `GeneralName`, que este paquete no tiene.
    public javax.security.auth.x500.X500Principal getCertificateIssuer() {
        return null;
    }

    // Dos entradas son la misma si codifican los mismos bytes, igual que en `Certificate`. Comparar
    // por numero de serie no alcanzaria: la misma serie de dos emisores distintos son dos
    // certificados distintos.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof X509CRLEntry)) {
            return false;
        }
        try {
            byte[] a = this.getEncoded();
            byte[] b = ((X509CRLEntry) other).getEncoded();
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

    // La entrada codificada en DER.
    public abstract byte[] getEncoded() throws CRLException;

    // El numero de serie del certificado revocado.
    public abstract BigInteger getSerialNumber();

    // Cuando se revoco. Con `KEY_COMPROMISE` esta fecha es la que decide si una firma vieja sigue
    // valiendo, asi que no es informativa.
    public abstract Date getRevocationDate();

    public abstract boolean hasExtensions();

    @Override
    public abstract String toString();

    // La razon de la revocacion, o null si la entrada no la dice.
    //
    // Se decodifica de verdad: la extension es un ENUMERATED y nada mas. Dos comportamientos que
    // parecen inconsistentes y son los del JDK, replicados a proposito:
    //
    //   - un codigo fuera de la lista conocida da `UNSPECIFIED`, no una excepcion. Es lo correcto:
    //     una razon que no se entiende no cambia el hecho de que **esta revocado**, y fallar ahi
    //     convertiria una CRL nueva en una CRL ilegible.
    //   - una extension mal formada da null, tambien sin lanzar. La consecuencia es que perder la
    //     razon nunca hace que se pierda la revocacion, que es el lado seguro del error.
    public CRLReason getRevocationReason() {
        if (!this.hasExtensions()) {
            return null;
        }
        byte[] ext = this.getExtensionValue(OID_CRL_REASON);
        if (ext == null) {
            return null;
        }
        try {
            byte[] value = DerReader.unwrapOctetString(ext);
            DerReader d = new DerReader(value, 0, value.length);
            int len = d.expect(DerReader.TAG_ENUMERATED);
            if (len < 1 || len > 4) {
                return null;
            }
            int from = d.skip(len);
            int codigo = 0;
            int i = 0;
            while (i < len) {
                codigo = (codigo << 8) | (value[from + i] & 0xff);
                i = i + 1;
            }
            CRLReason[] todas = CRLReason.values();
            if (codigo < 0 || codigo >= todas.length) {
                return CRLReason.UNSPECIFIED;
            }
            return todas[codigo];
        } catch (IOException e) {
            return null;
        }
    }
}
