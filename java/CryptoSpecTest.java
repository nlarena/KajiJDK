import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.DHGenParameterSpec;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.RC5ParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * `javax.crypto.spec`: las especificaciones de clave y de parametros.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo **sus** clases, asi que los numeros
 * que se esperan los dicta el JDK y no yo.
 *
 * <p>Lo que se comprueba de verdad: la paridad y las claves debiles de DES --que son tablas
 * concretas y es donde una transcripcion a mano se equivoca--, que las copias defensivas sean de
 * verdad defensivas en las dos direcciones, que `clearPassword` borre y que despues no se pueda
 * leer, y que `SecretKeySpec.equals` ignore mayusculas en el algoritmo.
 */
public class CryptoSpecTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    static byte[] bytes(int... vals) {
        byte[] out = new byte[vals.length];
        for (int i = 0; i < vals.length; i++) {
            out[i] = (byte) vals[i];
        }
        return out;
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- IvParameterSpec: la copia va en las dos direcciones
        byte[] iv = bytes(1, 2, 3, 4);
        IvParameterSpec ivs = new IvParameterSpec(iv);
        iv[0] = 99;
        ok("cambiar el arreglo de entrada no cambia el IV guardado", ivs.getIV()[0] == 1);
        byte[] salido = ivs.getIV();
        salido[0] = 88;
        ok("cambiar lo que devolvio getIV tampoco lo cambia", ivs.getIV()[0] == 1);
        ok("y getIV devuelve un arreglo nuevo cada vez", ivs.getIV() != ivs.getIV());

        IvParameterSpec tramo = new IvParameterSpec(bytes(9, 1, 2, 3, 9), 1, 3);
        ok("el constructor con tramo toma solo ese tramo",
                Arrays.equals(tramo.getIV(), bytes(1, 2, 3)));

        boolean corto = false;
        try {
            new IvParameterSpec(bytes(1, 2), 1, 5);
        } catch (IllegalArgumentException e) {
            corto = true;
        }
        ok("un tramo que no entra es IllegalArgument", corto);

        // ---- SecretKeySpec
        SecretKeySpec k1 = new SecretKeySpec(bytes(1, 2, 3, 4), "AES");
        ok("el formato es RAW", "RAW".equals(k1.getFormat()));
        ok("el algoritmo se conserva tal cual", "AES".equals(k1.getAlgorithm()));
        ok("los bytes se recuperan", Arrays.equals(k1.getEncoded(), bytes(1, 2, 3, 4)));

        SecretKeySpec k2 = new SecretKeySpec(bytes(1, 2, 3, 4), "aes");
        ok("el algoritmo se compara sin distinguir mayusculas", k1.equals(k2));
        ok("y el hashCode coincide", k1.hashCode() == k2.hashCode());

        SecretKeySpec k3 = new SecretKeySpec(bytes(1, 2, 3, 5), "AES");
        ok("distintos bytes, distinta clave", !k1.equals(k3));
        SecretKeySpec k4 = new SecretKeySpec(bytes(1, 2, 3, 4), "DES");
        ok("distinto algoritmo, distinta clave", !k1.equals(k4));

        boolean vacia = false;
        try {
            new SecretKeySpec(new byte[0], "AES");
        } catch (IllegalArgumentException e) {
            vacia = true;
        }
        ok("una clave vacia es IllegalArgument", vacia);

        boolean sinAlgoritmo = false;
        try {
            new SecretKeySpec(bytes(1), null);
        } catch (IllegalArgumentException e) {
            sinAlgoritmo = true;
        }
        ok("sin algoritmo es IllegalArgument", sinAlgoritmo);

        SecretKeySpec kt = new SecretKeySpec(bytes(9, 1, 2, 9), 1, 2, "AES");
        ok("el constructor con tramo toma solo ese tramo",
                Arrays.equals(kt.getEncoded(), bytes(1, 2)));

        // ---- DESKeySpec: paridad
        //
        // 0x01 tiene un solo bit en uno: paridad impar, ajustada. 0x00 no tiene ninguno: par.
        ok("una clave de ochos 0x01 esta ajustada",
                DESKeySpec.isParityAdjusted(bytes(1, 1, 1, 1, 1, 1, 1, 1), 0));
        ok("una de ceros no lo esta",
                !DESKeySpec.isParityAdjusted(bytes(0, 0, 0, 0, 0, 0, 0, 0), 0));
        ok("0xFE tiene siete unos: impar, ajustada",
                DESKeySpec.isParityAdjusted(
                        bytes(0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE), 0));
        ok("0xFF tiene ocho: par, no ajustada",
                !DESKeySpec.isParityAdjusted(
                        bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF), 0));
        ok("la paridad se mira desde el offset",
                DESKeySpec.isParityAdjusted(bytes(0, 1, 1, 1, 1, 1, 1, 1, 1), 1));

        // ---- DESKeySpec: claves debiles
        ok("la clave de todos 0x01 es debil",
                DESKeySpec.isWeak(bytes(1, 1, 1, 1, 1, 1, 1, 1), 0));
        ok("la de todos 0xFE es debil",
                DESKeySpec.isWeak(bytes(0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE, 0xFE), 0));
        ok("E0E0E0E0F1F1F1F1 es debil",
                DESKeySpec.isWeak(bytes(0xE0, 0xE0, 0xE0, 0xE0, 0xF1, 0xF1, 0xF1, 0xF1), 0));
        ok("1F1F1F1F0E0E0E0E es debil",
                DESKeySpec.isWeak(bytes(0x1F, 0x1F, 0x1F, 0x1F, 0x0E, 0x0E, 0x0E, 0x0E), 0));
        ok("01FE01FE01FE01FE es semidebil",
                DESKeySpec.isWeak(bytes(0x01, 0xFE, 0x01, 0xFE, 0x01, 0xFE, 0x01, 0xFE), 0));
        ok("FE01FE01FE01FE01 es su par",
                DESKeySpec.isWeak(bytes(0xFE, 0x01, 0xFE, 0x01, 0xFE, 0x01, 0xFE, 0x01), 0));
        ok("1FE01FE00EF10EF1 es semidebil",
                DESKeySpec.isWeak(bytes(0x1F, 0xE0, 0x1F, 0xE0, 0x0E, 0xF1, 0x0E, 0xF1), 0));
        ok("011F011F010E010E es semidebil",
                DESKeySpec.isWeak(bytes(0x01, 0x1F, 0x01, 0x1F, 0x01, 0x0E, 0x01, 0x0E), 0));
        ok("E0FEE0FEF1FEF1FE es semidebil",
                DESKeySpec.isWeak(bytes(0xE0, 0xFE, 0xE0, 0xFE, 0xF1, 0xFE, 0xF1, 0xFE), 0));

        // Una clave cualquiera no es debil.
        ok("una clave normal no es debil",
                !DESKeySpec.isWeak(bytes(0x13, 0x34, 0x57, 0x79, 0x9B, 0xBC, 0xDF, 0xF1), 0));
        // El bit de paridad SI cuenta, y es contraintuitivo: para DES, 0x00 y 0x01 son la misma
        // clave --se diferencian solo en el bit que el algoritmo ignora-- y sin embargo la de unos
        // es debil y la de ceros no. Esta comprobacion afirmaba lo contrario y el oraculo la
        // corrigio: `isWeak` responde por la lista del estandar, que esta publicada con la paridad
        // ajustada, y no por la clase de equivalencia.
        ok("la de todos ceros NO figura como debil",
                !DESKeySpec.isWeak(bytes(0, 0, 0, 0, 0, 0, 0, 0), 0));
        ok("la de todos 0xFF tampoco",
                !DESKeySpec.isWeak(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF), 0));
        ok("cambiar un solo bit de paridad la saca de la lista",
                !DESKeySpec.isWeak(bytes(0xE1, 0xE0, 0xE0, 0xE0, 0xF1, 0xF1, 0xF1, 0xF1), 0));

        DESKeySpec des = new DESKeySpec(bytes(1, 2, 3, 4, 5, 6, 7, 8));
        ok("DESKeySpec guarda los ocho bytes",
                Arrays.equals(des.getKey(), bytes(1, 2, 3, 4, 5, 6, 7, 8)));
        ok("DES_KEY_LEN es 8", DESKeySpec.DES_KEY_LEN == 8);

        boolean cortaDes = false;
        try {
            new DESKeySpec(bytes(1, 2, 3));
        } catch (InvalidKeyException e) {
            cortaDes = true;
        }
        ok("una clave DES corta es InvalidKeyException", cortaDes);

        // ---- DESedeKeySpec
        ok("DES_EDE_KEY_LEN es 24", DESedeKeySpec.DES_EDE_KEY_LEN == 24);
        byte[] ede = new byte[24];
        Arrays.fill(ede, (byte) 0x01);
        DESedeKeySpec e3 = new DESedeKeySpec(ede);
        ok("DESedeKeySpec guarda los veinticuatro", e3.getKey().length == 24);
        ok("y la paridad de las tres se comprueba junta",
                DESedeKeySpec.isParityAdjusted(ede, 0));
        byte[] edeMalo = new byte[24];
        Arrays.fill(edeMalo, (byte) 0x01);
        edeMalo[20] = 0x00;
        ok("si la tercera esta mal, el conjunto esta mal",
                !DESedeKeySpec.isParityAdjusted(edeMalo, 0));

        boolean cortaEde = false;
        try {
            new DESedeKeySpec(new byte[10]);
        } catch (InvalidKeyException e) {
            cortaEde = true;
        }
        ok("una clave Triple DES corta es InvalidKeyException", cortaEde);

        // ---- ChaCha20
        byte[] nonce = new byte[12];
        nonce[0] = 7;
        ChaCha20ParameterSpec cc = new ChaCha20ParameterSpec(nonce, 5);
        ok("el nonce se guarda", cc.getNonce()[0] == 7);
        ok("y el contador", cc.getCounter() == 5);
        ok("el nonce tambien se copia al salir", cc.getNonce() != cc.getNonce());

        boolean nonceMalo = false;
        try {
            new ChaCha20ParameterSpec(new byte[8], 0);
        } catch (IllegalArgumentException e) {
            nonceMalo = true;
        }
        ok("un nonce que no mide doce bytes es IllegalArgument", nonceMalo);

        // El contador se interpreta sin signo, asi que un negativo es legal.
        ChaCha20ParameterSpec ccNeg = new ChaCha20ParameterSpec(new byte[12], -1);
        ok("un contador negativo es legal", ccNeg.getCounter() == -1);

        // ---- GCM
        GCMParameterSpec gcm = new GCMParameterSpec(128, bytes(1, 2, 3));
        ok("el largo de etiqueta se guarda", gcm.getTLen() == 128);
        ok("y el IV", Arrays.equals(gcm.getIV(), bytes(1, 2, 3)));

        boolean tlenMalo = false;
        try {
            new GCMParameterSpec(-1, bytes(1));
        } catch (IllegalArgumentException e) {
            tlenMalo = true;
        }
        ok("un largo de etiqueta negativo es IllegalArgument", tlenMalo);

        // ---- Diffie-Hellman
        BigInteger p = BigInteger.valueOf(23);
        BigInteger g = BigInteger.valueOf(5);
        DHParameterSpec dh = new DHParameterSpec(p, g);
        ok("sin l, l vale cero", dh.getL() == 0);
        ok("p y g se guardan", dh.getP().equals(p) && dh.getG().equals(g));
        DHParameterSpec dhl = new DHParameterSpec(p, g, 512);
        ok("con l, se guarda", dhl.getL() == 512);

        DHGenParameterSpec gen = new DHGenParameterSpec(1024, 160);
        ok("los dos tamanos se guardan",
                gen.getPrimeSize() == 1024 && gen.getExponentSize() == 160);

        DHPublicKeySpec pub = new DHPublicKeySpec(BigInteger.valueOf(8), p, g);
        ok("la clave publica lleva y, p y g",
                pub.getY().equals(BigInteger.valueOf(8)) && pub.getP().equals(p)
                        && pub.getG().equals(g));
        DHPrivateKeySpec priv = new DHPrivateKeySpec(BigInteger.valueOf(6), p, g);
        ok("la privada lleva x, p y g",
                priv.getX().equals(BigInteger.valueOf(6)) && priv.getP().equals(p));

        // ---- PBE
        PBEParameterSpec pbe = new PBEParameterSpec(bytes(1, 2, 3), 1000);
        ok("la sal se guarda", Arrays.equals(pbe.getSalt(), bytes(1, 2, 3)));
        ok("las iteraciones tambien", pbe.getIterationCount() == 1000);
        ok("sin parametros de abajo, nulo", pbe.getParameterSpec() == null);
        PBEParameterSpec pbe2 = new PBEParameterSpec(bytes(1), 10, ivs);
        ok("con parametros de abajo, se guardan", pbe2.getParameterSpec() == ivs);

        char[] pw = new char[] { 's', 'e', 'c', 'r', 'e', 't', 'o' };
        PBEKeySpec pks = new PBEKeySpec(pw, bytes(1, 2), 2048, 256);
        ok("la contrasena se recupera", Arrays.equals(pks.getPassword(), pw));
        ok("y es una copia", pks.getPassword() != pks.getPassword());
        pw[0] = 'X';
        ok("cambiar el arreglo original no la cambia", pks.getPassword()[0] == 's');
        ok("la sal se guarda", Arrays.equals(pks.getSalt(), bytes(1, 2)));
        ok("las iteraciones y el largo tambien",
                pks.getIterationCount() == 2048 && pks.getKeyLength() == 256);

        pks.clearPassword();
        boolean borrada = false;
        try {
            pks.getPassword();
        } catch (IllegalStateException e) {
            borrada = true;
        }
        ok("despues de clearPassword, getPassword tira", borrada);

        PBEKeySpec solaPw = new PBEKeySpec(new char[] { 'a' });
        ok("sin sal, getSalt es nulo", solaPw.getSalt() == null);
        ok("y las iteraciones cero", solaPw.getIterationCount() == 0);

        boolean saltVacia = false;
        try {
            new PBEKeySpec(new char[] { 'a' }, new byte[0], 10);
        } catch (IllegalArgumentException e) {
            saltVacia = true;
        }
        ok("una sal vacia es IllegalArgument", saltVacia);

        boolean iterCero = false;
        try {
            new PBEKeySpec(new char[] { 'a' }, bytes(1), 0);
        } catch (IllegalArgumentException e) {
            iterCero = true;
        }
        ok("cero iteraciones es IllegalArgument", iterCero);

        // ---- PSource y OAEP
        ok("PSpecified.DEFAULT es la etiqueta vacia",
                PSource.PSpecified.DEFAULT.getValue().length == 0);
        ok("y su algoritmo es PSpecified",
                "PSpecified".equals(PSource.PSpecified.DEFAULT.getAlgorithm()));
        PSource.PSpecified ps = new PSource.PSpecified(bytes(7, 8));
        ok("la etiqueta se guarda", Arrays.equals(ps.getValue(), bytes(7, 8)));

        ok("OAEP.DEFAULT usa SHA-1", "SHA-1".equals(OAEPParameterSpec.DEFAULT.getDigestAlgorithm()));
        ok("y MGF1", "MGF1".equals(OAEPParameterSpec.DEFAULT.getMGFAlgorithm()));
        ok("y la etiqueta vacia",
                ((PSource.PSpecified) OAEPParameterSpec.DEFAULT.getPSource())
                        .getValue().length == 0);

        boolean oaepNulo = false;
        try {
            new OAEPParameterSpec(null, "MGF1", null, PSource.PSpecified.DEFAULT);
        } catch (NullPointerException e) {
            oaepNulo = true;
        }
        ok("un digesto nulo en OAEP es NullPointerException", oaepNulo);

        // ---- RC2 y RC5
        RC2ParameterSpec rc2 = new RC2ParameterSpec(40, bytes(1, 2, 3, 4, 5, 6, 7, 8));
        ok("RC2 guarda los bits efectivos", rc2.getEffectiveKeyBits() == 40);
        ok("y el IV de ocho", rc2.getIV().length == 8);
        RC2ParameterSpec rc2b = new RC2ParameterSpec(40, bytes(1, 2, 3, 4, 5, 6, 7, 8));
        ok("dos RC2 iguales lo son", rc2.equals(rc2b));
        ok("y comparten hashCode", rc2.hashCode() == rc2b.hashCode());
        ok("uno sin IV no es igual a uno con IV", !rc2.equals(new RC2ParameterSpec(40)));
        ok("sin IV, getIV es nulo", new RC2ParameterSpec(40).getIV() == null);

        RC5ParameterSpec rc5 = new RC5ParameterSpec(1, 12, 32, new byte[8]);
        ok("RC5 guarda los tres numeros",
                rc5.getVersion() == 1 && rc5.getRounds() == 12 && rc5.getWordSize() == 32);
        ok("y el IV de dos palabras de 32 bits son ocho bytes", rc5.getIV().length == 8);
        RC5ParameterSpec rc5w = new RC5ParameterSpec(1, 12, 64, new byte[16]);
        ok("con palabras de 64 bits, el IV son dieciseis", rc5w.getIV().length == 16);
        ok("y no son iguales", !rc5.equals(rc5w));

        boolean rc5Corto = false;
        try {
            new RC5ParameterSpec(1, 12, 64, new byte[8]);
        } catch (IllegalArgumentException e) {
            rc5Corto = true;
        }
        ok("un IV de RC5 de menos de dos palabras es IllegalArgument", rc5Corto);

        // ---- HKDF
        SecretKey prk = new SecretKeySpec(bytes(1, 2, 3, 4), "Generic");
        HKDFParameterSpec.Expand exp = HKDFParameterSpec.expandOnly(prk, bytes(9), 32);
        ok("expandOnly guarda la PRK", exp.prk() == prk);
        ok("el contexto", Arrays.equals(exp.info(), bytes(9)));
        ok("y el largo", exp.length() == 32);

        boolean largoCero = false;
        try {
            HKDFParameterSpec.expandOnly(prk, null, 0);
        } catch (IllegalArgumentException e) {
            largoCero = true;
        }
        ok("un largo de cero en expandOnly es IllegalArgument", largoCero);

        HKDFParameterSpec.Extract ext = HKDFParameterSpec.ofExtract()
                .addIKM(bytes(1, 2))
                .addIKM(bytes(3))
                .addSalt(bytes(4))
                .extractOnly();
        ok("el material se acumula en vez de reemplazarse", ext.ikms().size() == 2);
        ok("y la sal tambien", ext.salts().size() == 1);

        // Un arreglo vacio no agrega nada: concatenar cero bytes no cambia la entrada.
        HKDFParameterSpec.Extract extVacio = HKDFParameterSpec.ofExtract()
                .addIKM(bytes(1))
                .addIKM(new byte[0])
                .extractOnly();
        ok("un aporte vacio no se agrega", extVacio.ikms().size() == 1);

        HKDFParameterSpec.ExtractThenExpand ete = HKDFParameterSpec.ofExtract()
                .addIKM(bytes(1))
                .thenExpand(bytes(5, 6), 16);
        ok("extract-then-expand conserva las dos partes",
                ete.ikms().size() == 1 && ete.length() == 16
                        && Arrays.equals(ete.info(), bytes(5, 6)));

        List<SecretKey> ikms = ext.ikms();
        boolean deSoloLectura = false;
        try {
            ikms.add(prk);
        } catch (UnsupportedOperationException e) {
            deSoloLectura = true;
        }
        ok("las listas que devuelve son de solo lectura", deSoloLectura);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("CryptoSpecTest " + CryptoSpecTest.run());
    }
}
