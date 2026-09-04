package java.awt.datatransfer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Un formato en el que se pueden entregar datos transferidos.
 *
 * <p>Son **dos cosas a la vez**, y no entenderlo es la causa de casi toda la confusión con esta
 * clase:
 *
 * <ul>
 *   <li>un <strong>tipo MIME</strong>, que dice qué son los datos — `text/plain`, `image/png`;
 *   <li>una <strong>clase de representación</strong>, que dice en qué objeto de Java llegan —
 *       {@code String}, {@code InputStream}, {@code java.util.List}.
 * </ul>
 *
 * <p>Los dos hacen falta porque son preguntas distintas. `text/plain` puede llegar como cadena, como
 * lector o como flujo de bytes, y quien recibe no puede tratarlos igual. Por eso el tipo MIME de un
 * formato de Java lleva el parámetro `class=` adentro: es el tipo MIME el que carga con la clase.
 *
 * <p>La igualdad sigue esa lógica y sorprende: dos formatos son iguales si coinciden **el tipo MIME
 * y la clase**, y el nombre legible no cuenta. Cambiarle el nombre a un formato no lo vuelve otro,
 * porque ese nombre es para mostrárselo a una persona.
 *
 * <p>{@link #match} es la comparación floja, la que ignora el resto de los parámetros del tipo MIME
 * — como la codificación de un texto. Sirve para preguntar "¿esto es texto?" sin exigir que sea
 * exactamente el mismo texto.
 */
public class DataFlavor implements Externalizable, Cloneable {

    private static final long serialVersionUID = 8367026044764648243L;

    /** El tipo MIME de un objeto serializado de Java. */
    public static final String javaSerializedObjectMimeType =
            "application/x-java-serialized-object";

    /**
     * El tipo MIME de una referencia a un objeto **de esta misma máquina virtual**.
     *
     * <p>No se serializa nada: se pasa la referencia. Sólo sirve dentro del mismo proceso, y por eso
     * un formato así no cruza al portapapeles del sistema.
     */
    public static final String javaJVMLocalObjectMimeType = "application/x-java-jvm-local-objectref";

    /** El tipo MIME de una referencia a un objeto remoto. */
    public static final String javaRemoteObjectMimeType = "application/x-java-remote-object";

    /** Texto de Java, como {@code String}. */
    public static final DataFlavor stringFlavor =
            new DataFlavor(String.class, "Unicode String");

    /** Una imagen, como {@code java.awt.Image}. */
    public static final DataFlavor imageFlavor =
            new DataFlavor("image/x-java-image; class=java.awt.Image", "Image");

    /**
     * Texto plano, como {@code java.io.Reader}.
     *
     * @deprecated su tipo MIME dice `charset=unicode`, que no es un juego de caracteres real, y las
     *     implementaciones nunca se pusieron de acuerdo sobre qué significaba. Usar
     *     {@link #stringFlavor} o {@link #getTextPlainUnicodeFlavor}.
     */
    @Deprecated
    public static final DataFlavor plainTextFlavor =
            new DataFlavor("text/plain; charset=unicode; class=java.io.InputStream", "Plain Text");

    /** Una lista de archivos, como {@code java.util.List} de {@code java.io.File}. */
    public static final DataFlavor javaFileListFlavor =
            new DataFlavor("application/x-java-file-list; class=java.util.List",
                    "application/x-java-file-list");

    /** El HTML de lo que se seleccionó, sin el contexto que lo rodea. */
    public static final DataFlavor selectionHtmlFlavor =
            new DataFlavor("text/html; class=java.lang.String; document=selection; "
                    + "charset=Unicode", "HTML Selection");

    /** El HTML de lo seleccionado más las etiquetas que hacen falta para que se entienda. */
    public static final DataFlavor fragmentHtmlFlavor =
            new DataFlavor("text/html; class=java.lang.String; document=fragment; "
                    + "charset=Unicode", "HTML Fragment");

    /** El documento HTML entero. */
    public static final DataFlavor allHtmlFlavor =
            new DataFlavor("text/html; class=java.lang.String; document=all; charset=Unicode",
                    "HTML All");

    private String mimeType;
    private String primaryType;
    private String subType;
    private Map<String, String> parameters;
    private Class<?> representationClass;
    private String humanPresentableName;

    /**
     * Un formato vacío, para deserializar.
     *
     * <p>El objeto que sale no sirve para nada hasta que se lo llene con
     * {@link #readExternal}; está sólo porque {@link Externalizable} lo exige.
     */
    public DataFlavor() {
        this.mimeType = null;
        this.representationClass = null;
        this.humanPresentableName = null;
        this.parameters = new HashMap<String, String>();
    }

    /**
     * Un formato de objeto serializado con esa clase de representación.
     *
     * @throws NullPointerException si la clase es `null`
     */
    public DataFlavor(Class<?> representationClass, String humanPresentableName) {
        if (representationClass == null) {
            throw new NullPointerException("representationClass");
        }
        this.armar(javaSerializedObjectMimeType + "; class=" + representationClass.getName(),
                humanPresentableName, representationClass);
    }

    /**
     * Un formato a partir de su tipo MIME.
     *
     * @throws IllegalArgumentException si el tipo MIME está mal escrito
     * @throws NullPointerException si el tipo MIME es `null`
     */
    public DataFlavor(String mimeType, String humanPresentableName) {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        this.armar(mimeType, humanPresentableName, null);
    }

    /**
     * Como el anterior, cargando la clase con el cargador dado.
     *
     * @throws ClassNotFoundException si la clase de `class=` no se puede cargar
     * @throws IllegalArgumentException si el tipo MIME está mal escrito
     * @throws NullPointerException si el tipo MIME es `null`
     */
    public DataFlavor(String mimeType, String humanPresentableName, ClassLoader classLoader)
            throws ClassNotFoundException {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        this.armar(mimeType, humanPresentableName, null);
        String nombre = this.parameters.get("class");
        if (nombre != null) {
            this.representationClass = tryToLoadClass(nombre, classLoader);
        }
    }

    /**
     * Un formato a partir de su tipo MIME, con el nombre legible tomado del propio tipo.
     *
     * @throws ClassNotFoundException si la clase de `class=` no se puede cargar
     * @throws NullPointerException si el tipo MIME es `null`
     */
    public DataFlavor(String mimeType) throws ClassNotFoundException {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        this.armar(mimeType, null, null);
        String nombre = this.parameters.get("class");
        if (nombre != null) {
            this.representationClass = tryToLoadClass(nombre, null);
        }
    }

    /**
     * Parte el tipo MIME y guarda todo.
     *
     * @throws IllegalArgumentException si el tipo MIME no tiene la forma `tipo/subtipo`
     */
    private void armar(String mimeType, String humanPresentableName, Class<?> repClass) {
        this.parameters = new HashMap<String, String>();
        String[] partes = mimeType.split(";");
        String base = partes[0].trim();
        int barra = base.indexOf('/');
        if (barra < 0) {
            throw new IllegalArgumentException("failed to parse:" + mimeType);
        }
        this.primaryType = base.substring(0, barra).trim().toLowerCase();
        this.subType = base.substring(barra + 1).trim().toLowerCase();
        for (int i = 1; i < partes.length; i++) {
            String p = partes[i].trim();
            int igual = p.indexOf('=');
            if (igual > 0) {
                String clave = p.substring(0, igual).trim();
                String valor = p.substring(igual + 1).trim();
                // Las comillas del valor de un parametro MIME no son parte del valor.
                if (valor.length() >= 2 && valor.charAt(0) == '"'
                        && valor.charAt(valor.length() - 1) == '"') {
                    valor = valor.substring(1, valor.length() - 1);
                }
                this.parameters.put(clave, valor);
            }
        }
        this.mimeType = mimeType;
        if (repClass != null) {
            this.representationClass = repClass;
        } else {
            String nombre = this.parameters.get("class");
            if (nombre == null) {
                // Sin `class=`, la representacion por omision es un flujo de bytes: es lo unico que
                // se puede entregar de un tipo MIME cualquiera sin saber nada mas de el.
                this.representationClass = InputStream.class;
            } else {
                try {
                    this.representationClass = tryToLoadClass(nombre, null);
                } catch (ClassNotFoundException e) {
                    this.representationClass = null;
                }
            }
        }
        if (humanPresentableName != null) {
            this.humanPresentableName = humanPresentableName;
        } else {
            this.humanPresentableName = this.primaryType + "/" + this.subType;
        }
    }

    /**
     * Carga una clase por nombre.
     *
     * <p>Prueba con el cargador dado, después con el del contexto del hilo y por último con el de
     * esta clase. Es el orden del JDK, y el que hace que un formato definido en un módulo cargado
     * aparte se pueda resolver desde otro.
     *
     * @throws ClassNotFoundException si ninguno la encuentra
     */
    protected static final Class<?> tryToLoadClass(String className, ClassLoader fallback)
            throws ClassNotFoundException {
        if (fallback != null) {
            try {
                return Class.forName(className, true, fallback);
            } catch (ClassNotFoundException e) {
                // Se sigue probando con los otros cargadores.
            }
        }
        ClassLoader hilo = Thread.currentThread().getContextClassLoader();
        if (hilo != null) {
            try {
                return Class.forName(className, true, hilo);
            } catch (ClassNotFoundException e) {
                // Idem.
            }
        }
        return Class.forName(className);
    }

    /** El tipo MIME completo, con sus parámetros. */
    public String getMimeType() {
        return this.mimeType;
    }

    /** En qué clase de Java llegan los datos. */
    public Class<?> getRepresentationClass() {
        return this.representationClass;
    }

    /** El nombre para mostrarle a una persona. */
    public String getHumanPresentableName() {
        return this.humanPresentableName;
    }

    /** La parte de antes de la barra del tipo MIME. */
    public String getPrimaryType() {
        return this.primaryType;
    }

    /** La parte de después de la barra. */
    public String getSubType() {
        return this.subType;
    }

    /**
     * El valor de un parámetro del tipo MIME.
     *
     * <p>`humanPresentableName` se contesta desde el nombre legible y no desde los parámetros, que
     * es una rareza de la API que se conserva.
     *
     * @return el valor, o `null` si el parámetro no está
     */
    public String getParameter(String paramName) {
        if ("humanPresentableName".equals(paramName)) {
            return this.humanPresentableName;
        }
        return this.parameters.get(paramName);
    }

    /** Cambia el nombre para mostrar; no cambia la identidad del formato. */
    public void setHumanPresentableName(String humanPresentableName) {
        this.humanPresentableName = humanPresentableName;
    }

    /** Igualdad por tipo MIME y clase de representación; el nombre legible no cuenta. */
    public boolean equals(Object o) {
        return o instanceof DataFlavor && this.equals((DataFlavor) o);
    }

    /** Lo mismo, con el tipo ya conocido. */
    public boolean equals(DataFlavor that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (this.representationClass == null) {
            if (that.getRepresentationClass() != null) {
                return false;
            }
        } else if (!this.representationClass.equals(that.getRepresentationClass())) {
            return false;
        }
        if (this.primaryType == null) {
            return that.primaryType == null;
        }
        if (!this.primaryType.equals(that.primaryType) || !this.subType.equals(that.subType)) {
            return false;
        }
        // El texto compara ademas la codificacion: dos textos en juegos de caracteres distintos no
        // son el mismo formato, aunque digan lo mismo.
        if (this.isFlavorTextType()) {
            String a = this.getParameter("charset");
            String b = that.getParameter("charset");
            if (a == null) {
                return b == null;
            }
            return a.equalsIgnoreCase(b);
        }
        return true;
    }

    /**
     * Si el tipo MIME es igual a esa cadena.
     *
     * @deprecated no compara la clase de representación, así que dice que son iguales dos formatos
     *     que entregan objetos distintos. Usar {@link #isMimeTypeEqual(String)}.
     */
    @Deprecated
    public boolean equals(String s) {
        if (s == null || this.mimeType == null) {
            return false;
        }
        return this.isMimeTypeEqual(s);
    }

    public int hashCode() {
        int total = 0;
        if (this.primaryType != null) {
            total = total + this.primaryType.hashCode();
        }
        if (this.subType != null) {
            total = total + this.subType.hashCode();
        }
        if (this.representationClass != null) {
            total = total + this.representationClass.hashCode();
        }
        return total;
    }

    /**
     * La comparación floja: mismo tipo y subtipo y misma clase, ignorando el resto de los
     * parámetros.
     *
     * <p>Es lo que hay que usar para preguntar "¿esto es texto?" sin exigir que sea el mismo texto
     * con la misma codificación.
     */
    public boolean match(DataFlavor that) {
        if (that == null) {
            return false;
        }
        if (this.primaryType == null) {
            return that.primaryType == null;
        }
        if (!this.primaryType.equals(that.primaryType) || !this.subType.equals(that.subType)) {
            return false;
        }
        if (this.representationClass == null) {
            return that.representationClass == null;
        }
        return this.representationClass.equals(that.representationClass);
    }

    /**
     * Si el tipo MIME es igual a ese, ignorando los parámetros.
     *
     * @throws NullPointerException si la cadena es `null`
     * @throws IllegalArgumentException si la cadena no es un tipo MIME válido
     */
    public boolean isMimeTypeEqual(String mimeType) {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        if (this.mimeType == null) {
            return false;
        }
        String otro = mimeType.split(";")[0].trim().toLowerCase();
        return otro.equals(this.primaryType + "/" + this.subType);
    }

    /** Lo mismo, contra otro formato. */
    public final boolean isMimeTypeEqual(DataFlavor dataFlavor) {
        if (dataFlavor == null) {
            return false;
        }
        return this.isMimeTypeEqual(dataFlavor.getMimeType());
    }

    /** Si es un objeto serializado de Java. */
    public boolean isMimeTypeSerializedObject() {
        return this.isMimeTypeEqual(javaSerializedObjectMimeType);
    }

    /** La clase en la que llegan los datos si el tipo MIME no dice otra cosa. */
    public final Class<?> getDefaultRepresentationClass() {
        return InputStream.class;
    }

    /** El nombre de esa clase. */
    public final String getDefaultRepresentationClassAsString() {
        return this.getDefaultRepresentationClass().getName();
    }

    /** Si los datos llegan como flujo de bytes. */
    public boolean isRepresentationClassInputStream() {
        return InputStream.class.isAssignableFrom(this.representationClass);
    }

    /** Si los datos llegan como lector de caracteres. */
    public boolean isRepresentationClassReader() {
        return this.representationClass != null
                && Reader.class.isAssignableFrom(this.representationClass);
    }

    /** Si los datos llegan como buffer de caracteres. */
    public boolean isRepresentationClassCharBuffer() {
        return CharBuffer.class.equals(this.representationClass);
    }

    /** Si los datos llegan como buffer de bytes. */
    public boolean isRepresentationClassByteBuffer() {
        return ByteBuffer.class.equals(this.representationClass);
    }

    /** Si los datos llegan como objeto serializable. */
    public boolean isRepresentationClassSerializable() {
        return this.representationClass != null
                && Serializable.class.isAssignableFrom(this.representationClass);
    }

    /**
     * Si los datos llegan como objeto remoto.
     *
     * <p>Devuelve `false` siempre: `java.rmi.Remote` no está en esta biblioteca, así que ninguna
     * clase de representación puede implementarla. No es un relleno — es la verdad sobre cualquier
     * formato que se pueda construir acá.
     */
    public boolean isRepresentationClassRemote() {
        return false;
    }

    /** Si es un objeto serializado que además llega como clase serializable. */
    public boolean isFlavorSerializedObjectType() {
        return this.isRepresentationClassSerializable() && this.isMimeTypeSerializedObject();
    }

    /** Si es una referencia a un objeto remoto. */
    public boolean isFlavorRemoteObjectType() {
        return this.isRepresentationClassRemote()
                && this.isRepresentationClassSerializable()
                && this.isMimeTypeEqual(javaRemoteObjectMimeType);
    }

    /** Si es la lista de archivos. */
    public boolean isFlavorJavaFileListType() {
        if (this.mimeType == null || this.representationClass == null) {
            return false;
        }
        return java.util.List.class.isAssignableFrom(this.representationClass)
                && this.isMimeTypeEqual(javaFileListFlavor.mimeType);
    }

    /**
     * Si el formato es texto que se puede leer como caracteres.
     *
     * <p>No alcanza con que el tipo MIME empiece con `text/`: también tiene que entregarse en una
     * clase que sirva para leer texto. Un `text/plain` que llegue como un objeto cualquiera no es
     * texto a estos efectos.
     */
    public boolean isFlavorTextType() {
        if (!"text".equals(this.primaryType)) {
            return false;
        }
        Class<?> c = this.representationClass;
        if (c == null) {
            return false;
        }
        return String.class.equals(c) || Reader.class.isAssignableFrom(c)
                || CharBuffer.class.equals(c) || char[].class.equals(c)
                || InputStream.class.isAssignableFrom(c) || ByteBuffer.class.equals(c)
                || byte[].class.equals(c);
    }

    /** Texto plano en Unicode, entregado como {@code String}. */
    public static final DataFlavor getTextPlainUnicodeFlavor() {
        return new DataFlavor("text/plain; charset=UTF-8; class=java.lang.String", "Plain Text");
    }

    /**
     * El mejor de esos formatos de texto.
     *
     * <p>El criterio es el del JDK: se prefiere el que entrega caracteres —{@code String},
     * {@code Reader}, {@code CharBuffer}— sobre el que entrega bytes, porque con bytes hay que
     * adivinar la codificación. Entre dos de la misma categoría gana el que aparece primero.
     *
     * @return el mejor, o `null` si el arreglo es `null`, está vacío o no trae ningún texto
     */
    public static final DataFlavor selectBestTextFlavor(DataFlavor[] availableFlavors) {
        if (availableFlavors == null || availableFlavors.length == 0) {
            return null;
        }
        DataFlavor mejor = null;
        int mejorPuntaje = -1;
        for (int i = 0; i < availableFlavors.length; i++) {
            DataFlavor f = availableFlavors[i];
            if (f == null || !f.isFlavorTextType()) {
                continue;
            }
            Class<?> c = f.getRepresentationClass();
            int puntaje;
            if (String.class.equals(c)) {
                puntaje = 3;
            } else if (Reader.class.isAssignableFrom(c) || CharBuffer.class.equals(c)
                    || char[].class.equals(c)) {
                puntaje = 2;
            } else {
                puntaje = 1;
            }
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = f;
            }
        }
        return mejor;
    }

    /**
     * Un lector sobre el texto que entrega ese origen en este formato.
     *
     * <p>Envuelve lo que el origen devuelva —una cadena, un lector, un flujo— en un {@code Reader},
     * decodificando con el juego de caracteres del tipo MIME si hace falta.
     *
     * @throws IllegalArgumentException si este formato no es de texto
     * @throws UnsupportedFlavorException si el origen no lo admite
     * @throws IOException si los datos no se pueden leer
     */
    public Reader getReaderForText(Transferable transferable)
            throws UnsupportedFlavorException, IOException {
        Object datos = transferable.getTransferData(this);
        if (datos == null) {
            throw new IllegalArgumentException("getTransferData() returned null");
        }
        if (datos instanceof Reader) {
            return (Reader) datos;
        }
        if (datos instanceof String) {
            return new StringReader((String) datos);
        }
        if (datos instanceof CharBuffer) {
            CharBuffer cb = (CharBuffer) datos;
            char[] chars = new char[cb.remaining()];
            cb.get(chars);
            return new java.io.CharArrayReader(chars);
        }
        if (datos instanceof char[]) {
            return new java.io.CharArrayReader((char[]) datos);
        }
        InputStream in;
        if (datos instanceof InputStream) {
            in = (InputStream) datos;
        } else if (datos instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) datos;
            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            in = new java.io.ByteArrayInputStream(bytes);
        } else if (datos instanceof byte[]) {
            in = new java.io.ByteArrayInputStream((byte[]) datos);
        } else {
            throw new IllegalArgumentException("transferable is not a text flavor");
        }
        String charset = this.getParameter("charset");
        if (charset == null) {
            return new java.io.InputStreamReader(in);
        }
        return new java.io.InputStreamReader(in, charset);
    }

    /** Escribe el formato para serializarlo. */
    public synchronized void writeExternal(ObjectOutput os) throws IOException {
        os.writeObject(this.mimeType);
        os.writeObject(this.humanPresentableName);
    }

    /**
     * Lee un formato serializado.
     *
     * @throws ClassNotFoundException si la clase de representación no se puede cargar
     * @throws IOException si el flujo está mal
     */
    public synchronized void readExternal(ObjectInput is)
            throws IOException, ClassNotFoundException {
        String tipo = (String) is.readObject();
        String nombre = (String) is.readObject();
        if (tipo != null) {
            this.armar(tipo, nombre, null);
        }
    }

    /**
     * Una copia.
     *
     * @throws CloneNotSupportedException nunca: esta clase declara `Cloneable`
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Normaliza el valor de un parámetro del tipo MIME.
     *
     * @deprecated el JDK ya no la llama; la normalización está adentro del análisis del tipo MIME.
     */
    @Deprecated
    protected String normalizeMimeTypeParameter(String parameterName, String parameterValue) {
        return parameterValue;
    }

    /**
     * Normaliza un tipo MIME.
     *
     * @deprecated el JDK ya no la llama, por el mismo motivo.
     */
    @Deprecated
    protected String normalizeMimeType(String mimeType) {
        return mimeType;
    }

    public String toString() {
        return this.getClass().getName() + "[mimetype=" + this.primaryType + "/" + this.subType
                + ";representationclass="
                + (this.representationClass == null ? "null" : this.representationClass.getName())
                + "]";
    }
}
