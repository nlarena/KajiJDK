package javax.naming.ldap;

import java.io.IOException;

/**
 * Pide los resultados de a paginas.
 *
 * <h2>Por que hace falta</h2>
 *
 * <p>Una busqueda sobre un directorio grande puede devolver cientos de miles de entradas. Sin
 * paginar, el servidor las manda todas y el cliente las recibe todas — o el servidor corta en su
 * limite y el cliente no se entera de que falto la mitad.
 *
 * <h2>La galletita, que es como funciona</h2>
 *
 * <p>Cada respuesta trae un {@link PagedResultsResponseControl} con una <em>cookie</em>: un dato
 * opaco que representa "donde iba". Para pedir la pagina siguiente hay que mandar esa cookie de
 * vuelta.
 *
 * <p>Y de ahi la consecuencia que sorprende: la paginacion es <strong>con estado del lado del
 * servidor</strong>, asi que hay que recorrerla hasta el final o soltarla explicitamente — mandar
 * una cookie vacia—, porque las paginas abandonadas ocupan recursos alla hasta que venzan.
 */
public final class PagedResultsControl extends BasicControl {

    private static final long serialVersionUID = 6684806685736844298L;

    /** El OID de este control. */
    public static final String OID = "1.2.840.113556.1.4.319";

    /**
     * La primera pagina, de ese tamano.
     *
     * @param pageSize cuantas entradas por pagina; es un pedido, el servidor puede dar menos
     * @throws IOException si el control no se pudo codificar
     */
    public PagedResultsControl(int pageSize, boolean criticality) throws IOException {
        super(OID, criticality, codificar(pageSize, null));
    }

    /**
     * La pagina que sigue a esa cookie.
     *
     * @param cookie la que trajo la respuesta anterior; {@code null} o vacia arranca de cero
     * @throws IOException si el control no se pudo codificar
     */
    public PagedResultsControl(int pageSize, byte[] cookie, boolean criticality)
            throws IOException {
        super(OID, criticality, codificar(pageSize, cookie));
    }

    /**
     * El valor del control: una secuencia BER con el tamano y la cookie.
     *
     * <p>Se codifica a mano porque es una estructura de dos campos y traer un codificador BER
     * entero para esto seria desproporcionado. La forma es
     * {@code SEQUENCE { INTEGER size, OCTET STRING cookie }}, tal como la define el RFC 2696.
     */
    private static byte[] codificar(int pageSize, byte[] cookie) {
        byte[] galleta = cookie == null ? new byte[0] : cookie;
        byte[] tamano = enteroBer(pageSize);
        int largoContenido = tamano.length + 2 + galleta.length;
        byte[] out = new byte[2 + largoContenido];
        int i = 0;
        out[i++] = 0x30;                       // SEQUENCE
        out[i++] = (byte) largoContenido;
        System.arraycopy(tamano, 0, out, i, tamano.length);
        i += tamano.length;
        out[i++] = 0x04;                       // OCTET STRING
        out[i++] = (byte) galleta.length;
        System.arraycopy(galleta, 0, out, i, galleta.length);
        return out;
    }

    /** Un INTEGER de BER, con la cantidad minima de bytes y en complemento a dos. */
    private static byte[] enteroBer(int v) {
        int bytes = 1;
        int t = v;
        while (t > 127 || t < -128) {
            t = t >> 8;
            bytes++;
        }
        byte[] out = new byte[2 + bytes];
        out[0] = 0x02;
        out[1] = (byte) bytes;
        for (int i = 0; i < bytes; i++) {
            out[2 + i] = (byte) (v >> (8 * (bytes - 1 - i)));
        }
        return out;
    }
}
