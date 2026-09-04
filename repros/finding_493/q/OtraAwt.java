import java.awt.List;

/** Aca `List` es el widget de AWT: unidad distinta, import distinto. */
public class OtraAwt {
    public int run() { List l = new List(3, true); return l.getRows(); }
}
