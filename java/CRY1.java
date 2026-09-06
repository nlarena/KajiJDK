import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherSpi;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.Mac;
import javax.crypto.MacSpi;
import javax.crypto.NullCipher;
import javax.crypto.SealedObject;
import javax.crypto.ShortBufferException;

/**
 * Comprueba {@code javax.crypto} contra el JDK 25.
 *
 * <h2>Que se puede comparar</h2>
 *
 * <p>Dos cosas. {@link NullCipher}, que es el unico cifrador que las dos partes tienen de fabrica, y
 * con el los flujos y el objeto sellado; y la lectura y escritura de
 * {@link EncryptedPrivateKeyInfo}, que es DER y no depende de ningun algoritmo.
 *
 * <p>Lo que no se compara es pedir un algoritmo de verdad: el JDK trae proveedores con AES y esta
 * biblioteca no trae ninguno, a proposito --registrar un servicio que no se puede cumplir seria peor
 * que no tenerlo--.
 *
 * <p>La maquinaria de proveedores se comprueba aparte, en {@link #maquinaria()}, porque el JDK no
 * deja correrla: ver la nota de ese metodo.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class CRY1 {

    static final String[] ESPERADO = {
        "modos|1234123",
        "nulo|null|null|1|7|0000000000000000|null|null",
        "nulo-toString|Cipher.null, mode: not initialized, algorithm from: (no provider)",
        "update|0102030405|020304",
        "update-arr|5|01020304050000000000",
        "doFinal|nulo|0102030405|0203",
        "doFinal-arr|5|00000102030405000000",
        "buf-update|5|01020304050000000000",
        "buf-doFinal|5|01020304050000000000",
        "nulo-wrap|UnsupportedOperationException|UnsupportedOperationException|UnsupportedOperationException",
        "maxlen|2147483647|2147483647|null",
        "maxlen-nulo|NullPointerException",
        "gi-nulo|NoSuchAlgorithmException",
        "gi-vacio|NoSuchAlgorithmException",
        "gi-2partes|NoSuchAlgorithmException",
        "gi-4partes|NoSuchAlgorithmException",
        "gi-prov-nulo|IllegalArgumentException",
        "gi-prov-raro|NoSuchProviderException",
        "salida|4142434546",
        "entrada|65|4|4243454600000000|-1|false",
        "sellado|null|hola sellado",
        "epki|PBEWithMD5AndDES|010203|null|3012300b06092a864886f70d0105030403010203",
        "epki-raro|1.3.6.1.4|090807|300d300606042b0601040403090807",
        "oid|PBEWithMD5AndDES|3010300b06092a864886f70d010503040101|PBEWithMD5AndDES",
        "oid|PBEWithSHA1AndDESede|3011300c060a2a864886f70d010c0103040101|PBEWithSHA1AndDESede",
        "oid|PBEWithSHA1AndRC2_40|3011300c060a2a864886f70d010c0106040101|PBEWithSHA1AndRC2_40",
        "oid|PBEWithSHA1AndRC2_128|3011300c060a2a864886f70d010c0105040101|PBEWithSHA1AndRC2_128",
        "oid|PBEWithSHA1AndRC4_40|3011300c060a2a864886f70d010c0102040101|PBEWithSHA1AndRC4_40",
        "oid|PBEWithSHA1AndRC4_128|3011300c060a2a864886f70d010c0101040101|PBEWithSHA1AndRC4_128",
        "oid|PBES2|3010300b06092a864886f70d01050d040101|PBES2",
        "oid|PBKDF2WithHmacSHA1|3010300b06092a864886f70d01050c040101|PBKDF2WithHmacSHA1",
        "oid|AES|300f300a06086086480165030401040101|AES",
        "oid|AES_128/CBC/NoPadding|3010300b0609608648016503040102040101|AES_128/CBC/NoPadding",
        "oid|AES_256/CBC/NoPadding|3010300b060960864801650304012a040101|AES_256/CBC/NoPadding",
        "oid|DESede|300c300706052b0e030211040101|DESede",
        "oid|Blowfish|3011300c060a2b060104019755010102040101|Blowfish",
        "oid|HmacSHA256|300f300a06082a864886f70d0209040101|HmacSHA256",
        "oid|EC|300e300906072a8648ce3d0201040101|EC",
        "oid|DiffieHellman|3010300b06092a864886f70d010301040101|DiffieHellman",
        "oid|1.3.6.1.4.1.99999|300f300a06082b06010401868d1f040101|1.3.6.1.4.1.99999",
        "epki-raro-nombre|NoSuchAlgorithmException",
        "epki-nulo|NullPointerException",
        "epki-vacio|IllegalArgumentException",
        "epki-corto|EOFException",
        "epki-copia|true",
        "epki-keyspec|InvalidKeySpecException",
        "jerarquia|BadPaddingException,GeneralSecurityException,|GeneralSecurityException,|GeneralSecurityException,",
    };

    /** Un cifrado de juguete: cada byte contra la misma mascara. */
    public static class XorSpi extends CipherSpi {
        static String visto = "";

        @Override
        protected void engineSetMode(String mode) {
            visto = visto + "modo:" + mode + ";";
        }

        @Override
        protected void engineSetPadding(String padding) {
            visto = visto + "relleno:" + padding + ";";
        }

        @Override
        protected int engineGetBlockSize() {
            return 4;
        }

        @Override
        protected int engineGetOutputSize(int inputLen) {
            return inputLen;
        }

        @Override
        protected byte[] engineGetIV() {
            return null;
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            return null;
        }

        @Override
        protected void engineInit(int opmode, Key key, SecureRandom random) {
            visto = visto + "init:" + opmode + ";";
        }

        @Override
        protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params,
                SecureRandom random) {
            engineInit(opmode, key, random);
        }

        @Override
        protected void engineInit(int opmode, Key key, AlgorithmParameters params,
                SecureRandom random) {
            engineInit(opmode, key, random);
        }

        @Override
        protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
            if (input == null) {
                return null;
            }
            final byte[] out = new byte[inputLen];
            for (int i = 0; i < inputLen; i++) {
                out[i] = (byte) (input[inputOffset + i] ^ 0x5a);
            }
            return out;
        }

        @Override
        protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output,
                int outputOffset) throws ShortBufferException {
            final byte[] r = engineUpdate(input, inputOffset, inputLen);
            if (r == null) {
                return 0;
            }
            if (output.length - outputOffset < r.length) {
                throw new ShortBufferException("chico");
            }
            System.arraycopy(r, 0, output, outputOffset, r.length);
            return r.length;
        }

        @Override
        protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) {
            return engineUpdate(input, inputOffset, inputLen);
        }

        @Override
        protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
                int outputOffset) throws ShortBufferException {
            return engineUpdate(input, inputOffset, inputLen, output, outputOffset);
        }
    }

    /** Un codigo de autenticacion de juguete: la suma de los bytes. */
    public static class SumaSpi extends MacSpi {
        private int suma;

        @Override
        protected int engineGetMacLength() {
            return 1;
        }

        @Override
        protected void engineInit(Key key, AlgorithmParameterSpec params) {
            this.suma = 0;
        }

        @Override
        protected void engineUpdate(byte input) {
            this.suma += input;
        }

        @Override
        protected void engineUpdate(byte[] input, int offset, int len) {
            for (int i = 0; i < len; i++) {
                this.suma += input[offset + i];
            }
        }

        @Override
        protected byte[] engineDoFinal() {
            return new byte[] {(byte) this.suma};
        }

        @Override
        protected void engineReset() {
            this.suma = 0;
        }
    }

    /** Un servicio que construye sin reflexion, para no depender de como se cargan las clases. */
    static class Serv extends Provider.Service {
        Serv(Provider p, String tipo, String alg, String clase) {
            super(p, tipo, alg, clase, new ArrayList<String>(), new HashMap<String, String>());
        }

        @Override
        public Object newInstance(Object constructorParameter) {
            return "Cipher".equals(getType()) ? (Object) new XorSpi() : (Object) new SumaSpi();
        }
    }

    /** Un proveedor con dos servicios de juguete. */
    static class ProvPrueba extends Provider {
        private static final long serialVersionUID = 1L;

        ProvPrueba() {
            super("PruebaKaji", "1.0", "dos servicios de juguete");
            putService(new Serv(this, "Cipher", "XOR", "CRY1$XorSpi"));
            putService(new Serv(this, "Mac", "SUMA", "CRY1$SumaSpi"));
        }
    }

    /** Lo que hace el paquete, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        a.add("modos|" + Cipher.ENCRYPT_MODE + Cipher.DECRYPT_MODE + Cipher.WRAP_MODE
                + Cipher.UNWRAP_MODE + Cipher.PUBLIC_KEY + Cipher.PRIVATE_KEY + Cipher.SECRET_KEY);

        // El cifrador que no cifra.
        final Cipher n = new NullCipher();
        a.add("nulo|" + n.getAlgorithm() + "|" + n.getProvider() + "|" + n.getBlockSize()
                + "|" + n.getOutputSize(7) + "|" + hex(n.getIV()) + "|" + n.getParameters()
                + "|" + n.getExemptionMechanism());
        a.add("nulo-toString|" + n);
        final byte[] d = {1, 2, 3, 4, 5};
        a.add("update|" + hex(n.update(d)) + "|" + hex(n.update(d, 1, 3)));
        final byte[] o = new byte[10];
        a.add("update-arr|" + n.update(d, 0, 5, o) + "|" + hex(o));
        a.add("doFinal|" + hex(n.doFinal()) + "|" + hex(n.doFinal(d)) + "|" + hex(n.doFinal(d, 1, 2)));
        final byte[] o2 = new byte[10];
        a.add("doFinal-arr|" + n.doFinal(d, 0, 5, o2, 2) + "|" + hex(o2));
        final ByteBuffer bi = ByteBuffer.wrap(d);
        final ByteBuffer bo = ByteBuffer.allocate(10);
        a.add("buf-update|" + n.update(bi, bo) + "|" + hex(bo.array()));
        bi.rewind();
        bo.clear();
        a.add("buf-doFinal|" + n.doFinal(bi, bo) + "|" + hex(bo.array()));
        a.add("nulo-wrap|" + intentar(new Wrap(n)) + "|" + intentar(new Unwrap(n))
                + "|" + intentar(new Aad(n)));

        // La politica de exportacion no limita nada.
        a.add("maxlen|" + Cipher.getMaxAllowedKeyLength("AES")
                + "|" + Cipher.getMaxAllowedKeyLength("NoExiste")
                + "|" + Cipher.getMaxAllowedParameterSpec("AES"));
        a.add("maxlen-nulo|" + intentar(new MaxNulo()));

        // Los nombres mal formados y los proveedores que no estan.
        a.add("gi-nulo|" + intentar(new Gi(null)));
        a.add("gi-vacio|" + intentar(new Gi("")));
        a.add("gi-2partes|" + intentar(new Gi("a/b")));
        a.add("gi-4partes|" + intentar(new Gi("a/b/c/d")));
        a.add("gi-prov-nulo|" + intentar(new GiProv("AES", null)));
        a.add("gi-prov-raro|" + intentar(new GiProv("AES", "NoExisteJamas")));

        // Los flujos, con el cifrador que no cifra.
        final ByteArrayOutputStream salida = new ByteArrayOutputStream();
        final CipherOutputStream cos = new CipherOutputStream(salida, new NullCipher());
        cos.write(65);
        cos.write(new byte[] {66, 67});
        cos.write(new byte[] {68, 69, 70}, 1, 2);
        cos.flush();
        cos.close();
        a.add("salida|" + hex(salida.toByteArray()));
        final CipherInputStream cis =
                new CipherInputStream(new ByteArrayInputStream(salida.toByteArray()),
                        new NullCipher());
        final int primero = cis.read();
        final byte[] resto = new byte[8];
        final int leidos = cis.read(resto, 0, 8);
        final int fin = cis.read();
        a.add("entrada|" + primero + "|" + leidos + "|" + hex(resto) + "|" + fin
                + "|" + cis.markSupported());
        cis.close();

        // El objeto sellado.
        final SealedObject sellado = new SealedObject("hola sellado", new NullCipher());
        a.add("sellado|" + sellado.getAlgorithm() + "|"
                + sellado.getObject(new NullCipher()));

        // La estructura de la clave privada cifrada.
        final byte[] der = {
            0x30, 0x12,
            0x30, 0x0b, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d,
            0x01, 0x05, 0x03,
            0x04, 0x03, 0x01, 0x02, 0x03};
        final EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(der);
        a.add("epki|" + epki.getAlgName() + "|" + hex(epki.getEncryptedData())
                + "|" + epki.getAlgParameters() + "|" + hex(epki.getEncoded()));
        final byte[] raro = {
            0x30, 0x0d,
            0x30, 0x06, 0x06, 0x04, 0x2b, 0x06, 0x01, 0x04,
            0x04, 0x03, 0x09, 0x08, 0x07};
        final EncryptedPrivateKeyInfo epki2 = new EncryptedPrivateKeyInfo(raro);
        a.add("epki-raro|" + epki2.getAlgName() + "|" + hex(epki2.getEncryptedData())
                + "|" + hex(epki2.getEncoded()));
        final String[] nombres = {
            "PBEWithMD5AndDES", "PBEWithSHA1AndDESede", "PBEWithSHA1AndRC2_40",
            "PBEWithSHA1AndRC2_128", "PBEWithSHA1AndRC4_40", "PBEWithSHA1AndRC4_128",
            "PBES2", "PBKDF2WithHmacSHA1", "AES", "AES_128/CBC/NoPadding",
            "AES_256/CBC/NoPadding", "DESede", "Blowfish", "HmacSHA256", "EC",
            "DiffieHellman", "1.3.6.1.4.1.99999"};
        for (int i = 0; i < nombres.length; i++) {
            final EncryptedPrivateKeyInfo e =
                    new EncryptedPrivateKeyInfo(nombres[i], new byte[] {1});
            a.add("oid|" + nombres[i] + "|" + hex(e.getEncoded()) + "|"
                    + new EncryptedPrivateKeyInfo(e.getEncoded()).getAlgName());
        }
        a.add("epki-raro-nombre|" + intentar(new Epki("NoExisteJamas")));
        a.add("epki-nulo|" + intentar(new EpkiNulo()));
        a.add("epki-vacio|" + intentar(new EpkiVacio()));
        a.add("epki-corto|" + intentar(new EpkiBytes(new byte[] {1, 2, 3})));
        a.add("epki-copia|" + (epki.getEncryptedData() != epki.getEncryptedData()));
        a.add("epki-keyspec|" + intentar(new KeySpecNulo(epki)));

        // Las jerarquias de excepciones.
        a.add("jerarquia|" + padres(javax.crypto.AEADBadTagException.class)
                + "|" + padres(javax.crypto.IllegalBlockSizeException.class)
                + "|" + padres(javax.crypto.DecapsulateException.class));

        return a.toArray(new String[a.size()]);
    }

    /**
     * Comprueba la maquinaria de proveedores con un proveedor propio.
     *
     * <p>Esto no se puede comparar contra el JDK: {@code javax.crypto} del JDK exige que un
     * proveedor que ofrezca cifrados venga en un archivo firmado con un certificado de Oracle, y
     * tira {@code SecurityException} con cualquier otro. Es lo que queda de las restricciones de
     * exportacion, igual que {@link javax.crypto.ExemptionMechanism}, y esta biblioteca no lo
     * copia: la comprobacion depende de unos certificados concretos y no protege de nada aca.
     *
     * <p>Lo que se comprueba es que {@code getInstance} encuentre el servicio, le fije el modo y el
     * relleno, y le delegue todo. Los valores esperados no salen del JDK sino de la definicion del
     * cifrado de juguete, que es de esta misma prueba.
     *
     * @return el indice de la primera respuesta que no coincide, o -1
     */
    public static int maquinaria() {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final String[] esperado = {
            "prov|XOR/CBC/PKCS5Padding|PruebaKaji|4|modo:CBC;relleno:PKCS5Padding;",
            "prov-toString|Cipher.XOR/CBC/PKCS5Padding, mode: not initialized,"
                    + " algorithm from: PruebaKaji",
            "prov-init|modo:CBC;relleno:PKCS5Padding;init:1;|Cipher.XOR/CBC/PKCS5Padding,"
                    + " mode: initialized, algorithm from: PruebaKaji",
            "prov-cifra|5b5859",
            "prov-descifra|010203",
            "prov-mac|SUMA|PruebaKaji|1",
            "prov-mac-sin-init|IllegalStateException",
            "prov-mac-valor|3c|03",
            "flujo-xor|1215161b",
            "flujo-xor-vuelta|4|484f4c41",
        };
        try {
            // La maquinaria de proveedores, con un proveedor propio.
            Security.addProvider(new ProvPrueba());
            XorSpi.visto = "";
            final Cipher x = Cipher.getInstance("XOR/CBC/PKCS5Padding", "PruebaKaji");
            a.add("prov|" + x.getAlgorithm() + "|" + x.getProvider().getName()
                    + "|" + x.getBlockSize() + "|" + XorSpi.visto);
            a.add("prov-toString|" + x);
            x.init(Cipher.ENCRYPT_MODE, (Key) null);
            a.add("prov-init|" + XorSpi.visto + "|" + x);
            final byte[] cifrado = x.doFinal(new byte[] {1, 2, 3});
            a.add("prov-cifra|" + hex(cifrado));
            final Cipher x2 = Cipher.getInstance("XOR");
            x2.init(Cipher.DECRYPT_MODE, (Key) null);
            a.add("prov-descifra|" + hex(x2.doFinal(cifrado)));
            final Mac m = Mac.getInstance("SUMA");
            a.add("prov-mac|" + m.getAlgorithm() + "|" + m.getProvider().getName()
                    + "|" + m.getMacLength());
            a.add("prov-mac-sin-init|" + intentar(new MacSinInit(m)));
            m.init(null);
            m.update((byte) 10);
            m.update(new byte[] {20, 30});
            a.add("prov-mac-valor|" + hex(m.doFinal()) + "|" + hex(m.doFinal(new byte[] {1, 2})));

            // Los flujos y el objeto sellado tambien con el cifrador de juguete.
            final ByteArrayOutputStream s2 = new ByteArrayOutputStream();
            final Cipher xe = Cipher.getInstance("XOR");
            xe.init(Cipher.ENCRYPT_MODE, (Key) null);
            final CipherOutputStream cos2 = new CipherOutputStream(s2, xe);
            cos2.write(new byte[] {72, 79, 76, 65});
            cos2.close();
            a.add("flujo-xor|" + hex(s2.toByteArray()));
            final Cipher xd = Cipher.getInstance("XOR");
            xd.init(Cipher.DECRYPT_MODE, (Key) null);
            final CipherInputStream cis2 =
                    new CipherInputStream(new ByteArrayInputStream(s2.toByteArray()), xd);
            final byte[] vuelta = new byte[4];
            final int n2 = cis2.read(vuelta, 0, 4);
            cis2.close();
            a.add("flujo-xor-vuelta|" + n2 + "|" + hex(vuelta));
        } catch (Throwable t) {
            return 9000;
        }
        if (a.size() != esperado.length) {
            return 8000 + a.size();
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(esperado[i])) {
                return i;
            }
        }
        return -1;
    }

    /** La cadena de superclases hasta {@code Exception}, con nombres simples. */
    static String padres(Class<?> c) {
        final StringBuilder b = new StringBuilder();
        Class<?> p = c.getSuperclass();
        while (p != null && !p.getName().equals("java.lang.Exception")) {
            b.append(p.getSimpleName()).append(',');
            p = p.getSuperclass();
        }
        return b.toString();
    }

    /** Los bytes en hexadecimal, o {@code "nulo"}. */
    static String hex(byte[] b) {
        if (b == null) {
            return "nulo";
        }
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
            final String nn = t.getClass().getName();
            return nn.substring(nn.lastIndexOf('.') + 1);
        }
    }

    static class Wrap implements Tiro {
        private final Cipher c;

        Wrap(Cipher c) {
            this.c = c;
        }

        public void correr() throws Exception {
                c.wrap(null);
        }
    }

    static class Unwrap implements Tiro {
        private final Cipher c;

        Unwrap(Cipher c) {
            this.c = c;
        }

        public void correr() throws Exception {
                c.unwrap(new byte[] {1}, "AES", Cipher.SECRET_KEY);
        }
    }

    static class Aad implements Tiro {
        private final Cipher c;

        Aad(Cipher c) {
            this.c = c;
        }

        public void correr() throws Exception {
            c.updateAAD(new byte[] {1});
        }
    }

    static class MaxNulo implements Tiro {
        public void correr() throws Exception {
                Cipher.getMaxAllowedKeyLength(null);
        }
    }

    static class Gi implements Tiro {
        private final String t;

        Gi(String t) {
            this.t = t;
        }

        public void correr() throws Exception {
                Cipher.getInstance(t);
        }
    }

    static class GiProv implements Tiro {
        private final String t;
        private final String p;

        GiProv(String t, String p) {
            this.t = t;
            this.p = p;
        }

        public void correr() throws Exception {
                Cipher.getInstance(t, p);
        }
    }

    static class Epki implements Tiro {
        private final String nombre;

        Epki(String nombre) {
            this.nombre = nombre;
        }

        public void correr() throws Exception {
                new EncryptedPrivateKeyInfo(this.nombre, new byte[] {1});
        }
    }

    static class EpkiNulo implements Tiro {
        public void correr() throws Exception {
                new EncryptedPrivateKeyInfo((byte[]) null);
        }
    }

    static class EpkiVacio implements Tiro {
        public void correr() throws Exception {
                new EncryptedPrivateKeyInfo("PBEWithMD5AndDES", new byte[0]);
        }
    }

    static class EpkiBytes implements Tiro {
        private final byte[] b;

        EpkiBytes(byte[] b) {
            this.b = b;
        }

        public void correr() throws Exception {
                new EncryptedPrivateKeyInfo(this.b);
        }
    }

    static class KeySpecNulo implements Tiro {
        private final EncryptedPrivateKeyInfo e;

        KeySpecNulo(EncryptedPrivateKeyInfo e) {
            this.e = e;
        }

        public void correr() throws Exception {
                e.getKeySpec(new NullCipher());
        }
    }

    static class MacSinInit implements Tiro {
        private final Mac m;

        MacSinInit(Mac m) {
            this.m = m;
        }

        public void correr() throws Exception {
            m.update((byte) 1);
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
