import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.DEREncodable;
import java.security.PEMDecoder;
import java.security.PEMEncoder;
import java.security.PEMRecord;
import java.security.PublicKey;

import javax.crypto.EncryptedPrivateKeyInfo;

/**
 * Comprueba {@code PEMDecoder} y {@code PEMEncoder} contra el JDK 25.
 *
 * <h2>Que se puede comparar</h2>
 *
 * <p>Leer y escribir el formato: encontrar el bloque, separar la etiqueta, conservar lo que venia
 * antes, cortar el base64 en lineas. Eso no depende de ningun proveedor y es la mayor parte de las
 * dos clases.
 *
 * <p>De las etiquetas conocidas se compara {@code ENCRYPTED PRIVATE KEY}, que es la unica cuyo
 * objeto se puede construir sin proveedores. Las demas fallan de los dos lados con la misma
 * excepcion --{@link IllegalArgumentException}-- aunque por motivos distintos: el JDK porque los
 * datos de la prueba no son un certificado de verdad, esta biblioteca porque no hay fabrica. Se
 * comparan igual, y la nota queda escrita aca para que nadie lea coincidencia donde hay dos caminos.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class PEM1 {

    static final String[] ESPERADO = {
        "unico|true|true",
        "inmutable|false|false",
        "with-nulo|NullPointerException|NullPointerException|NullPointerException",
        "clase|true",
        "registro|COSA RARA|AQIDBA==|true",
        "vuelta|-----BEGIN COSA RARA-----\\r\\nAQIDBA==\\r\\n-----END COSA RARA-----\\r\\n",
        "bytes|true",
        "antes|basura antes\\r\\n|-----BEGIN COSA RARA-----\\r\\nAQIDBA==\\r\\n-----END COSA RARA-----\\r\\n",
        "corte|-----BEGIN X-----\\r\\nABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKL\\r\\nMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUV\\r\\n-----END X-----\\r\\n",
        "limpieza|AQIDBA==",
        "flujo|COSA RARA|AQIDBA==",
        "epki-escrito|-----BEGIN ENCRYPTED PRIVATE KEY-----\\r\\nMBIwCwYJKoZIhvcNAQUDBAMBAgM=\\r\\n-----END ENCRYPTED PRIVATE KEY-----\\r\\n",
        "epki-leido|true|PBEWithMD5AndDES|010203",
        "epki-clase|PBEWithMD5AndDES",
        "epki-como-registro|ENCRYPTED PRIVATE KEY",
        "nulo|NullPointerException",
        "vacio|IllegalArgumentException",
        "sin-bloque|IllegalArgumentException",
        "sin-cierre|IllegalArgumentException",
        "encode-nulo|NullPointerException",
        "clase-mala|ClassCastException",
        "cert|IllegalArgumentException",
        "publica|IllegalArgumentException",
    };

    /** Lo que hacen las dos clases, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final String nl = System.lineSeparator();

        final PEMDecoder d = PEMDecoder.of();
        final PEMEncoder e = PEMEncoder.of();
        a.add("unico|" + (PEMDecoder.of() == d) + "|" + (PEMEncoder.of() == e));
        a.add("inmutable|" + (d.withDecryption(new char[] {'x'}) == d)
                + "|" + (e.withEncryption(new char[] {'x'}) == e));
        a.add("with-nulo|" + intentar(new ConFabrica(d)) + "|" + intentar(new ConClave(d))
                + "|" + intentar(new ConCifrado(e)));

        // Un bloque de etiqueta desconocida vuelve como registro.
        final String pem = "-----BEGIN COSA RARA-----" + nl + "AQIDBA==" + nl
                + "-----END COSA RARA-----" + nl;
        final DEREncodable x = d.decode(pem);
        a.add("clase|" + (x instanceof PEMRecord));
        final PEMRecord r = d.decode(pem, PEMRecord.class);
        a.add("registro|" + r.type() + "|" + r.content() + "|" + (r.leadingData() == null));
        a.add("vuelta|" + escapar(e.encodeToString(r)));
        a.add("bytes|" + new String(e.encode(r), StandardCharsets.UTF_8)
                .equals(e.encodeToString(r)));

        // Lo que viene antes del bloque se conserva y no se vuelve a escribir.
        final PEMRecord r2 = d.decode("basura antes" + nl + pem, PEMRecord.class);
        a.add("antes|" + escapar(new String(r2.leadingData(), StandardCharsets.UTF_8))
                + "|" + escapar(e.encodeToString(r2)));

        // Un cuerpo largo se corta en lineas de sesenta y cuatro.
        final StringBuilder largo = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largo.append((char) ('A' + i % 26));
        }
        a.add("corte|" + escapar(new PEMRecord("X", largo.toString()).toString()));

        // Los saltos y los espacios del cuerpo se descartan al leer.
        final PEMRecord r3 = d.decode("-----BEGIN Y-----" + nl + "AQ  ID" + nl + "BA==" + nl
                + "-----END Y-----" + nl, PEMRecord.class);
        a.add("limpieza|" + r3.content());

        // Leer de un flujo da lo mismo que leer de un texto.
        final PEMRecord r4 = d.decode(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)), PEMRecord.class);
        a.add("flujo|" + r4.type() + "|" + r4.content());

        // La unica etiqueta conocida que se puede construir sin proveedores.
        final EncryptedPrivateKeyInfo epki =
                new EncryptedPrivateKeyInfo("PBEWithMD5AndDES", new byte[] {1, 2, 3});
        final String texto = e.encodeToString(epki);
        a.add("epki-escrito|" + escapar(texto));
        final DEREncodable leido = d.decode(texto);
        a.add("epki-leido|" + (leido instanceof EncryptedPrivateKeyInfo) + "|"
                + ((EncryptedPrivateKeyInfo) leido).getAlgName() + "|"
                + hex(((EncryptedPrivateKeyInfo) leido).getEncryptedData()));
        a.add("epki-clase|" + d.decode(texto, EncryptedPrivateKeyInfo.class).getAlgName());
        a.add("epki-como-registro|" + d.decode(texto, PEMRecord.class).type());

        // Los errores.
        a.add("nulo|" + intentar(new Decode(d, null)));
        a.add("vacio|" + intentar(new Decode(d, "")));
        a.add("sin-bloque|" + intentar(new Decode(d, "no es pem")));
        a.add("sin-cierre|" + intentar(new Decode(d, "-----BEGIN X-----" + nl + "AQID" + nl)));
        a.add("encode-nulo|" + intentar(new Encode(e)));
        a.add("clase-mala|" + intentar(new DecodeClase(d, pem)));
        a.add("cert|" + intentar(new Decode(d,
                "-----BEGIN CERTIFICATE-----" + nl + "AQID" + nl + "-----END CERTIFICATE-----" + nl)));
        a.add("publica|" + intentar(new Decode(d,
                "-----BEGIN PUBLIC KEY-----" + nl + "AQID" + nl + "-----END PUBLIC KEY-----" + nl)));

        return a.toArray(new String[a.size()]);
    }

    /** Los saltos de linea escritos, para que la comparacion no dependa del sistema. */
    static String escapar(String s) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** Los bytes en hexadecimal. */
    static String hex(byte[] b) {
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            final int v = b[i] & 0xff;
            s.append("0123456789abcdef".charAt(v >> 4));
            s.append("0123456789abcdef".charAt(v & 0xf));
        }
        return s.toString();
    }

    /** Algo que se corre para ver con que falla. */
    interface Tiro {
        void correr() throws Exception;
    }

    /** Corre eso y devuelve "ok" o el nombre simple de lo que haya tirado. */
    static String intentar(Tiro r) {
        try {
            r.correr();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class ConFabrica implements Tiro {
        private final PEMDecoder d;

        ConFabrica(PEMDecoder d) {
            this.d = d;
        }

        public void correr() throws Exception {
            d.withFactory(null);
        }
    }

    static class ConClave implements Tiro {
        private final PEMDecoder d;

        ConClave(PEMDecoder d) {
            this.d = d;
        }

        public void correr() throws Exception {
            d.withDecryption(null);
        }
    }

    static class ConCifrado implements Tiro {
        private final PEMEncoder e;

        ConCifrado(PEMEncoder e) {
            this.e = e;
        }

        public void correr() throws Exception {
            e.withEncryption(null);
        }
    }

    static class Decode implements Tiro {
        private final PEMDecoder d;
        private final String s;

        Decode(PEMDecoder d, String s) {
            this.d = d;
            this.s = s;
        }

        public void correr() throws Exception {
            d.decode(this.s);
        }
    }

    static class DecodeClase implements Tiro {
        private final PEMDecoder d;
        private final String s;

        DecodeClase(PEMDecoder d, String s) {
            this.d = d;
            this.s = s;
        }

        public void correr() throws Exception {
            d.decode(this.s, PublicKey.class);
        }
    }

    static class Encode implements Tiro {
        private final PEMEncoder e;

        Encode(PEMEncoder e) {
            this.e = e;
        }

        public void correr() throws Exception {
            e.encodeToString(null);
        }
    }

    /**
     * El indice de la primera respuesta que no coincide con la del JDK, o -1.
     *
     * @return el indice, o -1
     */
    public static int donde() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != ESPERADO.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = donde();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
