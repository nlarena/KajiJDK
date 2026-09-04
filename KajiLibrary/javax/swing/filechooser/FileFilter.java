package javax.swing.filechooser;

import java.io.File;

/**
 * Que archivos se le muestran al usuario en un selector.
 *
 * <h2>Por que es una clase abstracta y no la {@link java.io.FileFilter} que ya existe</h2>
 *
 * <p>Porque hace falta un segundo metodo: {@link #getDescription}. Un selector de archivos no solo
 * filtra — muestra una lista desplegable con los filtros disponibles, y cada uno necesita un texto
 * que una persona pueda leer, del estilo "Imagenes (*.jpg, *.png)". La interfaz de
 * {@code java.io} solo sabe decir si o no.
 *
 * <p>Y es una clase y no una interfaz para que agregar un metodo mas adelante no rompa a quien ya la
 * extendio. El precio es que un filtro no puede heredar de otra cosa, que en la practica no molesta.
 */
public abstract class FileFilter {

    /** Para las subclases. */
    protected FileFilter() {
    }

    /**
     * Si {@code f} se muestra.
     *
     * <p>Los directorios <strong>casi siempre</strong> tienen que pasar: filtrarlos dejaria al
     * usuario sin poder navegar hasta donde estan los archivos que el filtro si acepta. Es el error
     * mas comun al escribir uno.
     */
    public abstract boolean accept(File f);

    /** El texto que se muestra en la lista de filtros. */
    public abstract String getDescription();
}
