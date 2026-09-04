package com.sun.net.httpserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

/**
 * Los encabezados de un pedido o una respuesta.
 *
 * <h2>Las dos rarezas, y las dos son del protocolo</h2>
 *
 * <p><strong>El valor es una lista.</strong> HTTP permite repetir un encabezado, y varios lo
 * necesitan —{@code Set-Cookie} manda uno por galleta—. Un {@code Map<String, String>} obligaria a
 * pegarlos con comas, que para algunos encabezados es equivalente y para otros no.
 *
 * <p><strong>La clave no distingue mayusculas.</strong> {@code Content-Type} y
 * {@code content-type} son el mismo encabezado, asi que este mapa normaliza al guardar y al buscar.
 * Es la razon de que no sea un {@code HashMap} pelado.
 *
 * <p>La normalizacion es a la forma {@code Xxxx-Yyyy}, que es la convencional. Y usa
 * {@link Locale#ENGLISH} explicito: con el turco, la {@code i} sube a una I con punto y
 * {@code "if-match"} dejaria de coincidir con {@code "If-Match"}. Un encabezado HTTP no depende del
 * idioma de quien corre el servidor.
 */
public class Headers implements Map<String, List<String>> {

    private final Map<String, List<String>> map = new TreeMap<String, List<String>>();

    /** Vacios. */
    public Headers() {
    }

    /**
     * Con lo que traiga {@code headers}, normalizando las claves.
     *
     * @throws NullPointerException si el mapa, o alguna clave o valor, es {@code null}
     */
    public Headers(Map<String, List<String>> headers) {
        if (headers == null) {
            throw new NullPointerException("headers");
        }
        putAll(headers);
    }

    /**
     * {@code content-type} y {@code CONTENT-TYPE} se guardan los dos como {@code Content-Type}.
     *
     * <p>{@code null} pasa tal cual: es una clave invalida, pero rechazarla aca escondería el error
     * detras de una excepcion menos clara que la que tira el mapa.
     */
    private static String normalizar(String clave) {
        if (clave == null || clave.isEmpty()) {
            return clave;
        }
        StringBuilder sb = new StringBuilder(clave.length());
        boolean arranque = true;
        for (int i = 0; i < clave.length(); i++) {
            char c = clave.charAt(i);
            if (arranque) {
                sb.append(Character.toUpperCase(c));
                arranque = false;
            } else if (c == '-') {
                sb.append(c);
                arranque = true;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private static String comoClave(Object o) {
        return o instanceof String ? normalizar((String) o) : null;
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.map.containsKey(comoClave(key));
    }

    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    public List<String> get(Object key) {
        return this.map.get(comoClave(key));
    }

    /**
     * El primer valor, o {@code null} si no hay.
     *
     * <p>Es el acceso que se usa casi siempre: la mayoria de los encabezados aparecen una sola vez, y
     * pedir la lista para sacarle el elemento cero es ruido.
     */
    public String getFirst(String key) {
        List<String> l = this.map.get(normalizar(key));
        return l == null || l.isEmpty() ? null : l.get(0);
    }

    public List<String> put(String key, List<String> value) {
        return this.map.put(normalizar(key), value);
    }

    /** Agrega un valor mas, sin pisar los que ya estaban. */
    public void add(String key, String value) {
        String k = normalizar(key);
        List<String> l = this.map.get(k);
        if (l == null) {
            l = new LinkedList<String>();
            this.map.put(k, l);
        }
        l.add(value);
    }

    /** Deja este encabezado con ese unico valor, pisando lo anterior. */
    public void set(String key, String value) {
        List<String> l = new LinkedList<String>();
        l.add(value);
        put(key, l);
    }

    public List<String> remove(Object key) {
        return this.map.remove(comoClave(key));
    }

    public void putAll(Map<? extends String, ? extends List<String>> t) {
        for (Map.Entry<? extends String, ? extends List<String>> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        this.map.clear();
    }

    public Set<String> keySet() {
        return this.map.keySet();
    }

    public Collection<List<String>> values() {
        return this.map.values();
    }

    public Set<Map.Entry<String, List<String>>> entrySet() {
        return this.map.entrySet();
    }

    public void replaceAll(
            BiFunction<? super String, ? super List<String>, ? extends List<String>> function) {
        this.map.replaceAll(function);
    }

    public boolean equals(Object o) {
        return this.map.equals(o);
    }

    public int hashCode() {
        return this.map.hashCode();
    }

    public String toString() {
        return this.map.toString();
    }

    /**
     * Desde pares nombre/valor sueltos.
     *
     * @throws IllegalArgumentException si la cantidad es impar — un par a medias no es un encabezado
     * @throws NullPointerException si algun elemento es {@code null}
     */
    public static Headers of(String... pairs) {
        if (pairs == null) {
            throw new NullPointerException("pairs");
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("hacen falta pares nombre/valor");
        }
        Headers h = new Headers();
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i] == null || pairs[i + 1] == null) {
                throw new NullPointerException("ni el nombre ni el valor pueden ser null");
            }
            h.add(pairs[i], pairs[i + 1]);
        }
        return h;
    }

    /** Desde un mapa, copiando las listas para que no queden compartidas. */
    public static Headers of(Map<String, List<String>> headers) {
        if (headers == null) {
            throw new NullPointerException("headers");
        }
        Headers h = new Headers();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                throw new NullPointerException("ni la clave ni el valor pueden ser null");
            }
            h.put(e.getKey(), new ArrayList<String>(e.getValue()));
        }
        return h;
    }
}
