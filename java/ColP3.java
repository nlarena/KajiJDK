import java.util.*;
public class ColP3 {
    public static int run() {
        int r = 0;
        // ---- ListIterator --------------------------------------------------------------------
        ArrayList<String> li = new ArrayList<String>();
        li.add("1");
        li.add("2");
        li.add("3");
        ListIterator<String> it = li.listIterator();
        int pasos = 0;
        while (it.hasNext()) {
            it.next();
            pasos = pasos + 1;
        }
        r = r + pasos;                                   // 3
        int atras = 0;
        while (it.hasPrevious()) {
            it.previous();
            atras = atras + 1;
        }
        r = r + atras;                                   // 3

        ListIterator<String> it2 = li.listIterator();
        it2.next();
        it2.set("9");                                    // reemplaza el primero
        r = r + (li.get(0).equals("9") ? 1 : 0);
        it2.next();
        it2.remove();                                    // saca el segundo
        r = r + li.size();                               // 2

        // ---- subList es una VISTA ------------------------------------------------------------
        ArrayList<String> base = new ArrayList<String>();
        base.add("A");
        base.add("B");
        base.add("C");
        base.add("D");
        List<String> vista = base.subList(1, 3);         // [B, C]
        r = r + vista.size();                            // 2
        vista.set(0, "Z");
        r = r + (base.get(1).equals("Z") ? 1 : 0);       // escribir en la vista escribe atras
        r = r + base.lastIndexOf("D");                   // 3

        // ---- List.sort -----------------------------------------------------------------------
        ArrayList<String> orden = new ArrayList<String>();
        orden.add("c");
        orden.add("a");
        orden.add("b");
        orden.sort(null);
        r = r + (orden.get(0).equals("a") && orden.get(2).equals("c") ? 1 : 0);

        return r;
    }
    public static void main(String[] a){ System.out.println(run()); }
}
