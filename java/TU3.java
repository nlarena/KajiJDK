public class TU3 {
    static Class<?>[] tipos() {
        return new Class<?>[] {
            void.class, boolean.class, byte.class, char.class, short.class,
            int.class, long.class, float.class, double.class,
            Void.class, Boolean.class, Byte.class, Character.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class,
            Object.class, Number.class, String.class, CharSequence.class,
            Comparable.class, java.io.Serializable.class,
            Object[].class, int[].class, Integer[].class, String[].class,
            java.util.List.class, java.util.ArrayList.class
        };
    }
    public static int asig() {
        Class<?>[] t = tipos(); int h = 17;
        for (int i = 0; i < t.length; i++)
            for (int j = 0; j < t.length; j++)
                h = h * 31 + (t[j].isAssignableFrom(t[i]) ? 1 : 0);
        return h;
    }
    public static int prim() {
        Class<?>[] t = tipos(); int h = 17;
        for (int i = 0; i < t.length; i++) h = h * 31 + (t[i].isPrimitive() ? 1 : 0);
        return h;
    }
    public static int arr() {
        Class<?>[] t = tipos(); int h = 17;
        for (int i = 0; i < t.length; i++) h = h * 31 + (t[i].isArray() ? 1 : 0);
        return h;
    }
    public static int nom() {
        Class<?>[] t = tipos(); int h = 17;
        for (int i = 0; i < t.length; i++) h = h * 31 + t[i].getName().hashCode();
        return h;
    }
    // primera fila i donde asignabilidad difiere: devuelve el bitmap de la fila i
    public static int fila(int i) {
        Class<?>[] t = tipos(); int h = 0;
        for (int j = 0; j < t.length; j++) if (t[j].isAssignableFrom(t[i])) h |= (1 << j);
        return h;
    }
    public static void main(String[] a) {
        System.out.println(asig()+" "+prim()+" "+arr()+" "+nom());
        for (int i = 0; i < tipos().length; i++) System.out.println("fila "+i+" "+fila(i));
    }
}
