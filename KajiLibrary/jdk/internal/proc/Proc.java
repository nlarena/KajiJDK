package jdk.internal.proc;

/**
 * KajiLibrary's jdk.internal.proc.Proc — la costura con los procesos del sistema.
 *
 * <p>Es lo que faltaba para que {@link java.lang.ProcessBuilder#start()} pudiera existir. Hasta que
 * estos nativos estuvieron, `start()` **no se declaraba**: un `Process` que no representa ningún
 * proceso no es un miembro que se pueda escribir con honestidad.
 *
 * <p>Sigue el mismo criterio que {@link jdk.internal.io.Fs}: el nativo hace lo mínimo y **no sabe
 * nada de las clases de Java**. Toma y devuelve cadenas, arreglos y enteros; quién sea `Process` o
 * `ProcessBuilder` es problema del lado Java, que puede cambiar sin tocar Rust.
 *
 * <p>La diferencia con `Fs` es que acá **sí hay handle**. Un archivo se lee entero de una vez, pero un
 * proceso es estado que vive entre llamadas —sus tuberías, su código de salida— y no hay forma de
 * representarlo con operaciones de una sola vez. El handle es un índice en una tabla de la VM, y sus
 * entradas **no se reciclan**: un handle viejo nunca apunta a un proceso nuevo.
 *
 * <p>Los modos de redirección son los cinco que `ProcessBuilder.Redirect` distingue de verdad:
 * <b>0</b> tubería, <b>1</b> heredar, <b>2</b> descartar, <b>3</b> archivo pisando, <b>4</b> archivo
 * agregando.
 */
public final class Proc {

    private Proc() {
    }

    /**
     * Lanza el proceso.
     *
     * @param cmd el comando y sus argumentos; el primero es el ejecutable
     * @param dir el directorio de trabajo, o `null` para heredar el nuestro
     * @param envKV el entorno como pares aplanados (clave, valor, clave, valor…), o vacío para
     *     heredar el nuestro. Si no está vacío **reemplaza** el entorno entero, como
     *     `ProcessBuilder.environment()`
     * @param rutas las tres rutas de redirección (entrada, salida, error), con `null` donde no aplica
     * @param modos los tres modos
     * @param unirError si el error va a la misma tubería que la salida
     * @return el handle, o -1 si no se pudo lanzar
     */
    public static native int spawn(String[] cmd, String dir, String[] envKV, String[] rutas,
            int[] modos, boolean unirError);

    /** Espera a que termine y devuelve su código de salida. */
    public static native int waitFor(int handle);

    /**
     * El código de salida si ya terminó, o {@link Integer#MIN_VALUE} si sigue corriendo.
     *
     * <p>El centinela es lo que le permite a `Process.exitValue()` tirar
     * `IllegalThreadStateException` --que es lo que el contrato pide-- en vez de bloquearse.
     */
    public static native int exitValue(int handle);

    /** Si sigue corriendo. */
    public static native boolean isAlive(int handle);

    /**
     * Lo mata.
     *
     * @param forzar se acepta y **no cambia nada en Windows**, donde no hay una señal "amable"
     */
    public static native void destroy(int handle, boolean forzar);

    /** Su identificador de proceso, o -1. */
    public static native long pid(int handle);

    /** Escribe en su entrada estándar. `true` si se pudo. */
    public static native boolean writeIn(int handle, byte[] b, int off, int len);

    /** Cierra su entrada, que es como se le dice "no viene más". */
    public static native void closeIn(int handle);

    /** Lee de su salida. Devuelve cuántos bytes puso, o -1 en fin de flujo. */
    public static native int readOut(int handle, byte[] b);

    /** Lee de su salida de error. */
    public static native int readErr(int handle, byte[] b);
}
