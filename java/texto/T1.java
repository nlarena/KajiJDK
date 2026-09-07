import javax.swing.text.*;
public class T1 {
    public static int run() {
        System.out.println("//a");
        SimpleAttributeSet s = new SimpleAttributeSet();
        System.out.println("//b " + s.getAttributeCount());
        System.out.println("//c " + SimpleAttributeSet.EMPTY.getAttributeCount());
        System.out.println("//d " + StyleConstants.Bold);
        s.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        System.out.println("//e " + s.getAttributeCount());
        StyleContext ctx = new StyleContext();
        System.out.println("//f");
        return 0;
    }
}
