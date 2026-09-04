package q;

/** Subclase en OTRO paquete: tiene derecho a extender a Outer.Inner (es protected). */
public class Sub extends p.Outer {
    public Sub() { }
    class SubInner extends Inner { }
    public int armar() { return new SubInner().leer(); }
}
