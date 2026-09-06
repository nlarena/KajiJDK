import java.util.LinkedHashSet;
import java.util.Set;

import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SourceCodeAnalysis;

/**
 * Comprueba {@code jdk.jshell} contra el JDK 25.
 *
 * <h2>Que se puede comparar</h2>
 *
 * <p>Las cinco enumeraciones enteras, con sus preguntas: cuales clases de fragmento son
 * persistentes, cuales situaciones cuentan como activas, cual subclase es ejecutable y cual deja un
 * valor. Eso es la mayor parte del paquete y es aritmetica sobre tablas, asi que da lo mismo tenga o
 * no la biblioteca un compilador.
 *
 * <p>Del motor se compara el ciclo de vida: que una sesion recien creada este vacia, con que error
 * falla cada metodo con argumentos nulos, y que cambia despues de cerrar. Lo que no se compara es
 * evaluar, porque para eso hace falta un compilador en proceso que esta biblioteca no tiene.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class JSH3 {

    static final String[] ESPERADO = {
        "Kind|IMPORT|0|true|true",
        "Kind|TYPE_DECL|1|true|true",
        "Kind|METHOD|2|true|true",
        "Kind|VAR|3|true|true",
        "Kind|EXPRESSION|4|false|true",
        "Kind|STATEMENT|5|false|true",
        "Kind|ERRONEOUS|6|false|true",
        "Status|VALID|0|true|true",
        "Status|RECOVERABLE_DEFINED|1|true|true",
        "Status|RECOVERABLE_NOT_DEFINED|2|true|false",
        "Status|DROPPED|3|false|false",
        "Status|OVERWRITTEN|4|false|false",
        "Status|REJECTED|5|false|false",
        "Status|NONEXISTENT|6|false|false",
        "SubKind|SINGLE_TYPE_IMPORT_SUBKIND|0|false|false|IMPORT",
        "SubKind|TYPE_IMPORT_ON_DEMAND_SUBKIND|1|false|false|IMPORT",
        "SubKind|SINGLE_STATIC_IMPORT_SUBKIND|2|false|false|IMPORT",
        "SubKind|STATIC_IMPORT_ON_DEMAND_SUBKIND|3|false|false|IMPORT",
        "SubKind|MODULE_IMPORT_SUBKIND|4|false|false|IMPORT",
        "SubKind|CLASS_SUBKIND|5|false|false|TYPE_DECL",
        "SubKind|INTERFACE_SUBKIND|6|false|false|TYPE_DECL",
        "SubKind|ENUM_SUBKIND|7|false|false|TYPE_DECL",
        "SubKind|RECORD_SUBKIND|8|false|false|TYPE_DECL",
        "SubKind|ANNOTATION_TYPE_SUBKIND|9|false|false|TYPE_DECL",
        "SubKind|METHOD_SUBKIND|10|false|false|METHOD",
        "SubKind|VAR_DECLARATION_SUBKIND|11|true|true|VAR",
        "SubKind|VAR_DECLARATION_WITH_INITIALIZER_SUBKIND|12|true|true|VAR",
        "SubKind|TEMP_VAR_EXPRESSION_SUBKIND|13|true|true|VAR",
        "SubKind|VAR_VALUE_SUBKIND|14|true|true|EXPRESSION",
        "SubKind|ASSIGNMENT_SUBKIND|15|true|true|EXPRESSION",
        "SubKind|OTHER_EXPRESSION_SUBKIND|16|true|true|EXPRESSION",
        "SubKind|STATEMENT_SUBKIND|17|true|false|STATEMENT",
        "SubKind|UNKNOWN_SUBKIND|18|false|false|ERRONEOUS",
        "Compl|COMPLETE|0|true",
        "Compl|COMPLETE_WITH_SEMI|1|true",
        "Compl|DEFINITELY_INCOMPLETE|2|false",
        "Compl|CONSIDERED_INCOMPLETE|3|false",
        "Compl|EMPTY|4|false",
        "Compl|UNKNOWN|5|true",
        "Attr|DECLARATION|0",
        "Attr|DEPRECATED|1",
        "Attr|KEYWORD|2",
        "NOPOS|-1",
        "Highlight|3|9|[KEYWORD]",
        "Highlight-eq|true|false",
        "cadena|truetruetruetruetruetruetruetruetruetruetrue",
        "creado|true",
        "vacio|0|0|0|0|0",
        "sca|true",
        "status-nulo|NullPointerException",
        "drop-nulo|NullPointerException",
        "varValue-nulo|NullPointerException",
        "diag-nulo|NullPointerException",
        "unres-nulo|NullPointerException",
        "onEvent-nulo|NullPointerException",
        "onShutdown-nulo|NullPointerException",
        "unsub-nulo|NullPointerException",
        "classpath-nulo|NullPointerException",
        "stop|ok",
        "unsub-ajena|ok",
        "cierre|1",
        "eval-cerrado|IllegalStateException",
        "snippets-cerrado|ok",
        "sca-cerrado|ok",
        "sub-cerrado|IllegalStateException",
        "classpath-cerrado|IllegalStateException",
    };

    /** Lo que hace el paquete, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        final Snippet.Kind[] ks = Snippet.Kind.values();
        for (int i = 0; i < ks.length; i++) {
            a.add("Kind|" + ks[i] + "|" + ks[i].ordinal() + "|" + ks[i].isPersistent()
                    + "|" + Snippet.Kind.valueOf(ks[i].name()).equals(ks[i]));
        }

        final Snippet.Status[] ss = Snippet.Status.values();
        for (int i = 0; i < ss.length; i++) {
            a.add("Status|" + ss[i] + "|" + ss[i].ordinal() + "|" + ss[i].isActive()
                    + "|" + ss[i].isDefined());
        }

        final Snippet.SubKind[] sk = Snippet.SubKind.values();
        for (int i = 0; i < sk.length; i++) {
            a.add("SubKind|" + sk[i] + "|" + sk[i].ordinal() + "|" + sk[i].isExecutable()
                    + "|" + sk[i].hasValue() + "|" + sk[i].kind());
        }

        final SourceCodeAnalysis.Completeness[] cs = SourceCodeAnalysis.Completeness.values();
        for (int i = 0; i < cs.length; i++) {
            a.add("Compl|" + cs[i] + "|" + cs[i].ordinal() + "|" + cs[i].isComplete());
        }

        final SourceCodeAnalysis.Attribute[] at = SourceCodeAnalysis.Attribute.values();
        for (int i = 0; i < at.length; i++) {
            a.add("Attr|" + at[i] + "|" + at[i].ordinal());
        }

        a.add("NOPOS|" + Diag.NOPOS);

        final Set<SourceCodeAnalysis.Attribute> uno =
                new LinkedHashSet<SourceCodeAnalysis.Attribute>();
        uno.add(SourceCodeAnalysis.Attribute.KEYWORD);
        final SourceCodeAnalysis.Highlight h = new SourceCodeAnalysis.Highlight(3, 9, uno);
        a.add("Highlight|" + h.start() + "|" + h.end() + "|" + h.attributes());
        final Set<SourceCodeAnalysis.Attribute> otro =
                new LinkedHashSet<SourceCodeAnalysis.Attribute>();
        otro.add(SourceCodeAnalysis.Attribute.KEYWORD);
        a.add("Highlight-eq|" + h.equals(new SourceCodeAnalysis.Highlight(3, 9, otro))
                + "|" + h.equals(new SourceCodeAnalysis.Highlight(4, 9, otro)));

        // El constructor: todo devuelve el mismo constructor, para encadenar.
        final JShell.Builder b = JShell.builder();
        a.add("cadena|" + (b.in(null) == b) + (b.out(null) == b) + (b.err(null) == b)
                + (b.console(null) == b) + (b.tempVariableNameGenerator(null) == b)
                + (b.idGenerator(null) == b) + (b.remoteVMOptions() == b)
                + (b.compilerOptions() == b) + (b.executionEngine("local") == b)
                + (b.executionEngine(null, null) == b) + (b.fileManager(null) == b));

        // Una sesion recien creada esta vacia.
        final JShell js = JShell.create();
        a.add("creado|" + (js != null));
        a.add("vacio|" + js.snippets().count() + "|" + js.variables().count()
                + "|" + js.methods().count() + "|" + js.types().count()
                + "|" + js.imports().count());
        a.add("sca|" + (js.sourceCodeAnalysis() != null));

        a.add("status-nulo|" + intentar(new StatusNulo(js)));
        a.add("drop-nulo|" + intentar(new DropNulo(js)));
        a.add("varValue-nulo|" + intentar(new VarValueNulo(js)));
        a.add("diag-nulo|" + intentar(new DiagNulo(js)));
        a.add("unres-nulo|" + intentar(new UnresNulo(js)));
        a.add("onEvent-nulo|" + intentar(new OnEventNulo(js)));
        a.add("onShutdown-nulo|" + intentar(new OnShutdownNulo(js)));
        a.add("unsub-nulo|" + intentar(new UnsubNulo(js)));
        a.add("classpath-nulo|" + intentar(new ClasspathNulo(js)));
        a.add("stop|" + intentar(new Parar(js)));

        // Un identificador de otro interprete no hace nada.
        final JShell otroJs = JShell.create();
        final JShell.Subscription ajena = otroJs.onSnippetEvent(new Nada());
        a.add("unsub-ajena|" + intentar(new UnsubAjena(js, ajena)));

        // Los avisos de cierre llegan una sola vez.
        final int[] cuenta = new int[1];
        js.onShutdown(new Cuenta(cuenta));
        js.close();
        js.close();
        a.add("cierre|" + cuenta[0]);

        a.add("eval-cerrado|" + intentar(new EvalCerrado(js)));
        a.add("snippets-cerrado|" + intentar(new SnippetsCerrado(js)));
        a.add("sca-cerrado|" + intentar(new ScaCerrado(js)));
        a.add("sub-cerrado|" + intentar(new SubCerrado(js)));
        a.add("classpath-cerrado|" + intentar(new ClasspathCerrado(js)));

        otroJs.close();
        return a.toArray(new String[a.size()]);
    }

    /** Corre eso y devuelve "ok" o el nombre simple de lo que haya tirado. */
    static String intentar(Runnable r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    /** Un oyente que no hace nada. */
    static class Nada implements java.util.function.Consumer<jdk.jshell.SnippetEvent> {
        public void accept(jdk.jshell.SnippetEvent e) {
        }
    }

    /** Un oyente de cierre que cuenta cuantas veces le avisaron. */
    static class Cuenta implements java.util.function.Consumer<JShell> {
        private final int[] n;

        Cuenta(int[] n) {
            this.n = n;
        }

        public void accept(JShell j) {
            this.n[0]++;
        }
    }

    /** La base de las pruebas que necesitan el interprete. */
    abstract static class Con implements Runnable {
        final JShell js;

        Con(JShell js) {
            this.js = js;
        }
    }

    static class StatusNulo extends Con {
        StatusNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.status(null);
        }
    }

    static class DropNulo extends Con {
        DropNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.drop(null);
        }
    }

    static class VarValueNulo extends Con {
        VarValueNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.varValue(null);
        }
    }

    static class DiagNulo extends Con {
        DiagNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.diagnostics(null).count();
        }
    }

    static class UnresNulo extends Con {
        UnresNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.unresolvedDependencies(null).count();
        }
    }

    static class OnEventNulo extends Con {
        OnEventNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.onSnippetEvent(null);
        }
    }

    static class OnShutdownNulo extends Con {
        OnShutdownNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.onShutdown(null);
        }
    }

    static class UnsubNulo extends Con {
        UnsubNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.unsubscribe(null);
        }
    }

    static class ClasspathNulo extends Con {
        ClasspathNulo(JShell js) {
            super(js);
        }

        public void run() {
            js.addToClasspath(null);
        }
    }

    static class Parar extends Con {
        Parar(JShell js) {
            super(js);
        }

        public void run() {
            js.stop();
        }
    }

    static class UnsubAjena extends Con {
        private final JShell.Subscription s;

        UnsubAjena(JShell js, JShell.Subscription s) {
            super(js);
            this.s = s;
        }

        public void run() {
            js.unsubscribe(this.s);
        }
    }

    static class EvalCerrado extends Con {
        EvalCerrado(JShell js) {
            super(js);
        }

        public void run() {
            js.eval("1+1");
        }
    }

    static class SnippetsCerrado extends Con {
        SnippetsCerrado(JShell js) {
            super(js);
        }

        public void run() {
            js.snippets().count();
        }
    }

    static class ScaCerrado extends Con {
        ScaCerrado(JShell js) {
            super(js);
        }

        public void run() {
            js.sourceCodeAnalysis();
        }
    }

    static class SubCerrado extends Con {
        SubCerrado(JShell js) {
            super(js);
        }

        public void run() {
            js.onSnippetEvent(new Nada());
        }
    }

    static class ClasspathCerrado extends Con {
        ClasspathCerrado(JShell js) {
            super(js);
        }

        public void run() {
            js.addToClasspath(".");
        }
    }

    /**
     * El indice de la primera respuesta que no coincide con la del JDK, o -1.
     *
     * @return el indice, o -1
     */
    public static int donde() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != ESPERADO.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = donde();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
