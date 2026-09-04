package java.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Un elemento del documento mientras se lo lee: sus atributos, los argumentos que le fueron
// llegando de sus hijos, y el valor que produce.
//
// El valor se calcula UNA vez y tarde: no al abrir el elemento sino cuando alguien lo pide. Quien
// lo pide suele ser un hijo que necesita a su contenedor como objetivo —un `<void property="x">`
// necesita el bean sobre el que llamar `setX`—, y ese pedido es justamente la senal de que ya no
// van a llegar mas argumentos al constructor. De ahi que agregar un argumento a un elemento ya
// evaluado sea un error y no un descuido: significa que el documento describe un objeto construido
// con argumentos que el objeto ya no puede recibir.
final class Element {

    final Element parent;
    final String name;
    final Map<String, String> attributes = new HashMap<String, String>();
    final List<Object> args = new ArrayList<Object>();
    final StringBuilder text = new StringBuilder();

    boolean evaluated;
    boolean inProgress;
    Object value;

    // Un elemento que fallo al evaluarse no aporta valor: no se lo cuenta como argumento de su
    // contenedor ni como objeto del documento. Es la diferencia entre "vale null" y "no vale".
    boolean empty;

    Element(Element parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    // `<void>` describe un efecto sobre el contenedor, no un valor para el; `<java>` es la raiz y
    // no tiene contenedor. Todo lo demas es un valor que va a parar a los argumentos de quien lo
    // contiene.
    boolean isArgument() {
        return !this.name.equals("void") && !this.name.equals("java");
    }
}

// El armador del grafo a partir de los eventos de un documento de `java.beans`.
//
// Es un DefaultHandler de SAX de verdad —es lo que `XMLDecoder.createHandler` entrega, y el tipo
// que ese metodo declara devolver— y ademas es el motor que usa XMLDecoder por dentro, alimentado
// por el analizador propio de este paquete. Que sean el mismo objeto no es economia: es lo que
// garantiza que el documento se lea igual venga de donde venga.
//
// El dialecto que entiende es el de la persistencia larga de beans, el que escribe XMLEncoder:
//
//   `<java>`               la raiz. Su VALOR es el objeto que se le pasa al constructor —para
//                          XMLDecoder, el propio decodificador, que es lo que hace que
//                          `<object property="owner"/>` sea `decodificador.getOwner()`—. Los
//                          valores que cuelgan de ella son los objetos del documento.
//   `<null/>`              null.
//   `<string>`             el texto, con el valor de los hijos intercalado donde aparecen.
//   `<class>`              un java.lang.Class, incluidos los primitivos y los descriptores de
//                          arreglo.
//   `<boolean> <byte> <char> <short> <int> <long> <float> <double>`
//                          el envoltorio correspondiente. Los enteros se leen con `decode`, asi
//                          que `010` es 8 y `0x1f` es 31; es lo que hace el JDK.
//   `<array class= length=>`  un arreglo, con los elementos que pongan sus `<void index=>`.
//   `<object>` / `<void>`  una llamada. Igual salvo en una cosa: `<object>` aporta su resultado
//                          como argumento de quien lo contiene y `<void>` no.
//
// Los atributos de `<object>`/`<void>` se combinan asi: `idref` corta y devuelve la variable;
// `class` fija el objetivo (y sin `method` la llamada es el constructor); sin `class` el objetivo
// es el valor del elemento contenedor; `field` lee o escribe un campo; `property` se traduce a
// `getX`/`setX` segun haya o no argumento; e `index` gana sobre `property` y se traduce a
// `get`/`set` con el indice adelante, que es lo que el JDK hace y lo que la escritura de arreglos
// y listas necesita.
//
// Las variables (`id`) se anotan al ABRIR el elemento, y apuntan al elemento y no a su valor. Sin
// eso un grafo ciclico no se puede leer: `<object id="l0"><void method="add"><object idref="l0"/>`
// pide la variable antes de que su propio elemento haya terminado, y solo apuntando al elemento se
// la puede resolver forzando su evaluacion en ese momento.
//
// **Lo que NO hace**: los objetos que arma no se pueden sacar de aca por la interfaz publica.
// `createHandler` declara devolver un `DefaultHandler`, que no tiene por donde entregar un
// resultado, y el JDK esta igual: su manejador vive en un paquete interno que ningun modulo
// exporta. Lo que si es observable, y es para lo que ese metodo esta, son las llamadas que el
// documento hace sobre el `owner` desde el nivel de `<java>`.
final class BeansHandler extends org.xml.sax.helpers.DefaultHandler {

    private final Object rootValue;
    private final ClassLoader loader;
    private ExceptionListener listener;

    private final List<Object> objects = new ArrayList<Object>();
    private final Map<String, Element> variables = new HashMap<String, Element>();
    private Element current;

    BeansHandler(Object rootValue, ExceptionListener listener, ClassLoader loader) {
        this.rootValue = rootValue;
        this.listener = listener;
        this.loader = loader;
    }

    void setListener(ExceptionListener listener) {
        this.listener = listener;
    }

    ExceptionListener effectiveListener() {
        return this.listener != null ? this.listener : Delegados.LISTENER_POR_DEFECTO;
    }

    List<Object> objects() {
        return this.objects;
    }

    // ------------------------------------------------------------------ entrada, sin SAX

    // Las tres entradas que de verdad usa el armador. `startElement`/`characters`/`endElement` de
    // SAX no son mas que adaptadores a estas: asi el camino interno de XMLDecoder no depende de
    // que exista una implementacion de `org.xml.sax.Attributes`, que en este arbol es una interfaz.
    void open(String name, Map<String, String> attributes) {
        Element e = new Element(this.current, name);
        if (attributes != null) {
            e.attributes.putAll(attributes);
        }
        this.current = e;
        String id = e.attributes.get("id");
        if (id != null) {
            this.variables.put(id, e);
        }
    }

    void text(String s) {
        if (this.current != null) {
            this.current.text.append(s);
        }
    }

    void close() {
        Element e = this.current;
        if (e == null) {
            return;
        }
        this.current = e.parent;
        this.valueOf(e);
        if (!e.empty && e.isArgument() && e.parent != null) {
            this.addArgument(e.parent, e.value);
        }
    }

    // ------------------------------------------------------------------ SAX

    public void startDocument() {
        this.objects.clear();
        this.variables.clear();
        this.current = null;
    }

    public void startElement(String uri, String localName, String qName,
            org.xml.sax.Attributes attributes) {
        Map<String, String> m = new HashMap<String, String>();
        if (attributes != null) {
            int n = attributes.getLength();
            for (int i = 0; i < n; i++) {
                String key = attributes.getQName(i);
                if (key == null || key.length() == 0) {
                    key = attributes.getLocalName(i);
                }
                m.put(key, attributes.getValue(i));
            }
        }
        this.open(nameOf(qName, localName), m);
    }

    public void characters(char[] ch, int start, int length) {
        this.text(new String(ch, start, length));
    }

    public void endElement(String uri, String localName, String qName) {
        this.close();
    }

    private static String nameOf(String qName, String localName) {
        return qName != null && qName.length() > 0 ? qName : localName;
    }

    // ------------------------------------------------------------------ evaluacion

    private void addArgument(Element parent, Object v) {
        if (parent.name.equals("string")) {
            // Un valor dentro de un `<string>` se concatena donde aparece: `<string>a<int>9</int>b`
            // es "a9b". Por eso el texto se acumula en el mismo buffer y en orden.
            parent.text.append(v);
        } else if (parent.name.equals("java")) {
            // La raiz siempre acepta: cada valor que cuelga de ella es un objeto del documento, y
            // que ya se le haya pedido su valor —lo hace cualquier `<void>` de primer nivel— no
            // tiene por que cortar la lista.
            this.objects.add(v);
        } else if (parent.evaluated || parent.inProgress) {
            this.report(new IllegalStateException("Could not add argument to evaluated element"));
        } else {
            parent.args.add(v);
        }
    }

    // El valor del elemento, calculado una sola vez.
    private Object valueOf(Element e) {
        if (!e.evaluated) {
            if (e.inProgress) {
                this.report(new IllegalStateException(
                    "<" + e.name + "> depende de su propio valor para poder calcularlo"));
                return null;
            }
            e.inProgress = true;
            try {
                e.value = this.compute(e);
            } catch (Exception ex) {
                this.report(ex);
                e.value = null;
                // Una llamada que fallo no vale null: no vale. Un valor suelto que no se pudo leer
                // si vale null, que es como se comporta el JDK y lo que deja ver en la lista de
                // objetos que ese lugar del documento no se entendio.
                e.empty = isCall(e);
            } finally {
                e.inProgress = false;
                e.evaluated = true;
            }
        }
        return e.value;
    }

    private static boolean isCall(Element e) {
        return e.name.equals("object") || e.name.equals("void") || e.name.equals("array");
    }

    private Object compute(Element e) throws Exception {
        String n = e.name;
        Object r;
        if (n.equals("java")) {
            r = this.rootValue;
        } else if (n.equals("null")) {
            r = null;
        } else if (n.equals("string")) {
            r = e.text.toString();
        } else if (n.equals("class")) {
            r = classForName(e.text.toString(), this.loader);
        } else if (n.equals("object") || n.equals("void")) {
            r = this.computeCall(e);
        } else if (n.equals("array")) {
            r = this.computeArray(e);
        } else {
            r = primitive(n, e);
        }
        return r;
    }

    // Los ocho primitivos. Los enteros por `decode` y no por `parseX`: es lo que hace el JDK, y es
    // lo que le da sentido al atributo `code` de `<char>`, que se escribe en octal o en hexa.
    private static Object primitive(String n, Element e) {
        String s = e.text.toString();
        Object r;
        if (n.equals("boolean")) {
            if (s.equalsIgnoreCase("true")) {
                r = Boolean.TRUE;
            } else if (s.equalsIgnoreCase("false")) {
                r = Boolean.FALSE;
            } else {
                throw new IllegalArgumentException("Unsupported boolean argument: " + s);
            }
        } else if (n.equals("byte")) {
            r = Byte.decode(s);
        } else if (n.equals("short")) {
            r = Short.decode(s);
        } else if (n.equals("int")) {
            r = Integer.decode(s);
        } else if (n.equals("long")) {
            r = Long.decode(s);
        } else if (n.equals("float")) {
            r = Float.valueOf(s);
        } else if (n.equals("double")) {
            r = Double.valueOf(s);
        } else if (n.equals("char")) {
            String code = e.attributes.get("code");
            if (code != null) {
                r = Character.valueOf((char) Integer.decode(code).intValue());
            } else if (s.length() == 1) {
                r = Character.valueOf(s.charAt(0));
            } else {
                throw new IllegalArgumentException("Unsupported char argument: " + s);
            }
        } else {
            throw new IllegalArgumentException("Unsupported element: <" + n + ">");
        }
        return r;
    }

    // `class` es el tipo de COMPONENTE, no el del arreglo. Sin `length` el largo lo dan los valores
    // sueltos que cuelgan; con `length` el arreglo nace de ese tamano y lo llenan los
    // `<void index=>`. Si vinieran las dos cosas se usan las dos, que es leer el documento de la
    // forma mas obvia; el JDK en ese caso rechaza los valores sueltos.
    private Object computeArray(Element e) throws Exception {
        String className = e.attributes.get("class");
        if (className == null) {
            throw new IllegalArgumentException("<array> sin atributo class");
        }
        Class<?> component = classForName(className, this.loader);
        String length = e.attributes.get("length");
        int n = length != null ? Integer.parseInt(length) : e.args.size();
        Object array = Array.newInstance(component, n);
        for (int i = 0; i < e.args.size() && i < n; i++) {
            Statement.ponerEnArreglo(array, i, e.args.get(i));
        }
        return array;
    }

    private Object computeCall(Element e) throws Exception {
        String idref = e.attributes.get("idref");
        if (idref != null) {
            return this.variable(idref);
        }

        Class<?> type = null;
        String className = e.attributes.get("class");
        if (className != null) {
            type = classForName(className, this.loader);
        }
        Object[] args = e.args.toArray(new Object[e.args.size()]);

        String fieldName = e.attributes.get("field");
        if (fieldName != null) {
            Object base = type != null ? null : this.context(e);
            Class<?> owner = type != null ? type : base.getClass();
            Field f = owner.getField(fieldName);
            Object r = null;
            if (args.length == 0) {
                r = f.get(base);
            } else {
                f.set(base, args[0]);
            }
            return r;
        }

        Object target = type != null ? type : this.context(e);
        String method = e.attributes.get("method");
        String property = e.attributes.get("property");
        if (property != null) {
            method = (args.length == 0 ? "get" : "set") + PropertyDescriptor.capitalizar(property);
        }
        String index = e.attributes.get("index");
        if (index != null) {
            // El indice gana sobre la propiedad, igual que en el JDK: `index` describe acceso
            // posicional —el de un arreglo o una lista— y ese acceso se llama `get`/`set` a secas.
            method = args.length == 0 ? "get" : "set";
            Object[] withIndex = new Object[args.length + 1];
            withIndex[0] = Integer.valueOf(index);
            System.arraycopy(args, 0, withIndex, 1, args.length);
            args = withIndex;
        }
        if (method == null) {
            method = "new";
        }
        return new Expression(target, method, args).getValue();
    }

    private Object context(Element e) {
        if (e.parent == null) {
            throw new IllegalStateException("<" + e.name + "> fuera de todo elemento contenedor");
        }
        Object v = this.valueOf(e.parent);
        if (v == null) {
            throw new IllegalStateException("Context bean is not created");
        }
        return v;
    }

    private Object variable(String name) {
        Element e = this.variables.get(name);
        if (e == null) {
            throw new IllegalArgumentException("Unbound variable: " + name);
        }
        return this.valueOf(e);
    }

    // Los nueve nombres que no son de ninguna clase cargable, y despues el cargador.
    static Class<?> classForName(String name, ClassLoader loader) throws ClassNotFoundException {
        Class<?> r;
        if (name.equals("boolean")) { r = boolean.class; }
        else if (name.equals("byte")) { r = byte.class; }
        else if (name.equals("char")) { r = char.class; }
        else if (name.equals("short")) { r = short.class; }
        else if (name.equals("int")) { r = int.class; }
        else if (name.equals("long")) { r = long.class; }
        else if (name.equals("float")) { r = float.class; }
        else if (name.equals("double")) { r = double.class; }
        else if (name.equals("void")) { r = void.class; }
        else if (loader != null) { r = Class.forName(name, true, loader); }
        else { r = Class.forName(name); }
        return r;
    }

    void report(Exception e) {
        this.effectiveListener().exceptionThrown(e);
    }
}
