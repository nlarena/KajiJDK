import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SpfDbg {

    static class ConLista implements Serializable {
        int noSale;
        private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("uno", Integer.TYPE),
            new ObjectStreamField("dos", String.class)
        };
    }

    public static void main(String[] a) throws Exception {
        Field f = ConLista.class.getDeclaredField("serialPersistentFields");
        System.out.println("campo=" + f.getName());
        int m = f.getModifiers();
        System.out.println("mods=" + m + " priv=" + Modifier.isPrivate(m)
            + " stat=" + Modifier.isStatic(m) + " final=" + Modifier.isFinal(m));
        System.out.println("tipo=" + f.getType().getName());
        f.setAccessible(true);
        Object v = f.get(null);
        System.out.println("valor null? " + (v == null));
        System.out.println("es OSF[]? " + (v instanceof ObjectStreamField[]));
        if (v != null) {
            System.out.println("clase valor=" + v.getClass().getName());
        }
        ObjectStreamField[] fs = ObjectStreamClass.lookup(ConLista.class).getFields();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fs.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(fs[i].toString()).append('@').append(fs[i].getOffset());
        }
        System.out.println("campos=[" + sb + "]");
    }
}
