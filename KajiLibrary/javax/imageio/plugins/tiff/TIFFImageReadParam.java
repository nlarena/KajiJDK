package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageReadParam;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFImageReadParam -- que etiquetas reconocer al leer un
 * TIFF.
 *
 * <p>Un TIFF puede traer etiquetas de cualquier perfil, y el lector solo sabe interpretar las de los
 * conjuntos que se le declaren. Esta clase es esa declaracion.
 *
 * <p>Trae cuatro de fabrica --el basico, el de fax, el que lleva a los directorios Exif y el de
 * GeoTIFF--, que cubre lo que sale de una camara y de casi cualquier programa. Agregar uno propio con
 * {@link #addAllowedTagSet} es lo que hace que un lector entienda un TIFF especializado.
 *
 * <h2>{@link #setReadUnknownTags}</h2>
 *
 * <p>Apagado por omision: las etiquetas que ningun conjunto declara se <b>descartan</b> al leer.
 *
 * <p>Prenderlo las conserva como campos anonimos --con {@link TIFFTag#UNKNOWN_TAG_NAME} de nombre-- y
 * es lo que hace falta para reescribir un TIFF sin perder lo que no se entiende. Un flujo de leer y
 * volver a escribir con esto apagado tira en silencio todo lo que el lector no reconoce.
 */
public final class TIFFImageReadParam extends ImageReadParam {

    /** Los conjuntos que el lector va a reconocer, en orden de consulta. */
    private final List<TIFFTagSet> allowedTagSets = new ArrayList<TIFFTagSet>();

    /** Si conservar las etiquetas desconocidas. Ver la nota de la clase. */
    private boolean readUnknownTags = false;

    /** Con los cuatro conjuntos de fabrica. */
    public TIFFImageReadParam() {
        this.allowedTagSets.add(BaselineTIFFTagSet.getInstance());
        this.allowedTagSets.add(FaxTIFFTagSet.getInstance());
        this.allowedTagSets.add(ExifParentTIFFTagSet.getInstance());
        this.allowedTagSets.add(GeoTIFFTagSet.getInstance());
    }

    /**
     * Agrega un conjunto al final de la lista.
     *
     * <p>Agregar uno que ya esta no hace nada.
     *
     * @throws IllegalArgumentException si es null
     */
    public void addAllowedTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new IllegalArgumentException("tagSet == null!");
        }
        if (!this.allowedTagSets.contains(tagSet)) {
            this.allowedTagSets.add(tagSet);
        }
    }

    /**
     * Lo saca.
     *
     * @throws IllegalArgumentException si es null
     */
    public void removeAllowedTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new IllegalArgumentException("tagSet == null!");
        }
        this.allowedTagSets.remove(tagSet);
    }

    /**
     * Los conjuntos declarados.
     *
     * <p>Es una copia: agregar a la lista devuelta no declara nada. El JDK devuelve la lista viva, y
     * quien la modifique le cambia el parametro por atras; se pasa por {@link #addAllowedTagSet} y no
     * hay diferencia para el codigo que la use como lista de solo lectura.
     */
    public List<TIFFTagSet> getAllowedTagSets() {
        return new ArrayList<TIFFTagSet>(this.allowedTagSets);
    }

    /** Si conservar las desconocidas. Ver la nota de la clase. */
    public void setReadUnknownTags(boolean readUnknownTags) {
        this.readUnknownTags = readUnknownTags;
    }

    /** Si se conservan. */
    public boolean getReadUnknownTags() {
        return this.readUnknownTags;
    }
}
