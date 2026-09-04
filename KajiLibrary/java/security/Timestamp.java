package java.security;

import java.io.Serializable;
import java.security.cert.CertPath;
import java.util.Date;

// Una marca de tiempo firmada: cuando se firmo algo, segun quien lo atestigua.
//
// Existe por un problema concreto: los certificados vencen y se revocan, y sin marca de tiempo una
// firma hecha cuando el certificado era valido se vuelve indistinguible de una hecha despues. Con
// la marca, la pregunta pasa a ser "¿el certificado era valido **en ese momento**?", que si se
// puede contestar años despues.
//
// Que la fecha venga acompañada de un `CertPath` es lo que la hace util: una fecha sola la escribe
// cualquiera. El camino es el de la autoridad de sellado de tiempo que la firmo. Esta clase
// **guarda** los dos datos y no verifica ninguno — verificar la firma del sellado requiere
// criptografia que esta biblioteca no tiene.
public final class Timestamp implements Serializable {

    private final Date timestamp;
    private final CertPath signerCertPath;

    public Timestamp(Date timestamp, CertPath signerCertPath) {
        if (timestamp == null || signerCertPath == null) {
            throw new NullPointerException();
        }
        // Se copia: `Date` es mutable, y una marca de tiempo que el llamador pueda mover despues
        // de construirla no sirve para nada.
        this.timestamp = new Date(timestamp.getTime());
        this.signerCertPath = signerCertPath;
    }

    public Date getTimestamp() {
        return new Date(this.timestamp.getTime());
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    @Override
    public int hashCode() {
        return this.timestamp.hashCode() * 31 + this.signerCertPath.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Timestamp)) {
            return false;
        }
        Timestamp that = (Timestamp) obj;
        return this.timestamp.equals(that.timestamp)
            && this.signerCertPath.equals(that.signerCertPath);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(");
        b.append("timestamp: ");
        b.append(this.timestamp);
        java.util.List<? extends java.security.cert.Certificate> cs =
            this.signerCertPath.getCertificates();
        if (!cs.isEmpty()) {
            b.append("TSA: ");
            java.security.cert.Certificate c0 = cs.get(0);
            b.append(c0.toString());
        } else {
            b.append("TSA: <empty>");
        }
        b.append(")");
        return b.toString();
    }
}
