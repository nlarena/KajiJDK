package java.util;


// The skeleton for maps. In the JDK a subclass provides `entrySet()` and everything else —
// size, get, containsKey, the iteration — is derived from walking it.
//
// KajiLibrary's `Map` is the subset without the collection views, so there is no entry set to
// walk: `entrySet()` is declared (a subclass may still provide one) but the derivations that
// would need it are left to the subclass. What this class does give, and what makes it worth
// having, is the pair every map otherwise has to repeat: `isEmpty()` in terms of `size()`, and
// the mutators refusing by default so a read-only map inherits the right behaviour.
public abstract class AbstractMap<K, V> implements Map<K, V> {

    protected AbstractMap() {
    }

    public abstract Set<Map.Entry<K, V>> entrySet();

    // Las claves, derivadas de `entrySet()` — que es justamente el primitivo del que cuelga todo
    // `AbstractMap` (finding #205). Las subclases que puedan hacerlo mas barato lo sobrescriben.
    public Set<K> keySet() {
        HashSet<K> out = new HashSet<K>();
        Iterator<Map.Entry<K, V>> it = this.entrySet().iterator();
        while (it.hasNext()) {
            out.add(it.next().getKey());
        }
        return out;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        Iterator<? extends K> it = m.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            this.put(k, m.get(k));
        }
    }

    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
    // Los valores, como Collection.
    //
    // **Divergencia deliberada**, la misma que ya declara `keySet()`: la del JDK es una *vista*
    // respaldada por el mapa; esta es una copia. Y a diferencia de `keySet()`, los valores **si**
    // pueden repetirse, por eso es una Collection y no un Set.
    public Collection<V> values() {
        ArrayList<V> out = new ArrayList<V>();
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            out.add(this.get(it.next()));
        }
        return out;
    }

    /**
     * Igualdad por contenido: mismas claves, y cada una con el mismo valor.
     *
     * <p>Misma ausencia que la de AbstractList, y por la misma razon invisible: al heredar el
     * `equals` de Object, un HashMap y un LinkedHashMap con el mismo contenido daban false.
     *
     * <p>Se recorre por `keySet()` y `get()` en vez de comparar los dos entrySet como hace el
     * JDK, porque asi la igualdad no depende de que las entradas de cada implementacion tengan su
     * propio `equals` bien puesto: alcanza con que el mapa sepa buscar por clave.
     *
     * <p>El caso del valor null pide el paso extra de `containsKey`: "no esta la clave" y "esta,
     * y vale null" se ven igual desde `get`, y no son lo mismo.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map<?, ?> other = (Map<?, ?>) o;
        if (other.size() != this.size()) {
            return false;
        }
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            V v = this.get(k);
            Object w = other.get(k);
            if (v == null) {
                if (w != null || !other.containsKey(k)) {
                    return false;
                }
            } else {
                if (!v.equals(w)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * El hash que exige el contrato de Map: la SUMA de los hash de las entradas, y el de una
     * entrada es `hash(clave) ^ hash(valor)`.
     *
     * <p>Que sea una suma y no una combinacion posicional es a proposito: un mapa no tiene orden,
     * asi que la cuenta tiene que dar lo mismo recorrido como se lo recorra. Es la unica forma de
     * que un HashMap y un TreeMap iguales tengan el mismo hash.
     */
    public int hashCode() {
        int h = 0;
        Iterator<K> it = this.keySet().iterator();
        while (it.hasNext()) {
            K k = it.next();
            V v = this.get(k);
            h = h + ((k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode()));
        }
        return h;
    }

    /**
     * El mapa como {@code {clave=valor, clave=valor}}, en el orden en que lo recorre su iterador.
     *
     * <p>Sin esto, cualquier mapa que no lo defina por su cuenta --HashMap incluido-- cae en el
     * `toString` de Object y se imprime como `java.util.HashMap@3`. Es el tipo de agujero que no
     * rompe nada hasta que alguien loguea un mapa y lee una direccion en vez de sus datos.
     *
     * <p>El auto-referencia se imprime como "(this Map)" y no se recurre, que es lo que hace el
     * JDK: un mapa que se contiene a si mismo desbordaria la pila en la primera linea de log.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Iterator<K> it = this.keySet().iterator();
        boolean primero = true;
        while (it.hasNext()) {
            K k = it.next();
            V v = this.get(k);
            if (!primero) {
                sb.append(',').append(' ');
            }
            primero = false;
            // Los `Object` intermedios no son decoracion: `String.valueOf(k)` con `k` de tipo
            // variable elige la sobrecarga de `char[]` en este compilador (COMPILER_FINDINGS #341),
            // y sale una cadena vacia. Con el tipo escrito a mano, elige la de `Object`.
            Object ko = k;
            Object vo = v;
            sb.append(ko == this ? "(this Map)" : String.valueOf(ko));
            sb.append('=');
            sb.append(vo == this ? "(this Map)" : String.valueOf(vo));
        }
        sb.append('}');
        return sb.toString();
    }
}
