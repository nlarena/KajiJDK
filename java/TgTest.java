/**
 * Exercises java.lang.ThreadGroup. Every method returns the number of things that came out
 * wrong, so 0 is a pass. It also runs against the JDK through main, for comparison.
 */
public class TgTest {

    /** The tree: name, parent, and the root chain the current thread sits in. */
    public static int arbol() {
        ThreadGroup here = Thread.currentThread().getThreadGroup();
        int bad = 0;
        if (here == null) {
            return 1;
        }
        if (!here.getName().equals("main")) {
            bad = bad + 1;
        }
        ThreadGroup up = here.getParent();
        if (up == null || !up.getName().equals("system")) {
            bad = bad + 1;
        }
        if (up.getParent() != null) {
            bad = bad + 1;
        }
        return bad;
    }

    /** A child group inherits the priority cap and is enclosed by its parent. */
    public static int hijo() {
        ThreadGroup here = Thread.currentThread().getThreadGroup();
        ThreadGroup child = new ThreadGroup(here, "hijo");
        int bad = 0;
        if (child.getParent() != here) {
            bad = bad + 1;
        }
        if (child.getMaxPriority() != here.getMaxPriority()) {
            bad = bad + 1;
        }
        if (!here.parentOf(child)) {
            bad = bad + 1;
        }
        if (!here.parentOf(here)) {
            bad = bad + 1;
        }
        if (child.parentOf(here)) {
            bad = bad + 1;
        }
        if (child.isDestroyed()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Lowering the cap on a parent lowers it on the children too. */
    public static int prioridad() {
        ThreadGroup root = new ThreadGroup(Thread.currentThread().getThreadGroup(), "raiz");
        ThreadGroup child = new ThreadGroup(root, "rama");
        int bad = 0;
        root.setMaxPriority(4);
        if (root.getMaxPriority() != 4) {
            bad = bad + 1;
        }
        if (child.getMaxPriority() != 4) {
            bad = bad + 1;
        }
        // A child cannot exceed its parent.
        child.setMaxPriority(9);
        if (child.getMaxPriority() != 4) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Subgroups are counted and enumerated, recursively or not. */
    public static int subgrupos() {
        ThreadGroup root = new ThreadGroup(Thread.currentThread().getThreadGroup(), "conteo");
        ThreadGroup a = new ThreadGroup(root, "a");
        ThreadGroup b = new ThreadGroup(a, "b");
        int bad = 0;
        if (root.activeGroupCount() != 2) {
            bad = bad + 1;
        }
        ThreadGroup[] buf = new ThreadGroup[4];
        if (root.enumerate(buf, true) != 2) {
            bad = bad + 1;
        }
        if (root.enumerate(buf, false) != 1) {
            bad = bad + 1;
        }
        if (buf[0] != a) {
            bad = bad + 1;
        }
        if (b.getParent() != a) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The deprecated no-ops are no-ops, not throws. */
    public static int obsoletos() {
        ThreadGroup g = new ThreadGroup(Thread.currentThread().getThreadGroup(), "viejo");
        int bad = 0;
        g.checkAccess();
        g.destroy();
        if (g.isDestroyed()) {
            bad = bad + 1;
        }
        g.setDaemon(true);
        if (!g.isDaemon()) {
            bad = bad + 1;
        }
        g.setDaemon(false);
        if (g.isDaemon()) {
            bad = bad + 1;
        }
        if (!g.toString().equals("java.lang.ThreadGroup[name=viejo,maxpri=10]")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return TgTest.arbol() + TgTest.hijo() + TgTest.prioridad() + TgTest.subgrupos()
                + TgTest.obsoletos();
    }

    public static void main(String[] args) {
        System.out.println("arbol       " + TgTest.arbol());
        System.out.println("hijo        " + TgTest.hijo());
        System.out.println("prioridad   " + TgTest.prioridad());
        System.out.println("subgrupos   " + TgTest.subgrupos());
        System.out.println("obsoletos   " + TgTest.obsoletos());
        System.out.println("TOTAL       " + TgTest.todo());
    }
}
