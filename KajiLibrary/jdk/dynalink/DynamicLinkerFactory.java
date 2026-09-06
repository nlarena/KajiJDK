package jdk.dynalink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import jdk.dynalink.beans.BeansLinker;
import jdk.dynalink.linker.ConversionComparator;
import jdk.dynalink.linker.GuardedInvocationTransformer;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.GuardingDynamicLinkerExporter;
import jdk.dynalink.linker.GuardingTypeConverterFactory;
import jdk.dynalink.linker.MethodHandleTransformer;
import jdk.dynalink.linker.MethodTypeConversionStrategy;
import jdk.dynalink.linker.support.CompositeGuardingDynamicLinker;

/**
 * Arma un {@link DynamicLinker}.
 *
 * <h2>El orden de los enlazadores es la configuracion</h2>
 *
 * <p>Un sitio se le ofrece a cada enlazador por turno y se queda con el primero que sepa atenderlo.
 * Eso hace que el orden sea lo unico que hay que decidir, y por eso hay tres grupos: los
 * prioritarios, que ven todo primero; los que se cargan solos; y los de ultimo recurso.
 *
 * <p>Los del medio se descubren por {@link GuardingDynamicLinkerExporter}: es como un lenguaje que
 * corre encima --o una biblioteca que se agrega al camino de clases-- entra en el sistema sin que
 * nadie lo nombre. {@link #getAutoLoadingErrors} es donde se ven los que no se pudieron cargar; se
 * juntan en vez de tirar porque un exportador roto de una biblioteca no tiene que impedir que el
 * lenguaje arranque.
 *
 * <p>El de ultimo recurso por omision es {@link BeansLinker}, que atiende objetos Java comunes.
 * Ponerlo al final y no al principio importa: si viera los sitios primero, atenderia los objetos
 * propios del lenguaje como si fueran objetos Java.
 *
 * <h2>El umbral de inestabilidad</h2>
 *
 * <p>{@link #setUnstableRelinkThreshold} es cuantas veces se deja reenlazar un sitio antes de
 * declararlo inestable. Es una apuesta sobre el programa: la mayoria de los sitios ven un solo tipo,
 * unos pocos ven dos o tres, y muy pocos ven muchos. Encadenar sirve para los primeros y estorba
 * para los ultimos.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La fabrica funciona entera: el descubrimiento, la composicion y la configuracion. Lo que no
 * anda es enlazar, que es de {@link DynamicLinker} y necesita manijas de metodo.
 *
 * @since 9
 */
public final class DynamicLinkerFactory {

    /** Cuantas reenlazadas antes de declarar inestable un sitio, si no se dice otra cosa. */
    private static final int UMBRAL_POR_OMISION = 8;

    private ClassLoader cargador;
    private boolean cargadorPuesto;
    private List<GuardingDynamicLinker> prioritarios;
    private List<GuardingDynamicLinker> ultimoRecurso;
    private boolean sincronizar;
    private int umbralInestable = UMBRAL_POR_OMISION;
    private GuardedInvocationTransformer preenlace;
    private MethodTypeConversionStrategy autoConversion;
    private MethodHandleTransformer filtroInterno;
    private List<ServiceConfigurationError> errores = Collections.emptyList();

    /** Una. */
    public DynamicLinkerFactory() {
    }

    /**
     * Con que cargador de clases buscar los enlazadores que se cargan solos.
     *
     * <p>Pasar {@code null} no vuelve al de omision: apaga el descubrimiento. Es la forma de armar
     * un enlazador que solo tenga lo que se le puso a mano, que es lo que quiere un programa que no
     * confia en lo que haya en el camino de clases.
     *
     * @param classLoader el cargador, o {@code null} para no descubrir nada
     */
    public void setClassLoader(final ClassLoader classLoader) {
        this.cargador = classLoader;
        this.cargadorPuesto = true;
    }

    /**
     * Los enlazadores que ven los sitios antes que nadie.
     *
     * @param prioritizedLinkers los enlazadores, o {@code null} para que no haya ninguno
     * @throws NullPointerException si la lista tiene algun elemento {@code null}
     */
    public void setPrioritizedLinkers(final List<? extends GuardingDynamicLinker>
            prioritizedLinkers) {
        this.prioritarios = copiarSinNulos(prioritizedLinkers);
    }

    /**
     * Lo mismo, sueltos.
     *
     * @param prioritizedLinkers los enlazadores
     * @throws NullPointerException si alguno es {@code null}
     */
    public void setPrioritizedLinkers(final GuardingDynamicLinker... prioritizedLinkers) {
        setPrioritizedLinkers(comoLista(prioritizedLinkers));
    }

    /**
     * Uno solo con prioridad.
     *
     * @param prioritizedLinker el enlazador
     * @throws NullPointerException si es {@code null}
     */
    public void setPrioritizedLinker(final GuardingDynamicLinker prioritizedLinker) {
        if (prioritizedLinker == null) {
            throw new NullPointerException("prioritizedLinker");
        }
        final List<GuardingDynamicLinker> uno = new ArrayList<GuardingDynamicLinker>();
        uno.add(prioritizedLinker);
        this.prioritarios = uno;
    }

    /**
     * Los enlazadores de ultimo recurso.
     *
     * <p>Pasar {@code null} restituye el de omision --un {@link BeansLinker}--; pasar una lista
     * vacia deja al enlazador sin ultimo recurso, que es distinto: un sitio que nadie sepa atender
     * va a fallar en vez de tratarse como un objeto Java.
     *
     * @param fallbackLinkers los enlazadores, o {@code null} para el de omision
     * @throws NullPointerException si la lista tiene algun elemento {@code null}
     */
    public void setFallbackLinkers(final List<? extends GuardingDynamicLinker> fallbackLinkers) {
        this.ultimoRecurso = copiarSinNulos(fallbackLinkers);
    }

    /**
     * Lo mismo, sueltos.
     *
     * @param fallbackLinkers los enlazadores
     * @throws NullPointerException si alguno es {@code null}
     */
    public void setFallbackLinkers(final GuardingDynamicLinker... fallbackLinkers) {
        setFallbackLinkers(comoLista(fallbackLinkers));
    }

    /**
     * Si hay que reenlazar bajo el candado del sitio.
     *
     * <p>Hace falta cuando el sitio guarda una cadena de invocaciones que dos hilos podrian estar
     * modificando a la vez. Cuesta, asi que no esta prendido por omision.
     *
     * @param syncOnRelink cierto para sincronizar
     */
    public void setSyncOnRelink(final boolean syncOnRelink) {
        this.sincronizar = syncOnRelink;
    }

    /**
     * Cuantas reenlazadas antes de declarar inestable un sitio.
     *
     * @param unstableRelinkThreshold cuantas; cero declara inestable desde la primera
     * @throws IllegalArgumentException si es negativo
     */
    public void setUnstableRelinkThreshold(final int unstableRelinkThreshold) {
        if (unstableRelinkThreshold < 0) {
            throw new IllegalArgumentException("unstableRelinkThreshold < 0");
        }
        this.umbralInestable = unstableRelinkThreshold;
    }

    /**
     * Que hacerle a cada invocacion antes de instalarla.
     *
     * <p>Es donde un lenguaje mete lo suyo alrededor de toda invocacion: contar llamadas, revisar
     * permisos, envolver excepciones.
     *
     * @param prelinkTransformer que hacerle, o {@code null} para nada
     */
    public void setPrelinkTransformer(final GuardedInvocationTransformer prelinkTransformer) {
        this.preenlace = prelinkTransformer;
    }

    /**
     * Como convertir lo que el lenguaje de Java no sabe convertir.
     *
     * <p>Corre despues de la conversion de Java, no en su lugar.
     *
     * @param autoConversionStrategy la estrategia, o {@code null} para no tener ninguna
     */
    public void setAutoConversionStrategy(
            final MethodTypeConversionStrategy autoConversionStrategy) {
        this.autoConversion = autoConversionStrategy;
    }

    /**
     * Que hacerle a los objetos internos del lenguaje que salgan hacia afuera.
     *
     * <p>Un lenguaje que representa sus valores con clases propias no quiere que esas clases se
     * filtren al codigo Java que lo hospeda; esto es donde se las envuelve o se las convierte.
     *
     * @param internalObjectsFilter el filtro, o {@code null} para no filtrar nada
     */
    public void setInternalObjectsFilter(final MethodHandleTransformer internalObjectsFilter) {
        this.filtroInterno = internalObjectsFilter;
    }

    /**
     * Arma el enlazador con lo que se le dijo.
     *
     * <p>Se puede llamar mas de una vez: cada llamada vuelve a descubrir y arma otro enlazador.
     *
     * @return el enlazador
     */
    public DynamicLinker createLinker() {
        final List<ServiceConfigurationError> males = new ArrayList<ServiceConfigurationError>();
        final List<GuardingDynamicLinker> todos = new ArrayList<GuardingDynamicLinker>();
        if (this.prioritarios != null) {
            todos.addAll(this.prioritarios);
        }
        todos.addAll(descubrir(males));
        if (this.ultimoRecurso == null) {
            todos.add(new BeansLinker());
        } else {
            todos.addAll(this.ultimoRecurso);
        }
        this.errores = Collections.unmodifiableList(males);

        // Las fabricas de conversion y los comparadores salen de los mismos enlazadores: un
        // enlazador que sabe convertir lo dice implementando la interfaz, no registrandose aparte.
        final List<GuardingTypeConverterFactory> fabricas =
                new ArrayList<GuardingTypeConverterFactory>();
        final List<ConversionComparator> comparadores = new ArrayList<ConversionComparator>();
        for (int i = 0; i < todos.size(); i++) {
            final GuardingDynamicLinker l = todos.get(i);
            if (l instanceof GuardingTypeConverterFactory) {
                fabricas.add((GuardingTypeConverterFactory) l);
            }
            if (l instanceof ConversionComparator) {
                comparadores.add((ConversionComparator) l);
            }
        }
        final ServiciosDeEnlace servicios = new ServiciosDeEnlace(
                new CompositeGuardingDynamicLinker(todos), fabricas, comparadores,
                this.autoConversion, this.filtroInterno);
        return new DynamicLinker(servicios, this.preenlace, this.sincronizar,
                this.umbralInestable);
    }

    /**
     * Los enlazadores que no se pudieron cargar en el ultimo {@link #createLinker}.
     *
     * @return los errores, o una lista vacia si no hubo ninguno
     */
    public List<ServiceConfigurationError> getAutoLoadingErrors() {
        return this.errores;
    }

    /**
     * Los enlazadores que se anuncian solos.
     *
     * <p>Un exportador roto se anota y se sigue: la lista de enlazadores que si cargaron es mas
     * util que un arranque fallido, y quien quiera enterarse tiene {@link #getAutoLoadingErrors}.
     */
    private List<GuardingDynamicLinker> descubrir(final List<ServiceConfigurationError> males) {
        final List<GuardingDynamicLinker> out = new ArrayList<GuardingDynamicLinker>();
        if (this.cargadorPuesto && this.cargador == null) {
            return out;
        }
        final ClassLoader cl = this.cargadorPuesto ? this.cargador
                : Thread.currentThread().getContextClassLoader();
        final ServiceLoader<GuardingDynamicLinkerExporter> cargados =
                ServiceLoader.load(GuardingDynamicLinkerExporter.class, cl);
        final Iterator<GuardingDynamicLinkerExporter> it = cargados.iterator();
        while (true) {
            final GuardingDynamicLinkerExporter exportador;
            try {
                if (!it.hasNext()) {
                    return out;
                }
                exportador = it.next();
            } catch (final ServiceConfigurationError e) {
                males.add(e);
                continue;
            }
            final List<GuardingDynamicLinker> suyos = exportador.get();
            if (suyos != null) {
                for (int i = 0; i < suyos.size(); i++) {
                    if (suyos.get(i) != null) {
                        out.add(suyos.get(i));
                    }
                }
            }
        }
    }

    /** Una copia sin nulos, o {@code null} si la lista era {@code null}. */
    private static List<GuardingDynamicLinker> copiarSinNulos(
            final List<? extends GuardingDynamicLinker> lista) {
        if (lista == null) {
            return null;
        }
        final List<GuardingDynamicLinker> out = new ArrayList<GuardingDynamicLinker>();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i) == null) {
                throw new NullPointerException("List has at least one null element");
            }
            out.add(lista.get(i));
        }
        return out;
    }

    private static List<GuardingDynamicLinker> comoLista(final GuardingDynamicLinker[] a) {
        if (a == null) {
            return null;
        }
        final List<GuardingDynamicLinker> out = new ArrayList<GuardingDynamicLinker>();
        for (int i = 0; i < a.length; i++) {
            out.add(a[i]);
        }
        return out;
    }
}
