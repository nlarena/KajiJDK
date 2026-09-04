/**
 * Repro del finding #472: una espera CON PLAZO terminada por aviso dejaba su plazo armado, y al
 * vencer mas tarde reprogramaba un hilo ya terminado -- que no tiene marcos.
 *
 * <p>Sin el arreglo panica con "no frame on the call stack" en las tres corridas de tres. Con el
 * arreglo da -1. El JDK real da -1 siempre.
 */
public class Mon4 {
    static final Object lock = new Object();
    static volatile int state = 0;
    static volatile String echo = "sin-eco";

    static class Consumer implements Runnable {
        public void run() {
            try {
                synchronized (lock) {
                    while (state == 0) {
                        lock.notifyAll();
                        lock.wait(1000);
                    }
                    echo = "vio-" + state;
                }
            } catch (Throwable e) {
                echo = "tiro-" + e.getClass().getSimpleName();
            }
        }
    }

    public static int run() throws Exception {
        Thread c = new Thread(new Consumer());
        c.start();
        // El aviso llega ANTES de que venza el plazo: ese es el camino que dejaba el plazo sucio.
        Thread.sleep(200);
        synchronized (lock) {
            state = 7;
            lock.notifyAll();
        }
        c.join();
        System.out.println("eco: " + echo);
        return "vio-7".equals(echo) ? -1 : 0;
    }

    public static void main(String[] a) throws Exception { System.out.println(run()); }
}
