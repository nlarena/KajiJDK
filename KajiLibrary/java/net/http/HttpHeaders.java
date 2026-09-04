package java.net.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.function.BiPredicate;

/**
 * Los encabezados de un pedido o una respuesta, <strong>inmutables</strong>.
 *
 * <h2>En que se diferencia de {@code com.sun.net.httpserver.Headers}</h2>
 *
 * <p>Aquella implementa {@link Map} y se muta; esta no implementa nada y no se puede cambiar. La
 * diferencia no es de gusto: un {@link HttpResponse} se puede compartir entre hilos —el cliente es
 * asincronico— y unos encabezados mutables serian estado compartido sin sincronizar.
 *
 * <p>Por eso {@link #map} devuelve un mapa inmodificable y las listas de adentro tambien lo son:
 * hacerlo a medias dejaria una puerta abierta que nadie esperaria encontrar.
 *
 * <h2>El filtro de {@link #of}</h2>
 *
 * <p>La unica fabrica recibe un predicado que decide que encabezados entran. Existe porque los
 * encabezados vienen de la red: los del salto anterior, los que un proxy agrego, los que no
 * corresponde reenviar. Filtrar al construir es lo que evita que alguien tenga que acordarse de
 * hacerlo despues.
 *
 * <p>Las claves no distinguen mayusculas, como manda HTTP, pero se <strong>conserva</strong> como
 * venian escritas: {@link #map} las devuelve tal cual llegaron.
 *
 * @since 11
 */
public final class HttpHeaders {

    private final Map<String, List<String>> headers;

    private HttpHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    /** El primer valor de ese encabezado, si esta. */
    public Optional<String> firstValue(String name) {
        List<String> l = this.headers.get(name);
        return l == null || l.isEmpty() ? Optional.<String>empty() : Optional.of(l.get(0));
    }

    /**
     * El primer valor como {@code long}, si esta y es un numero.
     *
     * <p>Un {@link OptionalLong} y no un {@code Optional<Long>}: es el acceso para
     * {@code Content-Length}, que se mira en cada respuesta, y evita crear un objeto por consulta.
     *
     * @throws NumberFormatException si esta pero no es un numero — un encabezado mal formado es un
     *     error del otro lado, no un "no esta"
     */
    public OptionalLong firstValueAsLong(String name) {
        List<String> l = this.headers.get(name);
        if (l == null || l.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(l.get(0)));
    }

    /** Todos los valores de ese encabezado; vacia si no esta. */
    public List<String> allValues(String name) {
        List<String> l = this.headers.get(name);
        return l == null ? Collections.<String>emptyList() : l;
    }

    /** Todos los encabezados, inmodificables. */
    public Map<String, List<String>> map() {
        return this.headers;
    }

    /** Sobre el mapa entero, sin distinguir mayusculas en las claves. */
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HttpHeaders)) {
            return false;
        }
        return this.headers.equals(((HttpHeaders) obj).headers);
    }

    public final int hashCode() {
        // Sobre las claves en minuscula, para ser coherente con un `equals` que no las distingue.
        int h = 0;
        for (Map.Entry<String, List<String>> e : this.headers.entrySet()) {
            h = h + e.getKey().toLowerCase(java.util.Locale.ROOT).hashCode()
                    ^ e.getValue().hashCode();
        }
        return h;
    }

    public String toString() {
        return super.toString() + " { " + this.headers.toString() + " }";
    }

    /**
     * Los encabezados que pasen el filtro.
     *
     * @param headerMap de donde salen
     * @param filter recibe nombre y valor, y decide si entra
     * @throws NullPointerException si algo es {@code null}
     * @throws IllegalArgumentException si un nombre esta vacio, o si un nombre queda sin valores
     *     despues de filtrar — un encabezado sin valor no es un encabezado
     */
    public static HttpHeaders of(Map<String, List<String>> headerMap,
            BiPredicate<String, String> filter) {
        if (headerMap == null || filter == null) {
            throw new NullPointerException("headerMap y filter no pueden ser null");
        }
        // TreeMap con comparador que ignora mayusculas: es como se consigue la busqueda
        // insensible sin perder la escritura original de la clave.
        TreeMap<String, List<String>> out =
                new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> e : headerMap.entrySet()) {
            String nombre = e.getKey();
            if (nombre == null) {
                throw new NullPointerException("un nombre de encabezado es null");
            }
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("un nombre de encabezado esta vacio");
            }
            List<String> valores = e.getValue();
            if (valores == null) {
                throw new NullPointerException("los valores de " + nombre + " son null");
            }
            List<String> quedan = new ArrayList<String>();
            for (String v : valores) {
                if (v == null) {
                    throw new NullPointerException("un valor de " + nombre + " es null");
                }
                if (filter.test(nombre, v)) {
                    quedan.add(v);
                }
            }
            if (!quedan.isEmpty()) {
                out.put(nombre, Collections.unmodifiableList(quedan));
            }
        }
        return new HttpHeaders(Collections.unmodifiableMap(out));
    }
}
