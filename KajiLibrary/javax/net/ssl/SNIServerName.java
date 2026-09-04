package javax.net.ssl;

import java.util.Arrays;

/**
 * Un nombre de servidor de la extension SNI: su tipo y sus bytes.
 *
 * <h2>Que problema resuelve SNI</h2>
 *
 * <p>Uno de orden. En una misma direccion IP puede haber muchos sitios, cada uno con su
 * certificado, y el servidor tiene que elegir cual mandar <strong>antes</strong> de que el cliente
 * haya dicho nada de la aplicacion — el certificado viaja al principio del handshake, mucho antes
 * que cualquier encabezado {@code Host:}. SNI es el cliente diciendo a que sitio viene, en el
 * primer mensaje.
 *
 * <p>El tipo es un entero del registro de IANA y hoy solo hay uno,
 * {@link StandardConstants#SNI_HOST_NAME}. La clase es abstracta igual, para que agregar otro no
 * rompa nada.
 */
public abstract class SNIServerName {

    private final int type;
    private final byte[] encoded;

    /**
     * @throws IllegalArgumentException si el tipo no entra en un byte sin signo
     */
    protected SNIServerName(int type, byte[] encoded) {
        if (type < 0 || type > 255) {
            throw new IllegalArgumentException("tipo fuera de rango: " + String.valueOf(type));
        }
        if (encoded == null) {
            throw new NullPointerException("encoded");
        }
        this.type = type;
        this.encoded = encoded.clone();
    }

    /** El tipo de nombre. */
    public final int getType() {
        return this.type;
    }

    /** Una copia de los bytes; el arreglo interno no se presta. */
    public final byte[] getEncoded() {
        return this.encoded.clone();
    }

    /**
     * Sobre el tipo y los bytes.
     *
     * <p>Es {@code final} en la practica aunque no lleve la palabra: dos nombres con el mismo tipo y
     * los mismos bytes <strong>son</strong> el mismo nombre, y una subclase que lo redefiniera
     * romperia la busqueda por igualdad que hace el servidor.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SNIServerName) {
            SNIServerName o = (SNIServerName) other;
            return this.type == o.type && Arrays.equals(this.encoded, o.encoded);
        }
        return false;
    }

    public int hashCode() {
        return 31 * (17 + this.type) + Arrays.hashCode(this.encoded);
    }

    /** El tipo y los bytes en hexadecimal. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type == StandardConstants.SNI_HOST_NAME) {
            sb.append("host_name: ");
        } else {
            sb.append("type=(").append(String.valueOf(this.type)).append("): ");
        }
        for (int i = 0; i < this.encoded.length; i++) {
            int b = this.encoded[i] & 0xFF;
            if (b < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }
}
