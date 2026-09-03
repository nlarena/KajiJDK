package java.awt.image.renderable;

import java.awt.image.RenderedImage;
import java.io.Serializable;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.ParameterBlock -- los argumentos de una operacion, sin
 * tipos.
 *
 * <p>Dos listas: las <b>fuentes</b> --las imagenes de entrada-- y los <b>parametros</b> --todo lo
 * demas--. Estan separadas porque el sistema tiene que poder recorrer las fuentes para armar el
 * arbol de operaciones sin entender nada de los parametros.
 *
 * <p>Es una bolsa de {@code Object} sin verificacion, y hay que decirlo: nadie chequea que una
 * operacion que espera un radio y un color reciba eso. El error aparece al renderizar, con un
 * {@code ClassCastException} lejos de donde se armo el bloque. A cambio, una operacion nueva no
 * necesita ninguna clase nueva para sus argumentos.
 *
 * <h2>Los {@code add} y {@code set} primitivos, y el encadenado</h2>
 *
 * <p>Hay una sobrecarga por primitivo, y todas envuelven. Existen para que quien llama no tenga que
 * escribir el envoltorio, y para que el tipo del envoltorio sea siempre el que la operacion espera:
 * {@code add(1)} guarda un {@code Integer} y {@code add(1.0f)} un {@code Float}, que es la unica
 * forma de distinguirlos despues.
 *
 * <p>Los {@code add} devuelven el mismo bloque para poder encadenar. Los {@code get} tipados
 * --{@link #getIntParameter} y companía-- hacen el cast por uno, asi que fallan con
 * {@code ClassCastException} si el que se guardo era de otro tipo. Es a proposito: es preferible que
 * falle ahi a que convierta en silencio y la operacion haga otra cosa.
 *
 * <h2>Dos formas de copiar</h2>
 *
 * <p>{@link #clone} copia las dos listas, y {@link #shallowClone} las comparte. La segunda es lo que
 * se quiere al derivar un bloque de otro para una variante de la misma operacion: las fuentes son
 * las mismas imagenes y copiar la lista solo gastaria memoria.
 */
public class ParameterBlock implements Cloneable, Serializable {

    private static final long serialVersionUID = -7577115551785240750L;

    /** Las imagenes de entrada. Protegido porque las subclases del JDK lo tocan directo. */
    protected Vector<Object> sources = new Vector<Object>();

    /** Todo lo que no es una imagen de entrada. */
    protected Vector<Object> parameters = new Vector<Object>();

    /** Vacio. */
    public ParameterBlock() {
    }

    /** Con fuentes y sin parametros. */
    public ParameterBlock(Vector<Object> sources) {
        setSources(sources);
    }

    /** Con las dos listas. */
    public ParameterBlock(Vector<Object> sources, Vector<Object> parameters) {
        setSources(sources);
        setParameters(parameters);
    }

    /** Una copia que <b>comparte</b> las dos listas. Ver la nota de la clase. */
    public Object shallowClone() {
        ParameterBlock copy = new ParameterBlock();
        copy.sources = this.sources;
        copy.parameters = this.parameters;
        return copy;
    }

    /**
     * Una copia con listas propias.
     *
     * <p>Copia las listas, no lo que hay adentro: las imagenes y los parametros son los mismos
     * objetos. Es lo correcto -- una imagen es grande y compartirla es justamente el punto-- y hay
     * que saberlo si alguien guarda ahi algo mutable.
     */
    public Object clone() {
        ParameterBlock copy = new ParameterBlock();
        copy.sources = new Vector<Object>(this.sources);
        copy.parameters = new Vector<Object>(this.parameters);
        return copy;
    }

    /** Agrega una fuente al final. */
    public ParameterBlock addSource(Object source) {
        this.sources.addElement(source);
        return this;
    }

    /** La fuente en esa posicion. */
    public Object getSource(int index) {
        return this.sources.elementAt(index);
    }

    /**
     * Pone una fuente en esa posicion, agrandando la lista si hace falta.
     *
     * <p>Los huecos quedan en null: es lo que permite armar un bloque en cualquier orden.
     */
    public ParameterBlock setSource(Object source, int index) {
        if (this.sources.size() < index + 1) {
            this.sources.setSize(index + 1);
        }
        this.sources.setElementAt(source, index);
        return this;
    }

    /** La fuente en esa posicion, ya casteada. */
    public RenderedImage getRenderedSource(int index) {
        return (RenderedImage) this.sources.elementAt(index);
    }

    /** Idem, como imagen renderizable. */
    public RenderableImage getRenderableSource(int index) {
        return (RenderableImage) this.sources.elementAt(index);
    }

    /** Cuantas fuentes hay. */
    public int getNumSources() {
        return this.sources.size();
    }

    /** La lista de fuentes, en vivo. */
    public Vector<Object> getSources() {
        return this.sources;
    }

    /** Reemplaza la lista de fuentes. */
    public void setSources(Vector<Object> sources) {
        this.sources = sources;
    }

    /** Las vacia. */
    public void removeSources() {
        this.sources = new Vector<Object>();
    }

    /** Cuantos parametros hay. */
    public int getNumParameters() {
        return this.parameters.size();
    }

    /** La lista de parametros, en vivo. */
    public Vector<Object> getParameters() {
        return this.parameters;
    }

    /** Reemplaza la lista de parametros. */
    public void setParameters(Vector<Object> parameters) {
        this.parameters = parameters;
    }

    /** Los vacia. */
    public void removeParameters() {
        this.parameters = new Vector<Object>();
    }

    /** Agrega un parametro al final. */
    public ParameterBlock add(Object obj) {
        this.parameters.addElement(obj);
        return this;
    }

    /** Idem, envolviendo. Ver la nota de la clase sobre por que hay una por primitivo. */
    public ParameterBlock add(byte b) {
        return add(Byte.valueOf(b));
    }

    /** Idem. */
    public ParameterBlock add(char c) {
        return add(Character.valueOf(c));
    }

    /** Idem. */
    public ParameterBlock add(short s) {
        return add(Short.valueOf(s));
    }

    /** Idem. */
    public ParameterBlock add(int i) {
        return add(Integer.valueOf(i));
    }

    /** Idem. */
    public ParameterBlock add(long l) {
        return add(Long.valueOf(l));
    }

    /** Idem. */
    public ParameterBlock add(float f) {
        return add(Float.valueOf(f));
    }

    /** Idem. */
    public ParameterBlock add(double d) {
        return add(Double.valueOf(d));
    }

    /** Pone un parametro en esa posicion, agrandando la lista si hace falta. */
    public ParameterBlock set(Object obj, int index) {
        if (this.parameters.size() < index + 1) {
            this.parameters.setSize(index + 1);
        }
        this.parameters.setElementAt(obj, index);
        return this;
    }

    /** Idem, envolviendo. */
    public ParameterBlock set(byte b, int index) {
        return set(Byte.valueOf(b), index);
    }

    /** Idem. */
    public ParameterBlock set(char c, int index) {
        return set(Character.valueOf(c), index);
    }

    /** Idem. */
    public ParameterBlock set(short s, int index) {
        return set(Short.valueOf(s), index);
    }

    /** Idem. */
    public ParameterBlock set(int i, int index) {
        return set(Integer.valueOf(i), index);
    }

    /** Idem. */
    public ParameterBlock set(long l, int index) {
        return set(Long.valueOf(l), index);
    }

    /** Idem. */
    public ParameterBlock set(float f, int index) {
        return set(Float.valueOf(f), index);
    }

    /** Idem. */
    public ParameterBlock set(double d, int index) {
        return set(Double.valueOf(d), index);
    }

    /** El parametro en esa posicion, sin castear. */
    public Object getObjectParameter(int index) {
        return this.parameters.elementAt(index);
    }

    /**
     * El parametro en esa posicion como {@code byte}.
     *
     * @throws ClassCastException si el que se guardo era de otro tipo; ver la nota de la clase
     */
    public byte getByteParameter(int index) {
        return ((Byte) this.parameters.elementAt(index)).byteValue();
    }

    /** Idem, como {@code char}. */
    public char getCharParameter(int index) {
        return ((Character) this.parameters.elementAt(index)).charValue();
    }

    /** Idem, como {@code short}. */
    public short getShortParameter(int index) {
        return ((Short) this.parameters.elementAt(index)).shortValue();
    }

    /** Idem, como {@code int}. */
    public int getIntParameter(int index) {
        return ((Integer) this.parameters.elementAt(index)).intValue();
    }

    /** Idem, como {@code long}. */
    public long getLongParameter(int index) {
        return ((Long) this.parameters.elementAt(index)).longValue();
    }

    /** Idem, como {@code float}. */
    public float getFloatParameter(int index) {
        return ((Float) this.parameters.elementAt(index)).floatValue();
    }

    /** Idem, como {@code double}. */
    public double getDoubleParameter(int index) {
        return ((Double) this.parameters.elementAt(index)).doubleValue();
    }

    /**
     * Las clases de los parametros, en orden.
     *
     * <p>Para los envueltos devuelve la clase <b>primitiva</b> --{@code int.class} y no
     * {@code Integer.class}--, que es lo que hace falta para buscar por reflexion el metodo de la
     * operacion que los recibe.
     *
     * @throws NullPointerException si algun parametro es null. Es lo que hace el JDK y no una
     *     comprobacion nuestra: un null no tiene clase, y el hueco que deja un {@code set} mas alla
     *     del final es justamente un null
     */
    public Class<?>[] getParamClasses() {
        int count = this.parameters.size();
        Class<?>[] classes = new Class<?>[count];
        int i = 0;
        while (i < count) {
            Object o = getObjectParameter(i);
            if (o instanceof Byte) {
                classes[i] = byte.class;
            } else if (o instanceof Character) {
                classes[i] = char.class;
            } else if (o instanceof Short) {
                classes[i] = short.class;
            } else if (o instanceof Integer) {
                classes[i] = int.class;
            } else if (o instanceof Long) {
                classes[i] = long.class;
            } else if (o instanceof Float) {
                classes[i] = float.class;
            } else if (o instanceof Double) {
                classes[i] = double.class;
            } else {
                classes[i] = o.getClass();
            }
            i = i + 1;
        }
        return classes;
    }
}
