import javax.swing.*; import java.awt.*;
public class Rep505b {
    public static int run() {
        JScrollPane sp = new JScrollPane(new JLabel("x"));
        JScrollBar vsb = sp.getVerticalScrollBar();
        System.out.println("//val " + vsb.getValue());
        System.out.println("//ext " + vsb.getVisibleAmount());
        System.out.println("//max " + vsb.getMaximum());
        System.out.println("//bloque " + vsb.getBlockIncrement(1));
        System.out.println("//viewRect " + sp.getViewport().getViewRect());
        System.out.println("//unidad " + vsb.getUnitIncrement(1));
        return 0;
    }
}
