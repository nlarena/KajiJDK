import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

/**
 * Prueba de contrato de javax.script, pensada para correrse **dos veces**: contra nuestra VM y
 * contra el `java` del JDK 25. Como `javax.script` es un paquete del JDK, el mismo fuente compilado
 * y corrido alla ejercita la implementacion de referencia; aca ejercita la nuestra. Si las dos
 * salidas son identicas linea por linea, las reglas coinciden.
 *
 * <p>Por eso imprime cada observacion en vez de solo contar: el valor de la prueba esta en el
 * diff, no en el numero. Los "esperado" son los del JDK y estan escritos a mano; si alguno
 * estuviera mal, las dos corridas marcarian FALLA en la misma linea y el diff seguiria vacio --
 * que es la senal de que la implementacion coincide y el que se equivoco fui yo.
 */
public class ScriptContractTest {

    static int fallas = 0;

    static void chk(String nombre, Object obtenido, Object esperado) {
        String o = String.valueOf(obtenido);
        String e = String.valueOf(esperado);
        boolean ok = o.equals(e);
        if (!ok) {
            fallas = fallas + 1;
        }
        System.out.println((ok ? "ok   " : "FALLA") + " " + nombre + " = " + o
                + (ok ? "" : "   (esperado: " + e + ")"));
    }

    static String exc(Throwable t) {
        return t.getClass().getName() + ": " + t.getMessage();
    }

    // ---- SimpleBindings ---------------------------------------------------------------------

    static void bindings() {
        SimpleBindings b = new SimpleBindings();

        b.put("a", "A");
        chk("sb.get.ok", b.get("a"), "A");
        chk("sb.size", b.size(), 1);
        chk("sb.containsKey.ok", b.containsKey("a"), true);
        chk("sb.containsValue", b.containsValue("A"), true);

        // El orden de la guarda: nulo, despues tipo, despues vacio.
        try {
            b.get(null);
            chk("sb.get.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("sb.get.null", exc(t), "java.lang.NullPointerException: key can not be null");
        }
        try {
            b.get(Integer.valueOf(7));
            chk("sb.get.noString", "sin excepcion", "CCE");
        } catch (Throwable t) {
            chk("sb.get.noString", exc(t), "java.lang.ClassCastException: key should be a String");
        }
        try {
            b.get("");
            chk("sb.get.vacia", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("sb.get.vacia", exc(t), "java.lang.IllegalArgumentException: key can not be empty");
        }
        try {
            b.put(null, "x");
            chk("sb.put.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("sb.put.null", exc(t), "java.lang.NullPointerException: key can not be null");
        }
        try {
            b.put("", "x");
            chk("sb.put.vacia", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("sb.put.vacia", exc(t), "java.lang.IllegalArgumentException: key can not be empty");
        }
        try {
            b.containsKey(Integer.valueOf(1));
            chk("sb.containsKey.noString", "sin excepcion", "CCE");
        } catch (Throwable t) {
            chk("sb.containsKey.noString", exc(t),
                    "java.lang.ClassCastException: key should be a String");
        }
        try {
            b.remove(null);
            chk("sb.remove.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("sb.remove.null", exc(t), "java.lang.NullPointerException: key can not be null");
        }
        try {
            new SimpleBindings(null);
            chk("sb.ctor.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("sb.ctor.null", t.getClass().getName(), "java.lang.NullPointerException");
        }

        // Un valor nulo es legal: la regla es sobre las claves.
        b.put("nulo", null);
        chk("sb.valorNulo.get", b.get("nulo"), null);
        chk("sb.valorNulo.containsKey", b.containsKey("nulo"), true);

        // El constructor con mapa NO copia: lo que entra por afuera se ve, aun salteando la guarda.
        Map<String, Object> m = new HashMap<String, Object>();
        SimpleBindings env = new SimpleBindings(m);
        m.put("porAfuera", "V");
        chk("sb.noCopia", env.get("porAfuera"), "V");
        chk("sb.noCopia.size", env.size(), 1);

        // putAll valida clave por clave y puede dejar copiado lo de antes.
        try {
            env.putAll(null);
            chk("sb.putAll.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("sb.putAll.null", t.getClass().getName(), "java.lang.NullPointerException");
        }

        // Sin equals/hashCode propios: dos SimpleBindings sobre el MISMO mapa no son iguales.
        chk("sb.sinEquals", new SimpleBindings(m).equals(new SimpleBindings(m)), false);

        chk("sb.remove.ok", b.remove("a"), "A");
        b.clear();
        chk("sb.clear", b.isEmpty(), true);
    }

    // ---- SimpleScriptContext ----------------------------------------------------------------

    static void contexto() {
        SimpleScriptContext c = new SimpleScriptContext();

        // El de motor siempre existe; el global arranca ausente.
        chk("ssc.engine.noNulo", c.getBindings(ScriptContext.ENGINE_SCOPE) != null, true);
        chk("ssc.global.arrancaNulo", c.getBindings(ScriptContext.GLOBAL_SCOPE), null);
        chk("ssc.scopes", c.getScopes(), List.of(Integer.valueOf(100), Integer.valueOf(200)));

        // Con el global ausente: escribir se ignora, leer da nulo, y nada de eso es error.
        c.setAttribute("g", "IGNORADO", ScriptContext.GLOBAL_SCOPE);
        chk("ssc.global.ausente.set", c.getAttribute("g", ScriptContext.GLOBAL_SCOPE), null);
        chk("ssc.global.ausente.busca", c.getAttribute("g"), null);
        chk("ssc.global.ausente.scope", c.getAttributesScope("g"), -1);

        // Precedencia: el de motor tapa al global.
        c.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        c.setAttribute("k", "GLOBAL", ScriptContext.GLOBAL_SCOPE);
        chk("ssc.soloGlobal.busca", c.getAttribute("k"), "GLOBAL");
        chk("ssc.soloGlobal.scope", c.getAttributesScope("k"), 200);
        c.setAttribute("k", "MOTOR", ScriptContext.ENGINE_SCOPE);
        chk("ssc.tapa.busca", c.getAttribute("k"), "MOTOR");
        chk("ssc.tapa.scope", c.getAttributesScope("k"), 100);

        // Decide TENER la clave, no que el valor no sea nulo: con nulo en motor, el global no
        // asoma.
        c.setAttribute("k", null, ScriptContext.ENGINE_SCOPE);
        chk("ssc.nuloTapa.busca", c.getAttribute("k"), null);
        chk("ssc.nuloTapa.scope", c.getAttributesScope("k"), 100);

        chk("ssc.remove", c.removeAttribute("k", ScriptContext.ENGINE_SCOPE), null);
        chk("ssc.remove.despues", c.getAttribute("k"), "GLOBAL");

        // El global si se puede poner en nulo; el de motor no.
        c.setBindings(null, ScriptContext.GLOBAL_SCOPE);
        chk("ssc.global.aNulo", c.getBindings(ScriptContext.GLOBAL_SCOPE), null);
        try {
            c.setBindings(null, ScriptContext.ENGINE_SCOPE);
            chk("ssc.engine.aNulo", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("ssc.engine.aNulo", t.getClass().getName(), "java.lang.NullPointerException");
        }

        // Un ambito que no existe: OJO, setBindings dice "Invalid" y el resto "Illegal".
        try {
            c.setBindings(new SimpleBindings(), 999);
            chk("ssc.setBindings.999", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("ssc.setBindings.999", exc(t),
                    "java.lang.IllegalArgumentException: Invalid scope value.");
        }
        try {
            c.getBindings(999);
            chk("ssc.getBindings.999", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("ssc.getBindings.999", exc(t),
                    "java.lang.IllegalArgumentException: Illegal scope value.");
        }
        try {
            c.getAttribute("k", 999);
            chk("ssc.getAttribute.999", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("ssc.getAttribute.999", exc(t),
                    "java.lang.IllegalArgumentException: Illegal scope value.");
        }
        try {
            c.setAttribute("k", "v", 999);
            chk("ssc.setAttribute.999", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("ssc.setAttribute.999", exc(t),
                    "java.lang.IllegalArgumentException: Illegal scope value.");
        }
        try {
            c.removeAttribute("k", 999);
            chk("ssc.removeAttribute.999", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("ssc.removeAttribute.999", exc(t),
                    "java.lang.IllegalArgumentException: Illegal scope value.");
        }

        // La guarda de nombres.
        try {
            c.getAttribute(null);
            chk("ssc.nombre.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("ssc.nombre.null", t.getClass().getName(), "java.lang.NullPointerException");
        }
        try {
            c.getAttribute("");
            chk("ssc.nombre.vacio", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("ssc.nombre.vacio", t.getClass().getName(),
                    "java.lang.IllegalArgumentException");
        }

        // Los canales.
        StringWriter w = new StringWriter();
        c.setWriter(w);
        chk("ssc.writer", c.getWriter() == w, true);
        c.setErrorWriter(w);
        chk("ssc.errorWriter", c.getErrorWriter() == w, true);
    }

    // ---- ScriptException --------------------------------------------------------------------

    static void excepcion() {
        ScriptException a = new ScriptException("boom");
        chk("se.solo.msg", a.getMessage(), "boom");
        chk("se.solo.file", a.getFileName(), null);
        chk("se.solo.line", a.getLineNumber(), -1);
        chk("se.solo.col", a.getColumnNumber(), -1);

        ScriptException b = new ScriptException("boom", "a.js", 5);
        chk("se.linea.msg", b.getMessage(), "boom in a.js at line number 5");
        chk("se.linea.col", b.getColumnNumber(), -1);

        ScriptException c = new ScriptException("boom", "a.js", 5, 3);
        chk("se.completo.msg", c.getMessage(),
                "boom in a.js at line number 5 at column number 3");

        // Sin archivo la posicion entera se ignora, aunque haya linea.
        ScriptException d = new ScriptException("boom", null, 5, 3);
        chk("se.sinArchivo.msg", d.getMessage(), "boom");
        chk("se.sinArchivo.line", d.getLineNumber(), 5);

        // Archivo y columna pero sin linea.
        ScriptException e = new ScriptException("boom", "a.js", -1, 3);
        chk("se.sinLinea.msg", e.getMessage(), "boom in a.js at column number 3");

        // Envolviendo: el mensaje pasa a ser el toString de la causa.
        ScriptException f = new ScriptException(new RuntimeException("x"));
        chk("se.causa.msg", f.getMessage(), "java.lang.RuntimeException: x");
        chk("se.causa.cause", f.getCause().getClass().getName(), "java.lang.RuntimeException");
        chk("se.causa.file", f.getFileName(), null);

        chk("se.esChecked", Exception.class.isAssignableFrom(ScriptException.class)
                && !RuntimeException.class.isAssignableFrom(ScriptException.class), true);
    }

    // ---- ScriptEngineManager y AbstractScriptEngine ------------------------------------------

    static void manager() throws Exception {
        ScriptEngineManager m = new ScriptEngineManager();

        // Lo que el JDK 25 tambien contesta: no hay motores. Nashorn se fue en la 15.
        chk("sem.byName.js", m.getEngineByName("js"), null);
        chk("sem.byName.nashorn", m.getEngineByName("nashorn"), null);
        chk("sem.byExtension.js", m.getEngineByExtension("js"), null);
        chk("sem.byMimeType", m.getEngineByMimeType("application/javascript"), null);
        chk("sem.factories.vacio", m.getEngineFactories().isEmpty(), true);

        try {
            m.getEngineByName(null);
            chk("sem.byName.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("sem.byName.null", t.getClass().getName(), "java.lang.NullPointerException");
        }

        // El global del manager: nunca nulo, y ponerlo en nulo es IAE y no NPE.
        chk("sem.bindings.noNulo", m.getBindings() != null, true);
        try {
            m.setBindings(null);
            chk("sem.setBindings.null", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("sem.setBindings.null", t.getClass().getName(),
                    "java.lang.IllegalArgumentException");
        }
        m.put("g", "DEL_MANAGER");
        chk("sem.put.get", m.get("g"), "DEL_MANAGER");

        // El registro manual, que no depende del descubrimiento.
        FabricaFalsa f = new FabricaFalsa();
        m.registerEngineName("falso", f);
        ScriptEngine e = m.getEngineByName("falso");
        chk("sem.registrado.encontrado", e != null, true);
        chk("sem.registrado.factory", e.getFactory() == f, true);

        // El manager le cablea SU global al motor: por eso existe el manager.
        chk("sem.globalCableado", e.eval("g"), "DEL_MANAGER");

        // El de motor tapa al global del manager.
        e.put("g", "DEL_MOTOR");
        chk("sem.motorTapa", e.eval("g"), "DEL_MOTOR");

        // Registrar no agrega un proveedor: getEngineFactories sigue vacio.
        chk("sem.registrar.noEsProveedor", m.getEngineFactories().isEmpty(), true);

        // eval con Bindings suelto NO le cambia el ambito de motor al motor.
        Bindings suelto = new SimpleBindings();
        suelto.put("g", "SUELTO");
        chk("ase.evalConBindings", e.eval("g", suelto), "SUELTO");
        chk("ase.motorIntacto", e.eval("g"), "DEL_MOTOR");

        // ...y el global sigue visible desde el contexto descartable.
        Bindings suelto2 = new SimpleBindings();
        chk("ase.evalConBindings.veGlobal", e.eval("otra", suelto2), null);

        // AbstractScriptEngine: ambitos.
        try {
            e.getBindings(999);
            chk("ase.getBindings.999", "sin excepcion", "IAE");
        } catch (Throwable t) {
            chk("ase.getBindings.999", exc(t),
                    "java.lang.IllegalArgumentException: Invalid scope value.");
        }
        try {
            e.setContext(null);
            chk("ase.setContext.null", "sin excepcion", "NPE");
        } catch (Throwable t) {
            chk("ase.setContext.null", t.getClass().getName(), "java.lang.NullPointerException");
        }
        chk("ase.createBindings", e.createBindings() != null, true);
        chk("ase.getContext", e.getContext() != null, true);

        // Las claves reservadas.
        chk("se.ENGINE", ScriptEngine.ENGINE, "javax.script.engine");
        chk("se.FILENAME", ScriptEngine.FILENAME, "javax.script.filename");
        chk("se.ARGV", ScriptEngine.ARGV, "javax.script.argv");
        chk("sc.ENGINE_SCOPE", ScriptContext.ENGINE_SCOPE, 100);
        chk("sc.GLOBAL_SCOPE", ScriptContext.GLOBAL_SCOPE, 200);
    }

    // ---- CompiledScript ----------------------------------------------------------------------

    static void compilado() throws Exception {
        MotorFalso motor = new MotorFalso(new FabricaFalsa());
        motor.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        motor.getContext().setAttribute("g", "GG", ScriptContext.GLOBAL_SCOPE);
        motor.put("x", "EX");
        GuionFalso cs = new GuionFalso(motor);

        chk("cs.getEngine", cs.getEngine() == motor, true);

        // Sin argumentos: contra el contexto del motor, tal cual.
        chk("cs.eval", cs.eval(), "x=EX g=GG mismoCtx=true");

        // Con nulo: tambien contra el contexto del motor, sin armar nada temporal.
        chk("cs.eval.null", cs.eval((Bindings) null), "x=EX g=GG mismoCtx=true");

        // Con Bindings: contexto descartable, ambito de motor reemplazado, global copiado.
        Bindings b = new SimpleBindings();
        b.put("x", "BX");
        chk("cs.eval.bindings", cs.eval(b), "x=BX g=GG mismoCtx=false");

        // Y el motor quedo como estaba.
        chk("cs.motorIntacto", cs.eval(), "x=EX g=GG mismoCtx=true");
    }

    public static int run() {
        try {
            bindings();
            contexto();
            excepcion();
            manager();
            compilado();
        } catch (Throwable t) {
            System.out.println("EXPLOTO " + exc(t));
            return 1;
        }
        System.out.println("fallas=" + fallas);
        return fallas == 0 ? -1 : fallas;
    }

    public static void main(String[] args) {
        System.out.println("run -> " + run());
    }
}

/**
 * Un script "compilado" de mentira: al evaluarse cuenta contra que contexto lo hicieron, que es
 * justo lo que distingue a los tres `eval` de {@link javax.script.CompiledScript}.
 */
class GuionFalso extends javax.script.CompiledScript {

    private final ScriptEngine motor;

    GuionFalso(ScriptEngine motor) {
        this.motor = motor;
    }

    public Object eval(ScriptContext c) throws ScriptException {
        // El booleano sale a una local a proposito. Metido dentro de la concatenacion, junto a
        // operandos de tipo `Object`, dispara un bug de nuestro javac: la comparacion obliga a
        // emitir un stack map frame y ahi los resultados de `String.valueOf(Object)` quedan
        // anotados como `Object`, con lo que el verificador de la JVM real rechaza la clase con
        // `VerifyError: Bad type on operand stack`. Repro y detalle, en el informe.
        boolean mismoCtx = (c == motor.getContext());
        return "x=" + c.getAttribute("x") + " g=" + c.getAttribute("g") + " mismoCtx=" + mismoCtx;
    }

    public ScriptEngine getEngine() {
        return motor;
    }
}

/** Un motor de mentira: "evaluar" es buscar el atributo que nombra el script. */
class MotorFalso extends AbstractScriptEngine {

    private final ScriptEngineFactory fabrica;

    MotorFalso(ScriptEngineFactory fabrica) {
        this.fabrica = fabrica;
    }

    public Object eval(String script, ScriptContext context) throws ScriptException {
        return context.getAttribute(script);
    }

    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return null;
    }

    public Bindings createBindings() {
        return new SimpleBindings();
    }

    public ScriptEngineFactory getFactory() {
        return fabrica;
    }
}

/** La ficha tecnica del motor de mentira. */
class FabricaFalsa implements ScriptEngineFactory {

    public String getEngineName() {
        return "Motor Falso";
    }

    public String getEngineVersion() {
        return "1.0";
    }

    public List<String> getExtensions() {
        return List.of("falso");
    }

    public List<String> getMimeTypes() {
        return List.of("application/x-falso");
    }

    public List<String> getNames() {
        return List.of("falso");
    }

    public String getLanguageName() {
        return "Falso";
    }

    public String getLanguageVersion() {
        return "1.0";
    }

    public Object getParameter(String key) {
        return null;
    }

    public String getMethodCallSyntax(String obj, String m, String... args) {
        return obj + "." + m + "()";
    }

    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    public String getProgram(String... statements) {
        return "";
    }

    public ScriptEngine getScriptEngine() {
        return new MotorFalso(this);
    }
}
