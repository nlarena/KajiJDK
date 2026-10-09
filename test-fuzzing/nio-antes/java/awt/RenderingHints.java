package java.awt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The drawing preferences: antialiasing or not, quality versus speed, which interpolation to use
 * when scaling an image.
 *
 * <p>They are preferences and not orders --the rasterizer may ignore them-- and that is the whole
 * semantics. That is why the class can be written whole even without a rasterizer: it is a
 * {@code Map} with a validation rule.
 *
 * <h2>Why the keys are objects and not strings</h2>
 *
 * <p>{@code Key} is abstract and its {@code equals} is final and pure identity. The consequence is
 * that nobody can make a key "equal" to {@code KEY_ANTIALIASING} without having the constant, and
 * that two libraries that add their own keys do not step on each other even if they pick the same
 * name. With strings that could not be guaranteed.
 *
 * <p>And that is why {@code put} validates: each key knows which values it accepts, so putting
 * {@code VALUE_RENDER_QUALITY} under {@code KEY_ANTIALIASING} throws
 * {@code IllegalArgumentException} right away instead of giving an odd drawing much later.
 *
 * <h2>About the value constants</h2>
 *
 * <p>The {@code VALUE_*} are declared as {@code Object} in the API precisely so that their concrete
 * class can be private: the JDK uses an internal class of its own and this one uses another. The
 * only thing a program can --and should-- do with them is compare them by identity and pass them to
 * {@code put}. Their {@code toString()} is not specified anywhere and it is better not to depend on
 * it.
 */
public class RenderingHints implements Map<Object, Object>, Cloneable {

    /**
     * The key of a preference.
     *
     * <p>The {@code equals} and {@code hashCode} are final: a key is itself and nothing else. If
     * they could be overridden, a subclass could pass itself off as another key and sneak values
     * into another library's map.
     */
    public abstract static class Key {

        // Two different keys of the same class with the same private integer would be
        // indistinguishable for whoever implements them, so it is detected when they are built and
        // not later, when the symptom would be a preference that overwrites itself.
        private static HashMap<Object, Object> identitymap = new HashMap<Object, Object>(17);

        private int privatekey;

        private String getIdentity() {
            return getClass().getName()
                    + "@" + Integer.toHexString(System.identityHashCode(getClass()))
                    + ":" + Integer.toHexString(privatekey);
        }

        private static synchronized boolean recordIdentity(Key k) {
            Object identity = k.getIdentity();
            Object otherref = identitymap.get(identity);
            if (otherref != null) {
                return false;
            }
            identitymap.put(identity, k);
            return true;
        }

        protected Key(int privatekey) {
            this.privatekey = privatekey;
            if (!recordIdentity(this)) {
                throw new IllegalArgumentException(privatekey + " already in use");
            }
        }

        protected final int intKey() {
            return privatekey;
        }

        public final int hashCode() {
            return privatekey;
        }

        public final boolean equals(Object o) {
            return this == o;
        }

        public abstract boolean isCompatibleValue(Object val);
    }

    // --- the concrete key and value ---
    //
    // In the JDK they live in an internal package and are inaccessible on purpose. Here private
    // nested classes are used for the same reason: the API says `Key` and `Object`, and neither
    // concrete class is part of the contract.

    private static class HintKey extends Key {

        private int min;

        private int max;

        HintKey(int privatekey) {
            super(privatekey);
        }

        HintKey(int privatekey, int min, int max) {
            super(privatekey);
            this.min = min;
            this.max = max;
            this.integerValued = true;
        }

        private boolean integerValued;

        public boolean isCompatibleValue(Object val) {
            if (integerValued) {
                return val instanceof Integer
                        && ((Integer) val).intValue() >= min
                        && ((Integer) val).intValue() <= max;
            }
            return val instanceof HintValue && ((HintValue) val).owner == this;
        }
    }

    private static class HintValue {

        private HintKey owner;

        private String description;

        HintValue(HintKey owner, String description) {
            this.owner = owner;
            this.description = description;
        }

        public String toString() {
            return description;
        }
    }

    private static HintKey newKey(int i) {
        return new HintKey(i);
    }

    private static Object newValue(Key k, String d) {
        return new HintValue((HintKey) k, d);
    }

    private static final HintKey K_ANTIALIASING = newKey(1);

    private static final HintKey K_RENDERING = newKey(2);

    private static final HintKey K_DITHERING = newKey(3);

    private static final HintKey K_TEXT_ANTIALIASING = newKey(4);

    private static final HintKey K_FRACTIONALMETRICS = newKey(5);

    private static final HintKey K_INTERPOLATION = newKey(6);

    private static final HintKey K_ALPHA_INTERPOLATION = newKey(7);

    private static final HintKey K_COLOR_RENDERING = newKey(8);

    private static final HintKey K_STROKE_CONTROL = newKey(9);

    private static final HintKey K_RESOLUTION_VARIANT = newKey(10);

    public static final Key KEY_ANTIALIASING = K_ANTIALIASING;

    public static final Object VALUE_ANTIALIAS_ON = newValue(K_ANTIALIASING, "Antialiased rendering mode");

    public static final Object VALUE_ANTIALIAS_OFF = newValue(K_ANTIALIASING, "Nonantialiased rendering mode");

    public static final Object VALUE_ANTIALIAS_DEFAULT = newValue(K_ANTIALIASING, "Default antialiasing rendering mode");

    public static final Key KEY_RENDERING = K_RENDERING;

    public static final Object VALUE_RENDER_SPEED = newValue(K_RENDERING, "Fastest rendering methods");

    public static final Object VALUE_RENDER_QUALITY = newValue(K_RENDERING, "Highest quality rendering methods");

    public static final Object VALUE_RENDER_DEFAULT = newValue(K_RENDERING, "Default rendering methods");

    public static final Key KEY_DITHERING = K_DITHERING;

    public static final Object VALUE_DITHER_DISABLE = newValue(K_DITHERING, "Nondithered rendering mode");

    public static final Object VALUE_DITHER_ENABLE = newValue(K_DITHERING, "Dithered rendering mode");

    public static final Object VALUE_DITHER_DEFAULT = newValue(K_DITHERING, "Default dithering mode");

    public static final Key KEY_TEXT_ANTIALIASING = K_TEXT_ANTIALIASING;

    public static final Object VALUE_TEXT_ANTIALIAS_ON = newValue(K_TEXT_ANTIALIASING, "Antialiased text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_OFF = newValue(K_TEXT_ANTIALIASING, "Nonantialiased text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = newValue(K_TEXT_ANTIALIASING, "Default antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_GASP = newValue(K_TEXT_ANTIALIASING, "gasp antialiasing text mode");

    // The four LCD_* say in which physical order the screen's subpixels are. They are not synonyms:
    // picking the wrong one paints coloured fringes on the edges of the letters.
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HRGB = newValue(K_TEXT_ANTIALIASING, "LCD HRGB antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HBGR = newValue(K_TEXT_ANTIALIASING, "LCD HBGR antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VRGB = newValue(K_TEXT_ANTIALIASING, "LCD VRGB antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VBGR = newValue(K_TEXT_ANTIALIASING, "LCD VBGR antialiasing text mode");

    /** The only key whose value is an {@code Integer} and not a constant: 100 to 250. */
    public static final Key KEY_TEXT_LCD_CONTRAST = new HintKey(100, 100, 250);

    public static final Key KEY_FRACTIONALMETRICS = K_FRACTIONALMETRICS;

    public static final Object VALUE_FRACTIONALMETRICS_OFF = newValue(K_FRACTIONALMETRICS, "Integer text metrics mode");

    public static final Object VALUE_FRACTIONALMETRICS_ON = newValue(K_FRACTIONALMETRICS, "Fractional text metrics mode");

    public static final Object VALUE_FRACTIONALMETRICS_DEFAULT = newValue(K_FRACTIONALMETRICS, "Default fractional text metrics mode");

    public static final Key KEY_INTERPOLATION = K_INTERPOLATION;

    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = newValue(K_INTERPOLATION, "Nearest Neighbor image interpolation mode");

    public static final Object VALUE_INTERPOLATION_BILINEAR = newValue(K_INTERPOLATION, "Bilinear image interpolation mode");

    public static final Object VALUE_INTERPOLATION_BICUBIC = newValue(K_INTERPOLATION, "Bicubic image interpolation mode");

    public static final Key KEY_ALPHA_INTERPOLATION = K_ALPHA_INTERPOLATION;

    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED = newValue(K_ALPHA_INTERPOLATION, "Fastest alpha blending methods");

    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY = newValue(K_ALPHA_INTERPOLATION, "Highest quality alpha blending methods");

    public static final Object VALUE_ALPHA_INTERPOLATION_DEFAULT = newValue(K_ALPHA_INTERPOLATION, "Default alpha blending methods");

    public static final Key KEY_COLOR_RENDERING = K_COLOR_RENDERING;

    public static final Object VALUE_COLOR_RENDER_SPEED = newValue(K_COLOR_RENDERING, "Fastest color rendering mode");

    public static final Object VALUE_COLOR_RENDER_QUALITY = newValue(K_COLOR_RENDERING, "Highest quality color rendering mode");

    public static final Object VALUE_COLOR_RENDER_DEFAULT = newValue(K_COLOR_RENDERING, "Default color rendering mode");

    public static final Key KEY_STROKE_CONTROL = K_STROKE_CONTROL;

    public static final Object VALUE_STROKE_DEFAULT = newValue(K_STROKE_CONTROL, "Default stroke normalization");

    public static final Object VALUE_STROKE_NORMALIZE = newValue(K_STROKE_CONTROL, "Normalize strokes for consistent rendering");

    public static final Object VALUE_STROKE_PURE = newValue(K_STROKE_CONTROL, "Pure stroke conversion for accurate paths");

    public static final Key KEY_RESOLUTION_VARIANT = K_RESOLUTION_VARIANT;

    public static final Object VALUE_RESOLUTION_VARIANT_DEFAULT = newValue(K_RESOLUTION_VARIANT, "Choose image resolutions based on a default heuristic");

    public static final Object VALUE_RESOLUTION_VARIANT_BASE = newValue(K_RESOLUTION_VARIANT, "Use only the standard resolution of an image");

    public static final Object VALUE_RESOLUTION_VARIANT_SIZE_FIT = newValue(K_RESOLUTION_VARIANT, "Choose image resolutions based on the DPI of the screen and transform in the Graphics2D context");

    public static final Object VALUE_RESOLUTION_VARIANT_DPI_FIT = newValue(K_RESOLUTION_VARIANT, "Choose image resolutions based only on the DPI of the screen");

    HashMap<Object, Object> hintmap = new HashMap<Object, Object>();

    /** A {@code null} gives an empty map and not an exception: it is the "no preferences" case. */
    public RenderingHints(Map<Key, ?> init) {
        if (init != null) {
            hintmap.putAll(init);
        }
    }

    public RenderingHints(Key key, Object value) {
        hintmap.put(key, value);
    }

    public int size() {
        return hintmap.size();
    }

    public boolean isEmpty() {
        return hintmap.isEmpty();
    }

    public boolean containsKey(Object key) {
        // The cast is not decorative: it forces the key to be a Key and turns into a
        // ClassCastException what would otherwise be a silent "not there".
        return hintmap.containsKey((Key) key);
    }

    public boolean containsValue(Object value) {
        return hintmap.containsValue(value);
    }

    public Object get(Object key) {
        return hintmap.get((Key) key);
    }

    /** Validates before storing: an incompatible value is rejected here and not when drawing. */
    public Object put(Object key, Object value) {
        if (!((Key) key).isCompatibleValue(value)) {
            throw new IllegalArgumentException(value + " incompatible with " + key);
        }
        return hintmap.put((Key) key, value);
    }

    /**
     * Merges another set on top of this one, without validating. This javadoc justified that by
     * saying what came from another RenderingHints already went through {@code put}; the {@link
     * #RenderingHints(Map)} constructor copies without validating, so it may not have.
     */
    public void add(RenderingHints hints) {
        hintmap.putAll(hints.hintmap);
    }

    public void clear() {
        hintmap.clear();
    }

    public Object remove(Object key) {
        return hintmap.remove((Key) key);
    }

    public void putAll(Map<?, ?> m) {
        if (m instanceof RenderingHints) {
            hintmap.putAll(((RenderingHints) m).hintmap);
        } else {
            // An arbitrary Map may bring garbage, so each pair goes through put and is validated.
            java.util.Iterator<?> it = m.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Set<Object> keySet() {
        return hintmap.keySet();
    }

    public Collection<Object> values() {
        return hintmap.values();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return java.util.Collections.unmodifiableMap(hintmap).entrySet();
    }

    /** It is equal to any {@code Map} with the same content, not only to another RenderingHints. */
    public boolean equals(Object o) {
        if (o instanceof RenderingHints) {
            return hintmap.equals(((RenderingHints) o).hintmap);
        } else if (o instanceof Map) {
            return hintmap.equals(o);
        }
        return false;
    }

    public int hashCode() {
        return hintmap.hashCode();
    }

    /** A shallow copy of the map: the keys and values are shared singletons on purpose. */
    public Object clone() {
        RenderingHints rh;
        try {
            rh = (RenderingHints) super.clone();
            if (hintmap != null) {
                rh.hintmap = new HashMap<Object, Object>(hintmap);
            }
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        return rh;
    }

    public String toString() {
        if (hintmap == null) {
            return getClass().getName() + "@" + Integer.toHexString(hashCode()) + " (0 hints)";
        }
        return hintmap.toString();
    }
}
