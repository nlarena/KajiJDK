package p;

/** Una envolvente con una anidada `protected`: solo sus subclases pueden extenderla. */
public class Outer {
    protected class Inner {
        public int leer() { return 7; }
    }
}
