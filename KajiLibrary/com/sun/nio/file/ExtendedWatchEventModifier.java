package com.sun.nio.file;

import java.nio.file.WatchEvent;

/**
 * Modificadores de registro de un {@link java.nio.file.WatchService} fuera del conjunto estandar.
 */
public enum ExtendedWatchEventModifier implements WatchEvent.Modifier {

    /**
     * Vigilar el arbol entero y no solo el directorio registrado.
     *
     * <p>Es una capacidad del sistema operativo, no un bucle que el JDK haga por su cuenta: solo
     * anda donde el sistema sabe vigilar recursivamente —Windows lo hace, Linux no— y donde no,
     * registrar con esto tira {@link UnsupportedOperationException}. Ahi hay que registrar cada
     * subdirectorio a mano, con lo que eso implica: los que se creen despues no quedan vigilados
     * hasta que alguien los registre.
     */
    FILE_TREE
}
