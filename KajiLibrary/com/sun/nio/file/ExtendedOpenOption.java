package com.sun.nio.file;

import java.nio.file.OpenOption;

/**
 * Opciones de apertura de archivo que el JDK ofrece fuera del conjunto estandar.
 *
 * <p>Estan aca y no en {@link java.nio.file.StandardOpenOption} porque **no todas las plataformas
 * las pueden honrar**: las tres {@code NOSHARE_*} son un modo de bloqueo obligatorio que Windows
 * tiene y POSIX no, y {@link #DIRECT} depende de que el sistema de archivos admita saltear su
 * cache. Poner en el conjunto estandar algo que en media plataforma tira
 * {@link UnsupportedOperationException} seria prometer de mas.
 */
public enum ExtendedOpenOption implements OpenOption {

    /** Nadie mas puede abrir el archivo para leer mientras este canal lo tenga. */
    NOSHARE_READ,
    /** Nadie mas puede abrirlo para escribir. */
    NOSHARE_WRITE,
    /** Nadie mas puede borrarlo. */
    NOSHARE_DELETE,
    /**
     * Saltear la cache del sistema de archivos.
     *
     * <p>No es una optimizacion gratis: obliga a que las lecturas y escrituras esten alineadas al
     * tamano de bloque del dispositivo. Sirve para quien administra su propia cache —una base de
     * datos— y estorba a todos los demas.
     */
    DIRECT
}
