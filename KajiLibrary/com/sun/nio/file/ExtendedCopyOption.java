package com.sun.nio.file;

import java.nio.file.CopyOption;

/**
 * Opciones de copia fuera del conjunto estandar.
 */
public enum ExtendedCopyOption implements CopyOption {

    /**
     * La copia se puede interrumpir.
     *
     * <p>Copiar un archivo grande es una operacion larga y, por omision, sorda: interrumpir el hilo
     * no la detiene. Con esta opcion la copia atiende la interrupcion, aborta y tira
     * {@link java.nio.file.FileSystemException}. El precio es que hay que chequear el estado del
     * hilo cada tanto, y por eso no es el comportamiento por defecto.
     */
    INTERRUPTIBLE
}
