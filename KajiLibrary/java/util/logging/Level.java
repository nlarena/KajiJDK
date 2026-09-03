package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Level -- cuanto importa un mensaje.
 *
 * <p>Es una clase y no un enum, y eso es deliberado en el JDK: el constructor es `protected`
 * justamente para que alguien pueda inventar un nivel intermedio. Lo que realmente ordena es
 * {@link #intValue}, no la identidad -- comparar niveles por `==` funciona para los nueve estandar y
 * falla para cualquier nivel propio.
 *
 * <p>Los valores no son consecutivos (1000, 900, 800, 700, 500, 400, 300) y esa es la razon de que se
 * pueda intercalar uno nuevo sin renumerar nada.
 *
 * <p>{@link #OFF} y {@link #ALL} no son niveles de mensaje sino de **filtro**: nada los alcanza o
 * todo los pasa, y por eso valen `Integer.MAX_VALUE` y `Integer.MIN_VALUE`.
 */
public class Level implements java.io.Serializable {

    /** Nada se registra. */
    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE);

    /** Un fallo serio, de los que le importan a quien usa el programa. */
    public static final Level SEVERE = new Level("SEVERE", 1000);

    /** Algo que conviene mirar. */
    public static final Level WARNING = new Level("WARNING", 900);

    /** Informacion normal. */
    public static final Level INFO = new Level("INFO", 800);

    /** Mensajes de configuracion, para diagnosticar el entorno. */
    public static final Level CONFIG = new Level("CONFIG", 700);

    /** Traza gruesa, para seguir el programa. */
    public static final Level FINE = new Level("FINE", 500);

    /** Traza mas detallada; entradas y salidas de metodo. */
    public static final Level FINER = new Level("FINER", 400);

    /** Todo el detalle. */
    public static final Level FINEST = new Level("FINEST", 300);

    /** Todo se registra. */
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE);

    private final String name;
    private final int value;
    private final String resourceBundleName;

    protected Level(String name, int value) {
        this(name, value, null);
    }

    protected Level(String name, int value, String resourceBundleName) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
    }

    /** El nombre del nivel. */
    public String getName() {
        return this.name;
    }

    /** El nombre traducido; aca, el mismo: no hay localizacion. */
    public String getLocalizedName() {
        return this.name;
    }

    /** El paquete de recursos para traducirlo, o `null`. */
    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    /** El numero que ordena este nivel contra los demas. */
    public final int intValue() {
        return this.value;
    }

    public final String toString() {
        return this.name;
    }

    /**
     * El nivel de ese nombre, o el de ese numero escrito como texto.
     *
     * <p>Acepta las dos formas porque la configuracion viene de cadenas: `"FINE"` y `"500"` designan
     * al mismo nivel, y un numero que no corresponde a ninguno estandar da un nivel nuevo -- que es
     * lo que permite configurar un nivel intercalado sin declararlo.
     *
     * @throws IllegalArgumentException si no es ni un nombre conocido ni un numero
     */
    public static synchronized Level parse(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Level[] conocidos = new Level[] {OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST,
                ALL};
        int i = 0;
        while (i < conocidos.length) {
            if (conocidos[i].name.equals(name)) {
                return conocidos[i];
            }
            i = i + 1;
        }
        int valor;
        try {
            valor = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad level \"" + name + "\"");
        }
        i = 0;
        while (i < conocidos.length) {
            if (conocidos[i].value == valor) {
                return conocidos[i];
            }
            i = i + 1;
        }
        return new Level(name, valor);
    }

    public boolean equals(Object ox) {
        if (ox instanceof Level) {
            return ((Level) ox).value == this.value;
        }
        return false;
    }

    public int hashCode() {
        return this.value;
    }
}
