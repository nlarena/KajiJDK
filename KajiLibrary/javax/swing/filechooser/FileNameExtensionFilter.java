package javax.swing.filechooser;

import java.io.File;
import java.util.Locale;

/**
 * Un {@link FileFilter} por extension, que es el caso que cubre casi todos.
 *
 * <p>Ejemplo: {@code new FileNameExtensionFilter("Imagenes", "jpg", "png")}.
 *
 * <h2>Tres decisiones que no se ven en la firma</h2>
 *
 * <p><strong>Los directorios siempre pasan</strong>, sin mirar su nombre. Sin eso el usuario no
 * podria navegar hasta la carpeta donde estan sus imagenes — el filtro le habria escondido el
 * camino. Es la trampa contra la que advierte {@link FileFilter#accept}.
 *
 * <p><strong>La comparacion ignora mayusculas</strong>, y se hace pasando los dos lados a minusculas
 * con {@link Locale#ENGLISH} y no con la del sistema. No es un detalle: en turco, {@code "I"} en
 * minuscula no es {@code "i"}, asi que un archivo {@code FOTO.JPG} dejaria de coincidir con
 * {@code "jpg"} en una maquina turca. Un nombre de archivo no es texto de idioma.
 *
 * <p><strong>Las extensiones se copian</strong> en el constructor. Sin la copia, quien construyo el
 * filtro podria cambiar el arreglo despues y el filtro cambiaria de significado sin que nadie lo
 * toque — y lo mismo devuelve {@link #getExtensions}.
 */
public final class FileNameExtensionFilter extends FileFilter {

    private final String description;
    private final String[] extensions;
    private final String[] enMinuscula;

    /**
     * @param description el texto para la lista de filtros
     * @param extensions las extensiones, sin el punto
     * @throws IllegalArgumentException si no hay ninguna extension, o si alguna es {@code null} o
     *     vacia
     */
    public FileNameExtensionFilter(String description, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            throw new IllegalArgumentException("Hace falta al menos una extension");
        }
        this.description = description;
        this.extensions = new String[extensions.length];
        this.enMinuscula = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i] == null || extensions[i].isEmpty()) {
                throw new IllegalArgumentException("Una extension no puede ser null ni vacia");
            }
            this.extensions[i] = extensions[i];
            this.enMinuscula[i] = extensions[i].toLowerCase(Locale.ENGLISH);
        }
    }

    /** Si {@code f} es un directorio, o si su nombre termina en una de las extensiones. */
    public boolean accept(File f) {
        if (f == null) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        String nombre = f.getName();
        int punto = nombre.lastIndexOf('.');
        // Un nombre que empieza con punto y no tiene otro —`.gitignore`— no tiene extension: es un
        // nombre oculto. De ahi que el punto tenga que estar despues del primer caracter.
        if (punto > 0 && punto < nombre.length() - 1) {
            String ext = nombre.substring(punto + 1).toLowerCase(Locale.ENGLISH);
            for (int i = 0; i < this.enMinuscula.length; i++) {
                if (this.enMinuscula[i].equals(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** El texto para la lista de filtros. */
    public String getDescription() {
        return this.description;
    }

    /** Las extensiones, tal como se pasaron, en un arreglo nuevo. */
    public String[] getExtensions() {
        String[] copia = new String[this.extensions.length];
        for (int i = 0; i < this.extensions.length; i++) {
            copia[i] = this.extensions[i];
        }
        return copia;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[description=");
        sb.append(getDescription());
        sb.append(" extensions=[");
        for (int i = 0; i < this.extensions.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.extensions[i]);
        }
        sb.append("]]");
        return sb.toString();
    }
}
