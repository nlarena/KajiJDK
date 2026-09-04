package java.security;

import java.io.Serializable;
import java.security.cert.CertPath;

// Quien firmo: su cadena de certificados y, si la hubo, la marca de tiempo de la firma.
//
// La marca es opcional y esa opcionalidad importa. Sin ella la firma solo vale mientras el
// certificado del firmante siga vigente; con ella sigue valiendo despues, porque se puede
// comprobar que en el momento de firmar el certificado estaba bien. Por eso `getTimestamp()`
// devolviendo null no es un detalle: es una firma con fecha de vencimiento.
//
// Igual que `Timestamp`, esta clase guarda y compara; no verifica nada.
public final class CodeSigner implements Serializable {

    private final CertPath signerCertPath;
    private final Timestamp timestamp;

    public CodeSigner(CertPath signerCertPath, Timestamp timestamp) {
        if (signerCertPath == null) {
            throw new NullPointerException();
        }
        this.signerCertPath = signerCertPath;
        this.timestamp = timestamp;
    }

    public CertPath getSignerCertPath() {
        return this.signerCertPath;
    }

    // La marca de tiempo, o null si la firma no fue sellada.
    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    @Override
    public int hashCode() {
        if (this.timestamp == null) {
            return this.signerCertPath.hashCode();
        }
        return this.signerCertPath.hashCode() ^ this.timestamp.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CodeSigner)) {
            return false;
        }
        CodeSigner that = (CodeSigner) obj;
        if (!this.signerCertPath.equals(that.signerCertPath)) {
            return false;
        }
        // Firmado sin sellar y firmado con sello son cosas distintas, aunque el firmante sea el
        // mismo: solo una de las dos sobrevive al vencimiento del certificado.
        if (this.timestamp == null) {
            return that.timestamp == null;
        }
        return this.timestamp.equals(that.timestamp);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(");
        java.util.List<? extends java.security.cert.Certificate> cs =
            this.signerCertPath.getCertificates();
        b.append("Signer: ");
        if (!cs.isEmpty()) {
            java.security.cert.Certificate c0 = cs.get(0);
            b.append(c0.toString());
        } else {
            b.append("<empty>");
        }
        if (this.timestamp != null) {
            b.append("timestamp: ");
            b.append(this.timestamp.toString());
        }
        b.append(")");
        return b.toString();
    }
}
