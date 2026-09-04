package javax.imageio.metadata;

import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadata -- los metadatos de una imagen o de un flujo.
 *
 * <p>Todo lo que un archivo de imagen guarda ademas de los pixeles: resolucion, fecha, autor, perfil
 * de color, comentarios, datos de la camara.
 *
 * <h2>Los dos formatos, y por que hacen falta los dos</h2>
 *
 * <p>Los mismos metadatos se pueden ver de dos maneras:
 *
 * <ul>
 *   <li>el formato <b>nativo</b>, que refleja exactamente como el archivo los guarda. No pierde nada y
 *       no se parece al de ningun otro formato;
 *   <li>el formato <b>estandar</b> {@code javax_imageio_1.0}, comun a todos. Pierde lo que sea
 *       especifico, y a cambio permite escribir codigo que funcione con cualquier formato.
 * </ul>
 *
 * <p>{@link #isStandardMetadataFormatSupported} dice si el estandar esta disponible.
 * {@link #getMetadataFormatNames} lista todos los que este objeto entiende.
 *
 * <p>La conversion entre formatos --que es como se copian metadatos de PNG a JPEG-- pasa por el
 * estandar; ver {@code javax.imageio.ImageTranscoder}.
 *
 * <h2>{@link #mergeTree} contra {@link #setFromTree}</h2>
 *
 * <p>Es la distincion que hay que tener clara antes de tocar metadatos:
 *
 * <ul>
 *   <li>{@code mergeTree} <b>combina</b>: lo que el arbol nuevo dice reemplaza lo que habia, y lo que
 *       no menciona queda como estaba;
 *   <li>{@code setFromTree} <b>reemplaza</b>: primero borra todo y despues aplica el arbol.
 * </ul>
 *
 * <p>Usar el primero creyendo que hace el segundo deja metadatos viejos pegados, y es la causa mas
 * comun de que una imagen reescrita conserve datos que se querian borrar.
 *
 * <h2>Los metadatos pueden ser de solo lectura</h2>
 *
 * <p>{@link #isReadOnly} lo dice, y hay que preguntarlo: los de una imagen recien leida suelen serlo.
 * Intentar modificarlos lanza {@link IllegalStateException}.
 *
 * <h2>Los ocho nodos estandar</h2>
 *
 * <p>Los ocho {@code getStandardXxxNode} son protegidos y devuelven cada rama del formato estandar.
 * {@link #getStandardTree} los junta en el arbol completo, y es final: una subclase redefine las ramas
 * que sepa llenar y hereda el armado.
 *
 * <p>Por omision los ocho devuelven null, que significa "de esto no se nada". Es lo correcto: un
 * formato que no guarda fecha no deberia inventar una.
 */
public abstract class IIOMetadata {

    /** Si el formato estandar esta disponible. */
    protected boolean standardFormatSupported;

    /** Como se llama el formato nativo, o null si no hay. */
    protected String nativeMetadataFormatName = null;

    /** La clase que describe ese formato. */
    protected String nativeMetadataFormatClassName = null;

    /** Otros formatos que este objeto entiende. */
    protected String[] extraMetadataFormatNames = null;

    /** Las clases que los describen. */
    protected String[] extraMetadataFormatClassNames = null;

    /** El controlador de fabrica, o null. */
    protected IIOMetadataController defaultController = null;

    /** El que esta puesto. */
    protected IIOMetadataController controller = null;

    /** Sin formato estandar y sin nativo. */
    protected IIOMetadata() {
    }

    /**
     * Declarando que formatos se entienden.
     *
     * @param standardMetadataFormatSupported si el estandar esta disponible
     * @param nativeMetadataFormatName el nativo, o null
     * @param extraMetadataFormatNames otros, o null
     * @throws IllegalArgumentException si los arreglos de nombres y de clases no coinciden en largo,
     *     o si un nombre esta vacio
     */
    protected IIOMetadata(boolean standardMetadataFormatSupported,
                          String nativeMetadataFormatName,
                          String nativeMetadataFormatClassName,
                          String[] extraMetadataFormatNames,
                          String[] extraMetadataFormatClassNames) {
        this.standardFormatSupported = standardMetadataFormatSupported;
        this.nativeMetadataFormatName = nativeMetadataFormatName;
        this.nativeMetadataFormatClassName = nativeMetadataFormatClassName;
        if (extraMetadataFormatNames != null) {
            if (extraMetadataFormatNames.length == 0) {
                throw new IllegalArgumentException("extraMetadataFormatNames.length == 0!");
            }
            if (extraMetadataFormatClassNames == null) {
                throw new IllegalArgumentException("extraMetadataFormatClassNames == null!");
            }
            if (extraMetadataFormatClassNames.length != extraMetadataFormatNames.length) {
                throw new IllegalArgumentException(
                    "extraMetadataFormatClassNames.length != extraMetadataFormatNames.length!");
            }
            this.extraMetadataFormatNames = copy(extraMetadataFormatNames);
            this.extraMetadataFormatClassNames = copy(extraMetadataFormatClassNames);
        } else {
            if (extraMetadataFormatClassNames != null) {
                throw new IllegalArgumentException(
                    "extraMetadataFormatNames == null && extraMetadataFormatClassNames != null!");
            }
        }
    }

    /** Si el formato estandar esta disponible. */
    public boolean isStandardMetadataFormatSupported() {
        return this.standardFormatSupported;
    }

    /** Si no se pueden modificar. Ver la nota de la clase. */
    public abstract boolean isReadOnly();

    /** Como se llama el formato nativo, o null. */
    public String getNativeMetadataFormatName() {
        return this.nativeMetadataFormatName;
    }

    /** Los otros formatos, o null. Es una copia. */
    public String[] getExtraMetadataFormatNames() {
        return copy(this.extraMetadataFormatNames);
    }

    /**
     * Todos los formatos que este objeto entiende.
     *
     * <p>El nativo primero, despues el estandar si esta, y despues los extra. El orden importa: un
     * programa que quiera la maxima fidelidad toma el primero.
     *
     * @return null si no entiende ninguno
     */
    public String[] getMetadataFormatNames() {
        String nativeName = getNativeMetadataFormatName();
        String standardName = null;
        if (isStandardMetadataFormatSupported()) {
            standardName = IIOMetadataFormatImpl.standardMetadataFormatName;
        }
        String[] extraNames = getExtraMetadataFormatNames();
        int count = 0;
        if (nativeName != null) {
            count = count + 1;
        }
        if (standardName != null) {
            count = count + 1;
        }
        if (extraNames != null) {
            count = count + extraNames.length;
        }
        if (count == 0) {
            return null;
        }
        String[] result = new String[count];
        int at = 0;
        if (nativeName != null) {
            result[at] = nativeName;
            at = at + 1;
        }
        if (standardName != null) {
            result[at] = standardName;
            at = at + 1;
        }
        if (extraNames != null) {
            int i = 0;
            while (i < extraNames.length) {
                result[at] = extraNames[i];
                at = at + 1;
                i = i + 1;
            }
        }
        return result;
    }

    /**
     * El esquema de ese formato.
     *
     * <p>Se carga por reflexion desde el nombre de clase que se declaro, buscando su metodo estatico
     * {@code getInstance}. Es la convencion que la documentacion pide, y es como un formato definido
     * por un complemento se hace visible sin que esta clase lo conozca.
     *
     * @throws IllegalArgumentException si el nombre es null o no es uno de los declarados
     * @throws IllegalStateException si la clase que lo describe no se pudo cargar
     */
    public IIOMetadataFormat getMetadataFormat(String formatName) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (this.standardFormatSupported
            && formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return IIOMetadataFormatImpl.getStandardFormatInstance();
        }
        String className = null;
        if (formatName.equals(this.nativeMetadataFormatName)) {
            className = this.nativeMetadataFormatClassName;
        } else if (this.extraMetadataFormatNames != null) {
            int i = 0;
            while (i < this.extraMetadataFormatNames.length) {
                if (formatName.equals(this.extraMetadataFormatNames[i])) {
                    className = this.extraMetadataFormatClassNames[i];
                }
                i = i + 1;
            }
        }
        if (className == null) {
            throw new IllegalArgumentException("Unsupported format name");
        }
        try {
            Class<?> cls = Class.forName(className, true, getClass().getClassLoader());
            java.lang.reflect.Method meth = cls.getMethod("getInstance");
            return (IIOMetadataFormat) meth.invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException("Can't obtain format");
        }
    }

    /**
     * El arbol de metadatos en ese formato.
     *
     * @return null si este objeto no tiene nada que decir en ese formato
     * @throws IllegalArgumentException si el formato no es uno de los declarados
     */
    public abstract Node getAsTree(String formatName);

    /**
     * Combina ese arbol con lo que ya hay. Ver la nota de la clase.
     *
     * @throws IllegalStateException si son de solo lectura
     * @throws IllegalArgumentException si el formato no es uno de los declarados
     * @throws IIOInvalidTreeException si el arbol no cumple el formato
     */
    public abstract void mergeTree(String formatName, Node root) throws IIOInvalidTreeException;

    /** La rama de color del formato estandar, o null si no se sabe. */
    protected IIOMetadataNode getStandardChromaNode() {
        return null;
    }

    /** La de compresion, o null. */
    protected IIOMetadataNode getStandardCompressionNode() {
        return null;
    }

    /** La de organizacion de los datos, o null. */
    protected IIOMetadataNode getStandardDataNode() {
        return null;
    }

    /** La de tamano y resolucion, o null. */
    protected IIOMetadataNode getStandardDimensionNode() {
        return null;
    }

    /** La de fecha y version, o null. */
    protected IIOMetadataNode getStandardDocumentNode() {
        return null;
    }

    /** La de los textos incrustados, o null. */
    protected IIOMetadataNode getStandardTextNode() {
        return null;
    }

    /** La de mosaico, o null. */
    protected IIOMetadataNode getStandardTileNode() {
        return null;
    }

    /** La de transparencia, o null. */
    protected IIOMetadataNode getStandardTransparencyNode() {
        return null;
    }

    /**
     * El arbol estandar completo, armado con las ocho ramas.
     *
     * <p>Es final: una subclase redefine las ramas que sepa llenar y hereda el armado. Las que
     * devuelvan null no aparecen.
     */
    protected final IIOMetadataNode getStandardTree() {
        IIOMetadataNode root =
            new IIOMetadataNode(IIOMetadataFormatImpl.standardMetadataFormatName);
        appendIfPresent(root, getStandardChromaNode());
        appendIfPresent(root, getStandardCompressionNode());
        appendIfPresent(root, getStandardDataNode());
        appendIfPresent(root, getStandardDimensionNode());
        appendIfPresent(root, getStandardDocumentNode());
        appendIfPresent(root, getStandardTextNode());
        appendIfPresent(root, getStandardTileNode());
        appendIfPresent(root, getStandardTransparencyNode());
        return root;
    }

    /**
     * Reemplaza todo por ese arbol. Ver la nota de la clase.
     *
     * <p>Esta implementacion es {@link #reset} seguido de {@link #mergeTree}, que es exactamente lo
     * que significa.
     *
     * @throws IllegalStateException si son de solo lectura
     * @throws IllegalArgumentException si el formato no es uno de los declarados
     * @throws IIOInvalidTreeException si el arbol no cumple el formato
     */
    public void setFromTree(String formatName, Node root) throws IIOInvalidTreeException {
        reset();
        mergeTree(formatName, root);
    }

    /**
     * Vuelve al estado inicial.
     *
     * @throws IllegalStateException si son de solo lectura
     */
    public abstract void reset();

    /** Quien completa estos metadatos; null usa el de fabrica. */
    public void setController(IIOMetadataController controller) {
        this.controller = controller;
    }

    /** El que esta puesto. */
    public IIOMetadataController getController() {
        return this.controller;
    }

    /** El de fabrica, o null. */
    public IIOMetadataController getDefaultController() {
        return this.defaultController;
    }

    /** Si hay alguno. */
    public boolean hasController() {
        return getController() != null;
    }

    /**
     * Le pide al controlador que los complete.
     *
     * @return si el usuario acepto
     * @throws IllegalStateException si no hay controlador
     */
    public boolean activateController() {
        if (!hasController()) {
            throw new IllegalStateException("hasController() == false!");
        }
        return getController().activate(this);
    }

    /** Agrega la rama si no es null. */
    private static void appendIfPresent(IIOMetadataNode root, IIOMetadataNode node) {
        if (node != null) {
            root.appendChild(node);
        }
    }

    /** Una copia del arreglo, o null. */
    private static String[] copy(String[] source) {
        if (source == null) {
            return null;
        }
        String[] result = new String[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
