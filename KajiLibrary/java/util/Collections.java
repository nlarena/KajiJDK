package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.List;
import java.util.Comparator;
import java.util.random.RandomGenerator;

// java.util.Collections: los algoritmos y las fabricas que operan sobre las interfaces de
// coleccion, sin pertenecer a ninguna implementacion. No se instancia.
//
// La clase se lee en cuatro partes:
//
//   1. **Vacias y de un solo elemento** -- emptyList, singleton, nCopies y compania. Colecciones
//      inmutables construidas de una vez, mucho mas baratas que un HashSet de un elemento.
//   2. **Envoltorios** -- unmodifiableX, synchronizedX, checkedX. Los tres devuelven una VISTA de
//      la coleccion de atras, no una copia: lo que cambie por abajo se ve por arriba. La
//      maquinaria esta en GuardedCollection y GuardedMap.
//   3. **Algoritmos** -- sort, binarySearch, min/max, shuffle, rotate, frequency, disjoint.
//   4. **Puentes** -- enumeration/list entre Enumeration e Iterator, asLifoQueue, newSetFromMap.
//
// Sobre las vistas hay una trampa que conviene tener presente: `unmodifiable*` promete que NADIE
// puede modificar **a traves de la vista**, no que la coleccion sea inmutable. Quien conserve la
// referencia original sigue pudiendo escribir, y la vista lo refleja. Para inmutabilidad de
// verdad estan `List.of`, `Set.of` y `Map.of`, que copian.
public final class Collections {

    private Collections() {}

    // ---- vacias y de un solo elemento -----------------------------------------------------------
    //
    // Las tres constantes son crudas -- `List`, no `List<T>` -- porque son anteriores a los
    // genericos y tiparlas romperia el codigo que las usa. Las fabricas emptyList/emptySet/
    // emptyMap son la forma tipada de exactamente lo mismo, y son las que hay que usar.

    public static final List EMPTY_LIST = new FixedList(new Object[0]);

    public static final Set EMPTY_SET = FixedSet.fromArray(new Object[0], 0);

    public static final Map EMPTY_MAP = FixedMap.fromPairs(new Object[0], 0);

    public static final <T> List<T> emptyList() {
        return new FixedList<T>(new Object[0]);
    }

    public static final <T> Set<T> emptySet() {
        return FixedSet.fromArray(new Object[0], 0);
    }

    public static final <K, V> Map<K, V> emptyMap() {
        return FixedMap.fromPairs(new Object[0], 0);
    }

    public static <E> SortedSet<E> emptySortedSet() {
        return new EmptySortedSet<E>();
    }

    public static <E> NavigableSet<E> emptyNavigableSet() {
        return new EmptySortedSet<E>();
    }

    public static final <K, V> SortedMap<K, V> emptySortedMap() {
        return new EmptySortedMap<K, V>();
    }

    public static final <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return new EmptySortedMap<K, V>();
    }

    public static <T> Iterator<T> emptyIterator() {
        return new FixedListItr<T>(new Object[0]);
    }

    public static <T> ListIterator<T> emptyListIterator() {
        return new AbstractListLitr<T>(new FixedList<T>(new Object[0]), 0);
    }

    public static <T> Enumeration<T> emptyEnumeration() {
        return new EmptyEnumeration<T>();
    }

    // Un conjunto inmutable de un solo elemento. Existe porque sale mucho mas barato que un
    // HashSet de uno: sin tabla, sin hash, sin factor de carga. Lo mismo vale para las dos que
    // siguen.
    public static <T> Set<T> singleton(T o) {
        Object[] a = new Object[1];
        a[0] = o;
        return FixedSet.fromArray(a, 1);
    }

    public static <T> List<T> singletonList(T o) {
        Object[] a = new Object[1];
        a[0] = o;
        return new FixedList<T>(a);
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        Object[] kv = new Object[2];
        kv[0] = key;
        kv[1] = value;
        return FixedMap.fromPairs(kv, 2);
    }

    // Una lista inmutable con `o` repetido `n` veces.
    //
    // **Divergencia deliberada**: la del JDK guarda el elemento UNA vez y finge el largo, asi que
    // `nCopies(1000000, x)` no ocupa un millon de referencias. Esta materializa el arreglo. Es
    // correcta y mas cara; el dia que alguien la use con un `n` grande, hay que cambiarla.
    public static <T> List<T> nCopies(int n, T o) {
        if (n < 0) {
            throw new IllegalArgumentException("List length = " + n);
        }
        Object[] a = new Object[n];
        int i = 0;
        while (i < n) {
            a[i] = o;
            i = i + 1;
        }
        return new FixedList<T>(a);
    }

    // ---- envoltorios de solo lectura ------------------------------------------------------------
    //
    // Los tres argumentos de los Guarded* son, en orden: la coleccion de atras, la clase que se le
    // exige a cada elemento que entra (null = no se valida) y si es de solo lectura.

    public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> c) {
        return new GuardedCollection<T>((Collection<T>) c, null, true);
    }

    public static <T> SequencedCollection<T> unmodifiableSequencedCollection(
            SequencedCollection<? extends T> c) {
        return new GuardedSequencedCollection<T>((SequencedCollection<T>) c, null, true);
    }

    public static <T> List<T> unmodifiableList(List<? extends T> list) {
        return new GuardedList<T>((List<T>) list, null, true);
    }

    public static <T> Set<T> unmodifiableSet(Set<? extends T> s) {
        return new GuardedSet<T>((Set<T>) s, null, true);
    }

    public static <T> SequencedSet<T> unmodifiableSequencedSet(SequencedSet<? extends T> s) {
        return new GuardedSequencedSet<T>((SequencedSet<T>) s, null, true);
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> s) {
        return new GuardedSortedSet<T>(s, null, true);
    }

    public static <T> NavigableSet<T> unmodifiableNavigableSet(NavigableSet<T> s) {
        return new GuardedNavigableSet<T>(s, null, true);
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> m) {
        return new GuardedMap<K, V>((Map<K, V>) m, null, null, true);
    }

    public static <K, V> SequencedMap<K, V> unmodifiableSequencedMap(
            SequencedMap<? extends K, ? extends V> m) {
        return new GuardedSequencedMap<K, V>((SequencedMap<K, V>) m, null, null, true);
    }

    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> m) {
        return new GuardedSortedMap<K, V>((SortedMap<K, V>) m, null, null, true);
    }

    public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, ? extends V> m) {
        return new GuardedNavigableMap<K, V>((NavigableMap<K, V>) m, null, null, true);
    }

    // ---- envoltorios sincronizados ---------------------------------------------------------------
    //
    // Cada operacion suelta queda protegida, pero **una secuencia de operaciones no**. El caso
    // clasico es la recorrida: entre el `hasNext()` y el `next()` puede meterse otro hilo. Por eso
    // el JDK documenta que el usuario tiene que tomar el monitor de la coleccion devuelta el mismo:
    //
    //     List<X> l = Collections.synchronizedList(new ArrayList<X>());
    //     synchronized (l) { for (X x : l) { ... } }
    //
    // Que sea el monitor de la coleccion devuelta, y no otro, es parte del contrato: los
    // envoltorios de aca sincronizan sobre si mismos justamente para que esa linea funcione.

    public static <T> Collection<T> synchronizedCollection(Collection<T> c) {
        return new GuardedCollection<T>(c, null, false);
    }

    public static <T> List<T> synchronizedList(List<T> list) {
        return new GuardedList<T>(list, null, false);
    }

    public static <T> Set<T> synchronizedSet(Set<T> s) {
        return new GuardedSet<T>(s, null, false);
    }

    public static <T> SortedSet<T> synchronizedSortedSet(SortedSet<T> s) {
        return new GuardedSortedSet<T>(s, null, false);
    }

    public static <T> NavigableSet<T> synchronizedNavigableSet(NavigableSet<T> s) {
        return new GuardedNavigableSet<T>(s, null, false);
    }

    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> m) {
        return new GuardedMap<K, V>(m, null, null, false);
    }

    public static <K, V> SortedMap<K, V> synchronizedSortedMap(SortedMap<K, V> m) {
        return new GuardedSortedMap<K, V>(m, null, null, false);
    }

    public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(NavigableMap<K, V> m) {
        return new GuardedNavigableMap<K, V>(m, null, null, false);
    }

    // ---- envoltorios con chequeo de tipo ---------------------------------------------------------
    //
    // Tapan el agujero que dejan los genericos borrados: un `List<String>` pasado como `List`
    // cruda acepta un Integer sin chistar, y la ClassCastException salta mucho despues, en el
    // `get`, lejos de quien la causo. `checkedList` la adelanta al `add`.
    //
    // Es una herramienta de diagnostico: se envuelve mientras se busca quien ensucia la coleccion,
    // y despues se saca.

    public static <E> Collection<E> checkedCollection(Collection<E> c, Class<E> type) {
        return new GuardedCollection<E>(c, type, false);
    }

    public static <E> List<E> checkedList(List<E> list, Class<E> type) {
        return new GuardedList<E>(list, type, false);
    }

    public static <E> Set<E> checkedSet(Set<E> s, Class<E> type) {
        return new GuardedSet<E>(s, type, false);
    }

    public static <E> SortedSet<E> checkedSortedSet(SortedSet<E> s, Class<E> type) {
        return new GuardedSortedSet<E>(s, type, false);
    }

    public static <E> NavigableSet<E> checkedNavigableSet(NavigableSet<E> s, Class<E> type) {
        return new GuardedNavigableSet<E>(s, type, false);
    }

    public static <E> Queue<E> checkedQueue(Queue<E> queue, Class<E> type) {
        return new GuardedQueue<E>(queue, type, false);
    }

    public static <K, V> Map<K, V> checkedMap(Map<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new GuardedMap<K, V>(m, keyType, valueType, false);
    }

    public static <K, V> SortedMap<K, V> checkedSortedMap(SortedMap<K, V> m, Class<K> keyType,
            Class<V> valueType) {
        return new GuardedSortedMap<K, V>(m, keyType, valueType, false);
    }

    public static <K, V> NavigableMap<K, V> checkedNavigableMap(NavigableMap<K, V> m,
            Class<K> keyType, Class<V> valueType) {
        return new GuardedNavigableMap<K, V>(m, keyType, valueType, false);
    }

    // ---- algoritmos -------------------------------------------------------------------------------
    //
    // Los metodos que reciben `List<?>` empiezan casi todos con la misma linea: una vista tipada
    // `List<Object>` de la misma lista. Es que `List<?>` no deja escribir NADA -- ni siquiera lo
    // que se acaba de sacar de ella, porque el compilador no puede probar que sean el mismo tipo
    // capturado. La conversion es segura: solo se reacomodan elementos que ya estaban adentro.

    // Swap the elements at positions `i` and `j`.
    public static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // Reverse the order of the elements in place.
    public static <T> void reverse(List<T> list) {
        int size = list.size();
        for (int i = 0; i < size / 2; i++) {
            Collections.swap(list, i, size - 1 - i);
        }
    }

    // Replace every element with `obj`.
    public static <T> void fill(List<T> list, T obj) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            list.set(i, obj);
        }
    }

    // Sort ascending by a Comparator (stable-ish insertion sort over indexed access).
    public static <T> void sort(List<T> list, Comparator<? super T> c) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            T key = list.get(i);
            int j = i - 1;
            while (j >= 0 && c.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));
                j = j - 1;
            }
            list.set(j + 1, key);
        }
    }

    // Sort ascending by natural order (elements must be Comparable to each other).
    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            T key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).compareTo(key) > 0) {
                list.set(j + 1, list.get(j));
                j = j - 1;
            }
            list.set(j + 1, key);
        }
    }

    // El indice de `key` en una lista **ya ordenada**, o `-(donde_iria) - 1` si no esta.
    //
    // Que la lista tenga que venir ordenada no es un detalle de la documentacion: sobre una
    // desordenada no da error, da un resultado sin sentido. Es el precio de bajar de O(n) a
    // O(log n) -- se puede descartar la mitad en cada paso justamente porque se confia en el
    // orden.
    //
    // El negativo codifica el punto de insercion en vez de ser un simple -1, y ese detalle es lo
    // que hace util al metodo para mantener una lista ordenada: si no esta, ya se sabe donde va.
    public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
        return search((List<Object>) list, key, null);
    }

    public static <T> int binarySearch(List<? extends T> list, T key, Comparator<? super T> c) {
        return search((List<Object>) list, key, (Comparator<Object>) c);
    }

    private static int search(List<Object> list, Object key, Comparator<Object> c) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            // `>>> 1` y no `/ 2`: con listas grandes `low + high` puede desbordar, y el
            // desplazamiento sin signo devuelve el promedio correcto igual.
            int mid = (low + high) >>> 1;
            Object at = list.get(mid);
            int cmp;
            if (c != null) {
                cmp = c.compare(at, key);
            } else {
                cmp = ((Comparable<Object>) at).compareTo(key);
            }
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    // Baraja la lista: recorre de atras para adelante intercambiando cada posicion con una al azar
    // entre las que quedan (Fisher-Yates). Es el unico barajado que da las n! permutaciones con la
    // misma probabilidad; el ingenuo -- intercambiar cada posicion con una cualquiera de toda la
    // lista -- sesga el resultado y no se nota mirandolo.
    public static void shuffle(List<?> list) {
        shuffle(list, new Random());
    }

    public static void shuffle(List<?> list, Random rnd) {
        shuffle(list, (RandomGenerator) rnd);
    }

    public static void shuffle(List<?> list, RandomGenerator rnd) {
        List<Object> l = (List<Object>) list;
        int i = l.size() - 1;
        while (i > 0) {
            swap(l, i, rnd.nextInt(i + 1));
            i = i - 1;
        }
    }

    // Copia `src` sobre el principio de `dest`, que tiene que tener lugar suficiente. No agranda
    // la destino: `copy` sobreescribe, no inserta.
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        int n = src.size();
        if (dest.size() < n) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }
        List<Object> d = (List<Object>) dest;
        int i = 0;
        while (i < n) {
            d.set(i, src.get(i));
            i = i + 1;
        }
    }

    public static <T extends Comparable<? super T>> T min(Collection<? extends T> coll) {
        return (T) extreme(coll, null, true);
    }

    public static <T> T min(Collection<? extends T> coll, Comparator<? super T> comp) {
        return (T) extreme(coll, (Comparator<Object>) comp, true);
    }

    public static <T extends Comparable<? super T>> T max(Collection<? extends T> coll) {
        return (T) extreme(coll, null, false);
    }

    public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
        return (T) extreme(coll, (Comparator<Object>) comp, false);
    }

    // El comun de min y max. Recorre con un iterador y no por indice porque una Collection no
    // promete acceso indexado -- un HashSet no tiene un "elemento 3".
    //
    // Se guarda el `>`/`<` estricto a proposito: ante empate gana el primero que aparecio, que es
    // lo que documenta el JDK.
    private static Object extreme(Collection<?> coll, Comparator<Object> comp, boolean wantMin) {
        Iterator<?> it = coll.iterator();
        // Sobre una coleccion vacia esto tira NoSuchElementException, que es lo especificado.
        Object best = it.next();
        while (it.hasNext()) {
            Object next = it.next();
            int cmp;
            if (comp != null) {
                cmp = comp.compare(next, best);
            } else {
                cmp = ((Comparable<Object>) next).compareTo(best);
            }
            if (wantMin) {
                if (cmp < 0) {
                    best = next;
                }
            } else {
                if (cmp > 0) {
                    best = next;
                }
            }
        }
        return best;
    }

    // Corre los elementos `distance` lugares hacia el final, dando la vuelta.
    //
    // Se hace con tres inversiones -- toda la lista, despues cada una de las dos partes -- que es
    // el truco que resuelve la rotacion en O(n) y sin memoria extra. Rotar de a un lugar `distance`
    // veces seria O(n * distance).
    public static void rotate(List<?> list, int distance) {
        int n = list.size();
        if (n == 0) {
            return;
        }
        // floorMod y no `%`: en Java el resto conserva el signo del dividendo, asi que
        // `-1 % 5` es -1 y no 4, y una distancia negativa daria indices fuera de rango.
        int d = Math.floorMod(distance, n);
        if (d == 0) {
            return;
        }
        List<Object> l = (List<Object>) list;
        reverse(l);
        reverse(l.subList(0, d));
        reverse(l.subList(d, n));
    }

    public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) {
        boolean changed = false;
        int n = list.size();
        int i = 0;
        while (i < n) {
            if (Objects.equals(list.get(i), oldVal)) {
                list.set(i, newVal);
                changed = true;
            }
            i = i + 1;
        }
        return changed;
    }

    // El primer indice donde `target` aparece entero dentro de `source`, o -1.
    //
    // Es la busqueda ingenua, O(n * m): se prueba cada posicion. Alcanza para lo que se usa esto
    // -- listas cortas -- y evita el preproceso de un Knuth-Morris-Pratt, que sobre listas
    // genericas ademas obligaria a comparar con equals mas veces de las que ahorra.
    public static int indexOfSubList(List<?> source, List<?> target) {
        int n = source.size();
        int m = target.size();
        int i = 0;
        while (i + m <= n) {
            if (matchesAt(source, target, i)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public static int lastIndexOfSubList(List<?> source, List<?> target) {
        int n = source.size();
        int m = target.size();
        int i = n - m;
        while (i >= 0) {
            if (matchesAt(source, target, i)) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    private static boolean matchesAt(List<?> source, List<?> target, int at) {
        int m = target.size();
        int k = 0;
        while (k < m) {
            if (!Objects.equals(source.get(at + k), target.get(k))) {
                return false;
            }
            k = k + 1;
        }
        return true;
    }

    // Cuantas veces aparece `o`. Cuenta por equals, salvo que `o` sea null, y ahi cuenta los null.
    public static int frequency(Collection<?> c, Object o) {
        int n = 0;
        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            if (Objects.equals(it.next(), o)) {
                n = n + 1;
            }
        }
        return n;
    }

    // Si las dos colecciones no comparten ningun elemento.
    //
    // Se recorre `c1` preguntandole a `c2`, que es lo contrario de lo que parece natural: conviene
    // que la preguntada sea la de `contains` rapido (un Set) y la recorrida la otra. Quien llame
    // con dos ArrayList grandes va a pagar O(n * m), y no hay forma de evitarlo sin copiar una a
    // un conjunto -- cosa que el JDK tampoco hace.
    public static boolean disjoint(Collection<?> c1, Collection<?> c2) {
        Iterator<?> it = c1.iterator();
        while (it.hasNext()) {
            if (c2.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    // Agrega todos los elementos sueltos a la coleccion. Es la version comoda de un `addAll` con
    // una lista intermedia, y no es equivalente a `c.addAll(...)`: si uno de los `add` falla a
    // mitad de camino, los anteriores quedan.
    public static <T> boolean addAll(Collection<? super T> c, T... elements) {
        boolean changed = false;
        int i = 0;
        while (i < elements.length) {
            if (c.add(elements[i])) {
                changed = true;
            }
            i = i + 1;
        }
        return changed;
    }

    // El comparador que invierte el orden natural.
    public static <T> Comparator<T> reverseOrder() {
        return new ReverseComparator<T>(null);
    }

    // El que invierte otro comparador. Con `null` vuelve al orden natural invertido, que es lo que
    // hace util a esta forma: permite pasar "el orden de siempre, al reves" sin caso especial.
    public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
        return new ReverseComparator<T>(cmp);
    }

    // ---- puentes entre las APIs viejas y las nuevas ------------------------------------------------

    // Una Enumeration sobre la coleccion. El puente hacia lo anterior a `Iterator`, que todavia
    // pide Enumeration: Properties, ZipFile, ServletRequest.
    public static <T> Enumeration<T> enumeration(Collection<T> c) {
        return new ArrayEnumeration<T>(c.toArray());
    }

    // El puente en la otra direccion: vacia una Enumeration en una lista.
    public static <T> ArrayList<T> list(Enumeration<T> e) {
        ArrayList<T> out = new ArrayList<T>();
        while (e.hasMoreElements()) {
            out.add(e.nextElement());
        }
        return out;
    }

    // Un Set respaldado por el Map dado, que tiene que llegar vacio.
    //
    // Existe para un caso concreto: no hay IdentityHashSet ni ConcurrentHashSet en el JDK, y esta
    // es la forma de armarlos -- `newSetFromMap(new IdentityHashMap())` da un conjunto que compara
    // por identidad en vez de por equals.
    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        return new SetFromMap<E>(map);
    }

    public static <E> SequencedSet<E> newSequencedSetFromMap(SequencedMap<E, Boolean> map) {
        return new SequencedSetFromMap<E>(map);
    }

    // Una vista Queue del deque que saca por donde mete: una pila con cara de cola.
    public static <T> Queue<T> asLifoQueue(Deque<T> deque) {
        return new LifoQueue<T>(deque);
    }
}
