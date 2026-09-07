import java.io.File;

/**
 * Reproductor del heisenbug de referencias muertas: {@code new File(padre, hijo)} en un hilo
 * secundario mata el hilo, sin excepcion y sin traza.
 *
 * <h2>Que se ve</h2>
 *
 * <p>El hilo entra --lo imprime--, llega al {@code new File(File, String)} y desaparece. No tira
 * nada: un {@code catch (Throwable)} alrededor no atrapa nada, y {@code join} vuelve enseguida como
 * si el hilo hubiera terminado bien. En el hilo principal la misma llamada con el mismo camino
 * anda siempre.
 *
 * <h2>Por que es mejor reproductor que los anteriores</h2>
 *
 * <p>{@code BxDbgT} y {@code BxDbgY} reproducen alrededor del cincuenta por ciento de las veces y
 * necesitan que el Eden este lleno. Este falla casi siempre con un solo hilo y sin preparar nada,
 * y no depende de la excepcion espuria: el hilo simplemente muere.
 *
 * <p>La sensibilidad al camino es lo que lo delata como problema de recoleccion y no de logica:
 * {@code C:/tmp} anda, {@code C:/dir0/.../dir13} no; pero el mismo camino que falla solo, anda si
 * antes corrieron otros hilos. La entrada no decide -- decide en que momento cae el minor GC --.
 *
 * <h2>Como correrlo</h2>
 *
 * <pre>
 *   bin/javac.exe --emit -cp KajiLibrary java/BxDbgF.java
 *   target/release/run-headless.exe java/BxDbgF.class run
 * </pre>
 *
 * <p>Sale {@code //hilo ok=...} si anduvo y solo {@code //fin} si el hilo murio.
 * {@link #conPartes} toma la cantidad de tramos del camino, para barrer.
 */
public class BxDbgF {

    public static int run() {
        return conPartes(14);
    }

    /** Un camino de {@code n} tramos, y un {@code new File(padre, hijo)} en un hilo aparte. */
    public static int conPartes(int n) {
        StringBuilder b = new StringBuilder("C:");
        for (int i = 0; i < n; i++) {
            b.append("/dir").append(i);
        }
        final File p = new File(b.toString());
        System.out.println("//partes=" + n + " largo=" + p.getPath().length());
        // En el hilo principal anda siempre; es la linea de control.
        System.out.println("//principal ok=" + new File(p, "x").getPath().length());
        Thread t = new Thread() {
            public void run() {
                System.out.println("//el hilo arranco");
                try {
                    System.out.println("//hilo ok=" + new File(p, "x").getPath().length());
                } catch (Throwable e) {
                    // No pasa por aca: el hilo muere antes, sin excepcion.
                    System.out.println("//hilo falla: " + e);
                }
            }
        };
        t.start();
        try {
            t.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("//fin");
        return 0;
    }
}
