package javax.imageio.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KajiLibrary's javax.imageio.spi.ServiceRegistry -- un registro de proveedores, por categoria.
 *
 * <p>Es un {@link java.util.ServiceLoader} con dos cosas que aquel no tiene, y las dos importan:
 *
 * <ul>
 *   <li><b>se puede modificar en caliente</b>: registrar y dar de baja proveedores mientras el
 *       programa corre;
 *   <li><b>tiene orden parcial</b>: {@link #setOrdering} declara que un proveedor va antes que otro, y
 *       {@link #getServiceProviders} con {@code useOrdering} respeta esa relacion.
 * </ul>
 *
 * <h2>El orden es parcial, no total</h2>
 *
 * <p>Es la parte que se malinterpreta. No se declara una posicion sino <b>pares</b>: "A antes que B".
 * Los proveedores sin ninguna relacion entre si salen en cualquier orden.
 *
 * <p>Sirve exactamente para lo que hace falta --que un lector especializado de TIFF gane sobre el
 * generico-- sin obligar a nadie a inventar prioridades numericas.
 *
 * <p>Un ciclo en las relaciones lanza {@link IllegalArgumentException} al pedir el recorrido ordenado.
 *
 * <h2>Un proveedor por clase y por categoria</h2>
 *
 * <p>{@link #registerServiceProvider} reemplaza al que hubiera de la <b>misma clase</b> en esa
 * categoria. No es igualdad por {@code equals}: es la clase. Dos instancias distintas del mismo
 * proveedor no conviven, y eso evita que cargar el mismo complemento dos veces lo duplique.
 */
public class ServiceRegistry {

    /** Los proveedores de cada categoria, por clase. */
    private final Map<Class<?>, Map<Class<?>, Object>> categoryMap =
        new HashMap<Class<?>, Map<Class<?>, Object>>();

    /** Las categorias, en el orden en que se declararon. */
    private final List<Class<?>> categories = new ArrayList<Class<?>>();

    /** Las relaciones "va antes que", por categoria. */
    private final Map<Class<?>, Map<Object, Set<Object>>> orderings =
        new HashMap<Class<?>, Map<Object, Set<Object>>>();

    /**
     * @param categories las categorias que va a manejar
     * @throws IllegalArgumentException si es null
     */
    public ServiceRegistry(Iterator<Class<?>> categories) {
        if (categories == null) {
            throw new IllegalArgumentException("categories == null!");
        }
        while (categories.hasNext()) {
            Class<?> category = categories.next();
            this.categories.add(category);
            this.categoryMap.put(category, new java.util.LinkedHashMap<Class<?>, Object>());
            this.orderings.put(category, new HashMap<Object, Set<Object>>());
        }
    }

    /**
     * Los proveedores de ese tipo declarados como servicio.
     *
     * <p>Es {@link java.util.ServiceLoader} y nada mas; no toca este registro.
     */
    public static <T> Iterator<T> lookupProviders(Class<T> providerClass, ClassLoader loader) {
        return java.util.ServiceLoader.load(providerClass, loader).iterator();
    }

    /** Idem, con el cargador del contexto. */
    public static <T> Iterator<T> lookupProviders(Class<T> providerClass) {
        return java.util.ServiceLoader.load(providerClass).iterator();
    }

    /** Las categorias que maneja. */
    public Iterator<Class<?>> getCategories() {
        return new ArrayList<Class<?>>(this.categories).iterator();
    }

    /**
     * Registra un proveedor en esa categoria.
     *
     * <p>Ver la nota de la clase: reemplaza al de la misma clase si lo habia, y en ese caso al
     * reemplazado se le avisa con {@code onDeregistration}.
     *
     * @return si no habia ninguno de esa clase
     * @throws IllegalArgumentException si el proveedor es null o la categoria no esta declarada
     */
    public <T> boolean registerServiceProvider(T provider, Class<T> category) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        Map<Class<?>, Object> map = map(category);
        if (!category.isInstance(provider)) {
            throw new ClassCastException();
        }
        Object previous = map.put(provider.getClass(), provider);
        if (previous != null) {
            deregisterFromCategory(previous, category);
        }
        if (provider instanceof RegisterableService) {
            ((RegisterableService) provider).onRegistration(this, category);
        }
        return previous == null;
    }

    /**
     * Idem, en <b>todas</b> las categorias que le correspondan.
     *
     * @throws IllegalArgumentException si es null
     */
    public void registerServiceProvider(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        int i = 0;
        while (i < this.categories.size()) {
            Class<?> category = this.categories.get(i);
            if (category.isInstance(provider)) {
                registerUnchecked(provider, category);
            }
            i = i + 1;
        }
    }

    /**
     * Registra varios.
     *
     * @throws IllegalArgumentException si el iterador es null o trae un null
     */
    public void registerServiceProviders(Iterator<?> providers) {
        if (providers == null) {
            throw new IllegalArgumentException("providers == null!");
        }
        while (providers.hasNext()) {
            registerServiceProvider(providers.next());
        }
    }

    /**
     * Lo da de baja de esa categoria.
     *
     * @return si estaba
     * @throws IllegalArgumentException si es null o la categoria no esta declarada
     */
    public <T> boolean deregisterServiceProvider(T provider, Class<T> category) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        Map<Class<?>, Object> map = map(category);
        if (!category.isInstance(provider)) {
            throw new ClassCastException();
        }
        if (map.get(provider.getClass()) == provider) {
            map.remove(provider.getClass());
            deregisterFromCategory(provider, category);
            return true;
        }
        return false;
    }

    /**
     * Lo da de baja de todas.
     *
     * @throws IllegalArgumentException si es null
     */
    public void deregisterServiceProvider(Object provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider == null!");
        }
        int i = 0;
        while (i < this.categories.size()) {
            Class<?> category = this.categories.get(i);
            if (category.isInstance(provider)) {
                deregisterUnchecked(provider, category);
            }
            i = i + 1;
        }
    }

    /** Si ese proveedor esta registrado en alguna categoria. Por identidad, no por {@code equals}. */
    public boolean contains(Object provider) {
        if (provider == null) {
            return false;
        }
        int i = 0;
        while (i < this.categories.size()) {
            Map<Class<?>, Object> map = this.categoryMap.get(this.categories.get(i));
            if (map.get(provider.getClass()) == provider) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Los proveedores de esa categoria.
     *
     * @param useOrdering si respetar las relaciones de {@link #setOrdering}
     * @throws IllegalArgumentException si la categoria no esta declarada, o si el orden tiene un ciclo
     */
    public <T> Iterator<T> getServiceProviders(Class<T> category, boolean useOrdering) {
        return getServiceProviders(category, null, useOrdering);
    }

    /**
     * Idem, quedandose solo con los que pasen el filtro.
     *
     * @throws IllegalArgumentException si la categoria no esta declarada, o si el orden tiene un ciclo
     */
    public <T> Iterator<T> getServiceProviders(Class<T> category, Filter filter,
                                               boolean useOrdering) {
        Map<Class<?>, Object> map = map(category);
        List<Object> all = new ArrayList<Object>(map.values());
        if (useOrdering) {
            all = topologicalSort(all, this.orderings.get(category));
        }
        List<T> result = new ArrayList<T>();
        int i = 0;
        while (i < all.size()) {
            Object provider = all.get(i);
            if (filter == null || filter.filter(provider)) {
                result.add(category.cast(provider));
            }
            i = i + 1;
        }
        return result.iterator();
    }

    /**
     * El proveedor de esa clase exacta, o null.
     *
     * <p>Es por <b>clase</b> y no por categoria: sirve para encontrar un proveedor concreto del que se
     * sabe el nombre, que es como {@code ImageReaderSpi.getImageWriterSpiNames} se resuelve.
     *
     * @throws IllegalArgumentException si es null
     */
    public <T> T getServiceProviderByClass(Class<T> providerClass) {
        if (providerClass == null) {
            throw new IllegalArgumentException("providerClass == null!");
        }
        int i = 0;
        while (i < this.categories.size()) {
            Map<Class<?>, Object> map = this.categoryMap.get(this.categories.get(i));
            Iterator<Object> it = map.values().iterator();
            while (it.hasNext()) {
                Object provider = it.next();
                if (providerClass.isInstance(provider)) {
                    return providerClass.cast(provider);
                }
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * Declara que el primero va antes que el segundo. Ver la nota de la clase.
     *
     * @return si la relacion no estaba ya
     * @throws IllegalArgumentException si alguno es null o son el mismo objeto
     */
    public <T> boolean setOrdering(Class<T> category, T firstProvider, T secondProvider) {
        checkPair(firstProvider, secondProvider);
        Map<Object, Set<Object>> order = ordering(category);
        Set<Object> after = order.get(firstProvider);
        if (after == null) {
            after = new HashSet<Object>();
            order.put(firstProvider, after);
        }
        return after.add(secondProvider);
    }

    /**
     * Borra esa relacion.
     *
     * @return si estaba
     * @throws IllegalArgumentException si alguno es null o son el mismo objeto
     */
    public <T> boolean unsetOrdering(Class<T> category, T firstProvider, T secondProvider) {
        checkPair(firstProvider, secondProvider);
        Map<Object, Set<Object>> order = ordering(category);
        Set<Object> after = order.get(firstProvider);
        if (after == null) {
            return false;
        }
        return after.remove(secondProvider);
    }

    /**
     * Da de baja todos los de esa categoria.
     *
     * @throws IllegalArgumentException si la categoria no esta declarada
     */
    public void deregisterAll(Class<?> category) {
        Map<Class<?>, Object> map = map(category);
        List<Object> all = new ArrayList<Object>(map.values());
        map.clear();
        this.orderings.get(category).clear();
        int i = 0;
        while (i < all.size()) {
            deregisterFromCategory(all.get(i), category);
            i = i + 1;
        }
    }

    /** Vacia el registro entero. */
    public void deregisterAll() {
        int i = 0;
        while (i < this.categories.size()) {
            deregisterAll(this.categories.get(i));
            i = i + 1;
        }
    }

    /**
     * Vacia el registro al recolectarse.
     *
     * <p>Es publico, que para un {@code finalize} es raro, y viene asi del JDK. La finalizacion quedo
     * obsoleta y no hay que apoyarse en esto: un registro se vacia con {@link #deregisterAll}.
     */
    @Override
    public void finalize() throws Throwable {
        deregisterAll();
        super.finalize();
    }

    /** Con que quedarse de un recorrido de proveedores. */
    public interface Filter {

        /** Si ese proveedor sirve. */
        boolean filter(Object provider);
    }

    /** El mapa de esa categoria, o falla. */
    private Map<Class<?>, Object> map(Class<?> category) {
        Map<Class<?>, Object> map = this.categoryMap.get(category);
        if (map == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        return map;
    }

    /** Las relaciones de esa categoria, o falla. */
    private Map<Object, Set<Object>> ordering(Class<?> category) {
        Map<Object, Set<Object>> order = this.orderings.get(category);
        if (order == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        return order;
    }

    /** Registra sin comprobar la categoria, que ya se sabe buena. */
    private void registerUnchecked(Object provider, Class<?> category) {
        Map<Class<?>, Object> map = this.categoryMap.get(category);
        Object previous = map.put(provider.getClass(), provider);
        if (previous != null) {
            deregisterFromCategory(previous, category);
        }
        if (provider instanceof RegisterableService) {
            ((RegisterableService) provider).onRegistration(this, category);
        }
    }

    /** Da de baja sin comprobar. */
    private void deregisterUnchecked(Object provider, Class<?> category) {
        Map<Class<?>, Object> map = this.categoryMap.get(category);
        if (map.get(provider.getClass()) == provider) {
            map.remove(provider.getClass());
            deregisterFromCategory(provider, category);
        }
    }

    /** Le avisa al proveedor, si le interesa. */
    private void deregisterFromCategory(Object provider, Class<?> category) {
        if (provider instanceof RegisterableService) {
            ((RegisterableService) provider).onDeregistration(this, category);
        }
    }

    /** Que los dos existan y sean distintos. */
    private static void checkPair(Object first, Object second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("provider is null!");
        }
        if (first == second) {
            throw new IllegalArgumentException("providers are the same!");
        }
    }

    /**
     * Ordena respetando las relaciones declaradas.
     *
     * <p>Es un orden topologico por eliminacion repetida de los que no tienen a nadie delante. Los que
     * no tienen ninguna relacion salen en el orden en que se registraron, que es lo unico estable que
     * se puede prometer.
     *
     * @throws IllegalArgumentException si hay un ciclo
     */
    private static List<Object> topologicalSort(List<Object> providers,
                                                Map<Object, Set<Object>> orderings) {
        if (orderings.isEmpty()) {
            return providers;
        }
        List<Object> pending = new ArrayList<Object>(providers);
        List<Object> sorted = new ArrayList<Object>();
        while (!pending.isEmpty()) {
            Object next = null;
            int i = 0;
            while (i < pending.size() && next == null) {
                Object candidate = pending.get(i);
                if (!hasPredecessorIn(candidate, pending, orderings)) {
                    next = candidate;
                }
                i = i + 1;
            }
            if (next == null) {
                throw new IllegalArgumentException("Cycle detected in ordering!");
            }
            pending.remove(next);
            sorted.add(next);
        }
        return sorted;
    }

    /** Si alguno de los pendientes tiene que ir antes que ese. */
    private static boolean hasPredecessorIn(Object candidate, List<Object> pending,
                                            Map<Object, Set<Object>> orderings) {
        int i = 0;
        while (i < pending.size()) {
            Object other = pending.get(i);
            if (other != candidate) {
                Set<Object> after = orderings.get(other);
                if (after != null && after.contains(candidate)) {
                    return true;
                }
            }
            i = i + 1;
        }
        return false;
    }
}
