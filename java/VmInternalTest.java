import jdk.internal.io.JdkConsole;
import jdk.internal.io.JdkConsoleImpl;
import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;
import jdk.internal.vm.ContinuationSupport;
import jdk.internal.vm.ForeignLinkerSupport;
import jdk.internal.vm.ScopedValueContainer;
import jdk.internal.vm.SharedThreadContainer;
import jdk.internal.vm.StackChunk;
import jdk.internal.vm.StackableScope;
import jdk.internal.vm.ThreadContainer;
import jdk.internal.vm.ThreadContainers;
import jdk.internal.vm.ThreadDumper;
import jdk.internal.vm.VMSupport;
import java.nio.charset.StandardCharsets;

/**
 * `jdk.internal.io` y `jdk.internal.vm`.
 *
 * <p>Lo que se comprueba de {@link StackableScope} es lo que le da sentido: que {@code tryPop}
 * **falle** cuando el ambito no es el de arriba --si siempre saliera bien, el orden no estaria siendo
 * verificado por nadie-- y que {@code popForcefully} limpie lo que quedo encima. Los ambitos anotan su
 * etiqueta al cerrarse, asi que lo que se afirma es el **orden** del desarme y no solo que ocurrio: un
 * booleano por ambito no distingue "de arriba hacia abajo" de "en cualquier orden".
 *
 * <p>De {@link ThreadContainers} se arma un arbol anidado de verdad --padre y nieto apilados-- porque
 * el registro deduce la paternidad de la pila de ambitos, y con un solo contenedor esa deduccion no se
 * ejercita. {@code container(Thread)} tiene que dar con el contenedor que **tiene** el hilo y no con el
 * primero registrado, asi que el padre se deja vacio a proposito.
 *
 * <p>De {@link ThreadDumper} se comprueba que el JSON sea valido con un nombre de hilo hostil
 * --comillas, barras, tabulador, salto, retorno, controles sin escape corto y el DEL-- parseandolo con
 * un parser propio que **rechaza** un caracter de control crudo, y verificando que el nombre vuelva
 * identico. Un escapado que rompe el documento y uno que se come caracteres fallan los dos, y de
 * maneras distintas.
 *
 * <p><strong>NO se corre contra el JDK real, y no es por el `--add-exports`.</strong> Es mas de fondo:
 * `jdk.internal.vm` y `jdk.internal.io` son paquetes de `java.base`, y una clase del classpath en un
 * paquete que un modulo ya define **nunca se carga** --gana la del modulo--. Con
 * `--add-exports java.base/jdk.internal.vm=ALL-UNNAMED` el fuente compila y arranca, pero lo que
 * termina corriendo es `java.base/jdk.internal.io.JdkConsoleImpl` y compania, o sea el JDK probandose
 * a si mismo. Por eso no se agrega a la bateria de dos VMs: no habria nada nuestro bajo prueba.
 *
 * <p>Lo que si se pudo cruzar contra el JDK real es el **formato** de
 * {@code VMSupport.decodeAnnotations}: los bytes del vector de mas abajo los produjo el
 * `encodeAnnotations` del JDK 25 alla, y aca se comprueba que nuestro decodificador los lea igual.
 */
public class VmInternalTest {

    static int fallas = 0;

    // Un lugar donde dejar la continuacion para que su propio cuerpo la pueda mirar.
    static final Continuation[] k2Holder = new Continuation[1];

    static void ok(String que, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + que);
            fallas = fallas + 1;
        }
    }

    static final class Ambito extends StackableScope {
        boolean cerrado;
        protected boolean tryClose() { this.cerrado = true; return true; }
    }

    // Un ambito que anota EN QUE ORDEN se fue cerrando, para poder afirmar que el desarme va de
    // arriba hacia abajo y no al reves. Con un booleano por ambito eso no se puede distinguir.
    static final StringBuilder ordenCierre = new StringBuilder();

    static final class AmbitoOrden extends StackableScope {
        final String etiqueta;
        AmbitoOrden(String etiqueta) { this.etiqueta = etiqueta; }
        protected boolean tryClose() { ordenCierre.append(this.etiqueta); return true; }
    }

    // Un contenedor de hilos de mentira, con nombre y una lista propia, para poder armar un arbol
    // anidado de verdad: el registro deduce el padre de la pila de ambitos, asi que hay que apilar.
    static final class ContFalso extends ThreadContainer {
        final String nombre;
        final java.util.List<Thread> mios = new java.util.ArrayList<Thread>();
        ContFalso(String nombre) { super(false); this.nombre = nombre; }
        public String name() { return this.nombre; }
        public java.util.stream.Stream<Thread> threads() { return this.mios.stream(); }
    }

    // ---- un JSON minimo, para poder decir "es valido" en vez de "parece valido".
    //
    // Se escribe a mano y no se usa una biblioteca a proposito: lo que se esta probando es el
    // escapado de `ThreadDumper`, y comprobarlo con un parser que comparta codigo con el generador
    // no probaria nada. Este acepta el subconjunto que el volcado produce y **rechaza** un texto con
    // un caracter de control crudo o una comilla sin escapar, que es la falla que se busca.
    static final class Json {
        private final String s;
        private int i;
        Json(String s) { this.s = s; }

        static Object parse(String s) {
            Json p = new Json(s);
            p.blancos();
            Object v = p.valor();
            p.blancos();
            if (p.i != p.s.length()) { throw new RuntimeException("sobra texto en " + p.i); }
            return v;
        }

        private void blancos() {
            while (this.i < this.s.length()) {
                char c = this.s.charAt(this.i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') { this.i++; } else { break; }
            }
        }

        private char mirar() {
            if (this.i >= this.s.length()) { throw new RuntimeException("fin inesperado"); }
            return this.s.charAt(this.i);
        }

        private void esperar(char c) {
            if (this.mirar() != c) {
                throw new RuntimeException("se esperaba " + c + " en " + this.i);
            }
            this.i++;
        }

        private Object valor() {
            char c = this.mirar();
            if (c == '{') { return this.objeto(); }
            if (c == '[') { return this.arreglo(); }
            if (c == '"') { return this.texto(); }
            if (this.s.startsWith("null", this.i)) { this.i += 4; return null; }
            if (this.s.startsWith("true", this.i)) { this.i += 4; return Boolean.TRUE; }
            if (this.s.startsWith("false", this.i)) { this.i += 5; return Boolean.FALSE; }
            int j = this.i;
            while (j < this.s.length() && "-+.eE0123456789".indexOf(this.s.charAt(j)) >= 0) { j++; }
            if (j == this.i) { throw new RuntimeException("valor invalido en " + this.i); }
            String n = this.s.substring(this.i, j);
            this.i = j;
            return Double.valueOf(n);
        }

        private java.util.Map<String, Object> objeto() {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
            this.esperar('{');
            this.blancos();
            if (this.mirar() == '}') { this.i++; return m; }
            while (true) {
                this.blancos();
                String k = this.texto();
                this.blancos();
                this.esperar(':');
                this.blancos();
                m.put(k, this.valor());
                this.blancos();
                char c = this.mirar();
                this.i++;
                if (c == '}') { return m; }
                if (c != ',') { throw new RuntimeException("se esperaba , o } en " + this.i); }
            }
        }

        private java.util.List<Object> arreglo() {
            java.util.List<Object> l = new java.util.ArrayList<Object>();
            this.esperar('[');
            this.blancos();
            if (this.mirar() == ']') { this.i++; return l; }
            while (true) {
                this.blancos();
                l.add(this.valor());
                this.blancos();
                char c = this.mirar();
                this.i++;
                if (c == ']') { return l; }
                if (c != ',') { throw new RuntimeException("se esperaba , o ] en " + this.i); }
            }
        }

        private String texto() {
            this.esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = this.mirar();
                this.i++;
                if (c == '"') { return sb.toString(); }
                if (c == '\\') {
                    char e = this.mirar();
                    this.i++;
                    if (e == 'n') { sb.append('\n'); }
                    else if (e == 'r') { sb.append('\r'); }
                    else if (e == 't') { sb.append('\t'); }
                    else if (e == 'b') { sb.append('\b'); }
                    else if (e == 'f') { sb.append('\f'); }
                    else if (e == '"' || e == '\\' || e == '/') { sb.append(e); }
                    else if (e == 'u') {
                        sb.append((char) Integer.parseInt(this.s.substring(this.i, this.i + 4), 16));
                        this.i += 4;
                    } else {
                        throw new RuntimeException("escape invalido \\" + e);
                    }
                } else if (c < ' ') {
                    // Esta es la falla que se busca: un caracter de control crudo adentro de una
                    // cadena es JSON invalido, por mas que el texto "parezca" bien formado.
                    throw new RuntimeException("caracter de control crudo " + (int) c);
                } else {
                    sb.append(c);
                }
            }
        }
    }

    public static int run() throws Exception {
        fallas = 0;

        // ---- jdk.internal.io
        JdkConsole c = new JdkConsoleImpl(StandardCharsets.UTF_8, StandardCharsets.UTF_8);
        ok("charset es el de salida", c.charset() == StandardCharsets.UTF_8);
        ok("writer no es nulo", c.writer() != null);
        ok("reader no es nulo", c.reader() != null);
        ok("readLine en fin de entrada da null", c.readLine() == null);
        ok("readPassword en fin de entrada da null", c.readPassword() == null);
        ok("print encadena", c.print("x") == c);
        ok("println encadena", c.println("x") == c);
        ok("format encadena", c.format(java.util.Locale.ROOT, "%d", Integer.valueOf(1)) == c);
        c.flush();
        ok("reader esta en EOF", c.reader().read() == -1);

        // ---- soporte que esta VM no tiene: la respuesta correcta es "no"
        ok("no hay enlazador nativo", !ForeignLinkerSupport.isSupported());
        ok("no hay continuaciones", !ContinuationSupport.isSupported());
        boolean tiro = false;
        try {
            ContinuationSupport.ensureSupported();
        } catch (UnsupportedOperationException e) {
            tiro = true;
        }
        ok("ensureSupported corta", tiro);
        ContinuationSupport.pinIfSupported();
        ContinuationSupport.unpinIfSupported();

        StackChunk sc = new StackChunk();
        ok("un chunk nuevo esta vacio", sc.isEmpty());
        ok("y no tiene padre", sc.parent() == null);
        StackChunk.init();

        // ---- la pila de ambitos: el orden se verifica de verdad
        Ambito a = new Ambito();
        Ambito b = new Ambito();
        a.push();
        b.push();
        ok("el de abajo ve al de arriba como su envolvente", b.enclosingScope() == a);
        ok("el de abajo no tiene envolvente", a.enclosingScope() == null);
        ok("tryPop del que NO es cabeza falla", !a.tryPop());
        ok("tryPop de la cabeza anda", b.tryPop());
        ok("y ahora si sale el otro", a.tryPop());

        // popForcefully cierra lo que quedo encima
        Ambito x = new Ambito();
        Ambito y = new Ambito();
        x.push();
        y.push();
        ok("popForcefully saca desde abajo", x.popForcefully());
        ok("y cerro al que estaba encima", y.cerrado);

        // enclosingScope(Class) encuentra por tipo
        Ambito z = new Ambito();
        z.push();
        ScopedValueContainer.run(new Runnable() {
            public void run() {
                ok("latest encuentra el contenedor", ScopedValueContainer.latest() != null);
            }
        });
        ok("y despues de correr, no queda ninguno", ScopedValueContainer.latest() == null);
        z.tryPop();
        StackableScope.popAll();

        // La foto de ligaduras es un record: se compara por contenido.
        ScopedValueContainer.BindingsSnapshot s1 = ScopedValueContainer.captureBindings();
        ScopedValueContainer.BindingsSnapshot s2 = ScopedValueContainer.captureBindings();
        ok("dos fotos iguales se comparan iguales", s1.equals(s2));
        ok("y tienen el mismo hash", s1.hashCode() == s2.hashCode());
        ok("el accesor del record anda", s1.scopedValueBindings() == null);

        // ---- contenedores de hilos
        // `false` a proposito: `ThreadGroup` no lleva registro de miembros en esta VM, asi que un
        // hilo suelto no se puede encontrar, y decirlo es la respuesta correcta.
        ok("se avisa que el recorrido es parcial", !ThreadContainers.trackAllThreads());
        ThreadContainer raiz = ThreadContainers.root();
        ok("la raiz existe", raiz != null);
        ok("la raiz no tiene padre", raiz.parent() == null);
        ok("la raiz ve al menos este hilo", raiz.threadCount() >= 1L);

        SharedThreadContainer stc = SharedThreadContainer.create("prueba");
        ok("un contenedor compartido no tiene dueno", stc.owner() == null);
        ok("y arranca vacio", stc.threadCount() == 0L);
        Thread t = new Thread(new Runnable() { public void run() { } });
        stc.start(t);
        t.join();
        stc.onExit(t);
        ok("despues de salir queda vacio", stc.threadCount() == 0L);
        ok("el nombre se conserva", "prueba".equals(stc.name()));
        stc.close();
        stc.close(); // cerrar dos veces no hace nada
        boolean tiro2 = false;
        try {
            stc.start(new Thread(new Runnable() { public void run() { } }));
        } catch (IllegalStateException e) {
            tiro2 = true;
        }
        ok("no se puede arrancar en uno cerrado", tiro2);

        // ---- volcado de hilos
        byte[] texto = new byte[0];
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        ThreadDumper.dumpThreads(bo);
        texto = bo.toByteArray();
        ok("el volcado de texto no esta vacio", texto.length > 0);
        java.io.ByteArrayOutputStream bj = new java.io.ByteArrayOutputStream();
        ThreadDumper.dumpThreadsToJson(bj);
        String json = new String(bj.toByteArray(), StandardCharsets.UTF_8);
        ok("el JSON abre y cierra", json.startsWith("{") && json.trim().endsWith("}"));
        ok("y nombra el hilo actual", json.contains("threadDump"));

        // ---- VMSupport
        ok("propiedades del agente vacias", VMSupport.getAgentProperties().isEmpty());
        byte[] props = VMSupport.serializePropertiesToByteArray();
        ok("las propiedades de sistema serializan", props.length > 0);
        ok("y traen pares clave=valor",
                new String(props, StandardCharsets.UTF_8).indexOf('=') > 0);
        // Esta VM no define `java.io.tmpdir`, y el metodo lo dice en vez de inventar una ruta.
        ok("el temporal es null y no una ruta inventada",
                VMSupport.getVMTemporaryDirectory() == System.getProperty("java.io.tmpdir"));

        // ---- continuaciones: una que nunca se suspende
        ContinuationScope amb = new ContinuationScope("prueba");
        final int[] corrio = new int[1];
        Continuation k = new Continuation(amb, new Runnable() {
            public void run() {
                corrio[0] = corrio[0] + 1;
                // Suspender falla y devuelve false; el cuerpo SIGUE, que es el punto.
                ok("yield no puede suspender", !Continuation.yield(amb));
                ok("y esta clavada", Continuation.isPinned(amb));
                ok("la actual es esta", Continuation.getCurrentContinuation(amb) == k2Holder[0]);
                corrio[0] = corrio[0] + 1;
            }
        });
        k2Holder[0] = k;
        ok("el ambito se conserva", k.getScope() == amb);
        ok("arranca sin terminar", !k.isDone());
        k.run();
        ok("corrio hasta el final", corrio[0] == 2);
        ok("y quedo terminada", k.isDone());
        ok("nunca fue desalojada", !k.isPreempted());
        ok("desalojar no se soporta",
                k.tryPreempt(Thread.currentThread()) == Continuation.PreemptStatus.PERM_FAIL_UNSUPPORTED);
        ok("y ese fallo es permanente, no por clavado",
                Continuation.PreemptStatus.PERM_FAIL_UNSUPPORTED.pinned() == null);
        ok("un fallo transitorio si trae el motivo",
                Continuation.PreemptStatus.TRANSIENT_FAIL_PINNED_MONITOR.pinned()
                        == Continuation.Pinned.MONITOR);
        ok("fuera de toda continuacion no hay ninguna montada",
                Continuation.getCurrentContinuation(amb) == null);
        boolean dosVeces = false;
        try {
            k.run();
        } catch (IllegalStateException e) {
            dosVeces = true;
        }
        ok("no se puede correr dos veces", dosVeces);
        Continuation.pin();
        Continuation.unpin();

        // ---- el desarme cuando el cuerpo tira: el ORDEN, no solo que se cierre
        //
        // Con un booleano por ambito no se puede distinguir "cerro de arriba hacia abajo" de "cerro
        // en cualquier orden", y el orden es justamente la promesa de la clase. Por eso los ambitos
        // anotan su etiqueta al cerrarse y se compara la cadena entera.
        ordenCierre.setLength(0);
        AmbitoOrden o1 = new AmbitoOrden("1");
        AmbitoOrden o2 = new AmbitoOrden("2");
        AmbitoOrden o3 = new AmbitoOrden("3");
        o1.push();
        o2.push();
        o3.push();
        boolean tiroCuerpo = false;
        try {
            throw new IllegalStateException("el cuerpo se rompio con 2 y 3 abiertos");
        } catch (IllegalStateException e) {
            tiroCuerpo = true;
            ok("popForcefully desde el de abajo devuelve true", o1.popForcefully());
        }
        ok("el cuerpo tiro", tiroCuerpo);
        ok("cerro de arriba hacia abajo y solo lo de encima", "32".equals(ordenCierre.toString()));
        ok("y el que forzo NO se cierra a si mismo", ordenCierre.indexOf("1") < 0);
        ok("la pila quedo vacia", StackableScope.head() == null);
        ok("popForcefully sobre uno que ya salio da false", !o1.popForcefully());

        // tryPop que falla tiene que dejar la pila INTACTA, no a medio desarmar.
        AmbitoOrden p1 = new AmbitoOrden("a");
        AmbitoOrden p2 = new AmbitoOrden("b");
        p1.push();
        p2.push();
        ok("tryPop del de abajo falla", !p1.tryPop());
        ok("y no cerro nada", ordenCierre.indexOf("a") < 0 && ordenCierre.indexOf("b") < 0);
        ok("la cabeza sigue siendo la misma", StackableScope.head() == p2);
        ok("y el de abajo sigue siendo su envolvente", p2.enclosingScope() == p1);
        ok("despues del fallo, la cabeza si sale", p2.tryPop());
        ok("y el de abajo tambien", p1.tryPop());
        ok("un tryPop que salio bien no cierra", ordenCierre.indexOf("a") < 0);

        // popAll si cierra todo lo que haya, incluido el de mas abajo.
        ordenCierre.setLength(0);
        AmbitoOrden q1 = new AmbitoOrden("x");
        AmbitoOrden q2 = new AmbitoOrden("y");
        q1.push();
        q2.push();
        StackableScope.popAll();
        ok("popAll cierra todo, de arriba hacia abajo", "yx".equals(ordenCierre.toString()));
        ok("y deja la pila vacia", StackableScope.head() == null);

        // ---- el arbol de contenedores, anidado de verdad
        ContFalso padre = new ContFalso("padre");
        ContFalso hijo = new ContFalso("hijo");
        Object kPadre = ThreadContainers.registerContainer(padre);
        padre.push();
        Object kHijo = ThreadContainers.registerContainer(hijo);
        hijo.push();
        ok("el hijo ve al padre como su contenedor envolvente", hijo.parent() == padre);
        ok("el padre cuelga de la raiz", padre.parent() == ThreadContainers.root());
        ok("la raiz no tiene padre", ThreadContainers.root().parent() == null);
        ok("el padre tiene un solo hijo, y es ese",
                padre.children().count() == 1L
                        && padre.children().anyMatch(c -> c == hijo));
        ok("el hijo no tiene hijos", hijo.children().count() == 0L);
        ok("la raiz tiene al padre entre sus hijos",
                ThreadContainers.root().children().anyMatch(c -> c == padre));
        ok("la raiz NO tiene al nieto como hijo directo",
                !ThreadContainers.root().children().anyMatch(c -> c == hijo));

        // container(Thread) tiene que dar con el contenedor correcto, no con el primero registrado.
        Thread hiloDelHijo = new Thread(new Runnable() { public void run() { } }, "delHijo");
        hijo.mios.add(hiloDelHijo);
        ok("el hilo se encuentra en el contenedor que lo tiene",
                ThreadContainers.container(hiloDelHijo) == hijo);
        ok("y no en el padre, que esta vacio", padre.mios.isEmpty());
        Thread suelto = new Thread(new Runnable() { public void run() { } }, "suelto");
        ok("un hilo que no esta en ninguno cae en la raiz",
                ThreadContainers.container(suelto) == ThreadContainers.root());
        ok("el nombre del contenedor se conserva", "hijo".equals(hijo.name()));
        ok("y toString lo usa", hijo.toString().startsWith("hijo@"));
        ok("threadCount cuenta lo que hay", hijo.threadCount() == 1L);

        hijo.tryPop();
        ThreadContainers.deregisterContainer(kHijo);
        padre.tryPop();
        ThreadContainers.deregisterContainer(kPadre);
        ok("dado de baja, ya no figura como hijo de la raiz",
                !ThreadContainers.root().children().anyMatch(c -> c == padre));
        ok("y el hilo vuelve a caer en la raiz",
                ThreadContainers.container(hiloDelHijo) == ThreadContainers.root());
        StackableScope.popAll();

        // ---- el volcado JSON con nombres hostiles
        //
        // El nombre de un hilo lo elige el programa, asi que puede traer comillas, saltos de linea y
        // caracteres de control. Lo que se comprueba no es que el texto "parezca" JSON sino que un
        // parser lo acepte Y que el nombre vuelva **igual** al que se puso: un escapado que rompe el
        // documento y uno que se come caracteres fallan los dos, y de maneras distintas.
        String nombreOriginal = Thread.currentThread().getName();
        // Comilla, barra, tabulador, salto, retorno, dos controles sin escape corto --que
        // obligan a la forma \u00xx-- y el DEL.
        String hostil = "co\"mi\\lla\ty\nsalto\r" + ((char) 1) + "ctrl" + ((char) 31)
                + "y" + ((char) 127) + "del";
        try {
            Thread.currentThread().setName(hostil);
            java.io.ByteArrayOutputStream bh = new java.io.ByteArrayOutputStream();
            ThreadDumper.dumpThreadsToJson(bh);
            String jh = new String(bh.toByteArray(), StandardCharsets.UTF_8);

            Object raiz2 = null;
            boolean parseo = false;
            try {
                raiz2 = Json.parse(jh);
                parseo = true;
            } catch (RuntimeException e) {
                System.out.println("  el JSON no parsea: " + e.getMessage());
            }
            ok("el volcado con nombre hostil sigue siendo JSON valido", parseo);

            if (parseo) {
                java.util.Map<String, Object> m =
                        (java.util.Map<String, Object>) ((java.util.Map<String, Object>) raiz2)
                                .get("threadDump");
                ok("tiene el objeto threadDump", m != null);
                java.util.List<Object> conts =
                        (java.util.List<Object>) m.get("threadContainers");
                ok("tiene contenedores", conts != null && conts.size() == 1);
                java.util.Map<String, Object> c0 =
                        (java.util.Map<String, Object>) conts.get(0);
                java.util.List<Object> hilos2 = (java.util.List<Object>) c0.get("threads");
                // Esta es la asercion que antes no existia y que el volcado no cumplia: la lista de
                // hilos NO puede estar vacia mientras este hilo la esta pidiendo.
                ok("el volcado trae al menos el hilo que lo pide",
                        hilos2 != null && hilos2.size() >= 1);
                boolean encontrado = false;
                if (hilos2 != null) {
                    for (Object h : hilos2) {
                        java.util.Map<String, Object> hm = (java.util.Map<String, Object>) h;
                        if (hostil.equals(hm.get("name"))) { encontrado = true; }
                    }
                }
                ok("y el nombre hostil vuelve caracter por caracter", encontrado);
            }
        } finally {
            Thread.currentThread().setName(nombreOriginal);
        }

        // ---- VMSupport.decodeAnnotations
        //
        // Los bytes se arman a mano siguiendo el formato del JDK, no con nuestro codificador --que no
        // existe, y por eso el decodificador se prueba contra la especificacion y no contra si mismo--.
        Dec dec = new Dec();
        java.io.ByteArrayOutputStream be = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream de = new java.io.DataOutputStream(be);
        largo(de, 2);                       // dos anotaciones

        de.writeUTF("Marca");
        largo(de, 8);
        de.writeUTF("s"); de.writeByte('s'); de.writeUTF("ho\"la");
        de.writeUTF("i"); de.writeByte('I'); de.writeInt(42);
        de.writeUTF("j"); de.writeByte('J'); de.writeLong(9999999999L);
        de.writeUTF("z"); de.writeByte('Z'); de.writeBoolean(true);
        de.writeUTF("c"); de.writeByte('c'); de.writeUTF("java.lang.String");
        de.writeUTF("e"); de.writeByte('e'); de.writeUTF("Color"); de.writeUTF("ROJO");
        de.writeUTF("x"); de.writeByte('x'); de.writeUTF("no se pudo");
        de.writeUTF("a"); de.writeByte('@');
        de.writeUTF("Anidada"); largo(de, 1);
        de.writeUTF("b"); de.writeByte('B'); de.writeByte(7);

        // La segunda ejercita los arreglos, incluido el largo en CUATRO bytes: 200 > 127, que es la
        // rama del formato que el caso comun nunca toca.
        de.writeUTF("Arreglos");
        largo(de, 3);
        de.writeUTF("ints"); de.writeByte('['); de.writeByte('I');
        largo(de, 200);
        for (int i = 0; i < 200; i++) { de.writeInt(i); }
        de.writeUTF("enums"); de.writeByte('['); de.writeByte('e');
        de.writeUTF("Color"); largo(de, 2); de.writeUTF("ROJO"); de.writeUTF("AZUL");
        de.writeUTF("textos"); de.writeByte('['); de.writeByte('s');
        largo(de, 2); de.writeUTF("uno"); de.writeUTF("dos");
        de.flush();

        java.util.List<String> decs = VMSupport.decodeAnnotations(be.toByteArray(), dec);
        ok("decodifica las dos anotaciones", decs.size() == 2);
        String a0 = decs.get(0);
        ok("el tipo pasa por resolveType", a0.startsWith("T:Marca{"));
        ok("texto", a0.contains("s=ho\"la"));
        ok("int", a0.contains("i=42"));
        ok("long", a0.contains("j=9999999999"));
        ok("boolean", a0.contains("z=true"));
        ok("la clase pasa por resolveType", a0.contains("c=T:java.lang.String"));
        ok("el enum pasa por newEnumValue", a0.contains("e=E:T:Color.ROJO"));
        ok("el valor irresoluble pasa por newErrorValue", a0.contains("x=X:no se pudo"));
        ok("la anotacion anidada se decodifica entera", a0.contains("a=T:Anidada{b=7}"));
        String a1 = decs.get(1);
        ok("el arreglo largo usa la forma de cuatro bytes y sale entero",
                a1.contains("ints=[0, 1, 2,") && a1.contains(", 199]"));
        ok("arreglo de enums, con el tipo leido una sola vez",
                a1.contains("enums=[E:T:Color.ROJO, E:T:Color.AZUL]"));
        ok("arreglo de textos", a1.contains("textos=[uno, dos]"));
        ok("una lista vacia de anotaciones decodifica a una lista vacia",
                VMSupport.decodeAnnotations(new byte[] { (byte) 0x80 }, dec).isEmpty());
        boolean etiquetaMala = false;
        try {
            VMSupport.decodeAnnotations(new byte[] { (byte) 0x81, (byte) 0, (byte) 1, (byte) 'A',
                    (byte) 0x81, (byte) 0, (byte) 1, (byte) 'm', (byte) '?' }, dec);
        } catch (InternalError e) {
            etiquetaMala = true;
        }
        ok("una etiqueta desconocida es InternalError, no un valor inventado", etiquetaMala);


        // ---- el mismo formato, contra la implementacion de referencia
        //
        // Estos bytes NO los escribio nadie a mano: los produjo el `encodeAnnotations` del JDK 25 de
        // verdad, corriendo en la JVM real con `--add-exports java.base/jdk.internal.vm=ALL-UNNAMED`
        // (el generador quedo en `scratchpad/zz350/RealEnc2.java`). Es la prueba que las de arriba no
        // pueden dar: aquellas comparan nuestro decodificador contra nuestra lectura de la
        // especificacion, y esta lo compara contra el codificador que tiene que entenderlo del otro
        // lado.
        //
        // Cubre el formato ENTERO: los ocho primitivos, texto, clase, enum y anotacion anidada, mas
        // un arreglo de cada uno de ellos --incluidos `[@` y `[c`, que son los que ningun ejemplo
        // corto toca-- y las dos formas del largo, la de un byte y la de cuatro.
        byte[] deVerdad = new byte[] {
                (byte) -127, (byte) 0, (byte) 13, (byte) 82, (byte) 101, (byte) 97, (byte) 108,
                (byte) 69, (byte) 110, (byte) 99, (byte) 50, (byte) 36, (byte) 84, (byte) 111,
                (byte) 100, (byte) 111, (byte) -104, (byte) 0, (byte) 1, (byte) 98, (byte) 66,
                (byte) 7, (byte) 0, (byte) 1, (byte) 99, (byte) 67, (byte) 0, (byte) 107, (byte) 0,
                (byte) 1, (byte) 100, (byte) 68, (byte) 64, (byte) 4, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 102, (byte) 70, (byte) 63,
                (byte) -64, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 105, (byte) 73,
                (byte) 0, (byte) 0, (byte) 0, (byte) 42, (byte) 0, (byte) 1, (byte) 106, (byte) 74,
                (byte) 0, (byte) 0, (byte) 0, (byte) 2, (byte) 84, (byte) 11, (byte) -29,
                (byte) -1, (byte) 0, (byte) 1, (byte) 115, (byte) 83, (byte) 1, (byte) 44,
                (byte) 0, (byte) 1, (byte) 122, (byte) 90, (byte) 1, (byte) 0, (byte) 3,
                (byte) 116, (byte) 120, (byte) 116, (byte) 115, (byte) 0, (byte) 5, (byte) 104,
                (byte) 111, (byte) 34, (byte) 108, (byte) 97, (byte) 0, (byte) 3, (byte) 99,
                (byte) 108, (byte) 115, (byte) 99, (byte) 0, (byte) 16, (byte) 106, (byte) 97,
                (byte) 118, (byte) 97, (byte) 46, (byte) 108, (byte) 97, (byte) 110, (byte) 103,
                (byte) 46, (byte) 83, (byte) 116, (byte) 114, (byte) 105, (byte) 110, (byte) 103,
                (byte) 0, (byte) 1, (byte) 101, (byte) 101, (byte) 0, (byte) 14, (byte) 82,
                (byte) 101, (byte) 97, (byte) 108, (byte) 69, (byte) 110, (byte) 99, (byte) 50,
                (byte) 36, (byte) 67, (byte) 111, (byte) 108, (byte) 111, (byte) 114, (byte) 0,
                (byte) 4, (byte) 82, (byte) 79, (byte) 74, (byte) 79, (byte) 0, (byte) 2,
                (byte) 97, (byte) 110, (byte) 64, (byte) 0, (byte) 16, (byte) 82, (byte) 101,
                (byte) 97, (byte) 108, (byte) 69, (byte) 110, (byte) 99, (byte) 50, (byte) 36,
                (byte) 65, (byte) 110, (byte) 105, (byte) 100, (byte) 97, (byte) 100, (byte) 97,
                (byte) -127, (byte) 0, (byte) 1, (byte) 98, (byte) 66, (byte) 1, (byte) 0,
                (byte) 2, (byte) 97, (byte) 98, (byte) 91, (byte) 66, (byte) -126, (byte) 1,
                (byte) 2, (byte) 0, (byte) 2, (byte) 97, (byte) 99, (byte) 91, (byte) 67,
                (byte) -126, (byte) 0, (byte) 120, (byte) 0, (byte) 121, (byte) 0, (byte) 2,
                (byte) 97, (byte) 100, (byte) 91, (byte) 68, (byte) -126, (byte) 63, (byte) -32,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 63, (byte) -8,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 2,
                (byte) 97, (byte) 102, (byte) 91, (byte) 70, (byte) -126, (byte) 64, (byte) 32,
                (byte) 0, (byte) 0, (byte) 64, (byte) 96, (byte) 0, (byte) 0, (byte) 0, (byte) 2,
                (byte) 97, (byte) 105, (byte) 91, (byte) 73, (byte) -125, (byte) 0, (byte) 0,
                (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 2, (byte) 0, (byte) 0,
                (byte) 0, (byte) 3, (byte) 0, (byte) 2, (byte) 97, (byte) 106, (byte) 91,
                (byte) 74, (byte) -126, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 10, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                (byte) 0, (byte) 20, (byte) 0, (byte) 2, (byte) 97, (byte) 115, (byte) 91,
                (byte) 83, (byte) -126, (byte) 0, (byte) 4, (byte) 0, (byte) 5, (byte) 0, (byte) 2,
                (byte) 97, (byte) 122, (byte) 91, (byte) 90, (byte) -126, (byte) 1, (byte) 0,
                (byte) 0, (byte) 4, (byte) 97, (byte) 116, (byte) 120, (byte) 116, (byte) 91,
                (byte) 115, (byte) -126, (byte) 0, (byte) 3, (byte) 117, (byte) 110, (byte) 111,
                (byte) 0, (byte) 3, (byte) 100, (byte) 111, (byte) 115, (byte) 0, (byte) 4,
                (byte) 97, (byte) 99, (byte) 108, (byte) 115, (byte) 91, (byte) 99, (byte) -126,
                (byte) 0, (byte) 16, (byte) 106, (byte) 97, (byte) 118, (byte) 97, (byte) 46,
                (byte) 108, (byte) 97, (byte) 110, (byte) 103, (byte) 46, (byte) 83, (byte) 116,
                (byte) 114, (byte) 105, (byte) 110, (byte) 103, (byte) 0, (byte) 17, (byte) 106,
                (byte) 97, (byte) 118, (byte) 97, (byte) 46, (byte) 108, (byte) 97, (byte) 110,
                (byte) 103, (byte) 46, (byte) 73, (byte) 110, (byte) 116, (byte) 101, (byte) 103,
                (byte) 101, (byte) 114, (byte) 0, (byte) 2, (byte) 97, (byte) 101, (byte) 91,
                (byte) 101, (byte) 0, (byte) 14, (byte) 82, (byte) 101, (byte) 97, (byte) 108,
                (byte) 69, (byte) 110, (byte) 99, (byte) 50, (byte) 36, (byte) 67, (byte) 111,
                (byte) 108, (byte) 111, (byte) 114, (byte) -126, (byte) 0, (byte) 4, (byte) 82,
                (byte) 79, (byte) 74, (byte) 79, (byte) 0, (byte) 4, (byte) 65, (byte) 90,
                (byte) 85, (byte) 76, (byte) 0, (byte) 3, (byte) 97, (byte) 97, (byte) 110,
                (byte) 91, (byte) 64, (byte) -126, (byte) 0, (byte) 16, (byte) 82, (byte) 101,
                (byte) 97, (byte) 108, (byte) 69, (byte) 110, (byte) 99, (byte) 50, (byte) 36,
                (byte) 65, (byte) 110, (byte) 105, (byte) 100, (byte) 97, (byte) 100, (byte) 97,
                (byte) -127, (byte) 0, (byte) 1, (byte) 98, (byte) 66, (byte) 2, (byte) 0,
                (byte) 16, (byte) 82, (byte) 101, (byte) 97, (byte) 108, (byte) 69, (byte) 110,
                (byte) 99, (byte) 50, (byte) 36, (byte) 65, (byte) 110, (byte) 105, (byte) 100,
                (byte) 97, (byte) 100, (byte) 97, (byte) -127, (byte) 0, (byte) 1, (byte) 98,
                (byte) 66, (byte) 3
        };
        java.util.List<String> refs = VMSupport.decodeAnnotations(deVerdad, dec);
        ok("los bytes del JDK real decodifican a una sola anotacion", refs.size() == 1);
        ok("y dan exactamente lo que el decodificador del JDK da con ellos",
                ("T:RealEnc2$Todo{b=7, c=k, d=2.5, f=1.5, i=42, j=9999999999, s=300, z=true, "
                        + "txt=ho\"la, cls=T:java.lang.String, e=E:T:RealEnc2$Color.ROJO, "
                        + "an=T:RealEnc2$Anidada{b=1}, ab=[1, 2], ac=[x, y], ad=[0.5, 1.5], af=[2.5, "
                        + "3.5], ai=[1, 2, 3], aj=[10, 20], as=[4, 5], az=[true, false], atxt=[uno, "
                        + "dos], acls=[T:java.lang.String, T:java.lang.Integer], "
                        + "ae=[E:T:RealEnc2$Color.ROJO, E:T:RealEnc2$Color.AZUL], "
                        + "aan=[T:RealEnc2$Anidada{b=2}, T:RealEnc2$Anidada{b=3}]}").equals(refs.get(0)));

        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    // El largo del formato: un byte con el bit alto prendido si entra en siete bits, y si no un int.
    static void largo(java.io.DataOutputStream d, int n) throws Exception {
        if (n <= 127) {
            d.writeByte((byte) (0x80 | n));
        } else {
            d.writeInt(n);
        }
    }

    // Un decodificador que no fabrica anotaciones sino texto. Es exactamente el caso que justifica
    // que la interfaz exista: leer anotaciones sin cargar ninguna de las clases que mencionan.
    static final class Dec implements VMSupport.AnnotationDecoder<String, String, String, String> {
        public String resolveType(String name) { return "T:" + name; }
        public String newAnnotation(String type, java.util.Map.Entry<String, Object>[] elements) {
            StringBuilder sb = new StringBuilder(type).append('{');
            for (int i = 0; i < elements.length; i++) {
                if (i > 0) { sb.append(", "); }
                sb.append(elements[i].getKey()).append('=').append(elements[i].getValue());
            }
            return sb.append('}').toString();
        }
        public String newEnumValue(String enumType, String name) {
            return "E:" + enumType + "." + name;
        }
        public String newErrorValue(String description) { return "X:" + description; }
    }

    public static void main(String[] a) throws Exception {
        System.out.println("VmInternalTest " + VmInternalTest.run());
    }
}
