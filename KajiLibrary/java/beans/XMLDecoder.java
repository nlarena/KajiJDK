package java.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

// La contraparte de XMLEncoder: lee el documento y devuelve los objetos que describe.
//
// El grafo no se "deserializa" — se **reconstruye ejecutando**. Cada elemento del documento es una
// llamada (`Statement`/`Expression`) y leer es correrlas en orden: `<object class="Foo">` es
// `new Foo()`, `<void property="x">` es `setX(...)`, `<object idref="f0"/>` es "el mismo objeto de
// antes". Por eso un archivo de beans no depende de campos privados ni de un serialVersionUID:
// depende de que la clase siga teniendo el constructor y los setters que el archivo nombra.
//
// El valor del elemento `<java>` es **este decodificador**. Suena raro hasta que se mira lo que
// XMLEncoder escribe para el owner: `<void id="X0" property="owner"/>`, que es `getOwner()` sobre
// el valor de `<java>`. Poniendo ahi al decodificador esa llamada resuelve al owner que se le paso
// al constructor, que es justo el sentido de la palabra: "el objeto que este documento espera que
// le den al leerlo". Es lo mismo que hace el JDK.
//
// La lectura es entera y de una: el primer `readObject()` analiza todo el documento, arma el grafo
// y despues va entregando los objetos uno por uno. No podria ser perezosa — un `idref` puede
// apuntar a algo que se define mas abajo dentro de la misma raiz — y el JDK tampoco lo es.
// Agotada la lista, `readObject()` tira ArrayIndexOutOfBoundsException, que es como el JDK dice
// "no hay mas".
//
// ## De donde saca el XML
//
// **No hay un parser SAX del sistema.** En este arbol `org.xml.sax` son interfaces sin ninguna
// implementacion: `XMLReaderFactory` no tiene a quien instanciar. Asi que el analisis lo hace
// `AnalizadorXml`, el analizador propio de este paquete, que lee el dialecto de la persistencia de
// beans y nada mas —sin DTD, sin espacios de nombres, sin entidades del documento—. Es una
// limitacion declarada del alcance del parser, no del formato: todo lo que XMLEncoder escribe entra.
//
// Sobre el constructor que recibe un `org.xml.sax.InputSource`: se atienden las dos formas en que
// una fuente TRAE el documento —un flujo de bytes o un Reader— y, si solo trae un system id, se lo
// abre como archivo (aceptando el prefijo `file:`) y si no como URL. Un system id que ningun
// `URLStreamHandler` sepa abrir falla con IOException donde el JDK lo resolveria, y eso es
// exactamente lo que le falta a `java.net` en este arbol, no algo que este metodo decida.
public class XMLDecoder implements AutoCloseable {

    private final InputStream in;
    private final Reader reader;
    private final ClassLoader loader;

    private Object owner;
    private ExceptionListener exceptionListener;

    private Object[] objects;
    private int index;

    public XMLDecoder(InputStream in) {
        this(in, null, null, null);
    }

    public XMLDecoder(InputStream in, Object owner) {
        this(in, owner, null, null);
    }

    public XMLDecoder(InputStream in, Object owner, ExceptionListener exceptionListener) {
        this(in, owner, exceptionListener, null);
    }

    public XMLDecoder(InputStream in, Object owner, ExceptionListener exceptionListener,
            ClassLoader cl) {
        this.in = in;
        this.reader = null;
        this.owner = owner;
        this.exceptionListener = exceptionListener;
        this.loader = cl;
    }

    // Una fuente SAX. Se le pide el contenido en el orden en que la especificacion dice que hay
    // que mirarlo: primero el flujo de caracteres, despues el de bytes, y recien al final el
    // system id.
    public XMLDecoder(org.xml.sax.InputSource is) {
        if (is == null) {
            throw new IllegalArgumentException("input source is null");
        }
        this.owner = null;
        this.exceptionListener = null;
        this.loader = null;
        Reader r = is.getCharacterStream();
        InputStream s = r != null ? null : is.getByteStream();
        if (r == null && s == null) {
            s = openSystemId(is.getSystemId());
        }
        this.reader = r;
        this.in = s;
    }

    // `file:/x/y` y `/x/y` van derecho al sistema de archivos; cualquier otra cosa se intenta como
    // URL. Abrirlo aca y no en el primer readObject es lo que permite que el error salga como una
    // IllegalArgumentException del constructor, que es donde el llamador lo espera.
    private static InputStream openSystemId(String systemId) {
        if (systemId == null) {
            throw new IllegalArgumentException("input source carries neither a stream nor a system id");
        }
        try {
            if (systemId.startsWith("file:")) {
                String path = systemId.substring(5);
                while (path.startsWith("///")) {
                    path = path.substring(2);
                }
                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
                return new java.io.FileInputStream(path);
            }
            if (systemId.indexOf(':') < 0) {
                return new java.io.FileInputStream(systemId);
            }
            return new java.net.URL(systemId).openStream();
        } catch (IOException ex) {
            throw new IllegalArgumentException("no se pudo abrir " + systemId + ": " + ex);
        }
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    // Nunca null: sin uno puesto a mano, el que imprime y sigue. Es la misma promesa que hace
    // Encoder y la que el JDK documenta.
    public ExceptionListener getExceptionListener() {
        return this.exceptionListener != null
            ? this.exceptionListener : Delegados.LISTENER_POR_DEFECTO;
    }

    public void setOwner(Object owner) {
        this.owner = owner;
    }

    public Object getOwner() {
        return this.owner;
    }

    // El siguiente objeto del documento. Agotados, ArrayIndexOutOfBoundsException: el JDK usa esa
    // misma excepcion como fin de lista, y quien lee en bucle la atrapa para cortar.
    public Object readObject() {
        if (this.objects == null) {
            this.parse();
        }
        return this.objects[this.index++];
    }

    // Cierra la entrada. Antes analiza, porque un documento puede no tener ningun objeto de primer
    // nivel y consistir solo en llamadas sobre el owner: si cerrar no analizara, esas llamadas no
    // pasarian nunca.
    public void close() {
        if (this.objects == null) {
            this.parse();
        }
        try {
            if (this.reader != null) {
                this.reader.close();
            }
            if (this.in != null) {
                this.in.close();
            }
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }

    private void parse() {
        this.objects = new Object[0];
        if (this.in == null && this.reader == null) {
            return;
        }
        BeansHandler handler = new BeansHandler(this, this.exceptionListener, this.loader);
        try {
            NodoXml root = this.reader != null
                ? AnalizadorXml.parseText(AnalizadorXml.readAll(this.reader))
                : AnalizadorXml.analizar(this.in);
            handler.startDocument();
            replay(root, handler);
            List<Object> l = handler.objects();
            this.objects = l.toArray(new Object[l.size()]);
        } catch (Exception e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }

    // El arbol que devolvio el analizador, contado al manejador como si lo hubiera dictado un
    // parser SAX. Va por `content` y no por `hijos`/`texto` porque el orden entre el texto y los
    // elementos es informacion: dentro de un `<string>` decide donde queda cada pedazo.
    private static void replay(NodoXml node, BeansHandler handler) {
        handler.open(node.nombre, node.atributos);
        for (int i = 0; i < node.content.size(); i++) {
            Object child = node.content.get(i);
            if (child instanceof NodoXml) {
                replay((NodoXml) child, handler);
            } else {
                handler.text((String) child);
            }
        }
        handler.close();
    }

    // Un manejador SAX que arma el mismo grafo que arma este decodificador. Lo que el documento
    // haga sobre el `owner` desde el nivel de `<java>` es lo observable desde afuera: el tipo de
    // retorno es DefaultHandler y no tiene por donde entregar los objetos de primer nivel, y el
    // manejador del JDK esta en la misma posicion —vive en un paquete interno que nadie exporta—.
    public static org.xml.sax.helpers.DefaultHandler createHandler(Object owner,
            ExceptionListener el, ClassLoader cl) {
        return new BeansHandler(owner, el, cl);
    }
}
