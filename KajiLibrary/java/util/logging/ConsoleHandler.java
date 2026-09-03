package java.util.logging;

/**
 * KajiLibrary's java.util.logging.ConsoleHandler -- escribe al **error** estandar.
 *
 * <p>A `System.err` y no a `System.out`, a proposito: la traza no es la salida del programa, y
 * mezclarlas arruina el uso del programa en una tuberia.
 *
 * <p>Y vacia despues de cada mensaje. Es mas lento y es lo correcto para una consola: una traza que
 * se queda en el buffer cuando el programa se cae no sirve para nada, y el ultimo mensaje antes de
 * caerse suele ser el que importa.
 */
public class ConsoleHandler extends StreamHandler {

    public ConsoleHandler() {
        // Las propiedades propias pisan a las de `StreamHandler`, que el `super()` ya aplico.
        this.configurar("java.util.logging.ConsoleHandler");
        this.setOutputStream(System.err);
    }

    public void publish(LogRecord record) {
        super.publish(record);
        this.flush();
    }

    /** Vacia pero **no** cierra: `System.err` no es suyo. */
    public void close() {
        this.flush();
    }
}
