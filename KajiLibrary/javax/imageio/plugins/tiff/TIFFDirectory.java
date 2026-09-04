package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFDirectory -- un directorio de un TIFF, con sus campos.
 *
 * <p>Un TIFF es una cabecera de ocho bytes y una cadena de directorios; cada uno lista sus campos
 * ordenados por numero de etiqueta. Esto es uno de esos directorios, ya leido.
 *
 * <h2>Los directorios se anidan</h2>
 *
 * <p>Un campo cuya etiqueta es un puntero --ver {@link TIFFTag#isIFDPointer}-- no lleva un valor sino
 * <b>otro directorio</b>. Asi es como un JPEG con Exif tiene, por dentro, un TIFF con el directorio
 * Exif colgando del principal y el de posicion colgando de ese. {@link #getParentTag} dice de que
 * etiqueta cuelga este; null si es el de arriba de todo.
 *
 * <h2>Un solo campo por numero de etiqueta</h2>
 *
 * <p>{@link #addTIFFField} <b>reemplaza</b> el campo que hubiera con ese numero; el formato no permite
 * dos campos con la misma etiqueta en un directorio. {@link #getTIFFFields} los devuelve ordenados por
 * numero, que es como se escriben al archivo.
 *
 * <h2>Los conjuntos de etiquetas deciden que nombres hay</h2>
 *
 * <p>{@link #getTag} busca en los conjuntos declarados, en el orden en que se declararon. Un numero
 * que ningun conjunto conozca no tiene nombre, y el campo se puede guardar igual --como anonimo-- pero
 * nadie sabe que significa. Agregar el conjunto que falta con {@link #addTagSet} es lo que hace que
 * las mismas etiquetas pasen a tener sentido.
 */
public class TIFFDirectory implements Cloneable {

    /** Como se llama el formato de metadatos nativo de TIFF. */
    static final String NATIVE_FORMAT = "javax_imageio_tiff_image_1.0";

    /** Los conjuntos declarados, en orden de consulta. */
    private List<TIFFTagSet> tagSets;

    /** De que etiqueta cuelga este directorio, o null. */
    private TIFFTag parentTag;

    /** Los campos, ordenados por numero de etiqueta. */
    private TreeMap<Integer, TIFFField> fields = new TreeMap<Integer, TIFFField>();

    /**
     * Un directorio vacio.
     *
     * @param tagSets contra que conjuntos resolver los numeros de etiqueta
     * @param parentTag de que etiqueta cuelga, o null si es el principal
     * @throws NullPointerException si el arreglo de conjuntos es null
     */
    public TIFFDirectory(TIFFTagSet[] tagSets, TIFFTag parentTag) {
        if (tagSets == null) {
            throw new NullPointerException("tagSets == null!");
        }
        this.tagSets = new ArrayList<TIFFTagSet>(tagSets.length);
        int i = 0;
        while (i < tagSets.length) {
            this.tagSets.add(tagSets[i]);
            i = i + 1;
        }
        this.parentTag = parentTag;
    }

    /**
     * El directorio principal de esos metadatos.
     *
     * <p>Lee el arbol del formato nativo de TIFF, asi que sirve para cualquier {@link IIOMetadata} que
     * lo declare, no solo para el que devuelve {@link #getAsMetadata}.
     *
     * @throws NullPointerException si los metadatos son null
     * @throws IllegalArgumentException si no entienden el formato nativo de TIFF
     * @throws IIOInvalidTreeException si el arbol no tiene la forma que el formato pide
     */
    public static TIFFDirectory createFromMetadata(IIOMetadata tiffImageMetadata)
            throws IIOInvalidTreeException {
        if (tiffImageMetadata == null) {
            throw new NullPointerException("tiffImageMetadata == null");
        }
        if (!supportsNativeFormat(tiffImageMetadata)) {
            throw new IllegalArgumentException("Parameter does not support required metadata format!");
        }
        Node root = tiffImageMetadata.getAsTree(NATIVE_FORMAT);
        Node ifd = firstElement(root);
        if (ifd == null || !"TIFFIFD".equals(ifd.getNodeName())) {
            throw new IIOInvalidTreeException("Root must have a TIFFIFD child", root);
        }
        return fromIFDNode(ifd, null);
    }

    /** Los conjuntos declarados. Una copia del arreglo. */
    public TIFFTagSet[] getTagSets() {
        return this.tagSets.toArray(new TIFFTagSet[this.tagSets.size()]);
    }

    /**
     * Agrega un conjunto al final de la lista de consulta.
     *
     * <p>Agregar uno que ya esta no hace nada; en particular no lo mueve de lugar, asi que no cambia
     * quien gana cuando dos conjuntos declaran el mismo numero.
     *
     * @throws NullPointerException si es null
     */
    public void addTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new NullPointerException("tagSet == null");
        }
        if (!this.tagSets.contains(tagSet)) {
            this.tagSets.add(tagSet);
        }
    }

    /**
     * Lo saca. Los campos que ya estaban no se tocan: solo dejan de tener nombre nuevo.
     *
     * @throws NullPointerException si es null
     */
    public void removeTagSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new NullPointerException("tagSet == null");
        }
        this.tagSets.remove(tagSet);
    }

    /** De que etiqueta cuelga este directorio, o null si es el principal. */
    public TIFFTag getParentTag() {
        return this.parentTag;
    }

    /**
     * La etiqueta con ese numero, buscando en los conjuntos declarados en orden.
     *
     * @return null si ningun conjunto la conoce
     */
    public TIFFTag getTag(int tagNumber) {
        int i = 0;
        while (i < this.tagSets.size()) {
            TIFFTagSet set = this.tagSets.get(i);
            if (set != null) {
                TIFFTag tag = set.getTag(tagNumber);
                if (tag != null) {
                    return tag;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /** Cuantos campos hay. */
    public int getNumTIFFFields() {
        return this.fields.size();
    }

    /** Si hay un campo con ese numero de etiqueta. */
    public boolean containsTIFFField(int tagNumber) {
        return this.fields.containsKey(Integer.valueOf(tagNumber));
    }

    /**
     * Agrega el campo, reemplazando al que hubiera con el mismo numero. Ver la nota de la clase.
     *
     * @throws NullPointerException si es null
     */
    public void addTIFFField(TIFFField f) {
        if (f == null) {
            throw new NullPointerException("f == null");
        }
        this.fields.put(Integer.valueOf(f.getTagNumber()), f);
    }

    /**
     * El campo con ese numero de etiqueta.
     *
     * @return null si no esta
     */
    public TIFFField getTIFFField(int tagNumber) {
        return this.fields.get(Integer.valueOf(tagNumber));
    }

    /** Saca el campo con ese numero. Si no esta, no hace nada. */
    public void removeTIFFField(int tagNumber) {
        this.fields.remove(Integer.valueOf(tagNumber));
    }

    /** Todos los campos, ordenados por numero de etiqueta. Una copia del arreglo. */
    public TIFFField[] getTIFFFields() {
        return this.fields.values().toArray(new TIFFField[this.fields.size()]);
    }

    /** Saca todos los campos. Los conjuntos y la etiqueta padre quedan. */
    public void removeTIFFFields() {
        this.fields.clear();
    }

    /**
     * Este directorio como metadatos de imagen.
     *
     * <p>Es una <b>foto</b>: los cambios posteriores a este directorio no se ven en los metadatos, ni
     * al reves. {@link #createFromMetadata} deshace el viaje.
     */
    public IIOMetadata getAsMetadata() {
        TIFFDirectory snapshot;
        try {
            snapshot = clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage());
        }
        return new TIFFMetadata(snapshot);
    }

    /**
     * Una copia con sus propios campos.
     *
     * <p>La lista de campos y la de conjuntos se copian, y cada campo tambien --con
     * {@link TIFFField#clone}--, asi que sacar o cambiar campos en la copia no toca al original.
     *
     * <p>El JDK aca comparte parte del estado: su copia mantiene sus propias etiquetas de numero bajo
     * pero comparte el mapa de las altas, y un {@code removeTIFFFields()} sobre el original le vacia a
     * la copia la mitad de los campos. Esta implementacion copia las dos, que es lo que una copia
     * significa.
     */
    @Override
    public TIFFDirectory clone() throws CloneNotSupportedException {
        TIFFDirectory copy = (TIFFDirectory) super.clone();
        copy.tagSets = new ArrayList<TIFFTagSet>(this.tagSets);
        copy.fields = new TreeMap<Integer, TIFFField>();
        for (java.util.Map.Entry<Integer, TIFFField> entry : this.fields.entrySet()) {
            copy.fields.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    /** El arbol de este directorio en el formato nativo, como nodo {@code TIFFIFD}. */
    IIOMetadataNode getAsIFDNode() {
        IIOMetadataNode ifd = new IIOMetadataNode("TIFFIFD");
        if (this.parentTag != null) {
            ifd.setAttribute("parentTagNumber", Integer.toString(this.parentTag.getNumber()));
            String parentName = this.parentTag.getName();
            if (parentName != null) {
                ifd.setAttribute("parentTagName", parentName);
            }
        }
        StringBuilder names = new StringBuilder();
        int i = 0;
        while (i < this.tagSets.size()) {
            TIFFTagSet set = this.tagSets.get(i);
            if (set != null) {
                if (names.length() > 0) {
                    names.append(',');
                }
                names.append(set.getClass().getName());
            }
            i = i + 1;
        }
        ifd.setAttribute("tagSets", names.toString());
        for (TIFFField field : this.fields.values()) {
            if (field.hasDirectory()) {
                // Un puntero no se escribe como campo: se escribe como el directorio al que apunta,
                // anidado, con el numero de la etiqueta que lo trajo. Asi el arbol tiene la misma
                // forma que el archivo.
                ifd.appendChild(field.getDirectory().getAsIFDNode());
            } else {
                ifd.appendChild(field.getAsNativeNode());
            }
        }
        return ifd;
    }

    /** Reconstruye un directorio desde un nodo {@code TIFFIFD}. */
    static TIFFDirectory fromIFDNode(Node ifd, TIFFTag parentTag) throws IIOInvalidTreeException {
        List<TIFFTagSet> sets = new ArrayList<TIFFTagSet>();
        String names = attribute(ifd, "tagSets");
        if (names != null && names.length() > 0) {
            String[] pieces = names.split(",");
            int i = 0;
            while (i < pieces.length) {
                TIFFTagSet set = tagSetForClassName(pieces[i].trim());
                if (set != null) {
                    sets.add(set);
                }
                i = i + 1;
            }
        }
        TIFFDirectory dir =
            new TIFFDirectory(sets.toArray(new TIFFTagSet[sets.size()]), parentTag);
        NodeList children = ifd.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String name = child.getNodeName();
                if ("TIFFField".equals(name)) {
                    try {
                        dir.addTIFFField(TIFFField.createFromMetadataNode(null, child));
                    } catch (RuntimeException e) {
                        throw new IIOInvalidTreeException(e.getMessage(), e, child);
                    }
                } else if ("TIFFIFD".equals(name)) {
                    dir.addTIFFField(subdirectoryField(dir, child));
                } else {
                    throw new IIOInvalidTreeException("Unexpected node " + name, child);
                }
            }
            i = i + 1;
        }
        // Los campos se leyeron sin conjunto para no resolver contra uno equivocado; ahora que el
        // directorio ya tiene los suyos, se vuelven a resolver y toman su nombre.
        dir.resolveNames();
        return dir;
    }

    /** Vuelve a resolver los numeros de etiqueta contra los conjuntos de este directorio. */
    private void resolveNames() {
        TreeMap<Integer, TIFFField> resolved = new TreeMap<Integer, TIFFField>();
        for (java.util.Map.Entry<Integer, TIFFField> entry : this.fields.entrySet()) {
            TIFFField field = entry.getValue();
            TIFFTag tag = getTag(field.getTagNumber());
            if (tag != null && tag != field.getTag() && !field.hasDirectory()
                && tag.isDataTypeOK(field.getType())) {
                field = new TIFFField(tag, field.getType(), field.getCount(), field.getData());
            }
            resolved.put(entry.getKey(), field);
        }
        this.fields = resolved;
    }

    /** El campo puntero que representa a un directorio anidado del arbol. */
    private static TIFFField subdirectoryField(TIFFDirectory parent, Node ifd)
            throws IIOInvalidTreeException {
        String numberText = attribute(ifd, "parentTagNumber");
        if (numberText == null) {
            throw new IIOInvalidTreeException("Nested TIFFIFD without parentTagNumber", ifd);
        }
        int number;
        try {
            number = Integer.parseInt(numberText);
        } catch (NumberFormatException e) {
            throw new IIOInvalidTreeException("Bad parentTagNumber " + numberText, ifd);
        }
        TIFFTag tag = parent.getTag(number);
        if (tag == null) {
            String name = attribute(ifd, "parentTagName");
            if (name == null) {
                name = TIFFTag.UNKNOWN_TAG_NAME;
            }
            tag = new TIFFTag(name, number, 1 << TIFFTag.TIFF_LONG);
        }
        TIFFDirectory sub = fromIFDNode(ifd, tag);
        // El arbol no dice en que posicion del archivo estaba el directorio --no es un dato del
        // formato de metadatos-- asi que se usa 1, que es lo que hace el JDK.
        return new TIFFField(tag, TIFFTag.TIFF_LONG, 1L, sub);
    }

    /** El conjunto de fabrica que se llama asi, o null si no es ninguno de los siete. */
    private static TIFFTagSet tagSetForClassName(String className) {
        if ("javax.imageio.plugins.tiff.BaselineTIFFTagSet".equals(className)) {
            return BaselineTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.FaxTIFFTagSet".equals(className)) {
            return FaxTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifParentTIFFTagSet".equals(className)) {
            return ExifParentTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifTIFFTagSet".equals(className)) {
            return ExifTIFFTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifGPSTagSet".equals(className)) {
            return ExifGPSTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.ExifInteroperabilityTagSet".equals(className)) {
            return ExifInteroperabilityTagSet.getInstance();
        }
        if ("javax.imageio.plugins.tiff.GeoTIFFTagSet".equals(className)) {
            return GeoTIFFTagSet.getInstance();
        }
        return byReflection(className);
    }

    /**
     * Un conjunto de un complemento, por su {@code getInstance} estatico.
     *
     * <p>Es la convencion que el arbol da por sentada al guardar solo el nombre de la clase. Si algo
     * falla --la clase no esta, no tiene el metodo, no devuelve un conjunto-- el conjunto se saltea:
     * un TIFF al que le falta un perfil se sigue leyendo, con las etiquetas de ese perfil anonimas.
     */
    private static TIFFTagSet byReflection(String className) {
        try {
            Class<?> cls = Class.forName(className);
            Object instance = cls.getMethod("getInstance").invoke(null);
            if (instance instanceof TIFFTagSet) {
                return (TIFFTagSet) instance;
            }
        } catch (Throwable e) {
            return null;
        }
        return null;
    }

    /** Si esos metadatos declaran el formato nativo de TIFF. */
    private static boolean supportsNativeFormat(IIOMetadata metadata) {
        String[] names = metadata.getMetadataFormatNames();
        if (names == null) {
            return false;
        }
        int i = 0;
        while (i < names.length) {
            if (NATIVE_FORMAT.equals(names[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** El valor de ese atributo, o null si no esta. */
    private static String attribute(Node node, String name) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) {
            return null;
        }
        Node attr = attrs.getNamedItem(name);
        if (attr == null) {
            return null;
        }
        return attr.getNodeValue();
    }

    /** El primer hijo que sea un elemento, o null. */
    private static Node firstElement(Node node) {
        NodeList children = node.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return children.item(i);
            }
            i = i + 1;
        }
        return null;
    }
}
