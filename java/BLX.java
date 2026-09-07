import jdk.dynalink.beans.BeansLinker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BLX {

    public static class Padre {
        public static int campoEstPadre;
        public static final int FINAL_EST_PADRE = 1;
        public static void metodoEstPadre() { }
        public static String getEstPadre() { return ""; }
        public int campoInstPadre;
        public final int finalInstPadre = 3;
        public int getInstPadre() { return 1; }
        public static class AnidadaPadre { }
    }

    public static class Hijo extends Padre {
        public static int campoEstHijo;
        public static void metodoEstHijo() { }
        public static void setEstHijo(String s) { }
        public int campoInstHijo;
        public String getURL() { return ""; }
        public String getX() { return ""; }
        public String get() { return ""; }
        public String is() { return ""; }
        public String isTexto() { return ""; }
        public boolean isOk() { return true; }
        public Boolean isCaja() { return true; }
        public void getVoid() { }
        public int getConArg(int i) { return i; }
        public void setURL(String s) { }
        public void setDos(String a, String b) { }
        public static class AnidadaHijo { }
    }

    public interface ConConstante { int CONSTANTE = 4; }
    public static class ConIface implements ConConstante { }

    static int h(int acc, Set<String> s) {
        List<String> l = new ArrayList<String>(s);
        Collections.sort(l);
        for (int i = 0; i < l.size(); i++) {
            acc = acc * 31 + l.get(i).hashCode();
        }
        return acc * 31 + l.size();
    }

    static int uno(int acc, Class<?> c) {
        acc = h(acc, BeansLinker.getReadableInstancePropertyNames(c));
        acc = h(acc, BeansLinker.getWritableInstancePropertyNames(c));
        acc = h(acc, BeansLinker.getInstanceMethodNames(c));
        acc = h(acc, BeansLinker.getReadableStaticPropertyNames(c));
        acc = h(acc, BeansLinker.getWritableStaticPropertyNames(c));
        acc = h(acc, BeansLinker.getStaticMethodNames(c));
        return acc;
    }

    static Class<?> clase(int i) {
        if (i == 0) return Padre.class;
        if (i == 1) return Hijo.class;
        if (i == 2) return ConIface.class;
        if (i == 3) return ConConstante.class;
        if (i == 4) return Object.class;
        return int[].class;
    }

    /** hash de UN set: clase i, metodo j. */
    public static int uno1(int i, int j) {
        Class<?> c = clase(i);
        if (j == 0) return h(17, BeansLinker.getReadableInstancePropertyNames(c));
        if (j == 1) return h(17, BeansLinker.getWritableInstancePropertyNames(c));
        if (j == 2) return h(17, BeansLinker.getInstanceMethodNames(c));
        if (j == 3) return h(17, BeansLinker.getReadableStaticPropertyNames(c));
        if (j == 4) return h(17, BeansLinker.getWritableStaticPropertyNames(c));
        return h(17, BeansLinker.getStaticMethodNames(c));
    }

    public static int check() {
        int a = 17;
        a = uno(a, Padre.class);
        a = uno(a, Hijo.class);
        a = uno(a, ConIface.class);
        a = uno(a, ConConstante.class);
        a = uno(a, Object.class);
        a = uno(a, int[].class);
        return a;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                System.out.println("c" + i + "m" + j + " " + uno1(i, j));
            }
        }
    }
}
