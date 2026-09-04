import java.beans.BeanInfo;
import java.beans.EventHandler;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.IndexedPropertyChangeEvent;
import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.beans.PropertyVetoException;
import java.beans.Statement;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

// Pruebas de comportamiento de java.beans. Prueban lo que el paquete HACE, no que sus metodos
// existan: que los eventos llegan a quien se registro y no a quien se fue, que un veto revierte,
// que el Introspector encuentra los pares get/set de un bean escrito a mano, que Statement y
// Expression ejecutan lo que dicen, y que lo que XMLEncoder escribe XMLDecoder lo vuelve a armar
// —incluido el aliasing y un grafo que se contiene a si mismo—.
//
// El JDK 25 es el oraculo: este mismo fuente tiene que dar -1 con el `java` de verdad. Por eso no
// se prueba nada donde nuestra implementacion se permita divergir a proposito.
public class BeansTest {

    static int failures = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("FALLA: " + what);
        }
    }

    static void same(Object a, Object b, String what) {
        boolean ok = a == null ? b == null : a.equals(b);
        if (!ok) {
            failures++;
            System.out.println("FALLA: " + what + " -- esperaba " + b + " y vino " + a);
        }
    }

    // ------------------------------------------------------------------ beans de prueba

    public static class Person {
        private String name;
        private int age;
        private boolean active;
        private String[] nicknames = new String[0];

        public Person() {
        }

        public String getName() { return this.name; }
        public void setName(String v) { this.name = v; }

        public int getAge() { return this.age; }
        public void setAge(int v) { this.age = v; }

        public boolean isActive() { return this.active; }
        public void setActive(boolean v) { this.active = v; }

        public String[] getNicknames() { return this.nicknames; }
        public void setNicknames(String[] v) { this.nicknames = v; }
        public String getNicknames(int i) { return this.nicknames[i]; }
        public void setNicknames(int i, String v) { this.nicknames[i] = v; }

        // Solo lectura: el Introspector tiene que verla sin metodo de escritura.
        public int getNameLength() { return this.name == null ? 0 : this.name.length(); }
    }

    public static class Counter {
        public int times;
        public String last;

        public Counter() {
        }

        public void bump() { this.times++; }
        public void note(String s) { this.last = s; this.times++; }
    }

    public interface Notice {
        void happened(PropertyChangeEvent e);
    }

    // ------------------------------------------------------------------ PropertyChangeSupport

    static final class Spy implements PropertyChangeListener {
        final List<PropertyChangeEvent> seen = new ArrayList<PropertyChangeEvent>();
        public void propertyChange(PropertyChangeEvent e) { this.seen.add(e); }
        String names() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.seen.size(); i++) {
                sb.append(this.seen.get(i).getPropertyName()).append(';');
            }
            return sb.toString();
        }
    }

    static void testPropertyChangeSupport() {
        Person p = new Person();
        PropertyChangeSupport s = new PropertyChangeSupport(p);

        Spy all = new Spy();
        Spy nameOnly = new Spy();
        Spy dropped = new Spy();

        s.addPropertyChangeListener(all);
        s.addPropertyChangeListener("name", nameOnly);
        s.addPropertyChangeListener(dropped);
        s.removePropertyChangeListener(dropped);

        s.firePropertyChange("name", "ana", "eva");
        s.firePropertyChange("age", Integer.valueOf(1), Integer.valueOf(2));

        same(all.names(), "name;age;", "el oyente general recibe las dos");
        same(nameOnly.names(), "name;", "el oyente por nombre recibe solo la suya");
        same(Integer.valueOf(dropped.seen.size()), Integer.valueOf(0),
            "el oyente quitado no recibe nada");

        PropertyChangeEvent e = all.seen.get(0);
        same(e.getSource(), p, "la fuente del evento es el bean");
        same(e.getOldValue(), "ana", "valor viejo");
        same(e.getNewValue(), "eva", "valor nuevo");

        // Un cambio que no cambia nada no se avisa. Es la regla que evita los ciclos entre dos
        // beans que se escuchan mutuamente.
        int before = all.seen.size();
        s.firePropertyChange("name", "igual", "igual");
        same(Integer.valueOf(all.seen.size()), Integer.valueOf(before),
            "no se avisa cuando el valor no cambio");

        // Con los dos en null si se avisa: null significa "no se sabe cual era".
        s.firePropertyChange("name", null, null);
        same(Integer.valueOf(all.seen.size()), Integer.valueOf(before + 1),
            "con los dos en null si se avisa");

        check(s.hasListeners("name"), "hasListeners ve al general y al de la propiedad");
        check(s.hasListeners("otra"), "hasListeners ve al general para cualquier propiedad");

        // getPropertyChangeListeners() sin argumento devuelve los generales tal cual y los de
        // propiedad envueltos en un proxy.
        PropertyChangeListener[] ls = s.getPropertyChangeListeners();
        same(Integer.valueOf(ls.length), Integer.valueOf(2), "hay dos oyentes registrados");
        PropertyChangeListener[] forName = s.getPropertyChangeListeners("name");
        same(Integer.valueOf(forName.length), Integer.valueOf(1), "un oyente para `name`");
        same(forName[0], nameOnly, "y es el que se registro");

        s.removePropertyChangeListener("name", nameOnly);
        same(Integer.valueOf(s.getPropertyChangeListeners("name").length), Integer.valueOf(0),
            "quitar el oyente por nombre lo saca");

        // Indexada: el evento que llega es un IndexedPropertyChangeEvent con su indice.
        Spy indexed = new Spy();
        PropertyChangeSupport s2 = new PropertyChangeSupport(p);
        s2.addPropertyChangeListener(indexed);
        s2.fireIndexedPropertyChange("nicknames", 3, "a", "b");
        same(Integer.valueOf(indexed.seen.size()), Integer.valueOf(1), "llego el evento indexado");
        PropertyChangeEvent ie = indexed.seen.get(0);
        check(ie instanceof IndexedPropertyChangeEvent, "el evento indexado es de su tipo");
        same(Integer.valueOf(((IndexedPropertyChangeEvent) ie).getIndex()), Integer.valueOf(3),
            "el indice del evento");
    }

    // ------------------------------------------------------------------ VetoableChangeSupport

    static void testVetoableChangeSupport() {
        Person p = new Person();
        VetoableChangeSupport s = new VetoableChangeSupport(p);

        final List<String> seen = new ArrayList<String>();
        VetoableChangeListener watcher = new VetoableChangeListener() {
            public void vetoableChange(PropertyChangeEvent e) {
                seen.add(e.getPropertyName() + ":" + e.getOldValue() + "->" + e.getNewValue());
            }
        };
        VetoableChangeListener vetoer = new VetoableChangeListener() {
            public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException {
                if ("no".equals(e.getNewValue())) {
                    throw new PropertyVetoException("ni loco", e);
                }
            }
        };

        s.addVetoableChangeListener(watcher);
        s.addVetoableChangeListener(vetoer);

        boolean went = true;
        try {
            s.fireVetoableChange("name", "a", "b");
        } catch (PropertyVetoException ex) {
            went = false;
        }
        check(went, "un cambio que nadie veta pasa");
        same(seen.get(0), "name:a->b", "el oyente vio el cambio propuesto");

        seen.clear();
        boolean vetoed = false;
        try {
            s.fireVetoableChange("name", "si", "no");
        } catch (PropertyVetoException ex) {
            vetoed = true;
            same(ex.getPropertyChangeEvent().getPropertyName(), "name",
                "la excepcion trae el evento vetado");
        }
        check(vetoed, "el veto sale como PropertyVetoException");

        // Lo importante del veto: a los que ya avisaron que si, hay que desdecirles. El JDK les
        // manda el evento al reves — de `no` a `si` — para que deshagan lo que hayan hecho.
        same(Integer.valueOf(seen.size()), Integer.valueOf(2),
            "al oyente que ya paso se le avisa la vuelta atras");
        same(seen.get(0), "name:si->no", "primero el cambio propuesto");
        same(seen.get(1), "name:no->si", "y despues el mismo cambio al reves");

        // Un oyente por nombre de propiedad tampoco ve las ajenas.
        final List<String> ageOnly = new ArrayList<String>();
        VetoableChangeListener forAge = new VetoableChangeListener() {
            public void vetoableChange(PropertyChangeEvent e) { ageOnly.add(e.getPropertyName()); }
        };
        s.addVetoableChangeListener("age", forAge);
        try {
            s.fireVetoableChange("name", "x", "y");
            s.fireVetoableChange("age", "1", "2");
        } catch (PropertyVetoException ex) {
            check(false, "no deberia vetarse");
        }
        same(Integer.valueOf(ageOnly.size()), Integer.valueOf(1),
            "el oyente por nombre solo ve su propiedad");
        same(ageOnly.get(0), "age", "y es la suya");

        s.removeVetoableChangeListener(watcher);
        s.removeVetoableChangeListener(vetoer);
        check(!s.hasListeners("name"), "sin oyentes generales no hay quien escuche `name`");
        check(s.hasListeners("age"), "pero el de `age` sigue");
    }

    // ------------------------------------------------------------------ Introspector

    static PropertyDescriptor find(PropertyDescriptor[] ps, String name) {
        PropertyDescriptor r = null;
        for (int i = 0; i < ps.length; i++) {
            if (ps[i].getName().equals(name)) {
                r = ps[i];
            }
        }
        return r;
    }

    static void testIntrospector() throws Exception {
        BeanInfo bi = Introspector.getBeanInfo(Person.class);
        PropertyDescriptor[] ps = bi.getPropertyDescriptors();

        PropertyDescriptor name = find(ps, "name");
        check(name != null, "encontro la propiedad `name`");
        if (name != null) {
            same(name.getPropertyType(), String.class, "el tipo de `name`");
            same(name.getReadMethod().getName(), "getName", "el getter de `name`");
            same(name.getWriteMethod().getName(), "setName", "el setter de `name`");
        }

        PropertyDescriptor age = find(ps, "age");
        check(age != null, "encontro la propiedad `age`");
        if (age != null) {
            same(age.getPropertyType(), int.class, "el tipo de `age` es el primitivo");
        }

        // Un boolean se lee con `isX`, y eso es parte del contrato del Introspector.
        PropertyDescriptor active = find(ps, "active");
        check(active != null, "encontro la propiedad `active`");
        if (active != null) {
            same(active.getPropertyType(), boolean.class, "el tipo de `active`");
            same(active.getReadMethod().getName(), "isActive", "un boolean se lee con isX");
        }

        // Solo lectura: hay getter y no hay setter, y el descriptor tiene que decirlo.
        PropertyDescriptor readOnly = find(ps, "nameLength");
        check(readOnly != null, "encontro la propiedad de solo lectura");
        if (readOnly != null) {
            check(readOnly.getReadMethod() != null, "la de solo lectura tiene getter");
            check(readOnly.getWriteMethod() == null, "y no tiene setter");
        }

        // Indexada: el par get(int)/set(int,x) convive con el par de arreglo entero.
        PropertyDescriptor nicknames = find(ps, "nicknames");
        check(nicknames instanceof IndexedPropertyDescriptor, "`nicknames` es indexada");
        if (nicknames instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ip = (IndexedPropertyDescriptor) nicknames;
            same(ip.getIndexedPropertyType(), String.class, "el tipo del elemento indexado");
            same(ip.getPropertyType(), String[].class, "y el de la propiedad entera");
            same(ip.getIndexedReadMethod().getName(), "getNicknames", "el getter indexado");
            same(ip.getIndexedWriteMethod().getName(), "setNicknames", "el setter indexado");
        }

        // Los descriptores tienen que servir para leer y escribir de verdad, no solo describir.
        Person p = new Person();
        if (name != null) {
            name.getWriteMethod().invoke(p, new Object[] { "zoe" });
            same(name.getReadMethod().invoke(p, new Object[0]), "zoe",
                "escribir y leer por el descriptor");
        }

        // decapitalize es la regla de nombres del paquete, y tiene la excepcion de las siglas.
        same(Introspector.decapitalize("Name"), "name", "decapitalize normal");
        same(Introspector.decapitalize("URL"), "URL", "decapitalize deja las siglas");
        same(Introspector.decapitalize(""), "", "decapitalize de la vacia");
    }

    // ------------------------------------------------------------------ Statement / Expression

    static void testStatementExpression() throws Exception {
        Counter c = new Counter();
        new Statement(c, "bump", new Object[0]).execute();
        new Statement(c, "note", new Object[] { "hola" }).execute();
        same(Integer.valueOf(c.times), Integer.valueOf(2), "los dos Statement se ejecutaron");
        same(c.last, "hola", "y el segundo con su argumento");

        // "new" sobre una Class es el constructor.
        Expression e = new Expression(StringBuilder.class, "new", new Object[] { "abc" });
        Object sb = e.getValue();
        check(sb instanceof StringBuilder, "`new` construyo el objeto");
        same(sb.toString(), "abc", "y con el argumento dado");
        check(e.getValue() == sb, "el valor se calcula una sola vez");

        // Un estatico sobre una Class objetivo.
        same(new Expression(Integer.class, "valueOf", new Object[] { "42" }).getValue(),
            Integer.valueOf(42), "una llamada estatica");

        // Un primitivo declarado acepta su envoltorio: el argumento viaja como Integer y el metodo
        // pide int.
        same(new Expression("abcdef", "substring",
            new Object[] { Integer.valueOf(2), Integer.valueOf(4) }).getValue(), "cd",
            "un int declarado acepta un Integer");

        // `get`/`set` sobre un arreglo son acceso indexado: los arreglos no tienen metodos.
        int[] xs = new int[] { 1, 2, 3 };
        new Statement(xs, "set", new Object[] { Integer.valueOf(1), Integer.valueOf(9) }).execute();
        same(Integer.valueOf(xs[1]), Integer.valueOf(9), "set sobre un arreglo escribe");
        same(new Expression(xs, "get", new Object[] { Integer.valueOf(2) }).getValue(),
            Integer.valueOf(3), "get sobre un arreglo lee");

        // setValue gana sobre la ejecucion: la expresion ya tiene su valor y no llama a nadie.
        Expression fixed = new Expression(new Counter(), "bump", new Object[0]);
        fixed.setValue("puesto");
        same(fixed.getValue(), "puesto", "setValue evita la ejecucion");

        // Un metodo que no existe se dice, no se ignora.
        boolean threw = false;
        try {
            new Statement(c, "noExiste", new Object[0]).execute();
        } catch (Exception ex) {
            threw = true;
        }
        check(threw, "un metodo inexistente tira");
    }

    // ------------------------------------------------------------------ EventHandler

    static void testEventHandler() {
        Counter c = new Counter();

        // Sin propiedad del evento: la accion se llama sin argumentos.
        Notice n1 = EventHandler.create(Notice.class, c, "bump");
        n1.happened(new PropertyChangeEvent(BeansTest.class, "p", "v", "w"));
        same(Integer.valueOf(c.times), Integer.valueOf(1), "el proxy llamo a la accion");

        // Con propiedad del evento: se le pasa lo que el evento devuelva por ese getter.
        Counter c2 = new Counter();
        Notice n2 = EventHandler.create(Notice.class, c2, "note", "propertyName");
        n2.happened(new PropertyChangeEvent(BeansTest.class, "cual", "v", "w"));
        same(c2.last, "cual", "la accion recibio la propiedad del evento");

        // Una ruta con puntos camina propiedad por propiedad.
        Counter c3 = new Counter();
        Notice n3 = EventHandler.create(Notice.class, c3, "note", "source.name");
        n3.happened(new PropertyChangeEvent(BeansTest.class, "p", null, "texto"));
        same(c3.last, "BeansTest", "la ruta con puntos se camina entera");

        // Con nombre de metodo del oyente: solo reacciona a ese.
        Counter c4 = new Counter();
        Notice n4 = EventHandler.create(Notice.class, c4, "bump", null, "otro");
        n4.happened(new PropertyChangeEvent(BeansTest.class, "p", "v", "w"));
        same(Integer.valueOf(c4.times), Integer.valueOf(0),
            "un oyente atado a otro metodo no reacciona");

        // El objeto que sale implementa la interfaz de verdad, que es todo el punto.
        check(n1 instanceof Notice, "el proxy implementa la interfaz pedida");

        // Y los metodos de Object no se confunden con el metodo del oyente.
        Counter c5 = new Counter();
        Notice n5 = EventHandler.create(Notice.class, c5, "bump");
        n5.hashCode();
        n5.toString();
        check(!n5.equals(n1), "equals sobre el proxy es identidad");
        same(Integer.valueOf(c5.times), Integer.valueOf(0),
            "hashCode/toString/equals no disparan la accion");

        // Y el EventHandler que hay adentro se puede leer.
        EventHandler eh = new EventHandler(c, "bump", "source", "happened");
        same(eh.getTarget(), c, "getTarget");
        same(eh.getAction(), "bump", "getAction");
        same(eh.getEventPropertyName(), "source", "getEventPropertyName");
        same(eh.getListenerMethodName(), "happened", "getListenerMethodName");
    }

    // ------------------------------------------------------------------ XMLEncoder / XMLDecoder

    static byte[] ascii(String s) {
        byte[] bs = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            bs[i] = (byte) s.charAt(i);
        }
        return bs;
    }

    static String write(Object o) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLEncoder e = new XMLEncoder(out);
        e.writeObject(o);
        e.close();
        return new String(out.toByteArray());
    }

    static String writeThree(Object a, Object b, Object c) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLEncoder e = new XMLEncoder(out);
        e.writeObject(a);
        e.writeObject(b);
        e.writeObject(c);
        e.close();
        return new String(out.toByteArray());
    }

    static Object[] read(String xml, int howMany) {
        XMLDecoder d = new XMLDecoder(new ByteArrayInputStream(ascii(xml)));
        Object[] r = new Object[howMany];
        for (int i = 0; i < howMany; i++) {
            r[i] = d.readObject();
        }
        d.close();
        return r;
    }

    static String doc(String body) {
        return "<java version=\"1.0\" class=\"java.beans.XMLDecoder\">" + body + "</java>";
    }

    static void testXml() {
        // Ida y vuelta de un bean con propiedades de todos los sabores.
        Person p = new Person();
        p.setName("ana");
        p.setAge(33);
        p.setActive(true);
        p.setNicknames(new String[] { "a", "b" });

        Object[] back = read(write(p), 1);
        check(back[0] instanceof Person, "la vuelta da una Person");
        Person q = (Person) back[0];
        same(q.getName(), "ana", "sobrevivio el String");
        same(Integer.valueOf(q.getAge()), Integer.valueOf(33), "sobrevivio el int");
        same(Boolean.valueOf(q.isActive()), Boolean.TRUE, "sobrevivio el boolean");
        same(Integer.valueOf(q.getNicknames().length), Integer.valueOf(2), "sobrevivio el arreglo");
        same(q.getNicknames()[1], "b", "y su contenido");

        // El aliasing es lo que un formato de grafo tiene que preservar: dos lugares que apuntaban
        // al mismo objeto tienen que seguir apuntando al mismo objeto.
        List<Object> list = new ArrayList<Object>();
        Person shared = new Person();
        shared.setName("comun");
        list.add(shared);
        list.add(shared);
        Object[] back2 = read(write(list), 1);
        check(back2[0] instanceof List, "la vuelta da una lista");
        List<?> l2 = (List<?>) back2[0];
        same(Integer.valueOf(l2.size()), Integer.valueOf(2), "con sus dos elementos");
        check(l2.get(0) == l2.get(1), "y los dos son EL MISMO objeto, no dos iguales");

        // Un grafo que se contiene a si mismo: sin variables perezosas esto no se puede leer.
        List<Object> cycle = new ArrayList<Object>();
        cycle.add("a");
        cycle.add(cycle);
        Object[] back3 = read(write(cycle), 1);
        List<?> l3 = (List<?>) back3[0];
        same(Integer.valueOf(l3.size()), Integer.valueOf(2), "el ciclo volvio con sus dos lugares");
        same(l3.get(0), "a", "el primero es el literal");
        check(l3.get(1) == l3, "y el segundo es la lista misma");

        // Varios objetos en el mismo documento, en orden.
        Object[] three = read(writeThree("uno", Integer.valueOf(2), Boolean.TRUE), 3);
        same(three[0], "uno", "primer objeto del documento");
        same(three[1], Integer.valueOf(2), "segundo");
        same(three[2], Boolean.TRUE, "tercero");

        // Agotado el documento, readObject avisa con ArrayIndexOutOfBoundsException.
        XMLDecoder d = new XMLDecoder(new ByteArrayInputStream(ascii(write("solo"))));
        same(d.readObject(), "solo", "el unico objeto");
        boolean end = false;
        try {
            d.readObject();
        } catch (ArrayIndexOutOfBoundsException ex) {
            end = true;
        }
        check(end, "el fin del documento es un ArrayIndexOutOfBoundsException");
        d.close();

        // El dialecto, escrito a mano. Es el contrato con cualquier archivo que no haya salido de
        // nuestro encoder.
        Object[] prim = read(doc("<int>7</int><boolean>true</boolean><char>q</char>"
            + "<double>1.5</double><long>9</long><float>2.5</float><short>3</short>"
            + "<byte>4</byte><string>hola</string><null/><class>java.lang.Integer</class>"), 11);
        same(prim[0], Integer.valueOf(7), "<int>");
        same(prim[1], Boolean.TRUE, "<boolean>");
        same(prim[2], Character.valueOf('q'), "<char>");
        same(prim[3], Double.valueOf(1.5), "<double>");
        same(prim[4], Long.valueOf(9L), "<long>");
        same(prim[5], Float.valueOf(2.5f), "<float>");
        same(prim[6], Short.valueOf((short) 3), "<short>");
        same(prim[7], Byte.valueOf((byte) 4), "<byte>");
        same(prim[8], "hola", "<string>");
        same(prim[9], null, "<null/>");
        same(prim[10], Integer.class, "<class>");

        // Los enteros se leen con decode: `010` es octal y `0x1f` es hexa.
        Object[] radix = read(doc("<int>010</int><int>0x1f</int>"), 2);
        same(radix[0], Integer.valueOf(8), "un entero con cero adelante es octal");
        same(radix[1], Integer.valueOf(31), "y con 0x es hexa");

        // Un estatico, un constructor implicito y un campo estatico.
        Object[] calls = read(doc(
            "<object class=\"java.lang.Integer\" method=\"valueOf\"><string>42</string></object>"
            + "<object class=\"java.lang.StringBuilder\"><string>eco</string></object>"
            + "<object class=\"java.lang.Integer\" field=\"MAX_VALUE\"/>"), 3);
        same(calls[0], Integer.valueOf(42), "metodo estatico por nombre");
        same(calls[1].toString(), "eco", "sin `method` la llamada es el constructor");
        same(calls[2], Integer.valueOf(Integer.MAX_VALUE), "campo estatico por `field`");

        // Propiedades sobre el objeto contenedor.
        Object[] props = read(doc(
            "<object class=\"BeansTest$Person\">"
            + "<void property=\"name\"><string>zz</string></void>"
            + "<void property=\"age\"><int>5</int></void>"
            + "</object>"), 1);
        Person pp = (Person) props[0];
        same(pp.getName(), "zz", "<void property> escribe la propiedad");
        same(Integer.valueOf(pp.getAge()), Integer.valueOf(5), "y la otra tambien");

        // Arreglos: los `<void index>` ponen cada elemento en su lugar. El `String[]` se mira
        // metiendolo en un bean y el `int[]` pidiendole al propio documento que lo imprima; las
        // dos vueltas evitan escribir `(String[]) x` en la prueba, que en este arbol compila a un
        // .class que la JVM real rechaza (Finding #470: el checkcast a un tipo arreglo no se
        // emite). Que la prueba lo esquive no tapa el bug: esta anotado y tiene su repro.
        Object[] arrays = read(doc(
            "<object class=\"BeansTest$Person\"><void property=\"nicknames\">"
            + "<array class=\"java.lang.String\" length=\"2\">"
            + "<void index=\"0\"><string>a</string></void>"
            + "<void index=\"1\"><string>b</string></void>"
            + "</array></void></object>"
            + "<object class=\"java.util.Arrays\" method=\"toString\">"
            + "<array class=\"int\" length=\"3\"><void index=\"2\"><int>9</int></void></array>"
            + "</object>"), 2);
        Person withArray = (Person) arrays[0];
        same(withArray.getNicknames().getClass(), String[].class, "el arreglo es del tipo pedido");
        same(Integer.valueOf(withArray.getNicknames().length), Integer.valueOf(2),
            "y tiene el largo pedido");
        same(withArray.getNicknames()[1], "b", "con sus elementos");
        same(arrays[1], "[0, 0, 9]",
            "un arreglo primitivo nace del largo pedido y el <void index> pone lo suyo");

        // Un `idref` hacia un objeto que todavia se esta armando: el ciclo escrito a mano.
        Object[] refs = read(doc(
            "<object class=\"java.util.ArrayList\" id=\"l0\">"
            + "<void method=\"add\"><string>x</string></void>"
            + "<void method=\"add\"><object idref=\"l0\"/></void>"
            + "</object>"), 1);
        List<?> rl = (List<?>) refs[0];
        same(Integer.valueOf(rl.size()), Integer.valueOf(2), "la lista con idref");
        check(rl.get(1) == rl, "el idref apunta al objeto que lo contiene");

        // Acceso por indice sobre una lista: `<void index>` es set(i, v).
        Object[] byIndex = read(doc(
            "<object class=\"java.util.ArrayList\">"
            + "<void method=\"add\"><string>a</string></void>"
            + "<void method=\"add\"><string>b</string></void>"
            + "<void index=\"0\"><string>Z</string></void>"
            + "</object>"), 1);
        List<?> il = (List<?>) byIndex[0];
        same(il.get(0), "Z", "<void index> escribe por posicion");
        same(il.get(1), "b", "y no toca al resto");

        // El texto de un `<string>` conserva el orden de lo que tenga adentro.
        Object[] mixed = read(doc("<string>a<int>9</int>b</string>"), 1);
        same(mixed[0], "a9b", "un valor dentro de un <string> se concatena donde esta");

        // `<void>` no aporta un objeto al documento; `<object>` si.
        Object[] onlyOne = read(doc(
            "<void class=\"java.lang.Integer\" method=\"parseInt\"><string>3</string></void>"
            + "<int>1</int>"), 1);
        same(onlyOne[0], Integer.valueOf(1), "un <void> de primer nivel no es un objeto");

        // El owner: `<object property="owner"/>` es getOwner() sobre el valor de <java>.
        Counter owner = new Counter();
        XMLDecoder od = new XMLDecoder(
            new ByteArrayInputStream(ascii(doc("<object property=\"owner\"/>"))), owner);
        same(od.readObject(), owner, "<object property=\"owner\"/> devuelve el owner");
        od.close();
        same(od.getOwner(), owner, "getOwner");

        // Un documento roto no rompe al que lo lee: se avisa y se sigue.
        final List<Exception> reported = new ArrayList<Exception>();
        XMLDecoder bd = new XMLDecoder(
            new ByteArrayInputStream(ascii(doc("<object class=\"no.Existe\"/><int>5</int>"))),
            null,
            new ExceptionListener() {
                public void exceptionThrown(Exception e) { reported.add(e); }
            });
        Object survivor = bd.readObject();
        bd.close();
        check(reported.size() > 0, "una clase que no esta se avisa al ExceptionListener");
        same(survivor, Integer.valueOf(5),
            "y el resto del documento se lee igual: el objeto roto no cuenta");
    }

    // ------------------------------------------------------------------ PropertyEditor

    // Un editor propio: es lo que se puede probar en las dos vidas. Los editores de fabrica del
    // JDK viven en `sun.beans.editors`, que es implementacion y no API, asi que una prueba que los
    // diera por sentados estaria probando el JDK y no el paquete.
    public static class DoublingEditor extends PropertyEditorSupport {
        public DoublingEditor() {
        }
        public void setAsText(String text) {
            this.setValue(Integer.valueOf(Integer.parseInt(text) * 2));
        }
    }

    static void testPropertyEditor() {
        PropertyEditorManager.registerEditor(Person.class, DoublingEditor.class);
        PropertyEditor e = PropertyEditorManager.findEditor(Person.class);
        check(e instanceof DoublingEditor, "findEditor devuelve el editor registrado");
        if (e != null) {
            e.setAsText("17");
            same(e.getValue(), Integer.valueOf(34), "y es el editor de verdad, no otro");
            same(e.getAsText(), "34", "getAsText muestra el valor guardado");
        }
        PropertyEditorManager.registerEditor(Person.class, null);
        same(PropertyEditorManager.findEditor(Person.class), null,
            "registrar null borra el registro");

        PropertyEditorSupport s = new PropertyEditorSupport();
        final int[] fired = new int[1];
        s.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) { fired[0]++; }
        });
        s.setValue("hola");
        same(s.getValue(), "hola", "el editor guarda el valor");
        same(s.getAsText(), "hola", "y lo muestra como texto");
        same(Integer.valueOf(fired[0]), Integer.valueOf(1), "setValue avisa a los oyentes");
        check(!s.isPaintable(), "el editor base no pinta");
        check(!s.supportsCustomEditor(), "ni trae editor propio");
        same(s.getTags(), null, "ni tiene lista de valores");
    }

    // ------------------------------------------------------------------

    public static int run() {
        failures = 0;
        try {
            testPropertyChangeSupport();
            testVetoableChangeSupport();
            testIntrospector();
            testStatementExpression();
            testEventHandler();
            testXml();
            testPropertyEditor();
        } catch (Throwable t) {
            failures++;
            System.out.println("FALLA: excepcion inesperada " + t);
        }
        return failures == 0 ? -1 : failures;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
