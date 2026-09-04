import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Repro del finding #471: una tuberia por bloque entre dos hilos.
 *
 * <p>Sin el arreglo, todo lo que pase de un puñado de bytes termina en abrazo mortal
 * ({@code run()I -> None}, que es el "nothing left to run" del planificador). Con el arreglo pasan
 * los cinco tamaños. El JDK real pasa los cinco siempre.
 */
public class Tubo2 {
    static PipedInputStream in;
    static PipedOutputStream out;
    static int howMany;
    static String failure = "";

    static class Writer implements Runnable {
        public void run() {
            try {
                byte[] b = new byte[howMany];
                for (int i = 0; i < b.length; i++) { b[i] = (byte) (i & 0x7F); }
                out.write(b, 0, b.length);
                out.close();
            } catch (Throwable e) {
                failure = "escritor: " + e;
            }
        }
    }

    static int runCase(int n) throws Exception {
        howMany = n;
        in = new PipedInputStream();
        out = new PipedOutputStream(in);
        Thread t = new Thread(new Writer());
        t.start();
        byte[] dest = new byte[n];
        int total = 0;
        while (total < n) {
            int k = in.read(dest, total, n - total);
            if (k < 0) { break; }
            total = total + k;
        }
        t.join();
        in.close();
        return total;
    }

    public static int run() throws Exception {
        // 1024 es el tamaño del buffer: los tres ultimos obligan al escritor a esperar lugar.
        int[] cases = new int[] {10, 1000, 1024, 1025, 3000};
        for (int j = 0; j < cases.length; j++) {
            int readCount;
            try {
                readCount = runCase(cases[j]);
            } catch (Throwable e) {
                System.out.println("caso " + cases[j] + " tiro " + e);
                return j;
            }
            if (readCount != cases[j]) {
                System.out.println("caso " + cases[j] + ": leyo " + readCount
                    + " fallo=[" + failure + "]");
                return j;
            }
        }
        return -1;
    }

    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
