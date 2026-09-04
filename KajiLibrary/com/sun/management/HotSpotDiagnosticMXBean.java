package com.sun.management;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.util.List;

/**
 * Las herramientas de diagnostico de HotSpot: volcados de memoria, de hilos, y las opciones de la
 * VM en caliente.
 *
 * <h2>Por que las opciones se leen desde aca y no de la linea de comandos</h2>
 *
 * <p>Porque la linea de comandos dice lo que se <strong>pidio</strong>, no lo que quedo. La VM
 * ajusta sola un monton de valores segun el hardware, y una opcion puede terminar valiendo algo que
 * nadie escribio en ningun lado. {@link #getVMOption} devuelve el valor efectivo junto con
 * {@link VMOption#getOrigin su origen}, que es lo unico que permite distinguir un ajuste automatico
 * de una decision del operador.
 *
 * <h2>Escribir en caliente</h2>
 *
 * <p>{@link #setVMOption} solo acepta las opciones marcadas manejables — las que
 * {@link VMOption#isWriteable} da verdadero—. Son pocas a proposito: la mayoria dimensiona
 * estructuras que se arman al arrancar, y cambiarlas despues no significaria nada.
 *
 * @since 1.6
 */
public interface HotSpotDiagnosticMXBean extends PlatformManagedObject {

    /**
     * Escribe un volcado del monton en un archivo.
     *
     * <p>El archivo lo escribe <strong>la VM</strong>, no el que llama, asi que la ruta se
     * interpreta en el sistema donde corre el proceso. Por una conexion remota eso significa que el
     * archivo queda alla.
     *
     * @param outputFile la ruta, que tiene que terminar en {@code .hprof}
     * @param live si volcar solo los objetos alcanzables; obliga a una recoleccion completa antes
     * @throws IOException si no se pudo escribir
     * @throws NullPointerException si la ruta es {@code null}
     * @throws IllegalArgumentException si la ruta no termina en {@code .hprof} o ya existe
     */
    void dumpHeap(String outputFile, boolean live) throws IOException;

    /**
     * Todas las opciones de diagnostico de esta VM.
     *
     * @return las opciones
     */
    List<VMOption> getDiagnosticOptions();

    /**
     * Una opcion por nombre, con su valor efectivo y su origen.
     *
     * @param name el nombre
     * @return la opcion
     * @throws NullPointerException si el nombre es {@code null}
     * @throws IllegalArgumentException si no existe una opcion con ese nombre
     */
    VMOption getVMOption(String name);

    /**
     * Cambia el valor de una opcion manejable.
     *
     * @param name el nombre
     * @param value el valor nuevo, como texto
     * @throws NullPointerException si el nombre o el valor son {@code null}
     * @throws IllegalArgumentException si la opcion no existe, no es escribible, o el valor no le
     *     corresponde
     */
    void setVMOption(String name, String value);

    /**
     * Escribe un volcado de hilos en un archivo.
     *
     * <p>Por omision no esta soportado: es una operacion que se agrego despues de esta interfaz, y
     * una implementacion vieja no la tiene. Las implementaciones que si la tienen redefinen esto.
     *
     * @param outputFile la ruta, interpretada en el sistema donde corre la VM
     * @param format el formato
     * @throws IOException si no se pudo escribir
     * @throws UnsupportedOperationException si esta implementacion no lo soporta
     * @since 21
     */
    default void dumpThreads(String outputFile, ThreadDumpFormat format) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** El formato de un volcado de hilos. */
    enum ThreadDumpFormat {
        /** Texto para leer, el de siempre. */
        TEXT_PLAIN,
        /** JSON, para procesar con una herramienta. */
        JSON
    }
}
