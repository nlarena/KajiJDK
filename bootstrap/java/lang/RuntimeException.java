package java.lang;

// Our own minimal java.lang.RuntimeException — the unchecked-exception branch.
// Real hierarchy: RuntimeException -> Exception -> Throwable -> Object, all ours now.
public class RuntimeException extends Exception {
    public RuntimeException() {
    }
}
