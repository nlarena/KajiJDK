public class KajiBeansHumoV {
    static String prueba(String nombre, java.util.Map<Object,String> m) {
        try {
            m.put("k", "v1");
            m.put(null, "v2");
            StringBuilder sb = new StringBuilder();
            java.util.Iterator<String> it = m.values().iterator();
            while (it.hasNext()) { sb.append(it.next()).append(' '); }
            return nombre + ": OK size=" + m.size() + " [" + sb.toString().trim() + "]";
        } catch (Throwable t) { return nombre + ": ROTO " + t.getClass().getName(); }
    }
    public static int run() throws Exception {
        System.out.println(prueba("HashMap        ", new java.util.HashMap<Object,String>()));
        System.out.println(prueba("IdentityHashMap", new java.util.IdentityHashMap<Object,String>()));
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
