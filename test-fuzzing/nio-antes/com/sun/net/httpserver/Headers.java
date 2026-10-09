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
 * The headers of a request or of a response.
 *
 * <h2>The two oddities, and both are the protocol's</h2>
 *
 * <p><strong>The value is a list.</strong> HTTP allows a header to be repeated, and several
 * need it -- {@code Set-Cookie} sends one per cookie --. A {@code Map<String, String>} would
 * force them to be joined with commas, which for some headers is equivalent and for others is
 * not.
 *
 * <p><strong>The key ignores case.</strong> {@code Content-Type} and {@code content-type} are
 * the same header, so this map normalizes on keeping and on looking up. It is the reason it is
 * not a bare {@code HashMap}.
 *
 * <p>The normalization is to the {@code Xxxx-Yyyy} form, which is the conventional one. And it
 * uses an explicit {@link Locale#ENGLISH}: with Turkish, the {@code i} goes up to a dotted I
 * and {@code "if-match"} would stop matching {@code "If-Match"}. An HTTP header does not
 * depend on the language of whoever runs the server.
 */
public class Headers implements Map<String, List<String>> {

    private final Map<String, List<String>> map = new TreeMap<String, List<String>>();

    /** Empty. */
    public Headers() {
    }

    /**
     * With whatever {@code headers} brings, normalizing the keys.
     *
     * @throws NullPointerException if the map, or some key or value, is {@code null}
     */
    public Headers(Map<String, List<String>> headers) {
        if (headers == null) {
            throw new NullPointerException("headers");
        }
        putAll(headers);
    }

    /**
     * {@code content-type} and {@code CONTENT-TYPE} are both kept as {@code Content-Type}.
     *
     * <p>{@code null} passes as it is: it is an invalid key, but rejecting it here would hide the
     * mistake behind an exception less clear than the one the map throws.
     */
    private static String normalize(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        StringBuilder sb = new StringBuilder(key.length());
        boolean start = true;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (start) {
                sb.append(Character.toUpperCase(c));
                start = false;
            } else if (c == '-') {
                sb.append(c);
                start = true;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private static String asKey(Object o) {
        return o instanceof String ? normalize((String) o) : null;
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.map.containsKey(asKey(key));
    }

    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    public List<String> get(Object key) {
        return this.map.get(asKey(key));
    }

    /**
     * The first value, or {@code null} if there is none.
     *
     * <p>It is the access that is used almost always: most headers appear only once, and asking
     * for the list in order to take element zero out of it is noise.
     */
    public String getFirst(String key) {
        List<String> l = this.map.get(normalize(key));
        return l == null || l.isEmpty() ? null : l.get(0);
    }

    public List<String> put(String key, List<String> value) {
        return this.map.put(normalize(key), value);
    }

    /** It adds one more value, without overwriting those that were already there. */
    public void add(String key, String value) {
        String k = normalize(key);
        List<String> l = this.map.get(k);
        if (l == null) {
            l = new LinkedList<String>();
            this.map.put(k, l);
        }
        l.add(value);
    }

    /** It leaves this header with that single value, overwriting what was there. */
    public void set(String key, String value) {
        List<String> l = new LinkedList<String>();
        l.add(value);
        put(key, l);
    }

    public List<String> remove(Object key) {
        return this.map.remove(asKey(key));
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
     * From loose name/value pairs.
     *
     * @throws IllegalArgumentException if the number is odd -- half a pair is not a header
     * @throws NullPointerException if some element is {@code null}
     */
    public static Headers of(String... pairs) {
        if (pairs == null) {
            throw new NullPointerException("pairs");
        }
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("name/value pairs are needed");
        }
        Headers h = new Headers();
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i] == null || pairs[i + 1] == null) {
                throw new NullPointerException("neither the name nor the value may be null");
            }
            h.add(pairs[i], pairs[i + 1]);
        }
        return h;
    }

    /** From a map, copying the lists so that they do not end up shared. */
    public static Headers of(Map<String, List<String>> headers) {
        if (headers == null) {
            throw new NullPointerException("headers");
        }
        Headers h = new Headers();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                throw new NullPointerException("neither the key nor the value may be null");
            }
            h.put(e.getKey(), new ArrayList<String>(e.getValue()));
        }
        return h;
    }
}
