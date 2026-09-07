package javax.tools;

// KajiLibrary's javax.tools.ToolProvider — how a program gets hold of the tools the platform
// ships, without naming an implementation class. It is a pure static factory: the private
// constructor is the point, there is nothing to instantiate.
//
// The two getters return `null`, and that is NOT a stub: the JDK's contract explicitly says they
// return null when the platform does not provide the tool, and KajiJDK does not yet expose its
// compiler through this API (javac lives in `bin/javac.exe`, not as a javax.tools.JavaCompiler). A
// correct caller has to check for null anyway.
//
// `ClassLoader getSystemToolClassLoader()` used to be missing, because java.lang.ClassLoader did not
// exist in KajiLibrary. It exists now and so does the member.
public class ToolProvider {

    // Nobody instantiates a ToolProvider.
    private ToolProvider() {
    }

    /**
     * The loader the system tools were loaded from.
     *
     * <p>It returns the one loader there is. In the JDK this could be a **separate** loader --the
     * tools lived in `tools.jar`, outside the application's classpath-- and that is why the method
     * exists; since the tools became just another module, the JDK returns `null`. Here there is a
     * single loader and returning it is the most informative answer.
     */
    public static ClassLoader getSystemToolClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static JavaCompiler getSystemJavaCompiler() {
        return null;
    }

    public static DocumentationTool getSystemDocumentationTool() {
        return null;
    }
}
