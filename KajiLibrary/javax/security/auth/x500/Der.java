package javax.security.auth.x500;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.security.auth.x500.Der -- el nombre X.501 en su forma codificada.
 *
 * <p>La estructura entera son tres anidamientos y conviene tenerla a mano:
 *
 * <pre>
 *   Name  ::= SEQUENCE OF RelativeDistinguishedName
 *   RDN   ::= SET OF AttributeTypeAndValue
 *   ATV   ::= SEQUENCE { type OBJECT IDENTIFIER, value ANY }
 * </pre>
 *
 * <p>El SET del medio es el que sorprende: un paso del nombre puede tener **varios** pares, y por eso
 * es un conjunto y no un valor. En la practica casi siempre tiene uno.
 *
 * <p><strong>El orden va al reves que en el texto.</strong> El DER lista los pasos de lo general a lo
 * particular --pais primero, nombre comun ultimo-- y el texto al reves. Invertirlo aca es todo lo que
 * separa un nombre correcto de uno que parece bien y encadena mal.
 */
final class Der {

    private Der() {
    }

    static final int SEQUENCE = 0x30;
    static final int SET = 0x31;
    static final int OID = 0x06;
    static final int PRINTABLE = 0x13;
    static final int UTF8 = 0x0c;
    static final int IA5 = 0x16;
    static final int T61 = 0x14;
    static final int BMP = 0x1e;
    static final int UNIVERSAL = 0x1c;

    // ---- lectura -----------------------------------------------------------------------------------

    /** Los pasos del nombre, **ya dados vuelta** al orden del texto. */
    static X500Principal.Rdn[] readName(byte[] der) throws IOException {
        Cursor c = new Cursor(der, 0, der.length);
        Cursor seq = c.descend(SEQUENCE);
        List<X500Principal.Rdn> rdns = new ArrayList<X500Principal.Rdn>();
        while (seq.hay()) {
            rdns.add(readRdn(seq.descend(SET)));
        }
        if (c.hay()) {
            throw new IOException("sobran bytes despues del Name");
        }
        // Del orden del DER al del texto.
        X500Principal.Rdn[] out = new X500Principal.Rdn[rdns.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = rdns.get(out.length - 1 - i);
            i = i + 1;
        }
        return out;
    }

    private static X500Principal.Rdn readRdn(Cursor set) throws IOException {
        List<String> types = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        while (set.hay()) {
            Cursor atv = set.descend(SEQUENCE);
            types.add(readOid(atv));
            values.add(readAttributeValue(atv.restOfValue()));
            if (atv.hay()) {
                throw new IOException("sobran bytes en un AttributeTypeAndValue");
            }
        }
        if (types.isEmpty()) {
            throw new IOException("un RDN vacio");
        }
        return new X500Principal.Rdn(types.toArray(new String[types.size()]),
                values.toArray(new String[values.size()]));
    }

    private static String readOid(Cursor c) throws IOException {
        byte[] body = c.readBody(OID);
        if (body.length == 0) {
            throw new IOException("OID vacio");
        }
        StringBuilder sb = new StringBuilder();
        // El primer byte lleva **dos** arcos: `40*a + b`. Es la unica irregularidad de la
        // codificacion de OID, y viene de que el primer arco solo puede ser 0, 1 o 2.
        int first = body[0] & 0xff;
        sb.append(first / 40).append('.').append(first % 40);
        long acum = 0;
        int i = 1;
        while (i < body.length) {
            int b = body[i] & 0xff;
            acum = (acum << 7) | (long) (b & 0x7f);
            if ((b & 0x80) == 0) {
                sb.append('.').append(acum);
                acum = 0;
            }
            i = i + 1;
        }
        if (acum != 0) {
            throw new IOException("OID truncado");
        }
        return sb.toString();
    }

    /**
     * El valor de un atributo, como texto.
     *
     * <p>Los cinco tipos de cadena que un DN usa se leen igual --son bytes-- salvo `BMPString`, que
     * es UTF-16 de dos bytes por caracter. Un tipo que no sea ninguno de esos **no se inventa**: se
     * devuelve su forma hexadecimal con `#` adelante, que es exactamente lo que el JDK muestra.
     */
    static String readAttributeValue(byte[] der) throws IOException {
        if (der.length < 2) {
            throw new IOException("valor de atributo truncado");
        }
        Cursor c = new Cursor(der, 0, der.length);
        int tag = c.verTag();
        if (tag == PRINTABLE || tag == UTF8 || tag == IA5 || tag == T61 || tag == UNIVERSAL) {
            byte[] body = c.readBody(tag);
            return new String(body, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (tag == BMP) {
            byte[] body = c.readBody(tag);
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i + 1 < body.length) {
                sb.append((char) (((body[i] & 0xff) << 8) | (body[i + 1] & 0xff)));
                i = i + 2;
            }
            return sb.toString();
        }
        return "#" + toHex(der);
    }

    /** Lee **un** valor DER de un flujo y devuelve sus bytes, dejando el flujo justo despues. */
    static byte[] readOneValue(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int tag = is.read();
        if (tag < 0) {
            throw new IOException("flujo vacio");
        }
        out.write(tag);
        int first = is.read();
        if (first < 0) {
            throw new IOException("largo truncado");
        }
        out.write(first);
        int len;
        if ((first & 0x80) == 0) {
            len = first;
        } else {
            int n = first & 0x7f;
            if (n == 0 || n > 4) {
                throw new IOException("largo indefinido o demasiado grande");
            }
            len = 0;
            int i = 0;
            while (i < n) {
                int b = is.read();
                if (b < 0) {
                    throw new IOException("largo truncado");
                }
                out.write(b);
                len = (len << 8) | b;
                i = i + 1;
            }
        }
        int readCount = 0;
        while (readCount < len) {
            int b = is.read();
            if (b < 0) {
                throw new IOException("valor truncado");
            }
            out.write(b);
            readCount = readCount + 1;
        }
        return out.toByteArray();
    }

    // ---- escritura ---------------------------------------------------------------------------------

    /** El nombre en DER, **dando vuelta** los pasos al orden del DER. */
    static byte[] writeName(X500Principal.Rdn[] rdns) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int i = rdns.length - 1;
        while (i >= 0) {
            byte[] rdn = writeRdn(rdns[i]);
            body.write(rdn, 0, rdn.length);
            i = i - 1;
        }
        return envolver(SEQUENCE, body.toByteArray());
    }

    private static byte[] writeRdn(X500Principal.Rdn rdn) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int i = 0;
        while (i < rdn.types.length) {
            ByteArrayOutputStream atv = new ByteArrayOutputStream();
            byte[] oid = writeOid(rdn.types[i]);
            atv.write(oid, 0, oid.length);
            byte[] val = writeValue(rdn.values[i]);
            atv.write(val, 0, val.length);
            byte[] onePrincipal = envolver(SEQUENCE, atv.toByteArray());
            body.write(onePrincipal, 0, onePrincipal.length);
            i = i + 1;
        }
        return envolver(SET, body.toByteArray());
    }

    private static byte[] writeOid(String oid) {
        String[] arcos = partir(oid);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Los dos primeros arcos van juntos en un byte, igual que al leer.
        out.write(Integer.parseInt(arcos[0]) * 40 + Integer.parseInt(arcos[1]));
        int i = 2;
        while (i < arcos.length) {
            writeBase128(out, Long.parseLong(arcos[i]));
            i = i + 1;
        }
        return envolver(OID, out.toByteArray());
    }

    // Base 128 con el bit alto marcando "sigue", y el ultimo byte sin marcar.
    private static void writeBase128(ByteArrayOutputStream out, long v) {
        if (v == 0) {
            out.write(0);
            return;
        }
        byte[] tmp = new byte[10];
        int n = 0;
        long x = v;
        while (x > 0) {
            tmp[n] = (byte) (x & 0x7f);
            x = x >>> 7;
            n = n + 1;
        }
        int i = n - 1;
        while (i >= 0) {
            out.write(i > 0 ? (tmp[i] | 0x80) : tmp[i]);
            i = i - 1;
        }
    }

    /**
     * El valor con el tipo de cadena mas **angosto** que lo pueda representar.
     *
     * <p>`PrintableString` si entra --letras, digitos y un puñado de signos-- y `UTF8String` si no.
     * Elegir el mas angosto no es tacañeria: es lo que hace que el DER que emitimos sea el mismo que
     * emite cualquier otra implementacion para el mismo nombre, y eso es lo que permite comparar
     * certificados byte a byte.
     */
    static byte[] writeValue(String value) {
        // Un valor que quedo en forma hexadecimal es DER crudo: se devuelve tal cual.
        if (value.length() > 1 && value.charAt(0) == '#') {
            byte[] rawBytes = fromHex(value.substring(1, value.length()));
            if (rawBytes != null) {
                return rawBytes;
            }
        }
        if (esPrintable(value)) {
            return envolver(PRINTABLE, value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        return envolver(UTF8, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // El juego de `PrintableString` (X.680): es chico y no incluye ni `@` ni `_`, que es por lo que un
    // correo electronico siempre termina en UTF8String o IA5String.
    private static boolean esPrintable(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || " '()+,-./:=?".indexOf(c) >= 0;
            if (!ok) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // ---- utilidades --------------------------------------------------------------------------------

    private static byte[] envolver(int tag, byte[] body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        int n = body.length;
        if (n < 128) {
            out.write(n);
        } else {
            // Forma larga: un byte con la cantidad de bytes del largo, y despues el largo.
            int bytes = n < 256 ? 1 : (n < 65536 ? 2 : (n < 16777216 ? 3 : 4));
            out.write(0x80 | bytes);
            int i = bytes - 1;
            while (i >= 0) {
                out.write((n >>> (8 * i)) & 0xff);
                i = i - 1;
            }
        }
        out.write(body, 0, body.length);
        return out.toByteArray();
    }

    static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < b.length) {
            int v = b[i] & 0xff;
            sb.append(Character.forDigit(v >> 4, 16)).append(Character.forDigit(v & 0xf, 16));
            i = i + 1;
        }
        return sb.toString().toUpperCase();
    }

    private static byte[] fromHex(String h) {
        if (h.length() == 0 || h.length() % 2 != 0) {
            return null;
        }
        byte[] out = new byte[h.length() / 2];
        int i = 0;
        while (i < out.length) {
            int a = Character.digit(h.charAt(2 * i), 16);
            int b = Character.digit(h.charAt(2 * i + 1), 16);
            if (a < 0 || b < 0) {
                return null;
            }
            out[i] = (byte) ((a << 4) | b);
            i = i + 1;
        }
        return out;
    }

    private static String[] partir(String oid) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        int i = 0;
        while (i <= oid.length()) {
            if (i == oid.length() || oid.charAt(i) == '.') {
                out.add(oid.substring(from, i));
                from = i + 1;
            }
            i = i + 1;
        }
        return out.toArray(new String[out.size()]);
    }

    /**
     * Un cursor sobre un tramo de DER.
     *
     * <p>Existe para que leer una estructura anidada no sea una cuenta de indices: `descend(tag)`
     * devuelve un cursor sobre el **contenido** y avanza el de afuera, asi que el anidamiento del
     * codigo sigue al de los datos.
     */
    private static final class Cursor {
        private final byte[] b;
        private int pos;
        private final int end;

        Cursor(byte[] b, int from, int end) {
            this.b = b;
            this.pos = from;
            this.end = end;
        }

        boolean hay() {
            return this.pos < this.end;
        }

        int verTag() throws IOException {
            if (!this.hay()) {
                throw new IOException("DER truncado");
            }
            return this.b[this.pos] & 0xff;
        }

        Cursor descend(int expectedTag) throws IOException {
            int[] r = this.cabecera(expectedTag);
            Cursor inner = new Cursor(this.b, r[0], r[0] + r[1]);
            this.pos = r[0] + r[1];
            return inner;
        }

        byte[] readBody(int expectedTag) throws IOException {
            int[] r = this.cabecera(expectedTag);
            byte[] out = new byte[r[1]];
            int i = 0;
            while (i < out.length) {
                out[i] = this.b[r[0] + i];
                i = i + 1;
            }
            this.pos = r[0] + r[1];
            return out;
        }

        byte[] restOfValue() throws IOException {
            int from = this.pos;
            // Se mide el valor y se **consume entero** --cabecera y cuerpo--, y se devuelven sus
            // bytes con la cabecera puesta: quien lo reciba necesita el tag para saber que tipo de
            // cadena es.
            //
            // El `pos = body + len` no sobra: `cabecera` deja el cursor al **principio del
            // cuerpo**, que es lo que quiere `descend`. Sin esta linea se devolvia solo la cabecera y
            // el cursor quedaba corrido, y el error salia mucho despues como "el valor se pasa del
            // tramo" sobre un dato que estaba bien.
            int[] r = this.cabecera(-1);
            this.pos = r[0] + r[1];
            byte[] out = new byte[this.pos - from];
            int i = 0;
            while (i < out.length) {
                out[i] = this.b[from + i];
                i = i + 1;
            }
            return out;
        }

        // {posicion del cuerpo, largo}. `expectedTag` en -1 acepta cualquiera.
        private int[] cabecera(int expectedTag) throws IOException {
            if (this.pos + 1 >= this.end) {
                throw new IOException("DER truncado");
            }
            int tag = this.b[this.pos] & 0xff;
            if (expectedTag >= 0 && tag != expectedTag) {
                throw new IOException("se esperaba el tag 0x"
                        + Integer.toHexString(expectedTag) + " y vino 0x" + Integer.toHexString(tag));
            }
            int p = this.pos + 1;
            int first = this.b[p] & 0xff;
            p = p + 1;
            int len;
            if ((first & 0x80) == 0) {
                len = first;
            } else {
                int n = first & 0x7f;
                // La forma indefinida (`n == 0`) no existe en DER, solo en BER. Un largo de mas de
                // cuatro bytes no cabe en un `int` y no hay nombre que lo necesite.
                if (n == 0 || n > 4) {
                    throw new IOException("largo indefinido o demasiado grande");
                }
                len = 0;
                int i = 0;
                while (i < n) {
                    if (p >= this.end) {
                        throw new IOException("largo truncado");
                    }
                    len = (len << 8) | (this.b[p] & 0xff);
                    p = p + 1;
                    i = i + 1;
                }
            }
            if (len < 0 || p + len > this.end) {
                throw new IOException("el valor se pasa del tramo");
            }
            this.pos = p;
            return new int[] {p, len};
        }
    }
}
