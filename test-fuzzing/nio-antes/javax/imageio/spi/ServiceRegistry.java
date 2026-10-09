package javax.imageio.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KajiLibrary's javax.imageio.spi.ServiceRegistry -- a registry of providers, by category.
 *
 * <p>It is a {@link java.util.ServiceLoader} with two things that one lacks, and both matter:
 *
 * <ul>
 *   <li><b>it can be modified at run time</b>: registering and deregistering providers while the
 *       program runs;
 *   <li><b>it has a partial order</b>: {@link #setOrdering} declares that one provider goes before
 *       another, and {@link #getServiceProviders} with {@code useOrdering} respects that relation.
 * </ul>
 *
 * <h2>The order is partial, not total</h2>
 *
 * <p>It is the part that gets misread. You do not declare a position but <b>pairs</b>: "A before
 * B". Providers with no relation between them come out in any order.
 *
 * <p>It serves exactly what is needed --a specialized TIFF reader winning over the generic one--
 * without forcing anyone to invent numeric priorities.
 *
 * <p>A cycle in the relations throws {@link IllegalArgumentException} when the ordered traversal is
 * asked for. The JDK does not throw: its iterator simply leaves out the providers caught in the
 * cycle.
 *
 * <h2>One provider per class and per category</h2>
 *
 * <p>{@link #registerServiceProvider} replaces whatever there was of the <b>same class</b> in that
 * category. It is not equality by {@code equals}: it is the class. Two different instances of the
 * same provider do not coexist, and that prevents loading the same plug-in twice from duplicating
 * it.
 */
public class ServiceRegistry {

    /** The providers of each category, by class. */
    private final Map<Class<?>, Map<Class<?>, Object>> categoryMap =
        new HashMap<Class<?>, Map<Class<?>, Object>>();

    /** The categories, in the order they were declared. */
    private final List<Class<?>> categories = new ArrayList<Class<?>>();

    /** The "goes before" relations, per category. */
    private final Map<Class<?>, Map<Object, Set<Object>>> orderings =
        new HashMap<Class<?>, Map<Object, Set<Object>>>();

    /**
     * @param categories the categories it will handle
     * @throws IllegalArgumentException if it is null
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
     * The providers of that type declared as services.
     *
     * <p>It is {@link java.util.ServiceLoader} and nothing more; it does not touch this registry.
     */
    public static <T> Iterator<T> lookupProviders(Class<T> providerClass, ClassLoader loader) {
        return java.util.ServiceLoader.load(providerClass, loader).iterator();
    }

    /** Same, with the context class loader. */
    public static <T> Iterator<T> lookupProviders(Class<T> providerClass) {
        return java.util.ServiceLoader.load(providerClass).iterator();
    }

    /** The categories it handles. */
    public Iterator<Class<?>> getCategories() {
        return new ArrayList<Class<?>>(this.categories).iterator();
    }

    /**
     * Registers a provider in that category.
     *
     * <p>See the class note: it replaces the one of the same class if there was one, and in that
     * case the replaced one is notified with {@code onDeregistration}.
     *
     * @return whether there was none of that class
     * @throws IllegalArgumentException if the provider is null or the category is not declared
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
     * Same, in <b>all</b> the categories it belongs to.
     *
     * @throws IllegalArgumentException if it is null
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
     * Registers several.
     *
     * @throws IllegalArgumentException if the iterator is null or yields a null
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
     * Deregisters it from that category.
     *
     * @return whether it was there
     * @throws IllegalArgumentException if it is null or the category is not declared
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
     * Deregisters it from all of them.
     *
     * @throws IllegalArgumentException if it is null
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

    /** Whether that provider is registered in some category. By identity, not by {@code equals}. */
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
     * The providers of that category.
     *
     * @param useOrdering whether to respect the relations of {@link #setOrdering}
     * @throws IllegalArgumentException if the category is not declared, or if the order has a cycle
     *     (the JDK does not throw for a cycle; see the class note)
     */
    public <T> Iterator<T> getServiceProviders(Class<T> category, boolean useOrdering) {
        return getServiceProviders(category, null, useOrdering);
    }

    /**
     * Same, keeping only those that pass the filter.
     *
     * @throws IllegalArgumentException if the category is not declared, or if the order has a cycle
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
     * The provider of that exact class, or null.
     *
     * <p>It is by <b>class</b> and not by category: it serves to find a specific provider whose
     * name is known, which is how {@code ImageReaderSpi.getImageWriterSpiNames} is resolved.
     *
     * @throws IllegalArgumentException if it is null
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
     * Declares that the first goes before the second. See the class note.
     *
     * @return whether the relation was not there already
     * @throws IllegalArgumentException if either is null or they are the same object
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
     * Removes that relation.
     *
     * @return whether it was there
     * @throws IllegalArgumentException if either is null or they are the same object
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
     * Deregisters all the ones of that category.
     *
     * @throws IllegalArgumentException if the category is not declared
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

    /** Empties the whole registry. */
    public void deregisterAll() {
        int i = 0;
        while (i < this.categories.size()) {
            deregisterAll(this.categories.get(i));
            i = i + 1;
        }
    }

    /**
     * Empties the registry when collected.
     *
     * <p>It is public, which is odd for a {@code finalize}, and comes that way from the JDK, where
     * it is {@code @Deprecated(since="9", forRemoval=true)} (not marked here). Finalization is
     * obsolete and nothing should rely on this: a registry is emptied with {@link #deregisterAll}.
     */
    @Override
    public void finalize() throws Throwable {
        deregisterAll();
        super.finalize();
    }

    /** What to keep from a traversal of providers. */
    public interface Filter {

        /** Whether that provider will do. */
        boolean filter(Object provider);
    }

    /** The map of that category, or fails. */
    private Map<Class<?>, Object> map(Class<?> category) {
        Map<Class<?>, Object> map = this.categoryMap.get(category);
        if (map == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        return map;
    }

    /** The relations of that category, or fails. */
    private Map<Object, Set<Object>> ordering(Class<?> category) {
        Map<Object, Set<Object>> order = this.orderings.get(category);
        if (order == null) {
            throw new IllegalArgumentException("category unknown!");
        }
        return order;
    }

    /** Registers without checking the category, which is already known to be good. */
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

    /** Deregisters without checking. */
    private void deregisterUnchecked(Object provider, Class<?> category) {
        Map<Class<?>, Object> map = this.categoryMap.get(category);
        if (map.get(provider.getClass()) == provider) {
            map.remove(provider.getClass());
            deregisterFromCategory(provider, category);
        }
    }

    /** Notifies the provider, if it cares. */
    private void deregisterFromCategory(Object provider, Class<?> category) {
        if (provider instanceof RegisterableService) {
            ((RegisterableService) provider).onDeregistration(this, category);
        }
    }

    /** That both exist and are different. */
    private static void checkPair(Object first, Object second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("provider is null!");
        }
        if (first == second) {
            throw new IllegalArgumentException("providers are the same!");
        }
    }

    /**
     * Sorts respecting the declared relations.
     *
     * <p>It is a topological sort by repeatedly removing the ones with nobody in front. The ones
     * with no relation come out in the order they were registered, which is the only stable thing
     * that can be promised.
     *
     * @throws IllegalArgumentException if there is a cycle
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

    /** Whether any of the pending ones has to go before that one. */
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
