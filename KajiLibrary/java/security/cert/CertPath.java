package java.security.cert;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

// Una cadena de certificados: del que interesa hacia arriba, en orden.
//
// El orden es parte del tipo y no una convencion: el primero es el certificado del sujeto, cada
// uno esta firmado por el siguiente, y el ultimo suele ser el que se compara contra un ancla de
// confianza. Una lista desordenada seria un conjunto de certificados, no un camino, y validar un
// camino es exactamente seguir esa cadena.
//
// Igual que `Certificate`, la igualdad es por contenido —mismo tipo y misma lista— y todo lo que
// requiere criptografia es abstracto. Esta clase no valida nada; validar es trabajo de
// `CertPathValidator`, que no existe en esta biblioteca.
public abstract class CertPath implements Serializable {

    private final String type;

    protected CertPath(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    // Los nombres de las codificaciones soportadas, con la preferida primero.
    public abstract Iterator<String> getEncodings();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CertPath)) {
            return false;
        }
        CertPath o = (CertPath) other;
        if (!this.type.equals(o.getType())) {
            return false;
        }
        return this.getCertificates().equals(o.getCertificates());
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() * 31 + this.getCertificates().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(this.type);
        b.append(" Cert Path: length = ");
        List<? extends Certificate> cs = this.getCertificates();
        b.append(cs.size());
        b.append(".\n[\n");
        int i = 0;
        while (i < cs.size()) {
            Certificate c = cs.get(i);
            b.append("=========================================================Certificate ");
            b.append(i + 1);
            b.append("start.\n");
            b.append(c.toString());
            b.append("\n=========================================================Certificate ");
            b.append(i + 1);
            b.append("end.\n\n\n");
            i = i + 1;
        }
        b.append("\n]");
        return b.toString();
    }

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract byte[] getEncoded(String encoding) throws CertificateEncodingException;

    // Los certificados, del sujeto hacia la raiz. Inmutable.
    public abstract List<? extends Certificate> getCertificates();

    // A KajiLibrary subset, por el mismo motivo que en `Certificate`: sin `CertificateFactory` no
    // hay forma de reconstruir el camino al deserializar.
    protected Object writeReplace() throws ObjectStreamException {
        throw new java.io.NotSerializableException(
            "java.security.cert.CertPath: no CertificateFactory available to restore it");
    }
}
