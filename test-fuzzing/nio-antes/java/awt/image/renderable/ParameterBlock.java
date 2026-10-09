package java.awt.image.renderable;

import java.awt.image.RenderedImage;
import java.io.Serializable;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.ParameterBlock -- an operation's arguments, untyped.
 *
 * <p>Two lists: the <b>sources</b> --the input images-- and the <b>parameters</b> --everything
 * else--. They are separate because the system has to be able to walk the sources to build the
 * operation tree without understanding anything about the parameters.
 *
 * <p>It is a bag of {@code Object} without checking, and it should be said: nobody checks that an
 * operation expecting a radius and a colour receives that. The error shows up when rendering, with
 * a {@code ClassCastException} far from where the block was built. In exchange, a new operation
 * needs no new class for its arguments.
 *
 * <h2>The primitive {@code add} and {@code set}, and chaining</h2>
 *
 * <p>There is one overload per primitive, and all of them wrap. They exist so the caller does not
 * have to write the wrapper, and so the wrapper's type is always the one the operation expects:
 * {@code add(1)} stores an {@code Integer} and {@code add(1.0f)} a {@code Float}, which is the only
 * way to tell them apart later.
 *
 * <p>The {@code add}s return the same block so that calls can be chained. The typed {@code get}s
 * --{@link #getIntParameter} and company-- do the cast for you, so they fail with
 * {@code ClassCastException} if what was stored was of another type. That is on purpose: it is
 * better to fail there than to convert silently and have the operation do something else.
 *
 * <h2>Two ways to copy</h2>
 *
 * <p>{@link #clone} copies both lists, and {@link #shallowClone} shares them. The second is what is
 * wanted when deriving one block from another for a variant of the same operation: the sources are
 * the same images, and copying the list would only spend memory.
 */
public class ParameterBlock implements Cloneable, Serializable {

    private static final long serialVersionUID = -7577115551785240750L;

    /**
     * The input images. Protected, as in the JDK. (This comment said JDK subclasses touch it
     * directly; nothing in the JDK extends this class.)
     */
    protected Vector<Object> sources = new Vector<Object>();

    /** Everything that is not an input image. */
    protected Vector<Object> parameters = new Vector<Object>();

    /** Empty. */
    public ParameterBlock() {
    }

    /** With sources and no parameters. */
    public ParameterBlock(Vector<Object> sources) {
        setSources(sources);
    }

    /** With both lists. */
    public ParameterBlock(Vector<Object> sources, Vector<Object> parameters) {
        setSources(sources);
        setParameters(parameters);
    }

    /** A copy that <b>shares</b> both lists. See the class note. */
    public Object shallowClone() {
        ParameterBlock copy = new ParameterBlock();
        copy.sources = this.sources;
        copy.parameters = this.parameters;
        return copy;
    }

    /**
     * A copy with lists of its own.
     *
     * <p>It copies the lists, not what is inside: the images and the parameters are the same
     * objects. That is right -- an image is large, and sharing it is precisely the point -- and it
     * has to be known if someone stores something mutable there.
     */
    public Object clone() {
        ParameterBlock copy = new ParameterBlock();
        copy.sources = new Vector<Object>(this.sources);
        copy.parameters = new Vector<Object>(this.parameters);
        return copy;
    }

    /** Adds a source at the end. */
    public ParameterBlock addSource(Object source) {
        this.sources.addElement(source);
        return this;
    }

    /** The source at that position. */
    public Object getSource(int index) {
        return this.sources.elementAt(index);
    }

    /**
     * Puts a source at that position, growing the list if needed.
     *
     * <p>The gaps stay null: that is what allows building a block in any order.
     */
    public ParameterBlock setSource(Object source, int index) {
        if (this.sources.size() < index + 1) {
            this.sources.setSize(index + 1);
        }
        this.sources.setElementAt(source, index);
        return this;
    }

    /** The source at that position, already cast. */
    public RenderedImage getRenderedSource(int index) {
        return (RenderedImage) this.sources.elementAt(index);
    }

    /** The same, as a renderable image. */
    public RenderableImage getRenderableSource(int index) {
        return (RenderableImage) this.sources.elementAt(index);
    }

    /** How many sources there are. */
    public int getNumSources() {
        return this.sources.size();
    }

    /** The list of sources, live. */
    public Vector<Object> getSources() {
        return this.sources;
    }

    /** Replaces the list of sources. */
    public void setSources(Vector<Object> sources) {
        this.sources = sources;
    }

    /** Replaces them with an empty list. */
    public void removeSources() {
        this.sources = new Vector<Object>();
    }

    /** How many parameters there are. */
    public int getNumParameters() {
        return this.parameters.size();
    }

    /** The list of parameters, live. */
    public Vector<Object> getParameters() {
        return this.parameters;
    }

    /** Replaces the list of parameters. */
    public void setParameters(Vector<Object> parameters) {
        this.parameters = parameters;
    }

    /** Replaces them with an empty list. */
    public void removeParameters() {
        this.parameters = new Vector<Object>();
    }

    /** Adds a parameter at the end. */
    public ParameterBlock add(Object obj) {
        this.parameters.addElement(obj);
        return this;
    }

    /** The same, wrapping. See the class note on why there is one per primitive. */
    public ParameterBlock add(byte b) {
        return add(Byte.valueOf(b));
    }

    /** The same. */
    public ParameterBlock add(char c) {
        return add(Character.valueOf(c));
    }

    /** The same. */
    public ParameterBlock add(short s) {
        return add(Short.valueOf(s));
    }

    /** The same. */
    public ParameterBlock add(int i) {
        return add(Integer.valueOf(i));
    }

    /** The same. */
    public ParameterBlock add(long l) {
        return add(Long.valueOf(l));
    }

    /** The same. */
    public ParameterBlock add(float f) {
        return add(Float.valueOf(f));
    }

    /** The same. */
    public ParameterBlock add(double d) {
        return add(Double.valueOf(d));
    }

    /** Puts a parameter at that position, growing the list if needed. */
    public ParameterBlock set(Object obj, int index) {
        if (this.parameters.size() < index + 1) {
            this.parameters.setSize(index + 1);
        }
        this.parameters.setElementAt(obj, index);
        return this;
    }

    /** The same, wrapping. */
    public ParameterBlock set(byte b, int index) {
        return set(Byte.valueOf(b), index);
    }

    /** The same. */
    public ParameterBlock set(char c, int index) {
        return set(Character.valueOf(c), index);
    }

    /** The same. */
    public ParameterBlock set(short s, int index) {
        return set(Short.valueOf(s), index);
    }

    /** The same. */
    public ParameterBlock set(int i, int index) {
        return set(Integer.valueOf(i), index);
    }

    /** The same. */
    public ParameterBlock set(long l, int index) {
        return set(Long.valueOf(l), index);
    }

    /** The same. */
    public ParameterBlock set(float f, int index) {
        return set(Float.valueOf(f), index);
    }

    /** The same. */
    public ParameterBlock set(double d, int index) {
        return set(Double.valueOf(d), index);
    }

    /** The parameter at that position, uncast. */
    public Object getObjectParameter(int index) {
        return this.parameters.elementAt(index);
    }

    /**
     * The parameter at that position as a {@code byte}.
     *
     * @throws ClassCastException if what was stored was of another type; see the class note
     */
    public byte getByteParameter(int index) {
        return ((Byte) this.parameters.elementAt(index)).byteValue();
    }

    /** The same, as a {@code char}. */
    public char getCharParameter(int index) {
        return ((Character) this.parameters.elementAt(index)).charValue();
    }

    /** The same, as a {@code short}. */
    public short getShortParameter(int index) {
        return ((Short) this.parameters.elementAt(index)).shortValue();
    }

    /** The same, as an {@code int}. */
    public int getIntParameter(int index) {
        return ((Integer) this.parameters.elementAt(index)).intValue();
    }

    /** The same, as a {@code long}. */
    public long getLongParameter(int index) {
        return ((Long) this.parameters.elementAt(index)).longValue();
    }

    /** The same, as a {@code float}. */
    public float getFloatParameter(int index) {
        return ((Float) this.parameters.elementAt(index)).floatValue();
    }

    /** The same, as a {@code double}. */
    public double getDoubleParameter(int index) {
        return ((Double) this.parameters.elementAt(index)).doubleValue();
    }

    /**
     * The classes of the parameters, in order.
     *
     * <p>For the wrapped ones it returns the <b>primitive</b> class --{@code int.class} and not
     * {@code Integer.class}--, which is what is needed to find by reflection the operation's method
     * that receives them.
     *
     * @throws NullPointerException if some parameter is null. It is what the JDK does and not a
     *     check of ours: a null has no class, and the gap a {@code set} past the end leaves is
     *     precisely a null
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
