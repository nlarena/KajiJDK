package javax.swing.filechooser;

import java.io.File;

import javax.swing.Icon;

/**
 * Como se le muestra un archivo al usuario: su nombre, su icono, su descripcion.
 *
 * <h2>La convencion de devolver {@code null}</h2>
 *
 * <p>Los cinco metodos devuelven {@code null} por omision, y eso no significa "no se": significa
 * <strong>"usa lo que ibas a usar"</strong>. Quien pregunta cae de vuelta en el
 * {@link FileSystemView} del sistema.
 *
 * <p>Esa convencion es lo que hace practica a la clase. Un {@code FileView} que quiera solo cambiar
 * el icono de los {@code .java} escribe un metodo, contesta {@code null} para todo lo demas, y el
 * resto sigue viendose como el sistema lo muestra. Sin ella habria que reimplementar los cinco.
 *
 * <p>{@link #isTraversable} devuelve {@link Boolean} y no {@code boolean} justamente por esto:
 * necesita un tercer valor. Es la unica forma de distinguir "no es navegable" de "decidilo vos".
 */
public abstract class FileView {

    /** Para las subclases. */
    protected FileView() {
    }

    /** El nombre a mostrar, o {@code null} para dejar el del sistema. */
    public String getName(File f) {
        return null;
    }

    /** Una descripcion de este archivo en particular, o {@code null}. */
    public String getDescription(File f) {
        return null;
    }

    /**
     * Una descripcion del tipo de archivo, o {@code null}.
     *
     * <p>Distinta de {@link #getDescription}: aquella habla de <em>este</em> archivo, esta de su
     * clase — "Documento de texto" contra "Las notas de la reunion".
     */
    public String getTypeDescription(File f) {
        return null;
    }

    /** El icono, o {@code null} para dejar el del sistema. */
    public Icon getIcon(File f) {
        return null;
    }

    /**
     * Si se puede entrar en {@code f}, o {@code null} para dejar que decida el sistema.
     *
     * <p>No es lo mismo que ser un directorio: una carpeta comprimida puede ser navegable sin serlo,
     * y un directorio sin permisos puede no serlo siendolo.
     */
    public Boolean isTraversable(File f) {
        return null;
    }
}
