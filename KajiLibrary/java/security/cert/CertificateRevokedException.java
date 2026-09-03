package java.security.cert;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// El certificado esta revocado, y ademas: cuando, por que, quien lo dijo y con que extensiones.
//
// ===============================================================================================
// POR QUE ES UNA EXCEPCION CON DATOS Y NO UN MENSAJE
// ===============================================================================================
//
// El resto de las excepciones de este paquete dicen que fallo. Esta dice **que averiguo la
// validacion**, y eso tiene consecuencias practicas: quien la atrapa puede querer distinguir un
// certificado revocado por clave comprometida —que invalida todo lo que esa clave firmo alguna vez—
// de uno revocado porque el titular cambio de trabajo, que no invalida nada de lo anterior. Un
// mensaje de texto no sirve para eso; los campos si.
//
// La fecha de invalidez es el dato que mas se olvida y el que mas cambia la respuesta. `getRevocationDate()`
// dice cuando la CA **publico** la revocacion; `getInvalidityDate()` dice desde cuando se cree que
// la clave estaba comprometida, que puede ser mucho antes. Una firma hecha entre las dos fechas es
// sospechosa aunque en su momento la CRL no dijera nada.
//
// El objeto es **inmutable hacia afuera**: la fecha se copia al entrar y al salir, y el mapa de
// extensiones se copia al entrar y se devuelve inmutable. Sin eso, quien la atrapa podria cambiarle
// los datos a la excepcion antes de que la mire el que la relanza.
public class CertificateRevokedException extends CertificateException {

    private static final long serialVersionUID = 7839996631571608627L;

    private static final String OID_INVALIDITY_DATE = "2.5.29.24";

    private Date revocationDate;
    private final CRLReason reason;
    private final javax.security.auth.x500.X500Principal authority;
    private transient Map<String, Extension> extensions;

    // Los cuatro argumentos son obligatorios. No hay ninguno con un default razonable: una
    // revocacion sin fecha, sin motivo o sin autoridad no se puede evaluar, y un mapa nulo se
    // confundiria con "sin extensiones", que es distinto de "no se".
    public CertificateRevokedException(Date revocationDate, CRLReason reason,
            javax.security.auth.x500.X500Principal authority, Map<String, Extension> extensions) {
        if (revocationDate == null || reason == null || authority == null || extensions == null) {
            throw new NullPointerException();
        }
        this.revocationDate = new Date(revocationDate.getTime());
        this.reason = reason;
        this.authority = authority;
        this.extensions = new HashMap<String, Extension>(extensions);
    }

    // Cuando la CA publico la revocacion. Copia.
    public Date getRevocationDate() {
        return new Date(this.revocationDate.getTime());
    }

    public CRLReason getRevocationReason() {
        return this.reason;
    }

    // Quien la publico. `X500Principal` es inmutable, asi que se devuelve la misma instancia.
    public javax.security.auth.x500.X500Principal getAuthorityName() {
        return this.authority;
    }

    // Desde cuando se cree que el certificado dejo de ser confiable, o null si no se dijo.
    //
    // Sale de la extension InvalidityDate, que es un GeneralizedTime pelado. Si la extension no esta
    // —o si esta y no se puede leer— se devuelve null: es "no se dijo", que es lo unico honesto que
    // se puede afirmar. Es el JDK el que decide devolver null en vez de lanzar, y tiene sentido:
    // un dato accesorio ilegible no deberia tapar el hecho principal, que es que esta revocado.
    public Date getInvalidityDate() {
        Extension ext = this.getExtensions().get(OID_INVALIDITY_DATE);
        if (ext == null) {
            return null;
        }
        try {
            byte[] value = ext.getValue();
            if (value == null) {
                return null;
            }
            DerReader d = new DerReader(value, 0, value.length);
            // 0x18 es GeneralizedTime. El valor de la extension viene **sin** el OCTET STRING de
            // afuera: eso es lo que promete `Extension.getValue()`.
            int len = d.expect(0x18);
            int from = d.skip(len);
            return new Date(DerReader.generalizedTime(value, from, len));
        } catch (IOException e) {
            return null;
        }
    }

    // Las extensiones de la entrada de CRL, indexadas por OID. Inmutable.
    public Map<String, Extension> getExtensions() {
        return Collections.unmodifiableMap(this.extensions);
    }

    @Override
    public String getMessage() {
        return "Certificate has been revoked, reason: "
            + this.reason + ", revocation date: " + this.revocationDate
            + ", authority: " + this.authority + ", extension OIDs: "
            + this.extensions.keySet();
    }
}
