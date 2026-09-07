import jdk.jfr.*;
import java.util.List;

public class JFX {

    @Name("prueba.MiEvento")
    @Label("Mi Evento")
    @Description("Un evento de prueba")
    @Category({ "Pruebas", "Metadatos" })
    @Threshold("20 ms")
    @StackTrace(false)
    static class MiEvento extends Event {
        @Label("Ruta") String ruta;
        @Label("Bytes") @DataAmount long bytes;
        @Timespan(Timespan.MILLISECONDS) long espera;
        @Percentage double uso;
        String sinAnotacion;
        static int noCuenta;
        transient int tampoco;
        String[] arreglo;
    }

    static class Pelado extends Event {
        int x;
    }

    static int h(int a, String s) { return a * 31 + (s == null ? 0 : s.hashCode()); }
    static int hb(int a, boolean b) { return a * 31 + (b ? 1 : 0); }

    static int uno(int a, EventType t) {
        a = h(a, t.getName());
        a = h(a, t.getLabel());
        a = h(a, t.getDescription());
        List<String> cats = t.getCategoryNames();
        a = a * 31 + cats.size();
        for (int i = 0; i < cats.size(); i++) a = h(a, cats.get(i));
        List<ValueDescriptor> fs = t.getFields();
        a = a * 31 + fs.size();
        for (int i = 0; i < fs.size(); i++) {
            ValueDescriptor v = fs.get(i);
            a = h(a, v.getName());
            a = h(a, v.getTypeName());
            a = h(a, v.getLabel());
            a = h(a, v.getContentType());
            a = hb(a, v.isArray());
        }
        List<SettingDescriptor> ss = t.getSettingDescriptors();
        a = a * 31 + ss.size();
        for (int i = 0; i < ss.size(); i++) {
            a = h(a, ss.get(i).getName());
            a = h(a, ss.get(i).getDefaultValue());
        }
        return a;
    }

    static EventType t(int i) {
        return i == 0 ? EventType.getEventType(MiEvento.class) : EventType.getEventType(Pelado.class);
    }

    /** i = clase, j = aspecto. */
    public static int parte(int i, int j) {
        EventType t = t(i);
        int a = 17;
        if (j == 0) { a = h(a, t.getName()); a = h(a, t.getLabel()); a = h(a, t.getDescription()); return a; }
        if (j == 1) {
            List<String> c = t.getCategoryNames(); a = a * 31 + c.size();
            for (int k = 0; k < c.size(); k++) a = h(a, c.get(k));
            return a;
        }
        if (j == 2) {   // cuantos campos y sus nombres
            List<ValueDescriptor> f = t.getFields(); a = a * 31 + f.size();
            for (int k = 0; k < f.size(); k++) a = h(a, f.get(k).getName());
            return a;
        }
        if (j == 3) {   // tipos de campo + isArray
            List<ValueDescriptor> f = t.getFields();
            for (int k = 0; k < f.size(); k++) { a = h(a, f.get(k).getTypeName()); a = hb(a, f.get(k).isArray()); }
            return a;
        }
        if (j == 4) {   // label y contentType de cada campo
            List<ValueDescriptor> f = t.getFields();
            for (int k = 0; k < f.size(); k++) { a = h(a, f.get(k).getLabel()); a = h(a, f.get(k).getContentType()); }
            return a;
        }
        if (j == 7) {   // solo las etiquetas de los campos
            List<ValueDescriptor> f = t.getFields();
            for (int k = 0; k < f.size(); k++) a = h(a, f.get(k).getLabel());
            return a;
        }
        if (j == 8) {   // solo los tipos de contenido
            List<ValueDescriptor> f = t.getFields();
            for (int k = 0; k < f.size(); k++) a = h(a, f.get(k).getContentType());
            return a;
        }
        if (j >= 9) {   // etiqueta y ct del campo (j - 9)
            List<ValueDescriptor> f = t.getFields();
            int k = j - 9;
            if (k >= f.size()) return -1;
            a = h(a, f.get(k).getName());
            a = h(a, f.get(k).getLabel());
            a = h(a, f.get(k).getContentType());
            return a;
        }
        if (j == 5) {   // cuantos ajustes y sus nombres
            List<SettingDescriptor> ss = t.getSettingDescriptors(); a = a * 31 + ss.size();
            for (int k = 0; k < ss.size(); k++) a = h(a, ss.get(k).getName());
            return a;
        }
        List<SettingDescriptor> ss = t.getSettingDescriptors();   // j == 6: valores por omision
        for (int k = 0; k < ss.size(); k++) a = h(a, ss.get(k).getDefaultValue());
        return a;
    }

    public static int check() {
        int a = 17;
        a = uno(a, EventType.getEventType(MiEvento.class));
        a = uno(a, EventType.getEventType(Pelado.class));
        return a;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 2; i++) for (int j = 0; j < 20; j++)
            System.out.println("c" + i + "j" + j + " " + parte(i, j));
    }
}
