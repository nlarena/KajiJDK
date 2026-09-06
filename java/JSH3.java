import java.util.LinkedHashSet;
import java.util.Set;

import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SourceCodeAnalysis;

/**
 * Checks {@code jdk.jshell} against JDK 25.
 *
 * <h2>What can be compared</h2>
 *
 * <p>All five enums in full, with their questions: which kinds of snippet are persistent, which
 * states count as active, which subkind is executable and which leaves a value. That is most of the
 * package and it is arithmetic over tables, so it answers the same whether or not the library has a
 * compiler.
 *
 * <p>Of the engine, the life cycle is compared: that a freshly created session is empty, which error
 * each method fails with on null arguments, and what changes after closing. What is not compared is
 * evaluating, because that needs an in-process compiler this library does not have.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class JSH3 {

    static final String[] EXPECTED = {
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
        "chaining|truetruetruetruetruetruetruetruetruetruetrue",
        "created|true",
        "empty|0|0|0|0|0",
        "sca|true",
        "status-null|NullPointerException",
        "drop-null|NullPointerException",
        "varValue-null|NullPointerException",
        "diag-null|NullPointerException",
        "unres-null|NullPointerException",
        "onEvent-null|NullPointerException",
        "onShutdown-null|NullPointerException",
        "unsub-null|NullPointerException",
        "classpath-null|NullPointerException",
        "stop|ok",
        "unsub-foreign|ok",
        "shutdown|1",
        "eval-closed|IllegalStateException",
        "snippets-closed|ok",
        "sca-closed|ok",
        "sub-closed|IllegalStateException",
        "classpath-closed|IllegalStateException",
    };

    /** What the package does, one line per check. */
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

        final Set<SourceCodeAnalysis.Attribute> one =
                new LinkedHashSet<SourceCodeAnalysis.Attribute>();
        one.add(SourceCodeAnalysis.Attribute.KEYWORD);
        final SourceCodeAnalysis.Highlight h = new SourceCodeAnalysis.Highlight(3, 9, one);
        a.add("Highlight|" + h.start() + "|" + h.end() + "|" + h.attributes());
        final Set<SourceCodeAnalysis.Attribute> other =
                new LinkedHashSet<SourceCodeAnalysis.Attribute>();
        other.add(SourceCodeAnalysis.Attribute.KEYWORD);
        a.add("Highlight-eq|" + h.equals(new SourceCodeAnalysis.Highlight(3, 9, other))
                + "|" + h.equals(new SourceCodeAnalysis.Highlight(4, 9, other)));

        // The builder: everything returns the same builder, so calls can be chained.
        final JShell.Builder b = JShell.builder();
        a.add("chaining|" + (b.in(null) == b) + (b.out(null) == b) + (b.err(null) == b)
                + (b.console(null) == b) + (b.tempVariableNameGenerator(null) == b)
                + (b.idGenerator(null) == b) + (b.remoteVMOptions() == b)
                + (b.compilerOptions() == b) + (b.executionEngine("local") == b)
                + (b.executionEngine(null, null) == b) + (b.fileManager(null) == b));

        // A freshly created session is empty.
        final JShell js = JShell.create();
        a.add("created|" + (js != null));
        a.add("empty|" + js.snippets().count() + "|" + js.variables().count()
                + "|" + js.methods().count() + "|" + js.types().count()
                + "|" + js.imports().count());
        a.add("sca|" + (js.sourceCodeAnalysis() != null));

        a.add("status-null|" + attempt(new StatusNull(js)));
        a.add("drop-null|" + attempt(new DropNull(js)));
        a.add("varValue-null|" + attempt(new VarValueNull(js)));
        a.add("diag-null|" + attempt(new DiagNull(js)));
        a.add("unres-null|" + attempt(new UnresNull(js)));
        a.add("onEvent-null|" + attempt(new OnEventNull(js)));
        a.add("onShutdown-null|" + attempt(new OnShutdownNull(js)));
        a.add("unsub-null|" + attempt(new UnsubNull(js)));
        a.add("classpath-null|" + attempt(new ClasspathNull(js)));
        a.add("stop|" + attempt(new Stop(js)));

        // A token from another interpreter does nothing.
        final JShell otherJs = JShell.create();
        final JShell.Subscription foreign = otherJs.onSnippetEvent(new Nothing());
        a.add("unsub-foreign|" + attempt(new UnsubForeign(js, foreign)));

        // The shutdown notifications arrive once only.
        final int[] count = new int[1];
        js.onShutdown(new Count(count));
        js.close();
        js.close();
        a.add("shutdown|" + count[0]);

        a.add("eval-closed|" + attempt(new EvalClosed(js)));
        a.add("snippets-closed|" + attempt(new SnippetsClosed(js)));
        a.add("sca-closed|" + attempt(new ScaClosed(js)));
        a.add("sub-closed|" + attempt(new SubClosed(js)));
        a.add("classpath-closed|" + attempt(new ClasspathClosed(js)));

        otherJs.close();
        return a.toArray(new String[a.size()]);
    }

    /** Runs it and returns "ok" or the simple name of whatever it threw. */
    static String attempt(Runnable r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    /** A listener that does nothing. */
    static class Nothing implements java.util.function.Consumer<jdk.jshell.SnippetEvent> {
        public void accept(jdk.jshell.SnippetEvent e) {
        }
    }

    /** A shutdown listener that counts how many times it was told. */
    static class Count implements java.util.function.Consumer<JShell> {
        private final int[] n;

        Count(int[] n) {
            this.n = n;
        }

        public void accept(JShell j) {
            this.n[0]++;
        }
    }

    /** The base of the checks that need the interpreter. */
    abstract static class With implements Runnable {
        final JShell js;

        With(JShell js) {
            this.js = js;
        }
    }

    static class StatusNull extends With {
        StatusNull(JShell js) {
            super(js);
        }

        public void run() {
            js.status(null);
        }
    }

    static class DropNull extends With {
        DropNull(JShell js) {
            super(js);
        }

        public void run() {
            js.drop(null);
        }
    }

    static class VarValueNull extends With {
        VarValueNull(JShell js) {
            super(js);
        }

        public void run() {
            js.varValue(null);
        }
    }

    static class DiagNull extends With {
        DiagNull(JShell js) {
            super(js);
        }

        public void run() {
            js.diagnostics(null).count();
        }
    }

    static class UnresNull extends With {
        UnresNull(JShell js) {
            super(js);
        }

        public void run() {
            js.unresolvedDependencies(null).count();
        }
    }

    static class OnEventNull extends With {
        OnEventNull(JShell js) {
            super(js);
        }

        public void run() {
            js.onSnippetEvent(null);
        }
    }

    static class OnShutdownNull extends With {
        OnShutdownNull(JShell js) {
            super(js);
        }

        public void run() {
            js.onShutdown(null);
        }
    }

    static class UnsubNull extends With {
        UnsubNull(JShell js) {
            super(js);
        }

        public void run() {
            js.unsubscribe(null);
        }
    }

    static class ClasspathNull extends With {
        ClasspathNull(JShell js) {
            super(js);
        }

        public void run() {
            js.addToClasspath(null);
        }
    }

    static class Stop extends With {
        Stop(JShell js) {
            super(js);
        }

        public void run() {
            js.stop();
        }
    }

    static class UnsubForeign extends With {
        private final JShell.Subscription s;

        UnsubForeign(JShell js, JShell.Subscription s) {
            super(js);
            this.s = s;
        }

        public void run() {
            js.unsubscribe(this.s);
        }
    }

    static class EvalClosed extends With {
        EvalClosed(JShell js) {
            super(js);
        }

        public void run() {
            js.eval("1+1");
        }
    }

    static class SnippetsClosed extends With {
        SnippetsClosed(JShell js) {
            super(js);
        }

        public void run() {
            js.snippets().count();
        }
    }

    static class ScaClosed extends With {
        ScaClosed(JShell js) {
            super(js);
        }

        public void run() {
            js.sourceCodeAnalysis();
        }
    }

    static class SubClosed extends With {
        SubClosed(JShell js) {
            super(js);
        }

        public void run() {
            js.onSnippetEvent(new Nothing());
        }
    }

    static class ClasspathClosed extends With {
        ClasspathClosed(JShell js) {
            super(js);
        }

        public void run() {
            js.addToClasspath(".");
        }
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
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
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
