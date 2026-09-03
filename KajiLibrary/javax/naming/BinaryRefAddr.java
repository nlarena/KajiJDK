package javax.naming;

/**
 * Una direccion que son bytes opacos: un identificador de objeto de CORBA, un handle binario.
 *
 * <h2>Por que copia el arreglo y por que redefine las tres</h2>
 *
 * <p>El constructor **copia**. Un `byte[]` es mutable y la direccion se guarda dentro de una
 * `Reference` que puede quedar atada por tiempo indefinido: quedarse con el arreglo del que llama
 * dejaria que le cambien la direccion por debajo. Es la misma razon por la que `NamingException`
 * clona los nombres.
 *
 * <p>Y redefine `equals`/`hashCode`/`toString` porque los de `RefAddr` comparan el contenido con
 * `equals`, y el `equals` de un arreglo es identidad. Sin esto, dos direcciones con los mismos
 * bytes serian distintas, que es justo lo contrario de lo que dice el contrato de `RefAddr`.
 *
 * <p>El `toString` corta a los primeros 32 bytes: es un volcado de diagnostico, y una direccion
 * binaria puede ser de kilobytes.
 */
public class BinaryRefAddr extends RefAddr {

    private static final long serialVersionUID = -3415254970957330361L;

    private byte[] buf;

    public BinaryRefAddr(String addrType, byte[] src) {
        this(addrType, src, 0, src.length);
    }

    /** Copia `count` bytes desde `offset`: el arreglo del que llama no queda referenciado. */
    public BinaryRefAddr(String addrType, byte[] src, int offset, int count) {
        super(addrType);
        buf = new byte[count];
        System.arraycopy(src, offset, buf, 0, count);
    }

    @Override
    public Object getContent() {
        return buf;
    }

    /** Byte a byte: el `equals` heredado usaria la identidad del arreglo. */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BinaryRefAddr) {
            BinaryRefAddr target = (BinaryRefAddr) obj;
            if (addrType.compareTo(target.addrType) != 0) {
                return false;
            }
            if (buf == target.buf) {
                return true;
            }
            if (buf.length != target.buf.length) {
                return false;
            }
            for (int i = 0; i < buf.length; i++) {
                if (buf[i] != target.buf[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = addrType.hashCode();
        for (int i = 0; i < buf.length; i++) {
            hash += buf[i];
        }
        return hash;
    }

    /** Corta a 32 bytes: esto es diagnostico, no un volcado completo. */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Address Type: ");
        str.append(addrType).append("\n");
        str.append("AddressContents: ");
        for (int i = 0; i < buf.length && i < 32; i++) {
            str.append(buf[i]).append(" ");
        }
        if (buf.length >= 32) {
            str.append(" ...\n");
        }
        return str.toString();
    }
}
