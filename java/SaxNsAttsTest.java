import java.util.Enumeration;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

// Comportamiento de org.xml.sax.helpers.AttributesImpl y NamespaceSupport.
//
// Esta prueba esta escrita para correr en las DOS maquinas y dar lo mismo. En el `java` real,
// org.xml.sax vive en el modulo java.xml, y un modulo le gana al classpath: o sea que el mismo
// .class corrido alla ejercita la implementacion del JDK y corrido aca ejercita la nuestra. La
// comparacion de los dos resultados es el oraculo.
//
// Devuelve -1 si todo dio igual, y si no la cantidad de diferencias.
public class SaxNsAttsTest {

    static int fallas = 0;

    static void eq(String etiqueta, Object esperado, Object real) {
        boolean ok;
        if (esperado == null) {
            ok = (real == null);
        } else {
            ok = esperado.equals(real);
        }
        if (!ok) {
            fallas++;
            System.out.println("FALLA " + etiqueta + ": esperaba <" + esperado
                               + "> y vino <" + real + ">");
        }
    }

    static void eqi(String etiqueta, int esperado, int real) {
        if (esperado != real) {
            fallas++;
            System.out.println("FALLA " + etiqueta + ": esperaba " + esperado
                               + " y vino " + real);
        }
    }

    static void verdad(String etiqueta, boolean b) {
        if (!b) {
            fallas++;
            System.out.println("FALLA " + etiqueta);
        }
    }

    public static int run() {
        fallas = 0;
        atributos();
        indices();
        mutacion();
        nsBasico();
        nsAtributoSinPrefijo();
        nsPila();
        nsDeclaraciones();
        nsDeclUris();
        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    ////////////////////////////////////////////////////////////////////
    // AttributesImpl
    ////////////////////////////////////////////////////////////////////

    static AttributesImpl tres() {
        AttributesImpl a = new AttributesImpl();
        a.addAttribute("http://u1", "id", "id", "ID", "x1");
        a.addAttribute("http://u2", "href", "h:href", "CDATA", "x2");
        a.addAttribute("", "lang", "lang", "NMTOKEN", "x3");
        return a;
    }

    static void atributos() {
        AttributesImpl a = tres();
        eqi("length", 3, a.getLength());

        eq("getURI(0)", "http://u1", a.getURI(0));
        eq("getLocalName(1)", "href", a.getLocalName(1));
        eq("getQName(1)", "h:href", a.getQName(1));
        eq("getType(0)", "ID", a.getType(0));
        eq("getValue(2)", "x3", a.getValue(2));

        // Indice fuera de rango: null, no excepcion. Es la mitad del contrato que mas se olvida.
        eq("getURI(-1)", null, a.getURI(-1));
        eq("getURI(3)", null, a.getURI(3));
        eq("getLocalName(9)", null, a.getLocalName(9));
        eq("getQName(-2)", null, a.getQName(-2));
        eq("getType(3)", null, a.getType(3));
        eq("getValue(3)", null, a.getValue(3));

        // Copia por constructor y por setAttributes.
        AttributesImpl b = new AttributesImpl(a);
        eqi("copia length", 3, b.getLength());
        eq("copia qname", "h:href", b.getQName(1));
        b.setValue(1, "otro");
        eq("la copia es independiente", "x2", a.getValue(1));

        AttributesImpl c = new AttributesImpl();
        c.addAttribute("", "basura", "basura", "CDATA", "z");
        c.setAttributes(a);
        eqi("setAttributes reemplaza", 3, c.getLength());
        eq("setAttributes copia", "x1", c.getValue(0));

        c.clear();
        eqi("clear", 0, c.getLength());
        eq("clear deja null", null, c.getValue(0));
    }

    static void indices() {
        AttributesImpl a = tres();

        // Las tres formas de buscar lo mismo, cada una con su caso de "no esta".
        eqi("getIndex(uri,local)", 1, a.getIndex("http://u2", "href"));
        eqi("getIndex(uri,local) ausente", -1, a.getIndex("http://u2", "id"));
        eqi("getIndex(uri,local) uri ausente", -1, a.getIndex("http://no", "id"));
        eqi("getIndex(qname)", 2, a.getIndex("lang"));
        eqi("getIndex(qname) ausente", -1, a.getIndex("nope"));

        // El local name suelto NO es un qname: "href" existe como local pero el qname es
        // "h:href", asi que la busqueda por qname no lo encuentra.
        eqi("getIndex(qname) no mira el local", -1, a.getIndex("href"));

        eq("getType(uri,local)", "CDATA", a.getType("http://u2", "href"));
        eq("getType(uri,local) ausente", null, a.getType("http://u2", "id"));
        eq("getType(qname)", "NMTOKEN", a.getType("lang"));
        eq("getType(qname) ausente", null, a.getType("nope"));

        eq("getValue(uri,local)", "x2", a.getValue("http://u2", "href"));
        eq("getValue(uri,local) ausente", null, a.getValue("http://u9", "href"));
        eq("getValue(qname)", "x1", a.getValue("id"));
        eq("getValue(qname) ausente", null, a.getValue("nope"));

        // Sin namespace, el uri es "" y ahi si se encuentra.
        eqi("getIndex('' ,local)", 2, a.getIndex("", "lang"));
    }

    static void mutacion() {
        AttributesImpl a = tres();

        a.setURI(0, "http://nuevo");
        a.setLocalName(0, "otroLocal");
        a.setQName(0, "p:otroLocal");
        a.setType(0, "CDATA");
        a.setValue(0, "nuevoValor");
        eq("setURI", "http://nuevo", a.getURI(0));
        eq("setLocalName", "otroLocal", a.getLocalName(0));
        eq("setQName", "p:otroLocal", a.getQName(0));
        eq("setType", "CDATA", a.getType(0));
        eq("setValue", "nuevoValor", a.getValue(0));

        a.setAttribute(2, "http://u3", "l3", "q3", "ID", "v3");
        eq("setAttribute uri", "http://u3", a.getURI(2));
        eq("setAttribute value", "v3", a.getValue(2));

        // removeAttribute corre el resto hacia abajo.
        a.removeAttribute(0);
        eqi("removeAttribute length", 2, a.getLength());
        eq("removeAttribute corrio", "h:href", a.getQName(0));
        eq("removeAttribute corrio 2", "q3", a.getQName(1));
        eq("y el hueco quedo limpio", null, a.getQName(2));

        // Los setters SI tiran con indice invalido, al reves que los getters.
        eqi("setValue fuera de rango tira", 1, tiraAioobe(a, 9));
        eqi("setValue negativo tira", 1, tiraAioobe(a, -1));
        eqi("removeAttribute fuera de rango tira", 1, tiraAioobeRemove(a, 7));
    }

    static int tiraAioobe(AttributesImpl a, int i) {
        try {
            a.setValue(i, "x");
            return 0;
        } catch (ArrayIndexOutOfBoundsException e) {
            return 1;
        }
    }

    static int tiraAioobeRemove(AttributesImpl a, int i) {
        try {
            a.removeAttribute(i);
            return 0;
        } catch (ArrayIndexOutOfBoundsException e) {
            return 1;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // NamespaceSupport
    ////////////////////////////////////////////////////////////////////

    static void nsBasico() {
        NamespaceSupport ns = new NamespaceSupport();

        // "xml" viene predeclarado y no se puede volver a declarar.
        eq("xml predeclarado", NamespaceSupport.XMLNS, ns.getURI("xml"));
        verdad("declarePrefix('xml') da false",
               !ns.declarePrefix("xml", "http://otra"));
        verdad("declarePrefix('xmlns') da false",
               !ns.declarePrefix("xmlns", "http://otra"));
        eq("xml sigue igual", NamespaceSupport.XMLNS, ns.getURI("xml"));

        eq("XMLNS", "http://www.w3.org/XML/1998/namespace",
           NamespaceSupport.XMLNS);
        eq("NSDECL", "http://www.w3.org/xmlns/2000/", NamespaceSupport.NSDECL);

        ns.pushContext();
        verdad("declarePrefix normal da true",
               ns.declarePrefix("p", "http://p"));
        eq("getURI(p)", "http://p", ns.getURI("p"));
        eq("getPrefix", "p", ns.getPrefix("http://p"));
        eq("getPrefix de uri desconocido", null, ns.getPrefix("http://nada"));
        eq("getURI de prefijo desconocido", null, ns.getURI("q"));

        String[] parts = new String[3];
        String[] r = ns.processName("p:bar", parts, false);
        verdad("processName devuelve el mismo array", r == parts);
        eq("uri", "http://p", parts[0]);
        eq("local", "bar", parts[1]);
        eq("qname", "p:bar", parts[2]);

        // Prefijo sin declarar: null, no excepcion.
        eq("prefijo sin declarar", null,
           ns.processName("q:bar", new String[3], false));

        ns.popContext();
        eq("despues de pop", null, ns.getURI("p"));
    }

    // La regla que mas se lee mal: el prefijo por omision NO alcanza a los atributos.
    static void nsAtributoSinPrefijo() {
        NamespaceSupport ns = new NamespaceSupport();
        ns.pushContext();
        ns.declarePrefix("", "http://porOmision");
        ns.declarePrefix("p", "http://p");

        String[] e = ns.processName("foo", new String[3], false);
        eq("elemento sin prefijo toma la de omision", "http://porOmision", e[0]);
        eq("elemento local", "foo", e[1]);

        String[] a = ns.processName("foo", new String[3], true);
        eq("atributo sin prefijo NO toma la de omision", "", a[0]);
        eq("atributo local", "foo", a[1]);
        eq("atributo qname", "foo", a[2]);

        // Con prefijo, elemento y atributo resuelven igual.
        String[] pe = ns.processName("p:foo", new String[3], false);
        String[] pa = ns.processName("p:foo", new String[3], true);
        eq("elemento con prefijo", "http://p", pe[0]);
        eq("atributo con prefijo", "http://p", pa[0]);

        eq("getURI('')", "http://porOmision", ns.getURI(""));

        // xmlns="" apaga la de omision.
        ns.pushContext();
        ns.declarePrefix("", "");
        eq("xmlns='' apaga", null, ns.getURI(""));
        String[] e2 = ns.processName("foo", new String[3], false);
        eq("y el elemento queda sin namespace", "", e2[0]);
        ns.popContext();

        eq("y al volver reaparece", "http://porOmision", ns.getURI(""));
        ns.popContext();
    }

    static void nsPila() {
        NamespaceSupport ns = new NamespaceSupport();

        ns.pushContext();
        ns.declarePrefix("p", "http://uno");
        eq("nivel 1", "http://uno", ns.getURI("p"));

        // Un hijo que no declara nada hereda.
        ns.pushContext();
        eq("hereda", "http://uno", ns.getURI("p"));

        // Un nieto que redeclara el mismo prefijo tapa al abuelo.
        ns.pushContext();
        ns.declarePrefix("p", "http://dos");
        ns.declarePrefix("q", "http://tres");
        eq("redeclarado", "http://dos", ns.getURI("p"));
        eq("y el nuevo", "http://tres", ns.getURI("q"));

        ns.popContext();
        eq("al salir vuelve el de afuera", "http://uno", ns.getURI("p"));
        eq("y el nuevo desaparece", null, ns.getURI("q"));

        ns.popContext();
        ns.popContext();
        eq("en la raiz no hay nada", null, ns.getURI("p"));
        eq("pero xml sigue", NamespaceSupport.XMLNS, ns.getURI("xml"));

        // Mas hondo que 32 niveles, para forzar el crecimiento del arreglo.
        for (int i = 0; i < 50; i++) {
            ns.pushContext();
            ns.declarePrefix("d", "http://hondo/" + i);
        }
        eq("50 niveles", "http://hondo/49", ns.getURI("d"));
        for (int i = 0; i < 50; i++) {
            ns.popContext();
        }
        eq("y vuelve a nada", null, ns.getURI("d"));

        // reset() deja todo como al principio.
        ns.pushContext();
        ns.declarePrefix("z", "http://z");
        ns.reset();
        eq("reset borra", null, ns.getURI("z"));
        eq("reset repone xml", NamespaceSupport.XMLNS, ns.getURI("xml"));
        verdad("reset apaga namespaceDeclUris", !ns.isNamespaceDeclUris());
    }

    static void nsDeclaraciones() {
        NamespaceSupport ns = new NamespaceSupport();
        ns.pushContext();
        ns.declarePrefix("a", "http://mismo");
        ns.pushContext();
        ns.declarePrefix("b", "http://mismo");
        ns.declarePrefix("c", "http://otro");

        // getDeclaredPrefixes: solo lo declarado por ESTE nivel.
        eq("declarados aca", "b c", ordenado(ns.getDeclaredPrefixes()));

        // getPrefixes(uri): todos los prefijos en alcance que apuntan ahi.
        eq("todos los de http://mismo", "a b",
           ordenado(ns.getPrefixes("http://mismo")));
        eq("los de un uri sin prefijos", "",
           ordenado(ns.getPrefixes("http://nadie")));

        // getPrefixes() sin argumento incluye los heredados y el predeclarado xml.
        String todos = ordenado(ns.getPrefixes());
        verdad("getPrefixes trae a", todos.indexOf("a") >= 0);
        verdad("getPrefixes trae b", todos.indexOf("b") >= 0);
        verdad("getPrefixes trae c", todos.indexOf("c") >= 0);

        ns.popContext();
        eq("declarados del padre", "a", ordenado(ns.getDeclaredPrefixes()));
        ns.popContext();
    }

    static void nsDeclUris() {
        NamespaceSupport apagado = new NamespaceSupport();
        verdad("por omision apagado", !apagado.isNamespaceDeclUris());
        apagado.pushContext();
        String[] a = apagado.processName("xmlns", new String[3], true);
        eq("xmlns apagado", "", a[0]);
        apagado.popContext();

        // Prendido, sobre una instancia limpia: ver mas abajo por que tiene que ser limpia.
        NamespaceSupport ns = new NamespaceSupport();
        ns.setNamespaceDeclUris(true);
        verdad("prendido", ns.isNamespaceDeclUris());
        eq("prender declara xmlns", NamespaceSupport.NSDECL, ns.getURI("xmlns"));
        ns.pushContext();
        String[] b = ns.processName("xmlns", new String[3], true);
        eq("xmlns prendido", NamespaceSupport.NSDECL, b[0]);
        String[] bp = ns.processName("xmlns:p", new String[3], true);
        eq("xmlns:p prendido", NamespaceSupport.NSDECL, bp[0]);
        eq("xmlns:p local", "p", bp[1]);
        // Y un atributo comun sigue sin namespace.
        String[] c = ns.processName("id", new String[3], true);
        eq("id sigue sin namespace", "", c[0]);
        ns.popContext();

        // El "xmlns" de processName se compara por IDENTIDAD contra el literal, no con equals.
        // Un "xmlns" armado en tiempo de ejecucion no dispara la rama y sale sin namespace.
        // Es una rareza del JDK y esta reproducida a proposito.
        NamespaceSupport ident = new NamespaceSupport();
        ident.setNamespaceDeclUris(true);
        ident.pushContext();
        String armado = new StringBuilder("xml").append("ns").toString();
        String[] d = ident.processName(armado, new String[3], true);
        eq("xmlns no internado no entra por la rama", "", d[0]);
        ident.popContext();

        // La otra rareza: processName memoriza por contexto, y la memoria del contexto raiz
        // sobrevive a setNamespaceDeclUris. Preguntar apagado y despues prender deja la
        // respuesta vieja pegada.
        NamespaceSupport pegado = new NamespaceSupport();
        pegado.pushContext();
        pegado.processName("xmlns", new String[3], true);
        pegado.popContext();
        pegado.setNamespaceDeclUris(true);
        pegado.pushContext();
        String[] e = pegado.processName("xmlns", new String[3], true);
        eq("la respuesta cacheada sobrevive a prender", "", e[0]);
        pegado.popContext();

        // Cambiarlo con un elemento abierto no se puede.
        ns.pushContext();
        int tiro = 0;
        try {
            ns.setNamespaceDeclUris(false);
        } catch (IllegalStateException ex) {
            tiro = 1;
        }
        eqi("setNamespaceDeclUris con contexto abierto tira", 1, tiro);
        ns.popContext();

        ns.setNamespaceDeclUris(false);
        verdad("se puede apagar en la raiz", !ns.isNamespaceDeclUris());
        eq("apagar borra el xmlns", null, ns.getURI("xmlns"));
        eq("y xml sigue puesto", NamespaceSupport.XMLNS, ns.getURI("xml"));
    }

    // Los prefijos de una Enumeration, ordenados y separados por espacio, porque el orden en que
    // salen de la tabla no esta especificado y no es lo que se esta probando.
    static String ordenado(Enumeration<String> e) {
        String[] v = new String[64];
        int n = 0;
        while (e.hasMoreElements()) {
            v[n] = e.nextElement();
            n++;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (v[j].compareTo(v[i]) < 0) {
                    String t = v[i];
                    v[i] = v[j];
                    v[j] = t;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(v[i]);
        }
        return sb.toString();
    }
}
