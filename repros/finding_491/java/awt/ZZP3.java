// Un nombre que la clausura de Container no carga: sigue fallando, como debe.
package java.awt;
public class ZZP3 extends Container {
    public Object f() { return NumericShaper.getShaper(NumericShaper.ARABIC); }
}
