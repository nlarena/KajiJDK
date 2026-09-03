package org.ietf.jgss;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's org.ietf.jgss.Oid -- un identificador de objeto, del mundo ASN.1.
 *
 * <p>Una lista de numeros que nombra algo de forma unica y global: {@code 1.2.840.113554.1.2.2} es
 * Kerberos v5. El arbol lo reparten organismos de registro, y por eso no hacen falta acuerdos entre
 * las partes para que dos implementaciones se refieran a lo mismo.
 *
 * <h2>Dos reglas que no se adivinan</h2>
 *
 * <p>Hacen falta <b>al menos dos</b> arcos, y el primero solo puede ser 0, 1 o 2. No es capricho: la
 * codificacion DER mete los dos primeros arcos en un solo byte como {@code 40 * primero + segundo},
 * y eso solo cierra si el primero es chico. Por eso {@code "1"} y {@code "3.1"} se rechazan y
 * {@code "0.0"} se acepta.
 *
 * <h2>La codificacion</h2>
 *
 * <p>{@link #getDER} devuelve el TLV <b>completo</b> --etiqueta {@code 0x06}, largo, y contenido--
 * y no solo el contenido. Cada arco a partir del tercero va en base 128, con el bit alto prendido en
 * todos los bytes menos el ultimo; asi un arco grande ocupa lo que necesita y no hay largo fijo.
 *
 * <p>{@link #hashCode} sale de esos bytes. El valor concreto no es el mismo que el del JDK --el suyo
 * viene de una clase interna suya-- y no tiene por que serlo: lo unico que el contrato pide es que
 * dos iguales coincidan, y eso se cumple porque dos OID iguales tienen la misma codificacion.
 */
public class Oid {

    /** La etiqueta ASN.1 de un identificador de objeto. */
    private static final byte TAG = 0x06;

    /** Los arcos, en orden. */
    private final int[] arcs;

    /** El TLV completo, calculado una vez al construir. */
    private final byte[] der;

    /**
     * Desde la forma con puntos.
     *
     * @throws GSSException con {@link GSSException#FAILURE} si no es un OID valido
     */
    public Oid(String strOid) throws GSSException {
        if (strOid == null) {
            throw new GSSException(GSSException.FAILURE, 0,
                "Improperly formatted Object Identifier String - null");
        }
        int[] parsed = parse(strOid);
        if (parsed == null) {
            throw new GSSException(GSSException.FAILURE, 0,
                "Improperly formatted Object Identifier String - " + strOid);
        }
        this.arcs = parsed;
        this.der = encode(parsed);
    }

    /**
     * Desde un flujo con la codificacion DER.
     *
     * @throws GSSException si los bytes no son un OID
     */
    public Oid(InputStream derOid) throws GSSException {
        if (derOid == null) {
            throw new GSSException(GSSException.FAILURE, 0, "Null DER stream");
        }
        byte[] bytes;
        try {
            bytes = readTlv(derOid);
        } catch (IOException e) {
            throw new GSSException(GSSException.FAILURE, 0,
                "Could not read the DER encoding: " + e.getMessage());
        }
        this.arcs = decode(bytes);
        this.der = bytes;
    }

    /**
     * Desde la codificacion DER completa.
     *
     * @throws GSSException si los bytes no son un OID
     */
    public Oid(byte[] data) throws GSSException {
        if (data == null) {
            throw new GSSException(GSSException.FAILURE, 0, "Null DER encoding");
        }
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        this.arcs = decode(copy);
        this.der = copy;
    }

    /** La forma con puntos. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < this.arcs.length) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(this.arcs[i]);
            i = i + 1;
        }
        return sb.toString();
    }

    /** Igualdad por arcos. */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Oid)) {
            return false;
        }
        int[] theirs = ((Oid) other).arcs;
        if (theirs.length != this.arcs.length) {
            return false;
        }
        int i = 0;
        while (i < this.arcs.length) {
            if (this.arcs[i] != theirs[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * El TLV completo. Copia, para que nadie lo modifique.
     *
     * @throws GSSException nunca en esta implementacion; esta en la firma porque el JDK codifica
     *     de forma perezosa y ahi si puede fallar
     */
    public byte[] getDER() throws GSSException {
        byte[] copy = new byte[this.der.length];
        System.arraycopy(this.der, 0, copy, 0, this.der.length);
        return copy;
    }

    /**
     * Si este OID esta en ese conjunto.
     *
     * <p>Es lo que se usa para preguntar "soporta este mecanismo", que es la operacion mas comun del
     * tipo y la razon de que este metodo exista en vez de dejar el bucle a quien llama.
     */
    public boolean containedIn(Oid[] oids) {
        int i = 0;
        while (i < oids.length) {
            if (this.equals(oids[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** Sobre los bytes codificados; ver la nota de la clase. */
    public int hashCode() {
        int result = 1;
        int i = 0;
        while (i < this.der.length) {
            result = 31 * result + this.der[i];
            i = i + 1;
        }
        return result;
    }

    /**
     * Como el constructor de cadena, pero sin excepcion comprobada.
     *
     * <p>Existe para las constantes {@code NT_*} de {@link GSSName}: un inicializador de campo de
     * interfaz no puede atajar nada, y esos OID son literales de esta biblioteca que no pueden
     * fallar. Es paquete-privado a proposito -- no es parte del API y nadie de afuera lo ve.
     *
     * @return null si la cadena no es un OID, que para un literal de aca significa un error de tipeo
     */
    static Oid literal(String strOid) {
        try {
            return new Oid(strOid);
        } catch (GSSException e) {
            return null;
        }
    }

    // ---- adentro ---------------------------------------------------------------------------

    /** Los arcos de una cadena con puntos, o null si no es valida. Ver las dos reglas de la clase. */
    private static int[] parse(String text) {
        if (text.length() == 0) {
            return null;
        }
        String[] parts = text.split("\\.", -1);
        if (parts.length < 2) {
            return null;
        }
        int[] out = new int[parts.length];
        int i = 0;
        while (i < parts.length) {
            if (parts[i].length() == 0) {
                return null;
            }
            int value = 0;
            int j = 0;
            while (j < parts[i].length()) {
                char c = parts[i].charAt(j);
                if (c < '0' || c > '9') {
                    return null;
                }
                value = value * 10 + (c - '0');
                if (value < 0) {
                    return null; // se paso de int
                }
                j = j + 1;
            }
            out[i] = value;
            i = i + 1;
        }
        if (out[0] > 2) {
            return null;
        }
        // Con primer arco 0 o 1, el segundo no puede pasar de 39: los dos van en un solo byte.
        if (out[0] < 2 && out[1] > 39) {
            return null;
        }
        return out;
    }

    /** El TLV de esos arcos. */
    private static byte[] encode(int[] arcs) {
        List<Byte> content = new ArrayList<Byte>();
        appendBase128(content, arcs[0] * 40 + arcs[1]);
        int i = 2;
        while (i < arcs.length) {
            appendBase128(content, arcs[i]);
            i = i + 1;
        }
        byte[] out = new byte[2 + content.size()];
        out[0] = TAG;
        // El largo cabe en un byte mientras sea menor a 128, que es el caso de cualquier OID real.
        out[1] = (byte) content.size();
        int j = 0;
        while (j < content.size()) {
            out[2 + j] = content.get(j).byteValue();
            j = j + 1;
        }
        return out;
    }

    /** Un arco en base 128, con el bit alto prendido salvo en el ultimo byte. */
    private static void appendBase128(List<Byte> out, int value) {
        int shift = 28;
        boolean started = false;
        while (shift > 0) {
            int part = (value >>> shift) & 0x7f;
            if (part != 0 || started) {
                out.add(Byte.valueOf((byte) (part | 0x80)));
                started = true;
            }
            shift = shift - 7;
        }
        out.add(Byte.valueOf((byte) (value & 0x7f)));
    }

    /** Los arcos de un TLV. */
    private static int[] decode(byte[] tlv) throws GSSException {
        if (tlv.length < 3 || tlv[0] != TAG) {
            throw new GSSException(GSSException.FAILURE, 0, "Not a DER Object Identifier");
        }
        int length = tlv[1] & 0xff;
        if (length > 127 || length != tlv.length - 2) {
            throw new GSSException(GSSException.FAILURE, 0, "Malformed Object Identifier length");
        }
        List<Integer> arcs = new ArrayList<Integer>();
        int i = 2;
        // El primer byte lleva los dos primeros arcos; ver la nota de la clase.
        int first = readBase128(tlv, i);
        int consumed = base128Length(tlv, i);
        if (consumed < 0) {
            throw new GSSException(GSSException.FAILURE, 0, "Truncated Object Identifier");
        }
        i = i + consumed;
        if (first < 80) {
            arcs.add(Integer.valueOf(first / 40));
            arcs.add(Integer.valueOf(first % 40));
        } else {
            arcs.add(Integer.valueOf(2));
            arcs.add(Integer.valueOf(first - 80));
        }
        while (i < tlv.length) {
            int value = readBase128(tlv, i);
            int used = base128Length(tlv, i);
            if (used < 0) {
                throw new GSSException(GSSException.FAILURE, 0, "Truncated Object Identifier");
            }
            arcs.add(Integer.valueOf(value));
            i = i + used;
        }
        int[] out = new int[arcs.size()];
        int k = 0;
        while (k < arcs.size()) {
            out[k] = arcs.get(k).intValue();
            k = k + 1;
        }
        return out;
    }

    /** El valor del arco que empieza en `from`. */
    private static int readBase128(byte[] data, int from) {
        int value = 0;
        int i = from;
        while (i < data.length) {
            value = (value << 7) | (data[i] & 0x7f);
            if ((data[i] & 0x80) == 0) {
                return value;
            }
            i = i + 1;
        }
        return value;
    }

    /** Cuantos bytes ocupa ese arco, o -1 si se corta antes de terminar. */
    private static int base128Length(byte[] data, int from) {
        int i = from;
        while (i < data.length) {
            if ((data[i] & 0x80) == 0) {
                return i - from + 1;
            }
            i = i + 1;
        }
        return -1;
    }

    /** Lee un TLV completo del flujo: etiqueta, largo y contenido. */
    private static byte[] readTlv(InputStream in) throws IOException {
        int tag = in.read();
        int length = in.read();
        if (tag < 0 || length < 0) {
            throw new IOException("truncated DER encoding");
        }
        byte[] out = new byte[2 + length];
        out[0] = (byte) tag;
        out[1] = (byte) length;
        int read = 0;
        while (read < length) {
            int n = in.read(out, 2 + read, length - read);
            if (n < 0) {
                throw new IOException("truncated DER encoding");
            }
            read = read + n;
        }
        return out;
    }
}
