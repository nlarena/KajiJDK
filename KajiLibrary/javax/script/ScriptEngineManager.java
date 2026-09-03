package javax.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * KajiLibrary's javax.script.ScriptEngineManager -- el que encuentra motores y no encuentra
 * ninguno.
 *
 * <p>Hace tres cosas. Descubre las {@link ScriptEngineFactory} que haya en el classpath via
 * {@link ServiceLoader}; busca entre ellas por nombre corto, por extension de archivo o por tipo
 * MIME; y mantiene el {@link Bindings} global que comparten todos los motores que salgan de aca --
 * eso ultimo es la razon por la que el manager existe y no alcanza con un `ServiceLoader` pelado.
 *
 * <h2>Que va a devolver esto en la practica</h2>
 *
 * <p><b>Nulo.</b> {@code getEngineByName("js")} devuelve nulo, y {@code getEngineFactories()}
 * devuelve una lista vacia. No es una limitacion de KajiLibrary: **un JDK 25 real hace exactamente
 * lo mismo**. Nashorn, el unico motor que el JDK traia, se marco obsoleto en 11 y se elimino en
 * 15; desde entonces `java.scripting` es la API sin ninguna implementacion adentro, y un
 * `ScriptEngineManager` recien construido no tiene nada que ofrecer salvo que el classpath traiga
 * un motor de terceros. Se comprueba corriendo el mismo programa con el `java` de verdad.
 *
 * <p>Nuestro techo esta un escalon mas abajo y conviene decirlo igual: aca no se encontraria un
 * motor de terceros ni aunque estuviera bien declarado en el classpath. La diferencia no se ve
 * desde afuera mientras no haya motores, pero existe, y son **dos** frenos independientes --
 * arreglar uno solo no alcanza:
 *
 * <ul>
 *   <li>{@code ServiceLoader} no lee `META-INF/services`: su paso de descubrimiento devuelve la
 *       lista vacia sin consultar ningun recurso. Todo el resto de esa clase --parseo del archivo,
 *       instanciado, `ServiceConfigurationError`-- esta escrito y anda sobre lo que ese paso
 *       devuelva.
 *   <li>La busqueda de recursos de {@link ClassLoader} existe como API pero no sirve nada:
 *       {@code getSystemResource("java/lang/Object.class")} devuelve nulo, y eso que es la clase
 *       con la que se arranco. No es que el recurso no este en el classpath; es que
 *       `findResource`/`findResources` no lo miran.
 * </ul>
 *
 * <p>Comprobado con el mismo programa contra las dos VMs, con un `META-INF/services` de verdad en
 * el classpath: el `java` del JDK 25 encuentra el recurso y el proveedor, y nosotros contamos cero
 * en las dos cosas. (El comentario de nuestro `ServiceLoader` atribuye esto a que falta
 * `ClassLoader.getResources`; ese metodo hoy **existe**, asi que la razon que da quedo vieja.)
 *
 * <p>Lo que si funciona de punta a punta es el registro manual --
 * {@link #registerEngineName(String, ScriptEngineFactory)} y sus dos hermanos --, que no depende
 * del descubrimiento: quien tenga una fabrica en la mano la asocia a un nombre y la busqueda la
 * encuentra. Las asociaciones manuales se miran **antes** que las descubiertas.
 */
public class ScriptEngineManager {

    /** Las fabricas descubiertas. Orden de descubrimiento. */
    private final Set<ScriptEngineFactory> engineSpis;

    /** Nombre corto -&gt; fabrica, registrado a mano. */
    private final HashMap<String, ScriptEngineFactory> nameAssociations;

    /** Extension -&gt; fabrica, registrado a mano. */
    private final HashMap<String, ScriptEngineFactory> extensionAssociations;

    /** Tipo MIME -&gt; fabrica, registrado a mano. */
    private final HashMap<String, ScriptEngineFactory> mimeTypeAssociations;

    /** El ambito global que se le pone a cada motor que sale de aca. */
    private Bindings globalScope;

    /**
     * Descubre con el cargador de contexto del hilo actual.
     *
     * <p>Ese cargador es el que un contenedor cambia por aplicacion, y por eso es el correcto
     * cuando el manager se construye desde adentro de una.
     */
    public ScriptEngineManager() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Descubre con `loader`.
     *
     * <p>Con `loader` nulo se buscan solo los motores instalados con la plataforma, que en un JDK
     * moderno son cero.
     */
    public ScriptEngineManager(ClassLoader loader) {
        engineSpis = new LinkedHashSet<ScriptEngineFactory>();
        nameAssociations = new HashMap<String, ScriptEngineFactory>();
        extensionAssociations = new HashMap<String, ScriptEngineFactory>();
        mimeTypeAssociations = new HashMap<String, ScriptEngineFactory>();
        globalScope = new SimpleBindings();
        initEngines(loader);
    }

    /**
     * Junta las fabricas que el `ServiceLoader` sepa dar.
     *
     * <p>Una fabrica rota no voltea el descubrimiento: se saltea y se sigue con las demas, que es
     * lo unico razonable cuando el classpath lo arma otro.
     */
    private void initEngines(ClassLoader loader) {
        try {
            ServiceLoader<ScriptEngineFactory> sl;
            if (loader != null) {
                sl = ServiceLoader.load(ScriptEngineFactory.class, loader);
            } else {
                sl = ServiceLoader.loadInstalled(ScriptEngineFactory.class);
            }
            for (ScriptEngineFactory spi : sl) {
                if (spi != null) {
                    engineSpis.add(spi);
                }
            }
        } catch (ServiceConfigurationError err) {
            // Un proveedor mal declarado no puede dejar al manager sin construir.
        } catch (RuntimeException exp) {
            // Idem para una fabrica que explota en su propio constructor.
        }
    }

    /**
     * Cambia el ambito global.
     *
     * @throws IllegalArgumentException si `bindings` es nulo -- ojo, no es un NPE
     */
    public void setBindings(Bindings bindings) {
        if (bindings == null) {
            throw new IllegalArgumentException("Global scope cannot be null.");
        }
        globalScope = bindings;
    }

    /** El ambito global. Nunca es nulo. */
    public Bindings getBindings() {
        return globalScope;
    }

    /**
     * Define `key` en el ambito global.
     *
     * @throws NullPointerException si `key` es nulo
     * @throws IllegalArgumentException si `key` es vacio
     */
    public void put(String key, Object value) {
        globalScope.put(key, value);
    }

    /**
     * Lo que valga `key` en el ambito global.
     *
     * @throws NullPointerException si `key` es nulo
     * @throws IllegalArgumentException si `key` es vacio
     */
    public Object get(String key) {
        return globalScope.get(key);
    }

    /**
     * Un motor cuyo nombre corto sea `shortName`, o nulo si no hay ninguno.
     *
     * @throws NullPointerException si `shortName` es nulo
     */
    public ScriptEngine getEngineByName(String shortName) {
        Objects.requireNonNull(shortName);
        return buscar(shortName, nameAssociations, CLAVE_NOMBRES);
    }

    /**
     * Un motor que atienda la extension `extension`, o nulo.
     *
     * @throws NullPointerException si `extension` es nulo
     */
    public ScriptEngine getEngineByExtension(String extension) {
        Objects.requireNonNull(extension);
        return buscar(extension, extensionAssociations, CLAVE_EXTENSIONES);
    }

    /**
     * Un motor que atienda el tipo MIME `mimeType`, o nulo.
     *
     * @throws NullPointerException si `mimeType` es nulo
     */
    public ScriptEngine getEngineByMimeType(String mimeType) {
        Objects.requireNonNull(mimeType);
        return buscar(mimeType, mimeTypeAssociations, CLAVE_TIPOS);
    }

    /** Cual de las tres listas de una fabrica mirar. Interno, no forma parte del contrato. */
    private static final int CLAVE_NOMBRES = 0;
    private static final int CLAVE_EXTENSIONES = 1;
    private static final int CLAVE_TIPOS = 2;

    /** Las claves que `spi` publica para el criterio pedido. */
    private static List<String> clavesDe(ScriptEngineFactory spi, int criterio) {
        if (criterio == CLAVE_NOMBRES) {
            return spi.getNames();
        } else if (criterio == CLAVE_EXTENSIONES) {
            return spi.getExtensions();
        }
        return spi.getMimeTypes();
    }

    /**
     * Primero lo registrado a mano, despues lo descubierto; el primero que sirva.
     *
     * <p>Que lo manual gane no es un detalle: registrar es la forma de decir "para este nombre
     * quiero esta", y no serviria de nada si una descubierta pudiera adelantarsele.
     */
    private ScriptEngine buscar(String clave, Map<String, ScriptEngineFactory> asociadas,
            int criterio) {
        ScriptEngineFactory registrada = asociadas.get(clave);
        if (registrada != null) {
            ScriptEngine engine = motorDe(registrada);
            if (engine != null) {
                return engine;
            }
        }
        for (ScriptEngineFactory spi : engineSpis) {
            List<String> claves;
            try {
                claves = clavesDe(spi, criterio);
            } catch (RuntimeException exp) {
                continue;
            }
            if (claves == null) {
                continue;
            }
            for (String c : claves) {
                if (clave.equals(c)) {
                    ScriptEngine engine = motorDe(spi);
                    if (engine != null) {
                        return engine;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Un motor de esa fabrica, ya conectado al ambito global del manager, o nulo si la fabrica
     * fallo.
     *
     * <p>Conectar el global aca es todo el valor agregado del manager: dos motores pedidos al
     * mismo manager comparten lo que se puso con {@link #put(String, Object)}.
     */
    private ScriptEngine motorDe(ScriptEngineFactory spi) {
        try {
            ScriptEngine engine = spi.getScriptEngine();
            if (engine != null) {
                engine.setBindings(getBindings(), ScriptContext.GLOBAL_SCOPE);
            }
            return engine;
        } catch (RuntimeException exp) {
            return null;
        }
    }

    /**
     * Las fabricas descubiertas, en una lista inmutable.
     *
     * <p>No incluye las registradas a mano, igual que el original: `register*` asocia una clave,
     * no agrega un proveedor.
     */
    public List<ScriptEngineFactory> getEngineFactories() {
        return List.copyOf(new ArrayList<ScriptEngineFactory>(engineSpis));
    }

    /**
     * Asocia el nombre corto `name` a `factory`.
     *
     * @throws NullPointerException si alguno es nulo
     */
    public void registerEngineName(String name, ScriptEngineFactory factory) {
        asociar(nameAssociations, name, factory);
    }

    /**
     * Asocia el tipo MIME `type` a `factory`.
     *
     * @throws NullPointerException si alguno es nulo
     */
    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        asociar(mimeTypeAssociations, type, factory);
    }

    /**
     * Asocia la extension `extension` a `factory`.
     *
     * @throws NullPointerException si alguno es nulo
     */
    public void registerEngineExtension(String extension, ScriptEngineFactory factory) {
        asociar(extensionAssociations, extension, factory);
    }

    /** Las tres registraciones son la misma con distinto mapa. */
    private static void asociar(Map<String, ScriptEngineFactory> mapa, String clave,
            ScriptEngineFactory factory) {
        Objects.requireNonNull(clave);
        Objects.requireNonNull(factory);
        mapa.put(clave, factory);
    }
}
