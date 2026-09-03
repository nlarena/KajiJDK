package java.awt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Las preferencias de dibujo: antialias si o no, calidad contra velocidad, que interpolacion usar
 * al escalar una imagen.
 *
 * <p>Son preferencias y no ordenes --el rasterizador puede ignorarlas-- y esa es toda la semantica.
 * Por eso la clase se puede escribir entera aunque no haya ningun rasterizador: es un {@code Map}
 * con una regla de validacion.
 *
 * <h2>Por que las claves son objetos y no cadenas</h2>
 *
 * <p>{@code Key} es abstracta y su {@code equals} es final e identidad pura. La consecuencia es que
 * nadie puede fabricar una clave "igual" a {@code KEY_ANTIALIASING} sin tener la constante, y que
 * dos bibliotecas que agreguen sus propias claves no se pisan aunque elijan el mismo nombre. Con
 * cadenas eso no se podria garantizar.
 *
 * <p>Y por eso {@code put} valida: cada clave sabe que valores acepta, asi que meter
 * {@code VALUE_RENDER_QUALITY} bajo {@code KEY_ANTIALIASING} tira
 * {@code IllegalArgumentException} en el momento en vez de dar un dibujo raro mucho despues.
 *
 * <h2>Sobre las constantes de valor</h2>
 *
 * <p>Los {@code VALUE_*} estan declarados como {@code Object} en la API justamente para que su
 * clase concreta sea privada: el JDK usa una clase interna suya y aca se usa otra. Lo unico que un
 * programa puede --y debe-- hacer con ellos es compararlos por identidad y pasarlos a {@code put}.
 * Su {@code toString()} no esta especificado en ningun lado y no conviene depender de el.
 */
public class RenderingHints implements Map<Object, Object>, Cloneable {

    /**
     * La clave de una preferencia.
     *
     * <p>El {@code equals} y el {@code hashCode} son finales: una clave es ella misma y nada mas.
     * Si se pudieran redefinir, una subclase podria hacerse pasar por otra clave y colarse valores
     * en el mapa de otra biblioteca.
     */
    public abstract static class Key {

        // Dos claves distintas de la misma clase con el mismo entero privado serian
        // indistinguibles para quien las implementa, asi que se detecta al construirlas y no
        // despues, cuando el sintoma seria una preferencia que se pisa sola.
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

    // --- la clave y el valor concretos ---
    //
    // En el JDK viven en un paquete interno y son inaccesibles a proposito. Aca se usan clases
    // privadas anidadas por lo mismo: la API dice `Key` y `Object`, y ninguna de las dos clases
    // concretas es parte del contrato.

    private static class ValorKey extends Key {

        private int min;

        private int max;

        ValorKey(int privatekey) {
            super(privatekey);
        }

        ValorKey(int privatekey, int min, int max) {
            super(privatekey);
            this.min = min;
            this.max = max;
            this.esEntera = true;
        }

        private boolean esEntera;

        public boolean isCompatibleValue(Object val) {
            if (esEntera) {
                return val instanceof Integer
                        && ((Integer) val).intValue() >= min
                        && ((Integer) val).intValue() <= max;
            }
            return val instanceof Valor && ((Valor) val).duenia == this;
        }
    }

    private static class Valor {

        private ValorKey duenia;

        private String descripcion;

        Valor(ValorKey duenia, String descripcion) {
            this.duenia = duenia;
            this.descripcion = descripcion;
        }

        public String toString() {
            return descripcion;
        }
    }

    private static ValorKey clave(int i) {
        return new ValorKey(i);
    }

    private static Object valor(Key k, String d) {
        return new Valor((ValorKey) k, d);
    }

    private static final ValorKey K_ANTIALIASING = clave(1);

    private static final ValorKey K_RENDERING = clave(2);

    private static final ValorKey K_DITHERING = clave(3);

    private static final ValorKey K_TEXT_ANTIALIASING = clave(4);

    private static final ValorKey K_FRACTIONALMETRICS = clave(5);

    private static final ValorKey K_INTERPOLATION = clave(6);

    private static final ValorKey K_ALPHA_INTERPOLATION = clave(7);

    private static final ValorKey K_COLOR_RENDERING = clave(8);

    private static final ValorKey K_STROKE_CONTROL = clave(9);

    private static final ValorKey K_RESOLUTION_VARIANT = clave(10);

    public static final Key KEY_ANTIALIASING = K_ANTIALIASING;

    public static final Object VALUE_ANTIALIAS_ON = valor(K_ANTIALIASING, "Antialiased rendering mode");

    public static final Object VALUE_ANTIALIAS_OFF = valor(K_ANTIALIASING, "Nonantialiased rendering mode");

    public static final Object VALUE_ANTIALIAS_DEFAULT = valor(K_ANTIALIASING, "Default antialiasing rendering mode");

    public static final Key KEY_RENDERING = K_RENDERING;

    public static final Object VALUE_RENDER_SPEED = valor(K_RENDERING, "Fastest rendering methods");

    public static final Object VALUE_RENDER_QUALITY = valor(K_RENDERING, "Highest quality rendering methods");

    public static final Object VALUE_RENDER_DEFAULT = valor(K_RENDERING, "Default rendering methods");

    public static final Key KEY_DITHERING = K_DITHERING;

    public static final Object VALUE_DITHER_DISABLE = valor(K_DITHERING, "Nondithered rendering mode");

    public static final Object VALUE_DITHER_ENABLE = valor(K_DITHERING, "Dithered rendering mode");

    public static final Object VALUE_DITHER_DEFAULT = valor(K_DITHERING, "Default dithering mode");

    public static final Key KEY_TEXT_ANTIALIASING = K_TEXT_ANTIALIASING;

    public static final Object VALUE_TEXT_ANTIALIAS_ON = valor(K_TEXT_ANTIALIASING, "Antialiased text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_OFF = valor(K_TEXT_ANTIALIASING, "Nonantialiased text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = valor(K_TEXT_ANTIALIASING, "Default antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_GASP = valor(K_TEXT_ANTIALIASING, "gasp antialiasing text mode");

    // Las cuatro LCD_* dicen en que orden fisico estan los subpixeles de la pantalla. No son
    // sinonimos: elegir la equivocada pinta franjas de color en los bordes de las letras.
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HRGB = valor(K_TEXT_ANTIALIASING, "LCD HRGB antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HBGR = valor(K_TEXT_ANTIALIASING, "LCD HBGR antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VRGB = valor(K_TEXT_ANTIALIASING, "LCD VRGB antialiasing text mode");

    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VBGR = valor(K_TEXT_ANTIALIASING, "LCD VBGR antialiasing text mode");

    /** La unica clave cuyo valor es un {@code Integer} y no una constante: 100 a 250. */
    public static final Key KEY_TEXT_LCD_CONTRAST = new ValorKey(100, 100, 250);

    public static final Key KEY_FRACTIONALMETRICS = K_FRACTIONALMETRICS;

    public static final Object VALUE_FRACTIONALMETRICS_OFF = valor(K_FRACTIONALMETRICS, "Integer text metrics mode");

    public static final Object VALUE_FRACTIONALMETRICS_ON = valor(K_FRACTIONALMETRICS, "Fractional text metrics mode");

    public static final Object VALUE_FRACTIONALMETRICS_DEFAULT = valor(K_FRACTIONALMETRICS, "Default fractional text metrics mode");

    public static final Key KEY_INTERPOLATION = K_INTERPOLATION;

    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = valor(K_INTERPOLATION, "Nearest Neighbor image interpolation mode");

    public static final Object VALUE_INTERPOLATION_BILINEAR = valor(K_INTERPOLATION, "Bilinear image interpolation mode");

    public static final Object VALUE_INTERPOLATION_BICUBIC = valor(K_INTERPOLATION, "Bicubic image interpolation mode");

    public static final Key KEY_ALPHA_INTERPOLATION = K_ALPHA_INTERPOLATION;

    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED = valor(K_ALPHA_INTERPOLATION, "Fastest alpha blending methods");

    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY = valor(K_ALPHA_INTERPOLATION, "Highest quality alpha blending methods");

    public static final Object VALUE_ALPHA_INTERPOLATION_DEFAULT = valor(K_ALPHA_INTERPOLATION, "Default alpha blending methods");

    public static final Key KEY_COLOR_RENDERING = K_COLOR_RENDERING;

    public static final Object VALUE_COLOR_RENDER_SPEED = valor(K_COLOR_RENDERING, "Fastest color rendering mode");

    public static final Object VALUE_COLOR_RENDER_QUALITY = valor(K_COLOR_RENDERING, "Highest quality color rendering mode");

    public static final Object VALUE_COLOR_RENDER_DEFAULT = valor(K_COLOR_RENDERING, "Default color rendering mode");

    public static final Key KEY_STROKE_CONTROL = K_STROKE_CONTROL;

    public static final Object VALUE_STROKE_DEFAULT = valor(K_STROKE_CONTROL, "Default stroke normalization");

    public static final Object VALUE_STROKE_NORMALIZE = valor(K_STROKE_CONTROL, "Normalize strokes for consistent rendering");

    public static final Object VALUE_STROKE_PURE = valor(K_STROKE_CONTROL, "Pure stroke conversion for accurate paths");

    public static final Key KEY_RESOLUTION_VARIANT = K_RESOLUTION_VARIANT;

    public static final Object VALUE_RESOLUTION_VARIANT_DEFAULT = valor(K_RESOLUTION_VARIANT, "Choose image resolutions based on a default heuristic");

    public static final Object VALUE_RESOLUTION_VARIANT_BASE = valor(K_RESOLUTION_VARIANT, "Use only the standard resolution of an image");

    public static final Object VALUE_RESOLUTION_VARIANT_SIZE_FIT = valor(K_RESOLUTION_VARIANT, "Choose image resolutions based on the DPI of the screen and transform in the Graphics2D context");

    public static final Object VALUE_RESOLUTION_VARIANT_DPI_FIT = valor(K_RESOLUTION_VARIANT, "Choose image resolutions based only on the DPI of the screen");

    HashMap<Object, Object> hintmap = new HashMap<Object, Object>();

    /** Un {@code null} da un mapa vacio y no una excepcion: es el caso de "sin preferencias". */
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
        // El cast no es decorativo: obliga a que la clave sea una Key y convierte en
        // ClassCastException lo que si no seria un silencioso "no esta".
        return hintmap.containsKey((Key) key);
    }

    public boolean containsValue(Object value) {
        return hintmap.containsValue(value);
    }

    public Object get(Object key) {
        return hintmap.get((Key) key);
    }

    /** Valida antes de guardar: un valor incompatible se rechaza aca y no al dibujar. */
    public Object put(Object key, Object value) {
        if (!((Key) key).isCompatibleValue(value)) {
            throw new IllegalArgumentException(value + " incompatible with " + key);
        }
        return hintmap.put((Key) key, value);
    }

    /**
     * Mezcla otro conjunto encima de este. No valida: lo que venia de otro RenderingHints ya paso
     * por {@code put} cuando se guardo ahi.
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
            // Un Map cualquiera puede traer basura, asi que cada par pasa por put y se valida.
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

    /** Es igual a un {@code Map} cualquiera con el mismo contenido, no solo a otro RenderingHints. */
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

    /** Copia superficial del mapa: las claves y los valores son singletons compartidos a proposito. */
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
