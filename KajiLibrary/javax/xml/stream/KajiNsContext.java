package javax.xml.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * El alcance de espacios de nombres, como pila de pares prefijo-URI.
 *
 * <p>Dos arreglos paralelos y una pila de marcas: abrir un elemento apila una marca, cerrarlo
 * descarta todo lo que se declaro despues de ella. Buscar un prefijo es recorrer hacia atras, con
 * lo cual la declaracion mas interna gana sin que haya que copiar nada al entrar en cada elemento
 * --que es el costo que tiene la version con un mapa por nivel--.
 *
 * <p>Recorrer hacia atras es lineal en la cantidad de declaraciones vivas. En un documento real eso
 * es un punado; un mapa seria mas rapido en el caso patologico y mas lento en todos los demas,
 * ademas de necesitar deshacer las sombras al desapilar.
 *
 * <p>Los dos prefijos que la especificacion fija --{@code xml} y {@code xmlns}-- se contestan
 * aparte y no se pueden pisar, que es lo que pide {@link NamespaceContext}.
 *
 * <p>{@link #instantanea()} devuelve una copia inmutable, que es lo que necesita un
 * {@link javax.xml.stream.events.StartElement}: el evento sobrevive al parser, asi que no puede
 * quedarse mirando una pila que va a seguir cambiando.
 */
class KajiNsContext implements NamespaceContext {

    /** Prefijos declarados, del mas viejo al mas nuevo. */
    String[] prefixes = new String[8];

    /** URIs, en paralelo con {@link #prefijos}. */
    String[] uris = new String[8];

    /** Cuantas declaraciones vivas hay. */
    int n;

    /** Donde empieza cada nivel abierto. */
    int[] marks = new int[8];

    /** Cuantos niveles hay abiertos. */
    int levels;

    KajiNsContext() {
    }

    /** Abre un nivel: lo que se declare de aca en mas muere con el. */
    void openScope() {
        if (levels == marks.length) {
            int[] bigger = new int[marks.length * 2];
            System.arraycopy(marks, 0, bigger, 0, marks.length);
            marks = bigger;
        }
        marks[levels] = n;
        levels++;
    }

    /** Cierra el nivel de arriba y descarta sus declaraciones. */
    void closeScope() {
        if (levels > 0) {
            levels--;
            n = marks[levels];
        }
    }

    /** Cuantas declaraciones hizo el nivel de arriba. */
    int declaredInScope() {
        if (levels == 0) {
            return n;
        }
        return n - marks[levels - 1];
    }

    /** La i-esima declaracion del nivel de arriba. */
    int indexInScope(int i) {
        if (levels == 0) {
            return i;
        }
        return marks[levels - 1] + i;
    }

    /** Declara un prefijo en el nivel de arriba. */
    void declare(String prefix, String uri) {
        if (n == prefixes.length) {
            String[] p = new String[n * 2];
            String[] u = new String[n * 2];
            System.arraycopy(prefixes, 0, p, 0, n);
            System.arraycopy(uris, 0, u, 0, n);
            prefixes = p;
            uris = u;
        }
        prefixes[n] = prefix;
        uris[n] = uri;
        n++;
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("el prefijo no puede ser null");
        }
        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            return XMLConstants.XML_NS_URI;
        }
        if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        for (int i = n - 1; i >= 0; i--) {
            if (prefixes[i].equals(prefix)) {
                return uris[i];
            }
        }
        return XMLConstants.NULL_NS_URI;
    }

    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("el espacio de nombres no puede ser null");
        }
        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        for (int i = n - 1; i >= 0; i--) {
            if (uris[i].equals(namespaceURI) && getNamespaceURI(prefixes[i]).equals(namespaceURI)) {
                return prefixes[i];
            }
        }
        return null;
    }

    public Iterator<String> getPrefixes(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("el espacio de nombres no puede ser null");
        }
        List<String> r = new ArrayList<String>();
        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            r.add(XMLConstants.XML_NS_PREFIX);
            return r.iterator();
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            r.add(XMLConstants.XMLNS_ATTRIBUTE);
            return r.iterator();
        }
        for (int i = n - 1; i >= 0; i--) {
            if (uris[i].equals(namespaceURI)
                    && getNamespaceURI(prefixes[i]).equals(namespaceURI)
                    && !r.contains(prefixes[i])) {
                r.add(prefixes[i]);
            }
        }
        return r.iterator();
    }

    /** Una copia congelada, para colgarla de un evento que va a sobrevivir al parser. */
    KajiNsContext snapshot() {
        KajiNsContext c = new KajiNsContext();
        c.prefixes = new String[n < 1 ? 1 : n];
        c.uris = new String[n < 1 ? 1 : n];
        System.arraycopy(prefixes, 0, c.prefixes, 0, n);
        System.arraycopy(uris, 0, c.uris, 0, n);
        c.n = n;
        return c;
    }
}
