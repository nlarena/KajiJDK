package javax.naming.ldap;

import java.io.IOException;

/**
 * Lo que el servidor contesta a un {@link PagedResultsControl}: cuantas entradas hay y por donde iba.
 *
 * <p>{@link #getCookie} es lo que hay que devolver para pedir la pagina siguiente. Una cookie
 * <strong>vacia</strong> significa que no hay mas — es el fin del recorrido, y no un error.
 *
 * <p>{@link #getResultSize} suele venir en cero: el total estimado es opcional en el RFC 2696 y
 * calcularlo puede costarle al servidor tanto como la busqueda entera. No conviene apoyarse en el.
 */
public final class PagedResultsResponseControl extends BasicControl {

    private static final long serialVersionUID = -8819778744844514666L;

    /** El OID de este control. */
    public static final String OID = "1.2.840.113556.1.4.319";

    private final int resultSize;
    private final byte[] cookie;

    /**
     * Lo construye el proveedor a partir de lo que llego.
     *
     * @throws IOException si el valor no se pudo decodificar
     */
    public PagedResultsResponseControl(String id, boolean criticality, byte[] value)
            throws IOException {
        super(id == null ? OID : id, criticality, value);
        int[] pos = new int[] { 0 };
        byte[] datos = value == null ? new byte[0] : value;
        if (datos.length == 0) {
            this.resultSize = 0;
            this.cookie = new byte[0];
            return;
        }
        // SEQUENCE { INTEGER size, OCTET STRING cookie } -- la misma forma que arma
        // `PagedResultsControl`, leida al reves.
        esperar(datos, pos, 0x30);
        largo(datos, pos);
        esperar(datos, pos, 0x02);
        int nSize = largo(datos, pos);
        int size = 0;
        for (int i = 0; i < nSize; i++) {
            size = (size << 8) | (datos[pos[0]++] & 0xFF);
        }
        this.resultSize = size;
        esperar(datos, pos, 0x04);
        int nCookie = largo(datos, pos);
        byte[] c = new byte[nCookie];
        System.arraycopy(datos, pos[0], c, 0, nCookie);
        this.cookie = c;
    }

    private static void esperar(byte[] b, int[] pos, int etiqueta) throws IOException {
        if (pos[0] >= b.length || (b[pos[0]] & 0xFF) != etiqueta) {
            throw new IOException("el control paginado no tiene la forma esperada");
        }
        pos[0]++;
    }

    /** Un largo BER; solo la forma corta y la larga de un byte, que es lo que este control usa. */
    private static int largo(byte[] b, int[] pos) throws IOException {
        if (pos[0] >= b.length) {
            throw new IOException("el control paginado esta truncado");
        }
        int n = b[pos[0]++] & 0xFF;
        if (n < 0x80) {
            return n;
        }
        int bytes = n & 0x7F;
        int out = 0;
        for (int i = 0; i < bytes; i++) {
            out = (out << 8) | (b[pos[0]++] & 0xFF);
        }
        return out;
    }

    /** El total estimado, o {@code 0} si el servidor no lo informo. */
    public int getResultSize() {
        return this.resultSize;
    }

    /** Por donde iba; vacia significa que no hay mas paginas. */
    public byte[] getCookie() {
        return this.cookie.length == 0 ? null : this.cookie.clone();
    }
}
