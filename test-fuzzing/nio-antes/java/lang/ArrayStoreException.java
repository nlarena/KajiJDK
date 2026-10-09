package java.lang;

// KajiLibrary's java.lang.ArrayStoreException — thrown when a reference is stored into an
// array whose runtime element type does not accept it. This is the price of array
// covariance: `Object[] a = new String[1]` type-checks, so the store `a[0] = 42` can only
// be caught at runtime, by the VM's aastore check.
public class ArrayStoreException extends RuntimeException {

    public ArrayStoreException() {
    }

    public ArrayStoreException(String message) {
        super(message);
    }
}
