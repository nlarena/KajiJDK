package java.security.cert;

import java.io.IOException;

// Un calificador de politica de certificado (RFC 5280): un OID que dice de que tipo es, mas el
// valor, sin interpretar.
//
// Los dos que existen en la practica son CPS (1.3.6.1.5.5.7.2.1), una URL a la declaracion de
// practicas de la CA, y UserNotice (1.3.6.1.5.5.7.2.2), un texto para mostrarle a la persona. Esta
// clase **no** los interpreta: separa el OID del resto y entrega el resto en crudo. Es lo correcto,
// porque la lista de calificadores es abierta y quien conozca uno nuevo sabra que hacer con sus
// bytes.
//
// Es la unica clase del paquete con un constructor que parsea DER, y lo hace porque su contrato es
// exactamente ese: recibe bytes y tiene que devolver el OID de adentro. Lo que se lee es un
// SEQUENCE con un OBJECT IDENTIFIER adelante y nada mas — no hay ninguna decision de confianza
// involucrada, asi que se puede hacer bien. Ver `DerReader` para donde se puso el limite.
public class PolicyQualifierInfo {

    private final byte[] mEncoded;
    private final String mId;
    private final byte[] mData;

    // Lee el calificador de su codificacion DER.
    //
    // Se exige que los bytes sean **exactamente** un PolicyQualifierInfo: sobrar datos al final es
    // un error y no algo que se ignore. Aceptar cola de mas dejaria pasar dos codificaciones para
    // el mismo valor, que es justo lo que DER existe para impedir.
    public PolicyQualifierInfo(byte[] encoded) throws IOException {
        if (encoded.length < 3) {
            throw new IOException("Too short");
        }
        byte[] copyOf = new byte[encoded.length];
        System.arraycopy(encoded, 0, copyOf, 0, encoded.length);
        this.mEncoded = copyOf;

        DerReader d = new DerReader(copyOf, 0, copyOf.length);
        int tag = d.readTag();
        if (tag != DerReader.TAG_SEQUENCE) {
            throw new IOException("Invalid encoding for PolicyQualifierInfo");
        }
        int seqLen = d.readLength();
        int inicioSec = d.position();
        if (inicioSec + seqLen != copyOf.length) {
            throw new IOException("extra data at the end");
        }
        int oidLen = d.expect(DerReader.TAG_OID);
        int oidAt = d.skip(oidLen);
        this.mId = d.readOid(oidAt, oidLen);
        // Lo que queda del SEQUENCE es el calificador, tal cual vino. Puede ser de largo cero: un
        // PolicyQualifierInfo sin calificador es legal y devuelve un arreglo vacio, no null.
        int restAt = d.position();
        this.mData = d.copy(restAt, inicioSec + seqLen - restAt);
    }

    // El OID del tipo de calificador, en notacion de puntos.
    public final String getPolicyQualifierId() {
        return this.mId;
    }

    // Copia de la codificacion completa que se recibio.
    public final byte[] getEncoded() {
        byte[] c = new byte[this.mEncoded.length];
        System.arraycopy(this.mEncoded, 0, c, 0, this.mEncoded.length);
        return c;
    }

    // Copia del valor del calificador en DER, sin interpretar. Nunca null: vacio si no hay.
    public final byte[] getPolicyQualifier() {
        byte[] c = new byte[this.mData.length];
        System.arraycopy(this.mData, 0, c, 0, this.mData.length);
        return c;
    }

    // A KajiLibrary subset: el JDK imprime el calificador con su volcado hexadecimal interno, con
    // offsets y columna ASCII. Ese formato no esta especificado en ningun lado y no vale la pena
    // reproducirlo byte a byte; aca se imprime el hexadecimal plano. La estructura de las lineas y
    // los nombres de los campos si son los mismos.
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PolicyQualifierInfo: [\n");
        sb.append("  qualifierID: " + this.mId + "\n");
        sb.append("  qualifier: " + hex(this.mData) + "\n");
        sb.append("]");
        return sb.toString();
    }

    private static String hex(byte[] b) {
        String d = "0123456789ABCDEF";
        StringBuilder s = new StringBuilder();
        int i = 0;
        while (i < b.length) {
            int v = b[i] & 0xff;
            s.append(d.charAt(v >> 4));
            s.append(d.charAt(v & 15));
            i = i + 1;
        }
        return s.toString();
    }
}
