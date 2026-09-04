package java.beans;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// El Encoder que ademas imprime. Toda la inteligencia —que llamadas rehacen el grafo— ya la puso
// Encoder; lo que agrega esta clase es la representacion textual de esas llamadas y, sobre todo,
// **el aliasing**: decidir que objeto necesita un `id` porque lo apuntan varios, y cual se puede
// escribir en linea.
//
// Por eso no puede imprimir a medida que codifica. Mientras se recorre el grafo todavia no se sabe
// si un objeto va a aparecer una vez o cinco, y esa respuesta cambia como se lo escribe: en linea
// o como `<object id="foo0">` mas `<object idref="foo0"/>` en los otros lugares. Entonces la
// codificacion **acumula** —cada llamada se guarda en la lista de su objetivo— y `flush()` es el
// que recorre lo acumulado y recien ahi imprime. De ahi que `close()` sea obligatorio: sin un
// flush no sale nada.
//
// El conteo de referencias es lo unico delicado. `marcar` recorre el grafo una vez y suma una
// referencia por cada lugar donde el objeto aparece como argumento; `imprimirLlamada` le pone `id`
// solo a los que juntaron mas de una. Un objeto compartido que saliera dos veces en linea seria un
// grafo distinto al original —dos objetos donde habia uno—, y ese es justo el error que el `id`
// evita.
//
// ## Lo que NO hace
//
// **El `charset` solo puede ser UTF-8.** No es una simplificacion gratuita: en este arbol
// `java.io.OutputStreamWriter` ignora el charset que se le pasa y trunca cada char a su byte bajo
// (verificado: "ñ€" sale como `f1 ac` en vez de `c3 b1 e2 82 ac`), asi que apoyarse en
// el daria un archivo que dice `encoding="UTF-8"` y no lo es. Aca la codificacion UTF-8 se hace a
// mano sobre el OutputStream —correcta, con pares subrogados incluidos— y cualquier otro nombre de
// charset se rechaza en el constructor con IllegalArgumentException. El JDK tira
// UnsupportedCharsetException en ese mismo lugar, que TAMBIEN es una IllegalArgumentException, asi
// que quien atrapa el caso de error se comporta igual; lo que cambia es el conjunto de charsets
// aceptados, y eso es una limitacion declarada y no una mentira sobre el contenido del archivo.
public class XMLEncoder extends Encoder implements AutoCloseable {

    private static final String SALTO = saltoDeLinea();

    private final OutputStream salida;
    private final String charset;
    private final boolean declaracion;

    private Object owner;
    private int sangria;
    private boolean preambulo;

    // Mientras vale true, las llamadas que llegan vienen de la maquinaria de Encoder y no de un
    // `writeObject` del usuario. La distincion importa: un writeObject de afuera es una raiz del
    // documento, uno de adentro es una parte de algo que ya se esta escribiendo.
    private boolean interno;

    private final Map<Object, Datos> datos = new IdentityHashMap<Object, Datos>();
    private final Map<Object, List<Statement>> porObjetivo = new IdentityHashMap<Object, List<Statement>>();

    // Las mismas listas de `porObjetivo`, en un indice que se puede recorrer sin tocar las claves.
    // Ver `llamadaSuelta`.
    private final List<List<Statement>> todasLasListas = new ArrayList<List<Statement>>();

    // Generador de nombres para los `id`: "Vector0", "Vector1", ... Es de identidad porque dos
    // objetos iguales pero distintos tienen que llevar nombres distintos.
    private final Map<Object, String> nombres = new IdentityHashMap<Object, String>();
    private final Map<String, Integer> contadores = new HashMap<String, Integer>();

    // Lo que se sabe de un objeto del grafo mientras se lo acumula.
    private static final class Datos {
        Expression exp;     // la expresion que lo produce
        int refs;           // cuantas veces lo apuntan
        boolean marcado;    // ya lo recorrio `marcar`
        String nombre;      // el `id` que se le asigno, si lo necesito
    }

    public XMLEncoder(OutputStream out) {
        this(out, "UTF-8", true, 0);
    }

    public XMLEncoder(OutputStream out, String charset, boolean declaration, int indentation) {
        if (out == null) {
            throw new IllegalArgumentException("the output stream cannot be null");
        }
        if (indentation < 0) {
            throw new IllegalArgumentException("the indentation must be >= 0");
        }
        if (charset == null) {
            throw new IllegalArgumentException("the charset cannot be null");
        }
        if (!esUtf8(charset)) {
            throw new IllegalArgumentException("unsupported charset: " + charset
                + " (this implementation only encodes UTF-8)");
        }
        this.salida = out;
        this.charset = charset;
        this.declaracion = declaration;
        this.sangria = indentation;
    }

    private static boolean esUtf8(String nombre) {
        String n = nombre.toUpperCase();
        return n.equals("UTF-8") || n.equals("UTF8") || n.equals("UNICODE-1-1-UTF-8");
    }

    private static String saltoDeLinea() {
        String s = null;
        try {
            s = System.getProperty("line.separator");
        } catch (Exception e) {
            s = null;
        }
        return s == null ? "\n" : s;
    }

    public Object getOwner() {
        return this.owner;
    }

    // Ademas de guardarlo, se lo anota como valor de la expresion `getOwner()` sobre este
    // codificador. Asi, cuando el grafo apunte al owner, en vez de volver a describirlo entero
    // sale `<object property="owner">` — que es lo que el decodificador necesita para volver a
    // enchufarle SU owner al leer.
    public void setOwner(Object owner) {
        this.owner = owner;
        this.writeExpression(new Expression(owner, this, "getOwner", new Object[0]));
    }

    // Una raiz del documento. Se representa como la llamada `this.writeObject(o)`, y flush() la
    // reconoce por el nombre para imprimir el valor y no la llamada.
    public void writeObject(Object o) {
        if (this.interno) {
            super.writeObject(o);
        } else {
            this.writeStatement(new Statement(this, "writeObject", new Object[] { o }));
        }
    }

    public void writeStatement(Statement oldStm) {
        boolean previo = this.interno;
        this.interno = true;
        try {
            super.writeStatement(oldStm);
            // El marcado va ANTES de encolar: la llamada puede depender de valores que se
            // establecieron en llamadas anteriores de este mismo contexto.
            this.marcar(oldStm);
            Object objetivo = oldStm.getTarget();
            if (objetivo instanceof Field) {
                // Un `campo.get(x)` / `campo.set(x, v)` describe estado de `x`, no del Field: la
                // llamada tiene que quedar colgada de x o saldria fuera del objeto al que
                // pertenece.
                String metodo = oldStm.getMethodName();
                Object[] args = oldStm.getArguments();
                if (metodo != null && args != null) {
                    if (metodo.equals("get") && args.length == 1) {
                        objetivo = args[0];
                    } else if (metodo.equals("set") && args.length == 2) {
                        objetivo = args[0];
                    }
                }
            }
            this.listaDe(objetivo).add(oldStm);
        } catch (Exception e) {
            this.getExceptionListener().exceptionThrown(
                new Exception("XMLEncoder: discarding statement " + oldStm, e));
        }
        this.interno = previo;
    }

    public void writeExpression(Expression oldExp) {
        boolean previo = this.interno;
        this.interno = true;
        Object valor = this.valorDe(oldExp);
        // La condicion de la cadena es a proposito: una cadena que llega desde afuera —no desde la
        // maquinaria— se escribe aunque ya tenga enlace, porque el usuario la pidio como raiz.
        if (this.get(valor) == null || (valor instanceof String && !previo)) {
            this.datosDe(valor).exp = oldExp;
            super.writeExpression(oldExp);
        }
        this.interno = previo;
    }

    // Imprime todo lo acumulado y vacia el estado. El preambulo sale con el primer flush y no en
    // el constructor: escribir en el constructor haria que un XMLEncoder que nunca se usa deje un
    // archivo con un `<java>` sin cerrar.
    public void flush() {
        if (!this.preambulo) {
            if (this.declaracion) {
                this.linea("<?xml version=" + comillas("1.0")
                    + " encoding=" + comillas(this.charset) + "?>");
            }
            this.linea("<java version=" + comillas(versionDeJava())
                + " class=" + comillas("java.beans.XMLDecoder") + ">");
            this.preambulo = true;
        }

        this.sangria++;
        List<Statement> raices = this.listaDe(this);
        while (!raices.isEmpty()) {
            Statement s = raices.remove(0);
            if ("writeObject".equals(s.getMethodName())) {
                this.imprimirValor(s.getArguments()[0], this, true);
            } else {
                this.imprimirLlamada(s, this, false);
            }
        }
        this.sangria--;

        // Llamadas que quedaron colgadas de un objetivo que nunca se imprimio. Perderlas seria
        // perder estado del grafo en silencio, asi que se emiten igual al nivel de arriba.
        Statement suelta = this.llamadaSuelta();
        while (suelta != null) {
            this.imprimirLlamada(suelta, this, false);
            suelta = this.llamadaSuelta();
        }

        try {
            this.salida.flush();
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
        this.limpiar();
    }

    public void close() {
        this.flush();
        this.linea("</java>");
        try {
            this.salida.close();
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }

    private static String versionDeJava() {
        String v = null;
        try {
            v = System.getProperty("java.version");
        } catch (Exception e) {
            v = null;
        }
        return v == null ? "" : v;
    }

    private void limpiar() {
        this.limpiarEnlaces();
        this.datos.clear();
        this.porObjetivo.clear();
        this.todasLasListas.clear();
        this.nombres.clear();
        this.contadores.clear();
    }

    // Recorre las listas por el indice paralelo y no por `porObjetivo.values()`. Motivo concreto:
    // en este arbol `IdentityHashMap.values()` recorre hasheando las CLAVES, asi que una clave cuyo
    // `hashCode()` no termina —una coleccion que se contiene a si misma, que es justo el caso que
    // esta prueba— desborda la pila, aunque `put` y `get` sobre esa misma clave anden bien porque
    // van por identidad. El indice guarda las mismas listas y no toca ninguna clave.
    private Statement llamadaSuelta() {
        Statement r = null;
        Iterator<List<Statement>> it = this.todasLasListas.iterator();
        while (r == null && it.hasNext()) {
            List<Statement> l = it.next();
            for (int i = 0; r == null && i < l.size(); i++) {
                // Solo Statement puro: una Expression suelta no describe estado, describe un valor
                // que nadie termino usando.
                if (Statement.class == l.get(i).getClass()) {
                    r = l.remove(i);
                }
            }
        }
        return r;
    }

    // Las dos tablas se indexan por objetos del grafo, y `null` es una clave legitima: una llamada
    // sin valor —un `void`— tiene valor null, y sus datos y su cuerpo se guardan igual que los de
    // cualquier otro. En este arbol ni HashMap ni IdentityHashMap aceptan una clave nula (tiran
    // NullPointerException donde el JDK guarda la entrada), asi que null viaja como este centinela.
    // Es un objeto privado y unico: nunca puede chocar con un objeto del grafo.
    private static final Object CLAVE_NULA = new Object();

    private static Object clave(Object o) {
        return o == null ? CLAVE_NULA : o;
    }

    private Datos datosDe(Object o) {
        Object k = clave(o);
        Datos d = this.datos.get(k);
        if (d == null) {
            d = new Datos();
            this.datos.put(k, d);
        }
        return d;
    }

    private List<Statement> listaDe(Object objetivo) {
        Object k = clave(objetivo);
        List<Statement> l = this.porObjetivo.get(k);
        if (l == null) {
            l = new ArrayList<Statement>();
            this.porObjetivo.put(k, l);
            this.todasLasListas.add(l);
        }
        return l;
    }

    // Recorre el grafo desde una llamada sumando referencias. Cada objeto se recorre una sola vez
    // —el flag `marcado`—, pero su cuenta de referencias sube cada vez que aparece como argumento:
    // eso es exactamente "cuantos lugares lo apuntan", que es lo que decide si necesita un `id`.
    private void marcar(Statement stm) {
        Object[] args = stm.getArguments();
        for (int i = 0; i < args.length; i++) {
            this.marcar(args[i], true);
        }
        this.marcar(stm.getTarget(), stm instanceof Expression);
    }

    private void marcar(Object o, boolean esArgumento) {
        if (o == null || o == this) {
            return;
        }
        Datos d = this.datosDe(o);
        Expression exp = d.exp;
        // Una cadena literal —sin expresion que la produzca— se escribe en linea siempre: no tiene
        // identidad que valga la pena preservar. Una cadena que SI vino de una expresion (de un
        // resource bundle, por ejemplo) se marca como cualquier objeto.
        if (o.getClass() == String.class && exp == null) {
            return;
        }
        if (esArgumento) {
            d.refs++;
        }
        if (d.marcado || exp == null) {
            return;
        }
        d.marcado = true;
        Object objetivo = exp.getTarget();
        this.marcar(exp);
        if (!(objetivo instanceof Class)) {
            this.listaDe(objetivo).add(exp);
            d.refs++;
        }
    }

    private void imprimirValor(Object valor, Object contenedor, boolean esArgumento) {
        if (valor == null) {
            this.linea("<null/>");
            return;
        }
        if (valor instanceof Class) {
            this.linea("<class>" + ((Class<?>) valor).getName() + "</class>");
            return;
        }

        Datos d = this.datosDe(valor);
        Expression exp = d.exp;

        // Un envoltorio que se rehace con `new Integer("7")` se imprime como `<int>7</int>`: es la
        // forma corta del mismo hecho, y es la que hace legible el archivo.
        if (exp != null) {
            Class<?> primitivo = Statement.primitivoDelEnvoltorio(valor.getClass());
            if (primitivo != null && exp.getTarget() == valor.getClass()
                    && "new".equals(exp.getMethodName())) {
                String etiqueta = primitivo.getName();
                String texto = primitivo == char.class
                    ? escapar(String.valueOf(((Character) valor).charValue()))
                    : String.valueOf(valor);
                this.linea("<" + etiqueta + ">" + texto + "</" + etiqueta + ">");
                return;
            }
        }

        if (valor instanceof String && exp == null) {
            this.linea("<string>" + escapar((String) valor) + "</string>");
            return;
        }

        // Ya se lo imprimio antes y lleva nombre: aca va la referencia, no una segunda copia.
        if (d.nombre != null) {
            this.linea("<object idref=" + comillas(d.nombre) + "/>");
            return;
        }

        if (exp == null) {
            // Nadie supo como rehacerlo. Se dice, en vez de escribir un `<object>` vacio que al
            // leerlo daria otro objeto.
            this.getExceptionListener().exceptionThrown(
                new Exception("XMLEncoder: no expression for " + valor.getClass().getName()));
            this.linea("<null/>");
            return;
        }

        this.imprimirLlamada(exp, contenedor, esArgumento);
    }

    private void imprimirLlamada(Statement exp, Object contenedor, boolean esArgumento) {
        Object objetivo = exp.getTarget();
        String metodo = exp.getMethodName();
        Object[] args = exp.getArguments();
        boolean esExpresion = exp.getClass() == Expression.class;
        Object valor = null;
        if (esExpresion) {
            valor = this.valorDe((Expression) exp);
        }

        String etiqueta = (esExpresion && esArgumento) ? "object" : "void";
        StringBuilder atrs = new StringBuilder();
        Datos d = this.datosDe(valor);

        // La llamada sobre el objeto que ya estamos escribiendo no lleva atributo de objetivo: se
        // sobreentiende. Es tambien el caso del owner, cuyo objetivo es este mismo codificador.
        if (objetivo == contenedor) {
            atrs.setLength(0);
        } else if (objetivo == Array.class && "newInstance".equals(metodo)) {
            etiqueta = "array";
            atrs.append(" class=").append(comillas(((Class<?>) args[0]).getName()));
            atrs.append(" length=").append(comillas(String.valueOf(args[1])));
            args = new Object[0];
        } else if (objetivo != null && objetivo.getClass() == Class.class) {
            atrs.append(" class=").append(comillas(((Class<?>) objetivo).getName()));
        } else {
            // El objetivo es otro objeto del grafo. Hay que escribirlo A EL, y esta llamada pasa a
            // colgar de el. El `refs = 2` fuerza que lleve `id`: si algo se llama sobre un objeto,
            // ese objeto tiene identidad y no se puede escribir dos veces en linea.
            d.refs = 2;
            if (d.nombre == null) {
                Datos dObjetivo = this.datosDe(objetivo);
                dObjetivo.refs = dObjetivo.refs + 1;
                List<Statement> l = this.listaDe(objetivo);
                if (!l.contains(exp)) {
                    l.add(exp);
                }
                this.imprimirValor(objetivo, contenedor, false);
            }
            if (esExpresion) {
                this.imprimirValor(valor, contenedor, esArgumento);
            }
            return;
        }

        if (esExpresion && d.refs > 1) {
            String nombre = this.nombreDe(valor);
            d.nombre = nombre;
            atrs.append(" id=").append(comillas(nombre));
        }

        // Acceso indexado: `set(i, v)` y `get(i)` se escriben con el atributo `index`, no con el
        // indice como primer argumento. Es lo que hace legible un arreglo.
        if ((!esExpresion && "set".equals(metodo) && args.length == 2 && args[0] instanceof Integer)
                || (esExpresion && "get".equals(metodo) && args.length == 1 && args[0] instanceof Integer)) {
            atrs.append(" index=").append(comillas(String.valueOf(args[0])));
            args = args.length == 1 ? new Object[0] : new Object[] { args[1] };
        } else if ((!esExpresion && metodo.startsWith("set") && args.length == 1)
                || (esExpresion && metodo.startsWith("get") && args.length == 0)) {
            // Un par get/set se escribe como la propiedad que es. `setNombre(x)` -> property="nombre".
            if (metodo.length() > 3) {
                atrs.append(" property=").append(comillas(Introspector.decapitalize(metodo.substring(3))));
            }
        } else if (!"new".equals(metodo) && !"newInstance".equals(metodo)) {
            atrs.append(" method=").append(comillas(metodo));
        }

        List<Statement> cuerpo = this.listaDe(valor);

        if (args.length == 0 && cuerpo.size() == 0) {
            this.linea("<" + etiqueta + atrs + "/>");
            return;
        }

        this.linea("<" + etiqueta + atrs + ">");
        this.sangria++;
        for (int i = 0; i < args.length; i++) {
            this.imprimirValor(args[i], null, true);
        }
        while (!cuerpo.isEmpty()) {
            this.imprimirLlamada(cuerpo.remove(0), valor, false);
        }
        this.sangria--;
        this.linea("</" + etiqueta + ">");
    }

    // "java.util.Vector" -> "Vector0", "Vector1", ... Los arreglos llevan el sufijo "Array".
    private String nombreDe(Object o) {
        String r;
        if (o == null) {
            r = "null";
        } else if (o instanceof Class) {
            r = nombreCorto((Class<?>) o);
        } else {
            r = this.nombres.get(o);
            if (r == null) {
                String base = nombreCorto(o.getClass());
                Integer previo = this.contadores.get(base);
                int n = previo == null ? 0 : previo.intValue() + 1;
                this.contadores.put(base, Integer.valueOf(n));
                r = base + n;
                this.nombres.put(o, r);
            }
        }
        return r;
    }

    private static String nombreCorto(Class<?> tipo) {
        String r;
        if (tipo.isArray()) {
            r = nombreCorto(tipo.getComponentType()) + "Array";
        } else {
            String n = tipo.getName();
            r = n.substring(n.lastIndexOf('.') + 1);
        }
        return r;
    }

    private static String comillas(String s) {
        return "\"" + s + "\"";
    }

    // Los seis caracteres que no pueden ir crudos en XML. El `\r` va como referencia numerica
    // porque un parser normaliza los fines de linea y se lo comeria.
    private static String escapar(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') { sb.append("&amp;"); }
            else if (c == '<') { sb.append("&lt;"); }
            else if (c == '>') { sb.append("&gt;"); }
            else if (c == '"') { sb.append("&quot;"); }
            else if (c == '\'') { sb.append("&apos;"); }
            else if (c == '\r') { sb.append("&#13;"); }
            else { sb.append(c); }
        }
        return sb.toString();
    }

    private void linea(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.sangria; i++) {
            sb.append(' ');
        }
        sb.append(texto).append(SALTO);
        this.emitir(sb.toString());
    }

    // UTF-8 a mano. Ver el encabezado: OutputStreamWriter no codifica en este arbol.
    private void emitir(String s) {
        try {
            int n = s.length();
            int i = 0;
            while (i < n) {
                int cp = s.charAt(i);
                i++;
                if (cp >= 0xD800 && cp <= 0xDBFF && i < n) {
                    char bajo = s.charAt(i);
                    if (bajo >= 0xDC00 && bajo <= 0xDFFF) {
                        cp = 0x10000 + ((cp - 0xD800) << 10) + (bajo - 0xDC00);
                        i++;
                    }
                }
                if (cp < 0x80) {
                    this.salida.write(cp);
                } else if (cp < 0x800) {
                    this.salida.write(0xC0 | (cp >> 6));
                    this.salida.write(0x80 | (cp & 0x3F));
                } else if (cp < 0x10000) {
                    this.salida.write(0xE0 | (cp >> 12));
                    this.salida.write(0x80 | ((cp >> 6) & 0x3F));
                    this.salida.write(0x80 | (cp & 0x3F));
                } else {
                    this.salida.write(0xF0 | (cp >> 18));
                    this.salida.write(0x80 | ((cp >> 12) & 0x3F));
                    this.salida.write(0x80 | ((cp >> 6) & 0x3F));
                    this.salida.write(0x80 | (cp & 0x3F));
                }
            }
        } catch (IOException e) {
            this.getExceptionListener().exceptionThrown(e);
        }
    }
}
