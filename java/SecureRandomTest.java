import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import java.security.Security;
import java.util.Arrays;

/**
 * java.security.SecureRandom y su SPI.
 *
 * <p>Casi nada de lo que se puede probar de un generador criptografico es el valor de sus bytes: por
 * definicion no se pueden predecir, asi que la prueba no los compara contra nada. Lo que si se puede
 * comprobar, y es lo que decide si la clase sirve, es su <b>contrato</b>: que entregue la cantidad
 * pedida, que dos tiradas no sean iguales, que la semilla agregue en vez de reemplazar, y que los
 * errores sean los que el llamador espera atrapar.
 *
 * <p>Los dos casos estadisticos --que no devuelva todo ceros y que dos tiradas de 32 bytes difieran--
 * tienen probabilidad de falso negativo de 2^-256. No es una prueba de calidad del generador; es una
 * prueba de que <b>hay</b> un generador y no un arreglo sin tocar.
 *
 * <p>Las expectativas de las excepciones salieron de preguntarle al JDK 25. La divergencia declarada
 * --que aca getInstance("SHA1PRNG") no existe-- no se prueba en el caso positivo, por eso el caso
 * negativo usa un nombre que no existe en ninguna de las dos.
 */
public class SecureRandomTest {

    /** Un SPI de juguete: determinista a proposito, para mirar el reenvio y no la aleatoriedad. */
    static class CountingSpi extends SecureRandomSpi {
        int seeded = 0;
        int filled = 0;
        byte next = 1;

        protected void engineSetSeed(byte[] seed) {
            seeded = seeded + 1;
        }

        protected void engineNextBytes(byte[] bytes) {
            filled = filled + 1;
            for (int k = 0; k < bytes.length; k++) {
                bytes[k] = next;
                next = (byte) (next + 1);
            }
        }

        protected byte[] engineGenerateSeed(int numBytes) {
            return new byte[numBytes];
        }
    }

    static class TestRandom extends SecureRandom {
        TestRandom(SecureRandomSpi spi) {
            super(spi, null);
        }
    }

    private static boolean allZero(byte[] b) {
        for (int k = 0; k < b.length; k++) {
            if (b[k] != 0) {
                return false;
            }
        }
        return true;
    }

    public static int run() {
        int i = 0;

        // ======================================================================================
        // el generador por omision existe y entrega bytes
        // ======================================================================================
        SecureRandom r = new SecureRandom();
        if (r.getProvider() == null) { return i; } i++;
        if (r.getAlgorithm() == null || r.getAlgorithm().length() == 0) { return i; } i++;
        // Es un java.util.Random, y esa herencia es lo que hace que confundirlos sea tan facil.
        if (!(r instanceof java.util.Random)) { return i; } i++;

        byte[] a = new byte[32];
        r.nextBytes(a);
        if (allZero(a)) { return i; } i++;
        byte[] b = new byte[32];
        r.nextBytes(b);
        if (Arrays.equals(a, b)) { return i; } i++;
        // Un arreglo vacio no es un error.
        r.nextBytes(new byte[0]);
        i++;

        // generateSeed entrega la cantidad exacta, y cero es valido.
        if (r.generateSeed(16).length != 16) { return i; } i++;
        if (r.generateSeed(0).length != 0) { return i; } i++;
        if (allZero(r.generateSeed(32))) { return i; } i++;
        if (SecureRandom.getSeed(16).length != 16) { return i; } i++;

        // Todo lo que hereda de Random pasa por next(int), asi que tambien es criptografico.
        if (r.nextInt() == r.nextInt() && r.nextInt() == r.nextInt()) { return i; } i++;
        if (r.nextLong() == r.nextLong() && r.nextLong() == r.nextLong()) { return i; } i++;

        // ======================================================================================
        // la semilla AGREGA, no reemplaza: dos con la misma semilla NO dan lo mismo
        // ======================================================================================
        byte[] semilla = new byte[] {1, 2, 3, 4};
        byte[] x = new byte[32];
        byte[] y = new byte[32];
        new SecureRandom(semilla).nextBytes(x);
        new SecureRandom(semilla).nextBytes(y);
        if (Arrays.equals(x, y)) { return i; } i++;
        // Y setSeed no rompe ni cambia el contrato.
        r.setSeed(new byte[] {9, 9});
        r.setSeed(12345L);
        // setSeed(0) no hace nada: el constructor de Random lo llama antes de que el SPI exista.
        r.setSeed(0L);
        i++;

        // ======================================================================================
        // los errores que el llamador espera atrapar
        // ======================================================================================
        boolean threw = false;
        try { r.nextBytes(null); } catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { r.setSeed((byte[]) null); } catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { r.generateSeed(-1); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        threw = false;
        try { SecureRandom.getInstance((String) null); }
        catch (NullPointerException e) { threw = true; }
        catch (NoSuchAlgorithmException e) { threw = false; }
        if (!threw) { return i; } i++;
        threw = false;
        try { SecureRandom.getInstance("NoExisteEsteAlgoritmo"); }
        catch (NoSuchAlgorithmException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { SecureRandom.getInstance("NoExisteEsteAlgoritmo", "NoExisteEsteProveedor"); }
        catch (NoSuchProviderException e) { threw = true; }
        catch (NoSuchAlgorithmException e) { threw = false; }
        if (!threw) { return i; } i++;

        // getInstanceStrong siempre da algo si hay algun generador instalado.
        try {
            if (SecureRandom.getInstanceStrong() == null) { return i; }
            i++;
        } catch (NoSuchAlgorithmException e) {
            return i;
        }

        // ======================================================================================
        // el generador de fabrica esta registrado como servicio del proveedor
        // ======================================================================================
        boolean hayServicio = false;
        Provider[] provs = Security.getProviders();
        for (int p = 0; p < provs.length; p++) {
            java.util.Iterator<Provider.Service> it = provs[p].getServices().iterator();
            while (it.hasNext()) {
                if ("SecureRandom".equals(it.next().getType())) { hayServicio = true; }
            }
        }
        if (!hayServicio) { return i; } i++;
        // Y se puede pedir por su nombre, sea cual sea.
        try {
            SecureRandom porNombre = SecureRandom.getInstance(r.getAlgorithm());
            if (!porNombre.getAlgorithm().equals(r.getAlgorithm())) { return i; }
            i++;
            byte[] z = new byte[16];
            porNombre.nextBytes(z);
            if (allZero(z)) { return i; }
            i++;
        } catch (NoSuchAlgorithmException e) {
            return i;
        }
        // Pedirlo a un proveedor que no lo tiene es NoSuchAlgorithmException, no NoSuchProvider.
        threw = false;
        try { SecureRandom.getInstance("NoExisteEsteAlgoritmo", r.getProvider()); }
        catch (NoSuchAlgorithmException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // el SPI: el reenvio, y los defaults que rechazan
        // ======================================================================================
        CountingSpi spi = new CountingSpi();
        SecureRandom conSpi = new TestRandom(spi);
        // Sin algoritmo conocido, getAlgorithm dice "unknown" y no null.
        if (!conSpi.getAlgorithm().equals("unknown")) { return i; } i++;
        if (conSpi.getProvider() != null) { return i; } i++;
        byte[] cuatro = new byte[4];
        conSpi.nextBytes(cuatro);
        if (spi.filled != 1) { return i; } i++;
        if (cuatro[0] != 1 || cuatro[3] != 4) { return i; } i++;
        conSpi.setSeed(new byte[] {7});
        if (spi.seeded != 1) { return i; } i++;
        // setSeed(long) distinto de cero tambien llega al SPI; el cero no.
        conSpi.setSeed(5L);
        if (spi.seeded != 2) { return i; } i++;
        conSpi.setSeed(0L);
        if (spi.seeded != 2) { return i; } i++;
        // next(int) pasa por nextBytes, que es lo que hace criptografico a todo lo heredado.
        int antes = spi.filled;
        conSpi.nextInt();
        if (spi.filled != antes + 1) { return i; } i++;
        // Un SPI sin parametros los contesta null.
        if (conSpi.getParameters() != null) { return i; } i++;

        // Los dos defaults del SPI rechazan en vez de fingir.
        threw = false;
        try { conSpi.reseed(); } catch (UnsupportedOperationException e) { threw = true; }
        if (!threw) { return i; } i++;

        // Lo que el generador DE FABRICA hace con reseed() no se prueba aca, y es a proposito: es
        // una divergencia declarada. El del JDK es un DRBG con estado interno y resiembra; el de
        // esta biblioteca es un pase directo al sistema operativo y no tiene estado que resembrar,
        // asi que lanza. Las dos respuestas son correctas para lo que cada uno es, y una prueba que
        // corre en las dos VMs no puede exigir ninguna de las dos.

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
