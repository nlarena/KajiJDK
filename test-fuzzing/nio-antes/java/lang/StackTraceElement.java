package java.lang;

import java.io.Serializable;

// KajiLibrary's java.lang.StackTraceElement — one frame of a stack trace, as data.
//
// It is a SNAPSHOT and not a live view of a frame: by the time anyone reads it the method has
// usually returned. That is why every field is a plain String or int copied out at capture
// time, and why the class is final and immutable — a stack trace has to survive being stored,
// serialized and printed long after the stack it came from is gone.
//
// The seven-argument constructor carries the module layer information (class loader name,
// module name and version) that the four-argument one predates. Both are public because a
// trace can legitimately be SYNTHESISED — by a test, by a remote-call framework rebuilding a
// server-side trace on the client — not only captured.
public final class StackTraceElement implements Serializable {

    private final String classLoaderName;
    private final String moduleName;
    private final String moduleVersion;
    private final String declaringClass;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;

    /**
     * Creates a frame for a class, method, file and line.
     *
     * @param declaringClass the fully qualified name of the class
     * @param methodName the method name, or {@code <init>} / {@code <clinit>}
     * @param fileName the source file, or {@code null} if unknown
     * @param lineNumber the line, {@code -1} if unknown, {@code -2} for a native method
     */
    public StackTraceElement(String declaringClass, String methodName, String fileName,
            int lineNumber) {
        if (declaringClass == null || methodName == null) {
            throw new NullPointerException();
        }
        this.classLoaderName = null;
        this.moduleName = null;
        this.moduleVersion = null;
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    /** Creates a frame that also records where the class came from. */
    public StackTraceElement(String classLoaderName, String moduleName, String moduleVersion,
            String declaringClass, String methodName, String fileName, int lineNumber) {
        if (declaringClass == null || methodName == null) {
            throw new NullPointerException();
        }
        this.classLoaderName = classLoaderName;
        this.moduleName = moduleName;
        this.moduleVersion = moduleVersion;
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getFileName() {
        return this.fileName;
    }

    /** The line, or a negative number: {@code -1} unknown, {@code -2} native. */
    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getModuleName() {
        return this.moduleName;
    }

    public String getModuleVersion() {
        return this.moduleVersion;
    }

    public String getClassLoaderName() {
        return this.classLoaderName;
    }

    public String getClassName() {
        return this.declaringClass;
    }

    public String getMethodName() {
        return this.methodName;
    }

    /** A native method has no line to point at, which is what the {@code -2} encodes. */
    public boolean isNativeMethod() {
        return this.lineNumber == -2;
    }

    /**
     * The frame as it appears in a printed stack trace.
     *
     * <p>The shape is {@code loader/module@version/Class.method(File:line)}, with each prefix
     * dropped when it is absent — which is why an ordinary application frame prints as the
     * familiar {@code Class.method(File:line)} and nothing more.
     */
    @Override
    public String toString() {
        String prefix = "";
        if (this.classLoaderName != null && !this.classLoaderName.isEmpty()) {
            prefix = this.classLoaderName + "/";
        }
        if (this.moduleName != null && !this.moduleName.isEmpty()) {
            prefix = prefix + this.moduleName;
            if (this.moduleVersion != null && !this.moduleVersion.isEmpty()) {
                prefix = prefix + "@" + this.moduleVersion;
            }
            prefix = prefix + "/";
        }
        String where;
        if (this.isNativeMethod()) {
            where = "(Native Method)";
        } else if (this.fileName == null) {
            where = "(Unknown Source)";
        } else if (this.lineNumber >= 0) {
            where = "(" + this.fileName + ":" + this.lineNumber + ")";
        } else {
            where = "(" + this.fileName + ")";
        }
        return prefix + this.declaringClass + "." + this.methodName + where;
    }

    /** Two frames are equal when every recorded field is, file and line included. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof StackTraceElement)) {
            return false;
        }
        StackTraceElement other = (StackTraceElement) obj;
        return this.lineNumber == other.lineNumber
                && StackTraceElement.same(this.classLoaderName, other.classLoaderName)
                && StackTraceElement.same(this.moduleName, other.moduleName)
                && StackTraceElement.same(this.moduleVersion, other.moduleVersion)
                && this.declaringClass.equals(other.declaringClass)
                && this.methodName.equals(other.methodName)
                && StackTraceElement.same(this.fileName, other.fileName);
    }

    // Null-tolerant equality, since four of the seven fields are optional.
    private static boolean same(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Override
    public int hashCode() {
        int result = 31 * this.declaringClass.hashCode() + this.methodName.hashCode();
        result = 31 * result + StackTraceElement.hash(this.classLoaderName);
        result = 31 * result + StackTraceElement.hash(this.moduleName);
        result = 31 * result + StackTraceElement.hash(this.moduleVersion);
        result = 31 * result + StackTraceElement.hash(this.fileName);
        result = 31 * result + this.lineNumber;
        return result;
    }

    private static int hash(String s) {
        if (s == null) {
            return 0;
        }
        return s.hashCode();
    }
}
