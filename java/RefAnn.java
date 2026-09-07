import jdk.jfr.*;
public class RefAnn {
    static class E extends Event {
        @Label("Ruta") String ruta;
        @Label("Bytes") @DataAmount long bytes;
    }
    @Label("EnClase") static class C { }

    public static int nFieldAnns() throws Exception {
        return E.class.getDeclaredField("ruta").getAnnotations().length;
    }
    public static int nFieldAnns2() throws Exception {
        return E.class.getDeclaredField("bytes").getAnnotations().length;
    }
    public static int nClassAnns() { return C.class.getAnnotations().length; }
    public static int labelDirecto() throws Exception {
        Label l = E.class.getDeclaredField("ruta").getAnnotation(Label.class);
        return l == null ? -1 : l.value().hashCode();
    }
    public static int labelClase() {
        Label l = C.class.getAnnotation(Label.class);
        return l == null ? -1 : l.value().hashCode();
    }
}
