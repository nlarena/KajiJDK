package javax.imageio.plugins.tiff;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFTagSet -- un grupo de etiquetas TIFF relacionadas.
 *
 * <p>Los numeros de etiqueta de TIFF no son globales: el 0x8769 significa una cosa en el directorio
 * principal y otra adentro de un directorio Exif. Un conjunto es el que da el contexto.
 *
 * <p>De ahi que los conjuntos vengan de a familias --la base de TIFF, Exif, GPS, fax, GeoTIFF-- y que
 * un lector tenga que saber en cual esta parado.
 *
 * <p>Se busca por numero, que es lo que trae el archivo, o por nombre, que es lo que escribe una
 * persona. Los dos devuelven null si no esta.
 *
 * <p>Es inmutable: la lista se copia al construir y los dos conjuntos que devuelve son de solo
 * lectura. Una subclase concreta arma su lista en un {@code static} y no se toca mas.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>De este paquete estan implementados {@link TIFFTag} y esta clase, que son el nucleo y no dependen
 * de nada. Los siete conjuntos concretos --{@code BaselineTIFFTagSet} y companeros-- y las tres clases
 * que manejan directorios y campos necesitan {@code javax.imageio} y {@code javax.imageio.metadata},
 * que esta biblioteca todavia no tiene.
 */
public class TIFFTagSet {

    /** Por numero. */
    private final Map<Integer, TIFFTag> byNumber = new HashMap<Integer, TIFFTag>();

    /** Por nombre. */
    private final Map<String, TIFFTag> byName = new HashMap<String, TIFFTag>();

    /** Los numeros, ordenados y de solo lectura. */
    private final SortedSet<Integer> numbers;

    /** Los nombres, ordenados y de solo lectura. */
    private final SortedSet<String> names;

    /**
     * @param tags las etiquetas del grupo; se copian
     * @throws IllegalArgumentException si la lista es null o tiene algo que no es un {@link TIFFTag}
     */
    public TIFFTagSet(List<TIFFTag> tags) {
        if (tags == null) {
            throw new IllegalArgumentException("tags == null!");
        }
        TreeSet<Integer> allNumbers = new TreeSet<Integer>();
        TreeSet<String> allNames = new TreeSet<String>();
        Iterator<TIFFTag> it = tags.iterator();
        while (it.hasNext()) {
            TIFFTag tag = it.next();
            if (tag == null) {
                throw new IllegalArgumentException("tags contains a null!");
            }
            Integer number = Integer.valueOf(tag.getNumber());
            String name = tag.getName();
            this.byNumber.put(number, tag);
            this.byName.put(name, tag);
            allNumbers.add(number);
            allNames.add(name);
        }
        this.numbers = Collections.unmodifiableSortedSet(allNumbers);
        this.names = Collections.unmodifiableSortedSet(allNames);
    }

    /** La etiqueta con ese numero, o null. */
    public TIFFTag getTag(int tagNumber) {
        return this.byNumber.get(Integer.valueOf(tagNumber));
    }

    /** La etiqueta con ese nombre, o null. */
    public TIFFTag getTag(String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException("tagName == null!");
        }
        return this.byName.get(tagName);
    }

    /** Los numeros, ordenados; de solo lectura. */
    public SortedSet<Integer> getTagNumbers() {
        return this.numbers;
    }

    /** Los nombres, ordenados; de solo lectura. */
    public SortedSet<String> getTagNames() {
        return this.names;
    }
}
