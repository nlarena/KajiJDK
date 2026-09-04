package javax.imageio.plugins.tiff;

import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFField -- una etiqueta con su valor.
 *
 * <p>{@link TIFFTag} describe una etiqueta; esto es una <b>instancia</b> de esa etiqueta en un
 * archivo concreto, ya con los datos.
 *
 * <h2>El dato es un {@link Object} y su clase depende del tipo</h2>
 *
 * <p>Es lo que hay que entender antes de tocar {@link #getData}:
 *
 * <table border="1">
 * <caption>Que arreglo lleva cada tipo</caption>
 * <tr><th>tipo</th><th>clase del dato</th></tr>
 * <tr><td>BYTE, SBYTE, UNDEFINED</td><td>{@code byte[]}</td></tr>
 * <tr><td>ASCII</td><td>{@code String[]}</td></tr>
 * <tr><td>SHORT</td><td>{@code char[]}</td></tr>
 * <tr><td>SSHORT</td><td>{@code short[]}</td></tr>
 * <tr><td>LONG, IFD_POINTER</td><td>{@code long[]}</td></tr>
 * <tr><td>SLONG</td><td>{@code int[]}</td></tr>
 * <tr><td>RATIONAL</td><td>{@code long[][]}</td></tr>
 * <tr><td>SRATIONAL</td><td>{@code int[][]}</td></tr>
 * <tr><td>FLOAT</td><td>{@code float[]}</td></tr>
 * <tr><td>DOUBLE</td><td>{@code double[]}</td></tr>
 * </table>
 *
 * <p>Las dos que sorprenden son SHORT en {@code char[]} --porque un short de TIFF es <b>sin
 * signo</b>, y {@code char} es el unico entero sin signo de Java-- y LONG en {@code long[]}, por lo
 * mismo: un long de TIFF son 32 bits sin signo, que no entran en un {@code int}.
 *
 * <h2>Los {@code getAsXxx()} en plural son casts, no conversiones</h2>
 *
 * <p>Salvo {@link #getAsInts}, que si convierte desde {@code char[]} y {@code short[]}, los metodos
 * que devuelven el arreglo entero son <b>casts</b>: {@link #getAsDoubles} sobre un RATIONAL tira
 * {@link ClassCastException}, no divide.
 *
 * <p>Los que toman un indice --{@link #getAsLong}, {@link #getAsDouble}-- si convierten, y son la
 * forma segura de leer un valor sin saber de que tipo vino.
 *
 * <h2>Los racionales</h2>
 *
 * <p>Un RATIONAL son <b>dos</b> enteros: numerador y denominador. {@link #getAsRational} devuelve el
 * par; {@link #getAsDouble} hace la division. La resolucion de un TIFF se guarda asi, y por eso 300
 * puntos por pulgada aparece como {@code 300/1}.
 *
 * <p>{@link #getValueAsString} de un racional devuelve {@code "72/1"}, no {@code "72.0"}: es la forma
 * textual del formato, no el resultado de la cuenta.
 */
public final class TIFFField implements Cloneable {

    /** Como se llama cada tipo. La posicion 0 no se usa. */
    private static final String[] TYPE_NAMES = {
        null, "Byte", "Ascii", "Short", "Long", "Rational", "SByte", "Undefined", "SShort",
        "SLong", "SRational", "Float", "Double", "IFDPointer",
    };

    /** Que etiqueta es. */
    private final TIFFTag tag;

    /** De que tipo son los datos. */
    private final int type;

    /** Cuantos valores. */
    private final int count;

    /** El arreglo; su clase depende del tipo. Ver la nota de la clase. */
    private Object data;

    /** El directorio al que apunta, si es una etiqueta puntero. */
    private TIFFDirectory dir = null;

    /**
     * Una etiqueta con sus datos.
     *
     * <p>El arreglo <b>no</b> se copia, pero si se valida: tiene que ser de la clase que el tipo pide
     * y de largo exactamente {@code count}.
     *
     * @param type uno de los {@code TIFF_} de {@link TIFFTag}
     * @param count cuantos valores
     * @param data el arreglo de la clase que corresponde al tipo
     * @throws NullPointerException si la etiqueta o los datos son null
     * @throws IllegalArgumentException si el tipo no existe, si no sirve para esa etiqueta, si la
     *     cantidad es negativa, o si el arreglo no es de la clase o el largo que el tipo pide
     */
    public TIFFField(TIFFTag tag, int type, int count, Object data) {
        if (tag == null) {
            throw new NullPointerException("tag == null!");
        }
        if (type < TIFFTag.MIN_DATATYPE || type > TIFFTag.MAX_DATATYPE) {
            throw new IllegalArgumentException("Unknown data type " + type);
        }
        if (!tag.isDataTypeOK(type)) {
            throw new IllegalArgumentException("Illegal data type " + type + " for "
                + tag.getName() + " tag");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count < 0!");
        }
        if (data == null) {
            throw new NullPointerException("data == null!");
        }
        if (!isDataOK(type, count, data)) {
            throw new IllegalArgumentException("Illegal class or length for data array");
        }
        this.tag = tag;
        this.type = type;
        this.count = count;
        this.data = data;
    }

    /**
     * Una etiqueta sin datos: un arreglo vacio del tipo, del largo pedido.
     *
     * @throws NullPointerException si la etiqueta es null
     * @throws IllegalArgumentException si el tipo no sirve o la cantidad es negativa
     */
    public TIFFField(TIFFTag tag, int type, int count) {
        this(tag, type, count, createArrayForType(type, count));
    }

    /**
     * Un solo valor entero, con el tipo mas chico que lo aguante.
     *
     * <p>Elige SHORT si el valor entra en 16 bits sin signo y LONG si no. Ojo: elige <b>antes</b> de
     * mirar que tipos acepta la etiqueta, asi que un 7 en una etiqueta que solo acepta LONG no
     * termina en LONG sino en un {@link IllegalArgumentException}. Es asi tambien en el JDK.
     *
     * @throws NullPointerException si la etiqueta es null
     * @throws IllegalArgumentException si el valor es negativo, si no entra en 32 bits sin signo, o
     *     si el tipo elegido no sirve para esa etiqueta
     */
    public TIFFField(TIFFTag tag, long value) {
        if (tag == null) {
            throw new NullPointerException("tag == null!");
        }
        if (value < 0) {
            throw new IllegalArgumentException("value < 0!");
        }
        if (value > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("value > 0xffffffff!");
        }
        this.tag = tag;
        this.count = 1;
        if (value < 0x10000L) {
            this.type = TIFFTag.TIFF_SHORT;
            this.data = new char[] { (char) value };
        } else {
            this.type = TIFFTag.TIFF_LONG;
            this.data = new long[] { value };
        }
        if (!tag.isDataTypeOK(this.type)) {
            throw new IllegalArgumentException("Illegal data type " + TYPE_NAMES[this.type]
                + " for tag \"" + tag.getName() + "\"");
        }
    }

    /**
     * Una etiqueta puntero, con el directorio al que apunta.
     *
     * @param type tiene que ser {@link TIFFTag#TIFF_LONG} o {@link TIFFTag#TIFF_IFD_POINTER}
     * @param offset la posicion del directorio en el archivo; tiene que ser positiva
     * @throws NullPointerException si la etiqueta o el directorio son null
     * @throws IllegalArgumentException si el tipo no es uno de esos dos, si no sirve para la
     *     etiqueta, o si el desplazamiento no es positivo
     */
    public TIFFField(TIFFTag tag, int type, long offset, TIFFDirectory dir) {
        if (type != TIFFTag.TIFF_LONG && type != TIFFTag.TIFF_IFD_POINTER) {
            throw new IllegalArgumentException("type " + type
                + " is neither TIFFTag.TIFF_LONG nor TIFFTag.TIFF_IFD_POINTER");
        }
        if (tag == null) {
            throw new NullPointerException("tag == null!");
        }
        if (!tag.isDataTypeOK(type)) {
            throw new IllegalArgumentException("Illegal data type " + type + " for "
                + tag.getName() + " tag");
        }
        // Un desplazamiento cero o negativo no es una posicion posible: los primeros ocho bytes de un
        // TIFF son la cabecera, asi que ningun directorio empieza ahi.
        if (offset <= 0) {
            throw new IllegalArgumentException("offset " + offset + " is non-positive");
        }
        if (dir == null) {
            throw new NullPointerException("dir == null");
        }
        this.tag = tag;
        this.type = type;
        this.count = 1;
        this.data = new long[] { offset };
        this.dir = dir;
    }

    /** Que etiqueta es. */
    public TIFFTag getTag() {
        return this.tag;
    }

    /** Su numero. */
    public int getTagNumber() {
        return this.tag.getNumber();
    }

    /** De que tipo son los datos. */
    public int getType() {
        return this.type;
    }

    /**
     * Como se llama ese tipo: {@code "Rational"}, {@code "SLong"}...
     *
     * @throws IllegalArgumentException si no es uno de los trece
     */
    public static String getTypeName(int dataType) {
        if (dataType < TIFFTag.MIN_DATATYPE || dataType > TIFFTag.MAX_DATATYPE) {
            throw new IllegalArgumentException("Unknown data type " + dataType);
        }
        return TYPE_NAMES[dataType];
    }

    /**
     * El numero de ese tipo, o -1 si no existe.
     *
     * <p>Distingue mayusculas: {@code "SRational"} si, {@code "SRATIONAL"} no.
     */
    public static int getTypeByName(String typeName) {
        int i = TIFFTag.MIN_DATATYPE;
        while (i <= TIFFTag.MAX_DATATYPE) {
            if (TYPE_NAMES[i].equals(typeName)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * Un arreglo vacio de la clase que ese tipo pide. Ver la nota de la clase.
     *
     * @throws IllegalArgumentException si el tipo no es uno de los trece o la cantidad es negativa
     */
    public static Object createArrayForType(int dataType, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0!");
        }
        if (dataType == TIFFTag.TIFF_BYTE || dataType == TIFFTag.TIFF_SBYTE
            || dataType == TIFFTag.TIFF_UNDEFINED) {
            return new byte[count];
        }
        if (dataType == TIFFTag.TIFF_ASCII) {
            return new String[count];
        }
        if (dataType == TIFFTag.TIFF_SHORT) {
            return new char[count];
        }
        if (dataType == TIFFTag.TIFF_SSHORT) {
            return new short[count];
        }
        if (dataType == TIFFTag.TIFF_LONG || dataType == TIFFTag.TIFF_IFD_POINTER) {
            return new long[count];
        }
        if (dataType == TIFFTag.TIFF_SLONG) {
            return new int[count];
        }
        if (dataType == TIFFTag.TIFF_RATIONAL) {
            return new long[count][2];
        }
        if (dataType == TIFFTag.TIFF_SRATIONAL) {
            return new int[count][2];
        }
        if (dataType == TIFFTag.TIFF_FLOAT) {
            return new float[count];
        }
        if (dataType == TIFFTag.TIFF_DOUBLE) {
            return new double[count];
        }
        throw new IllegalArgumentException("Unknown data type " + dataType);
    }

    /**
     * Este campo como arbol del formato nativo de TIFF.
     *
     * <p>La forma normal es un {@code TIFFField} con el numero y el nombre, un hijo con el
     * <b>plural</b> del tipo --{@code TIFFLongs}-- y un nieto por valor. La pluralizacion no es
     * cosmetica: es como el formato nativo distingue el campo de sus valores.
     *
     * <p>UNDEFINED es la excepcion: no tiene plural ni un nodo por valor, sino un unico
     * {@code TIFFUndefined} con todos los bytes separados por comas y <b>sin signo</b>. Tiene sentido
     * --un UNDEFINED puede tener miles de bytes y no vale la pena un elemento por cada uno-- pero hay
     * que tenerlo presente al recorrer el arbol.
     */
    public Node getAsNativeNode() {
        IIOMetadataNode field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", Integer.toString(getTagNumber()));
        String tagName = this.tag.getName();
        if (tagName != null) {
            field.setAttribute("name", tagName);
        }
        if (this.type == TIFFTag.TIFF_UNDEFINED) {
            IIOMetadataNode values = new IIOMetadataNode("TIFFUndefined");
            byte[] bytes = (byte[]) this.data;
            StringBuilder text = new StringBuilder();
            int i = 0;
            while (i < this.count) {
                if (i > 0) {
                    text.append(',');
                }
                text.append(bytes[i] & 0xFF);
                i = i + 1;
            }
            values.setAttribute("value", text.toString());
            field.appendChild(values);
            return field;
        }
        String typeName = TYPE_NAMES[this.type];
        IIOMetadataNode values = new IIOMetadataNode("TIFF" + typeName + "s");
        int i = 0;
        while (i < this.count) {
            IIOMetadataNode value = new IIOMetadataNode("TIFF" + typeName);
            value.setAttribute("value", getValueAsString(i));
            values.appendChild(value);
            i = i + 1;
        }
        field.appendChild(values);
        return field;
    }

    /**
     * Lee un campo de un arbol del formato nativo. La inversa de {@link #getAsNativeNode}.
     *
     * <p>El atributo {@code name} del nodo se ignora: el nombre sale de resolver el numero contra
     * {@code tagSet}. Si no hay conjunto, o el numero no esta en el, la etiqueta queda anonima --
     * {@link TIFFTag#UNKNOWN_TAG_NAME}, cantidad -1 y el tipo que traiga el arbol como unico tipo
     * aceptado--.
     *
     * @param tagSet contra que conjunto resolver el numero; null lo deja anonimo
     * @throws IllegalArgumentException si el nodo es null o no tiene la forma esperada
     * @throws NullPointerException si falta el atributo {@code number} o el {@code value} de un valor
     */
    public static TIFFField createFromMetadataNode(TIFFTagSet tagSet, Node node) {
        if (node == null) {
            // Envuelto a proposito: el metodo esta especificado para tirar IllegalArgumentException,
            // pero el JDK deja el NullPointerException adentro como causa y esto lo copia.
            throw new IllegalArgumentException(new NullPointerException("node == null!"));
        }
        String name = node.getNodeName();
        if (!"TIFFField".equals(name)) {
            throw new IllegalArgumentException("!name.equals(\"TIFFField\")");
        }
        int number = Integer.parseInt(attribute(node, "number"));
        Node valuesNode = firstElement(node);
        if (valuesNode == null) {
            throw new IllegalArgumentException("TIFFField node has no children");
        }
        String container = valuesNode.getNodeName();
        int type;
        int count;
        Object data;
        if ("TIFFUndefined".equals(container)) {
            type = TIFFTag.TIFF_UNDEFINED;
            String[] pieces = attribute(valuesNode, "value").split(",");
            count = pieces.length;
            byte[] bytes = new byte[count];
            int i = 0;
            while (i < count) {
                bytes[i] = (byte) Integer.parseInt(pieces[i].trim());
                i = i + 1;
            }
            data = bytes;
        } else {
            // El JDK recorta cuatro por delante y uno por detras sin mirar; un nombre que no sea
            // "TIFF<tipo>s" cae despues en getTypeByName, con el recorte a la vista en el mensaje.
            String typeName = container.substring(4, container.length() - 1);
            type = getTypeByName(typeName);
            if (type == -1) {
                throw new IllegalArgumentException("typeName = " + typeName);
            }
            NodeList children = valuesNode.getChildNodes();
            count = countElements(children);
            data = count == 0 ? null : createArrayForType(type, count);
            int at = 0;
            int i = 0;
            while (i < children.getLength()) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    setFromString(data, at, attribute(child, "value"));
                    at = at + 1;
                }
                i = i + 1;
            }
        }
        TIFFTag tag = null;
        if (tagSet != null) {
            tag = tagSet.getTag(number);
        }
        if (tag == null) {
            tag = new TIFFTag(TIFFTag.UNKNOWN_TAG_NAME, number, 1 << type);
        }
        try {
            return new TIFFField(tag, type, count, data);
        } catch (NullPointerException e) {
            // Un contenedor sin valores deja los datos en null; ahi el constructor tira un
            // NullPointerException que este metodo tiene que presentar como argumento invalido.
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Si los valores son enteros.
     *
     * <p>Los racionales, los flotantes y el texto no lo son; UNDEFINED si, porque son bytes.
     */
    public boolean isIntegral() {
        return this.type != TIFFTag.TIFF_ASCII && this.type != TIFFTag.TIFF_RATIONAL
            && this.type != TIFFTag.TIFF_SRATIONAL && this.type != TIFFTag.TIFF_FLOAT
            && this.type != TIFFTag.TIFF_DOUBLE;
    }

    /** Cuantos valores. */
    public int getCount() {
        return this.count;
    }

    /** El arreglo, sin copiar. Ver la nota de la clase sobre su clase. */
    public Object getData() {
        return this.data;
    }

    /**
     * Los datos como bytes. Es un cast.
     *
     * @throws ClassCastException si el tipo no guarda bytes
     */
    public byte[] getAsBytes() {
        return (byte[]) this.data;
    }

    /**
     * Como caracteres; es el tipo SHORT. Es un cast. Ver la nota de la clase.
     *
     * @throws ClassCastException si el tipo no es SHORT
     */
    public char[] getAsChars() {
        return (char[]) this.data;
    }

    /**
     * Como shorts con signo. Es un cast.
     *
     * @throws ClassCastException si el tipo no es SSHORT
     */
    public short[] getAsShorts() {
        return (short[]) this.data;
    }

    /**
     * Como enteros.
     *
     * <p>El unico en plural que convierte: acepta {@code char[]} y {@code short[]} ademas de
     * {@code int[]}. LONG no entra en un int y hay que pedir {@link #getAsLongs}.
     *
     * @throws ClassCastException si los datos no son char[], short[] ni int[]
     */
    public int[] getAsInts() {
        if (this.data instanceof int[]) {
            return (int[]) this.data;
        }
        if (this.data instanceof char[]) {
            char[] source = (char[]) this.data;
            int[] result = new int[source.length];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i] & 0xFFFF;
                i = i + 1;
            }
            return result;
        }
        if (this.data instanceof short[]) {
            short[] source = (short[]) this.data;
            int[] result = new int[source.length];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i];
                i = i + 1;
            }
            return result;
        }
        throw new ClassCastException("Data not char[], short[], or int[]!");
    }

    /**
     * Como enteros largos. Es un cast.
     *
     * @throws ClassCastException si el tipo no es LONG ni IFD_POINTER
     */
    public long[] getAsLongs() {
        return (long[]) this.data;
    }

    /**
     * Como coma flotante de cuatro bytes. Es un cast.
     *
     * @throws ClassCastException si el tipo no es FLOAT
     */
    public float[] getAsFloats() {
        return (float[]) this.data;
    }

    /**
     * Como coma flotante de ocho bytes. Es un cast: no divide racionales.
     *
     * @throws ClassCastException si el tipo no es DOUBLE
     */
    public double[] getAsDoubles() {
        return (double[]) this.data;
    }

    /**
     * Como pares con signo. Es un cast.
     *
     * @throws ClassCastException si el tipo no es SRATIONAL
     */
    public int[][] getAsSRationals() {
        return (int[][]) this.data;
    }

    /**
     * Como pares sin signo. Es un cast.
     *
     * @throws ClassCastException si el tipo no es RATIONAL
     */
    public long[][] getAsRationals() {
        return (long[][]) this.data;
    }

    /**
     * El valor numero {@code index} como entero, convirtiendo lo que haga falta.
     *
     * @throws ClassCastException si el tipo no se puede convertir
     */
    public int getAsInt(int index) {
        return (int) getAsLong(index);
    }

    /**
     * Como entero largo, convirtiendo: un byte se lee sin signo, un flotante se trunca, un racional
     * se divide y se trunca, y un texto se parsea.
     *
     * @throws ClassCastException si el tipo no se puede convertir
     * @throws NumberFormatException si es texto y no es un numero
     */
    public long getAsLong(int index) {
        if (this.data instanceof byte[]) {
            // SBYTE es el unico byte con signo del formato; BYTE y UNDEFINED se leen sin el.
            byte value = ((byte[]) this.data)[index];
            return this.type == TIFFTag.TIFF_SBYTE ? value : value & 0xFF;
        }
        if (this.data instanceof char[]) {
            return ((char[]) this.data)[index] & 0xFFFF;
        }
        if (this.data instanceof short[]) {
            return ((short[]) this.data)[index];
        }
        if (this.data instanceof int[]) {
            return ((int[]) this.data)[index];
        }
        if (this.data instanceof long[]) {
            return ((long[]) this.data)[index];
        }
        if (this.data instanceof String[]) {
            return Long.parseLong(((String[]) this.data)[index]);
        }
        return (long) getAsDouble(index);
    }

    /**
     * Como coma flotante, convirtiendo.
     *
     * @throws ClassCastException si el tipo no se puede convertir
     */
    public float getAsFloat(int index) {
        return (float) getAsDouble(index);
    }

    /**
     * Como coma flotante de ocho bytes, convirtiendo: un racional se divide aca, y un texto se
     * parsea.
     *
     * @throws ClassCastException si el tipo no se puede convertir
     * @throws NumberFormatException si es texto y no es un numero
     */
    public double getAsDouble(int index) {
        if (this.data instanceof float[]) {
            return ((float[]) this.data)[index];
        }
        if (this.data instanceof double[]) {
            return ((double[]) this.data)[index];
        }
        if (this.data instanceof long[][]) {
            long[] pair = ((long[][]) this.data)[index];
            return (double) pair[0] / (double) pair[1];
        }
        if (this.data instanceof int[][]) {
            int[] pair = ((int[][]) this.data)[index];
            return (double) pair[0] / (double) pair[1];
        }
        if (this.data instanceof String[]) {
            return Double.parseDouble(((String[]) this.data)[index]);
        }
        return getAsLong(index);
    }

    /**
     * El texto numero {@code index}.
     *
     * @throws ClassCastException si el tipo no es ASCII
     */
    public String getAsString(int index) {
        return ((String[]) this.data)[index];
    }

    /**
     * El par con signo numero {@code index}.
     *
     * @throws ClassCastException si el tipo no es SRATIONAL
     */
    public int[] getAsSRational(int index) {
        return ((int[][]) this.data)[index];
    }

    /**
     * El par sin signo numero {@code index}.
     *
     * @throws ClassCastException si el tipo no es RATIONAL
     */
    public long[] getAsRational(int index) {
        return ((long[][]) this.data)[index];
    }

    /**
     * El valor numero {@code index} en la forma textual del formato.
     *
     * <p>Un racional sale como {@code "72/1"}, sin dividir; un byte sale sin signo. Ver la nota de la
     * clase.
     */
    public String getValueAsString(int index) {
        if (this.data instanceof String[]) {
            return ((String[]) this.data)[index];
        }
        if (this.data instanceof long[][]) {
            long[] pair = ((long[][]) this.data)[index];
            return pair[0] + "/" + pair[1];
        }
        if (this.data instanceof int[][]) {
            int[] pair = ((int[][]) this.data)[index];
            return pair[0] + "/" + pair[1];
        }
        if (this.data instanceof float[]) {
            return Float.toString(((float[]) this.data)[index]);
        }
        if (this.data instanceof double[]) {
            return Double.toString(((double[]) this.data)[index]);
        }
        return Long.toString(getAsLong(index));
    }

    /** Si esta etiqueta apunta a otro directorio. */
    public boolean hasDirectory() {
        return this.dir != null;
    }

    /** El directorio al que apunta, o null si no es un puntero. */
    public TIFFDirectory getDirectory() {
        return this.dir;
    }

    /**
     * Una copia con su propio arreglo.
     *
     * <p>Los datos se copian de verdad --incluidos los pares de un racional, elemento por elemento--:
     * una copia que compartiera el arreglo no seria una copia. La etiqueta si se comparte, porque es
     * inmutable.
     */
    @Override
    public TIFFField clone() throws CloneNotSupportedException {
        TIFFField copy = (TIFFField) super.clone();
        copy.data = cloneData(this.data);
        return copy;
    }

    /** Copia el arreglo, en profundidad si es de pares. */
    private static Object cloneData(Object data) {
        if (data instanceof long[][]) {
            long[][] source = (long[][]) data;
            long[][] result = new long[source.length][];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i].clone();
                i = i + 1;
            }
            return result;
        }
        if (data instanceof int[][]) {
            int[][] source = (int[][]) data;
            int[][] result = new int[source.length][];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i].clone();
                i = i + 1;
            }
            return result;
        }
        if (data instanceof byte[]) {
            return ((byte[]) data).clone();
        }
        if (data instanceof char[]) {
            return ((char[]) data).clone();
        }
        if (data instanceof short[]) {
            return ((short[]) data).clone();
        }
        if (data instanceof int[]) {
            return ((int[]) data).clone();
        }
        if (data instanceof long[]) {
            return ((long[]) data).clone();
        }
        if (data instanceof float[]) {
            return ((float[]) data).clone();
        }
        if (data instanceof double[]) {
            return ((double[]) data).clone();
        }
        if (data instanceof String[]) {
            return ((String[]) data).clone();
        }
        return data;
    }

    /**
     * Si el arreglo es de la clase y del largo que el tipo pide.
     *
     * <p>El largo se mira igual que la clase: un {@code char[5]} con {@code count} 1 miente sobre
     * cuantos valores hay, y el campo quedaria describiendo mal su propio contenido.
     */
    private static boolean isDataOK(int type, int count, Object data) {
        if (type == TIFFTag.TIFF_BYTE || type == TIFFTag.TIFF_SBYTE
            || type == TIFFTag.TIFF_UNDEFINED) {
            return data instanceof byte[] && ((byte[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_ASCII) {
            return data instanceof String[] && ((String[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_SHORT) {
            return data instanceof char[] && ((char[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_SSHORT) {
            return data instanceof short[] && ((short[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_LONG || type == TIFFTag.TIFF_IFD_POINTER) {
            return data instanceof long[] && ((long[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_SLONG) {
            return data instanceof int[] && ((int[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_RATIONAL) {
            if (!(data instanceof long[][])) {
                return false;
            }
            long[][] pairs = (long[][]) data;
            if (pairs.length != count) {
                return false;
            }
            int i = 0;
            while (i < pairs.length) {
                if (pairs[i].length != 2) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }
        if (type == TIFFTag.TIFF_SRATIONAL) {
            if (!(data instanceof int[][])) {
                return false;
            }
            int[][] pairs = (int[][]) data;
            if (pairs.length != count) {
                return false;
            }
            int i = 0;
            while (i < pairs.length) {
                if (pairs[i].length != 2) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }
        if (type == TIFFTag.TIFF_FLOAT) {
            return data instanceof float[] && ((float[]) data).length == count;
        }
        return data instanceof double[] && ((double[]) data).length == count;
    }

    /**
     * El valor de ese atributo.
     *
     * @throws NullPointerException si el atributo no esta, como en el JDK
     */
    private static String attribute(Node node, String name) {
        NamedNodeMap attrs = node.getAttributes();
        return attrs.getNamedItem(name).getNodeValue();
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

    /** Cuantos elementos hay en la lista. */
    private static int countElements(NodeList children) {
        int total = 0;
        int i = 0;
        while (i < children.getLength()) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                total = total + 1;
            }
            i = i + 1;
        }
        return total;
    }

    /** Mete un valor leido del arbol en la posicion que le toca. */
    private static void setFromString(Object data, int index, String value) {
        if (data instanceof String[]) {
            ((String[]) data)[index] = value;
        } else if (data instanceof byte[]) {
            ((byte[]) data)[index] = (byte) Integer.parseInt(value);
        } else if (data instanceof char[]) {
            ((char[]) data)[index] = (char) Long.parseLong(value);
        } else if (data instanceof short[]) {
            ((short[]) data)[index] = (short) Integer.parseInt(value);
        } else if (data instanceof int[]) {
            ((int[]) data)[index] = Integer.parseInt(value);
        } else if (data instanceof long[]) {
            ((long[]) data)[index] = Long.parseLong(value);
        } else if (data instanceof float[]) {
            ((float[]) data)[index] = Float.parseFloat(value);
        } else if (data instanceof double[]) {
            ((double[]) data)[index] = Double.parseDouble(value);
        } else if (data instanceof long[][]) {
            int slash = value.indexOf('/');
            ((long[][]) data)[index] = new long[] {
                Long.parseLong(value.substring(0, slash)),
                Long.parseLong(value.substring(slash + 1)),
            };
        } else if (data instanceof int[][]) {
            int slash = value.indexOf('/');
            ((int[][]) data)[index] = new int[] {
                Integer.parseInt(value.substring(0, slash)),
                Integer.parseInt(value.substring(slash + 1)),
            };
        }
    }
}
