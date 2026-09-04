package java.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// Un atributo de PKCS#12: un OID y un conjunto de valores, guardados en su forma DER.
//
// ===============================================================================================
// POR QUE ESTA CLASE SI SE PUEDE ESCRIBIR Y CASI NINGUNA OTRA DE ESTE PAQUETE
// ===============================================================================================
//
// El resto de `java.security` que falta esta trabado por dos cosas que esta VM no tiene: entropia
// del sistema operativo (`SecureRandom`, y con el las quince firmas que lo nombran) y algun
// proveedor que sepa RSA o ECDSA. Este atributo no necesita ninguna de las dos: es **solo
// codificacion**. Un OID, un SET de valores, DER. Se puede implementar entero y de verdad, asi que
// se implementa entero.
//
// ===============================================================================================
// LA FORMA
// ===============================================================================================
//
//     SEQUENCE { OBJECT IDENTIFIER, SET OF ANY }
//
// El objeto es **inmutable y esta definido por sus bytes**: `equals` y `hashCode` comparan el DER,
// no el par nombre/valor. Es la unica definicion que se sostiene, porque dos codificaciones
// distintas del mismo texto son atributos distintos para quien despues los firme.
//
// El constructor de texto decide el tipo de cada valor por su forma: si es una tira de pares
// hexadecimales separados por dos puntos --y hacen falta **al menos dos** pares, un "01" suelto no
// cuenta-- va como OCTET STRING; si no, como UTF8String. Un valor entre corchetes y separado por
// ", " es una lista de varios.
//
// Ojo con una rareza heredada del JDK que se copio a proposito: los pares hexadecimales pasan por
// un {@link BigInteger}, asi que **los ceros de la izquierda se pierden**. "00:01" se codifica como
// el unico byte 01 y al releerlo vuelve como "01". Es sorprendente, pero cambiarlo daria bytes
// distintos a los del JDK para la misma entrada, y estos bytes terminan dentro de cosas firmadas.
//
// ===============================================================================================
// LO QUE NO DECODIFICA, Y POR QUE ES UNA EXCEPCION Y NO UNA RESPUESTA
// ===============================================================================================
//
// {@link #PKCS12Attribute(byte[])} **rechaza** un atributo cuyo valor sea UTCTime o
// GeneralizedTime. No es que no sepamos leer la fecha: el JDK convierte esos valores a
// {@code java.util.Date} y devuelve su {@code toString()}, y el {@code Date.toString()} de esta
// biblioteca es distinto a proposito (imprime los milisegundos, porque aca no hay zona horaria con
// la cual armar un reloj de pared honesto; ver java/util/Date.java).
//
// O sea que los mismos bytes darian un {@code getValue()} distinto aca que en cualquier otra JVM.
// Entre devolver en silencio un valor que no coincide con el de nadie y fallar fuerte en el
// constructor, falla. Quien se lo cruza se entera en el momento; el otro camino no se nota hasta
// que algo se compara contra el valor de una JVM real. Todo el resto de los tipos --las cadenas,
// los OID, los enteros, los booleanos, los OCTET STRING y el hexadecimal de reserva para las
// etiquetas raras-- se decodifica igual que en el JDK.
public final class PKCS12Attribute implements KeyStore.Entry.Attribute {

    // Al menos dos pares: es lo que dice el `+` del grupo en el JDK, y es lo que separa un valor
    // hexadecimal de un texto de dos caracteres que casualmente sean digitos hex.
    private static final String PARES_HEX = "^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2})+$";

    private static final int TAG_BOOLEAN = 0x01;
    private static final int TAG_INTEGER = 0x02;
    private static final int TAG_OCTET_STRING = 0x04;
    private static final int TAG_OID = 0x06;
    private static final int TAG_UTF8 = 0x0c;
    private static final int TAG_NUMERIC = 0x12;
    private static final int TAG_PRINTABLE = 0x13;
    private static final int TAG_T61 = 0x14;
    private static final int TAG_IA5 = 0x16;
    private static final int TAG_UTC_TIME = 0x17;
    private static final int TAG_GENERALIZED_TIME = 0x18;
    private static final int TAG_VISIBLE = 0x1a;
    private static final int TAG_GENERAL = 0x1b;
    private static final int TAG_BMP = 0x1e;
    private static final int TAG_SEQUENCE = 0x30;
    private static final int TAG_SET = 0x31;

    private String name;

    private String value;

    private final byte[] encoded;

    // -1 marca "todavia no calculado". Un atributo real puede tener hash 0 y no pasa nada: se
    // recalcularia una vez de mas, que es barato, y nunca da una respuesta incorrecta.
    private int hashValue = -1;

    public PKCS12Attribute(String name, String value) {
        if (name == null || value == null) {
            throw new NullPointerException();
        }

        byte[] oid;
        try {
            oid = oidADer(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Incorrect format: name", e);
        }
        this.name = name;

        // Los corchetes marcan una lista. Se mira el largo antes de indexar porque "[" solo mide 1
        // y seria a la vez primer y ultimo caracter.
        int largo = value.length();
        String[] valores;
        if (largo > 1 && value.charAt(0) == '[' && value.charAt(largo - 1) == ']') {
            valores = value.substring(1, largo - 1).split(", ");
        } else {
            valores = new String[] { value };
        }
        this.value = value;

        try {
            this.encoded = codificar(oid, valores);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Incorrect format: value", e);
        }
    }

    /**
     * Se clona al entrar y al salir: el atributo es inmutable y un `byte[]` compartido con quien lo
     * construyo no lo seria.
     */
    public PKCS12Attribute(byte[] encoded) {
        if (encoded == null) {
            throw new NullPointerException();
        }
        this.encoded = encoded.clone();
        try {
            interpretar(this.encoded);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Incorrect format: encoded", e);
        }
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public byte[] getEncoded() {
        return encoded.clone();
    }

    /**
     * La identidad son los bytes, no el par nombre/valor: dos DER distintos que se lean igual como
     * texto siguen siendo atributos distintos para quien los vaya a firmar.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PKCS12Attribute)) {
            return false;
        }
        return java.util.Arrays.equals(encoded, ((PKCS12Attribute) obj).encoded);
    }

    @Override
    public int hashCode() {
        int h = hashValue;
        if (h == -1) {
            hashValue = h = java.util.Arrays.hashCode(encoded);
        }
        return h;
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    // ---- codificacion -------------------------------------------------------------------------

    private static byte[] codificar(byte[] oid, String[] valores) {
        Buf contenido = new Buf();
        for (int i = 0; i < valores.length; i++) {
            String v = valores[i];
            if (Pattern.matches(PARES_HEX, v)) {
                // Por BigInteger, igual que el JDK: es lo que hace que "00:01" pierda el cero.
                byte[] bytes = new BigInteger(v.replace(":", ""), 16).toByteArray();
                if (bytes.length > 0 && bytes[0] == 0) {
                    byte[] recorte = new byte[bytes.length - 1];
                    System.arraycopy(bytes, 1, recorte, 0, recorte.length);
                    bytes = recorte;
                }
                contenido.tlv(TAG_OCTET_STRING, bytes);
            } else {
                contenido.tlv(TAG_UTF8, v.getBytes(StandardCharsets.UTF_8));
            }
        }

        Buf atributo = new Buf();
        atributo.tlv(TAG_OID, oid);
        atributo.tlv(TAG_SET, contenido.bytes());

        Buf afuera = new Buf();
        afuera.tlv(TAG_SEQUENCE, atributo.bytes());
        return afuera.bytes();
    }

    /**
     * Un OID en texto punteado a su contenido DER (sin etiqueta ni largo).
     *
     * <p>Los dos primeros arcos van juntos en un solo numero, {@code 40*a + b}. No es una
     * compresion caprichosa: como el primer arco solo puede valer 0, 1 o 2, y con 0 o 1 el segundo
     * no pasa de 39, la suma se puede deshacer sin ambiguedad. Con arco 2 el segundo no tiene tope,
     * y por eso el numero combinado puede necesitar varios bytes.
     */
    private static byte[] oidADer(String oid) {
        String[] partes = oid.split("\\.");
        if (partes.length < 2) {
            throw new IllegalArgumentException("OID con menos de dos arcos: " + oid);
        }
        long[] arcos = new long[partes.length];
        for (int i = 0; i < partes.length; i++) {
            arcos[i] = arco(partes[i]);
        }
        if (arcos[0] > 2) {
            throw new IllegalArgumentException("primer arco fuera de 0..2: " + oid);
        }
        if (arcos[0] < 2 && arcos[1] > 39) {
            throw new IllegalArgumentException("segundo arco fuera de 0..39: " + oid);
        }

        Buf b = new Buf();
        b.base128(arcos[0] * 40 + arcos[1]);
        for (int i = 2; i < arcos.length; i++) {
            b.base128(arcos[i]);
        }
        return b.bytes();
    }

    private static long arco(String s) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("arco vacio");
        }
        long v = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("arco no numerico: " + s);
            }
            v = v * 10 + (c - '0');
            // El tope es artificial pero honesto: con `long` no se puede representar mas, y un
            // desborde silencioso daria un OID distinto al pedido.
            if (v > (1L << 56)) {
                throw new IllegalArgumentException("arco demasiado grande: " + s);
            }
        }
        return v;
    }

    // ---- lectura ------------------------------------------------------------------------------

    private void interpretar(byte[] der) {
        Lec fuera = new Lec(der, 0, der.length);
        Lec sec = fuera.tlv(TAG_SEQUENCE);
        // A proposito NO se exige que el SEQUENCE agote el arreglo: el JDK acepta bytes de sobra
        // despues del atributo y se los queda dentro de `encoded`, asi que un `getEncoded()` los
        // devuelve. Comprobado contra el JDK 25; ser mas estrictos aca haria que un atributo que la
        // JVM real lee sin quejarse fuera rechazado por la nuestra.
        byte[] oid = sec.tlv(TAG_OID).resto();
        Lec conjunto = sec.tlv(TAG_SET);
        // Adentro del SEQUENCE si se exige: tiene que haber exactamente el OID y el SET. Un tercer
        // elemento lo rechaza tambien el JDK.
        sec.exigirFin();

        // Se cuenta primero para poder dimensionar el arreglo sin una lista de por medio.
        int n = 0;
        Lec cuenta = conjunto.copia();
        while (!cuenta.fin()) {
            cuenta.saltarTlv();
            n++;
        }
        // Un SET vacio es valido: el JDK lo acepta y el valor queda en "[]", que es lo que da
        // `Arrays.toString` de un arreglo sin elementos. Rechazarlo seria inventar una regla.
        String[] valores = new String[n];
        for (int i = 0; i < n; i++) {
            valores[i] = valorDe(conjunto);
        }

        this.name = derAOid(oid);
        this.value = n == 1 ? valores[0] : listaDe(valores);
    }

    private static String valorDe(Lec l) {
        int etiqueta = l.etiquetaActual();
        Lec cuerpo = l.tlv(etiqueta);
        byte[] datos = cuerpo.resto();

        switch (etiqueta) {
            case TAG_OCTET_STRING:
                return hexConDosPuntos(datos);
            case TAG_UTF8:
                return new String(datos, StandardCharsets.UTF_8);
            case TAG_NUMERIC:
            case TAG_PRINTABLE:
            case TAG_T61:
            case TAG_IA5:
            case TAG_VISIBLE:
            case TAG_GENERAL:
                return new String(datos, StandardCharsets.ISO_8859_1);
            case TAG_BMP:
                return new String(datos, StandardCharsets.UTF_16BE);
            case TAG_OID:
                return derAOid(datos);
            case TAG_INTEGER:
                if (datos.length == 0) {
                    throw new IllegalArgumentException("INTEGER vacio");
                }
                return new BigInteger(datos).toString();
            case TAG_BOOLEAN:
                if (datos.length != 1) {
                    throw new IllegalArgumentException("BOOLEAN de largo " + datos.length);
                }
                return String.valueOf(datos[0] != 0);
            case TAG_UTC_TIME:
            case TAG_GENERALIZED_TIME:
                // Ver el comentario de la clase: se rechaza en vez de contestar un texto que no
                // coincidiria con el de ninguna otra JVM.
                throw new IllegalArgumentException(
                        "valor de tipo tiempo (etiqueta 0x" + Integer.toHexString(etiqueta)
                                + "): esta biblioteca no lo convierte a texto, ver PKCS12Attribute");
            default:
                // Igual que el JDK: lo que no se reconoce sale como el hexadecimal de su contenido.
                return hexConDosPuntos(datos);
        }
    }

    private static String listaDe(String[] valores) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                s.append(", ");
            }
            s.append(valores[i]);
        }
        return s.append(']').toString();
    }

    private static String hexConDosPuntos(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            if (i > 0) {
                s.append(':');
            }
            s.append(Character.forDigit((b[i] >> 4) & 0xf, 16));
            s.append(Character.forDigit(b[i] & 0xf, 16));
        }
        return s.toString();
    }

    private static String derAOid(byte[] c) {
        if (c.length == 0) {
            throw new IllegalArgumentException("OID vacio");
        }
        StringBuilder s = new StringBuilder();
        int i = 0;
        long primero = leerBase128(c, i);
        i = finBase128(c, i);
        if (primero < 40) {
            s.append('0').append('.').append(primero);
        } else if (primero < 80) {
            s.append('1').append('.').append(primero - 40);
        } else {
            s.append('2').append('.').append(primero - 80);
        }
        while (i < c.length) {
            s.append('.').append(leerBase128(c, i));
            i = finBase128(c, i);
        }
        return s.toString();
    }

    private static long leerBase128(byte[] c, int i) {
        long v = 0;
        while (true) {
            if (i >= c.length) {
                throw new IllegalArgumentException("OID truncado");
            }
            int b = c[i] & 0xff;
            v = (v << 7) | (b & 0x7f);
            if (v > (1L << 56)) {
                throw new IllegalArgumentException("arco demasiado grande en el OID");
            }
            if ((b & 0x80) == 0) {
                return v;
            }
            i++;
        }
    }

    private static int finBase128(byte[] c, int i) {
        while ((c[i] & 0x80) != 0) {
            i++;
            if (i >= c.length) {
                throw new IllegalArgumentException("OID truncado");
            }
        }
        return i + 1;
    }

    // ---- dos ayudantes minimos de DER ---------------------------------------------------------

    /** Un `byte[]` que crece, con lo justo para escribir DER. */
    private static final class Buf {

        private byte[] a = new byte[64];

        private int n;

        void byt(int b) {
            if (n == a.length) {
                byte[] mas = new byte[a.length * 2];
                System.arraycopy(a, 0, mas, 0, n);
                a = mas;
            }
            a[n++] = (byte) b;
        }

        void todos(byte[] b) {
            for (int i = 0; i < b.length; i++) {
                byt(b[i]);
            }
        }

        /**
         * Etiqueta, largo y contenido.
         *
         * <p>El largo va en forma corta hasta 127 y en forma larga a partir de ahi. DER no deja
         * elegir: para un mismo largo hay una sola codificacion valida, y por eso el que escribe usa
         * siempre la mas corta que alcance.
         */
        void tlv(int etiqueta, byte[] contenido) {
            byt(etiqueta);
            int largo = contenido.length;
            if (largo < 128) {
                byt(largo);
            } else {
                int octetos = 0;
                for (int v = largo; v != 0; v >>>= 8) {
                    octetos++;
                }
                byt(0x80 | octetos);
                for (int i = octetos - 1; i >= 0; i--) {
                    byt((largo >>> (i * 8)) & 0xff);
                }
            }
            todos(contenido);
        }

        /** Un entero en base 128, siete bits por byte, con el bit alto encendido salvo en el ultimo. */
        void base128(long v) {
            int octetos = 1;
            for (long t = v >>> 7; t != 0; t >>>= 7) {
                octetos++;
            }
            for (int i = octetos - 1; i >= 0; i--) {
                int siete = (int) ((v >>> (i * 7)) & 0x7f);
                byt(i == 0 ? siete : (siete | 0x80));
            }
        }

        byte[] bytes() {
            byte[] r = new byte[n];
            System.arraycopy(a, 0, r, 0, n);
            return r;
        }
    }

    /** Una ventana sobre el `byte[]` que se va consumiendo. No copia nada hasta que hace falta. */
    private static final class Lec {

        private final byte[] a;

        private int i;

        private final int fin;

        Lec(byte[] a, int i, int fin) {
            this.a = a;
            this.i = i;
            this.fin = fin;
        }

        Lec copia() {
            return new Lec(a, i, fin);
        }

        boolean fin() {
            return i >= fin;
        }

        int etiquetaActual() {
            if (i >= fin) {
                throw new IllegalArgumentException("se esperaba una etiqueta y no habia mas bytes");
            }
            return a[i] & 0xff;
        }

        /** Consume un TLV de la etiqueta pedida y devuelve una ventana sobre su contenido. */
        Lec tlv(int esperada) {
            if (etiquetaActual() != esperada) {
                throw new IllegalArgumentException("se esperaba la etiqueta 0x"
                        + Integer.toHexString(esperada) + " y vino 0x"
                        + Integer.toHexString(etiquetaActual()));
            }
            i++;
            int largo = largo();
            if (largo > fin - i) {
                throw new IllegalArgumentException("largo " + largo + " mas alla del final");
            }
            Lec dentro = new Lec(a, i, i + largo);
            i += largo;
            return dentro;
        }

        void saltarTlv() {
            etiquetaActual();
            i++;
            int largo = largo();
            if (largo > fin - i) {
                throw new IllegalArgumentException("largo " + largo + " mas alla del final");
            }
            i += largo;
        }

        private int largo() {
            if (i >= fin) {
                throw new IllegalArgumentException("largo truncado");
            }
            int b = a[i++] & 0xff;
            if (b < 128) {
                return b;
            }
            int octetos = b & 0x7f;
            // La forma indefinida (0x80) no existe en DER, y mas de cuatro octetos no entra en un
            // int: las dos son entradas invalidas, no casos que sepamos manejar.
            if (octetos == 0 || octetos > 4) {
                throw new IllegalArgumentException("largo mal formado");
            }
            int v = 0;
            for (int k = 0; k < octetos; k++) {
                if (i >= fin) {
                    throw new IllegalArgumentException("largo truncado");
                }
                v = (v << 8) | (a[i++] & 0xff);
            }
            if (v < 0) {
                throw new IllegalArgumentException("largo negativo");
            }
            return v;
        }

        void exigirFin() {
            if (i != fin) {
                throw new IllegalArgumentException("sobran " + (fin - i) + " bytes");
            }
        }

        byte[] resto() {
            byte[] r = new byte[fin - i];
            System.arraycopy(a, i, r, 0, r.length);
            i = fin;
            return r;
        }
    }
}
