import java.security.AllPermission;
import java.security.Permissions;
import java.util.PropertyPermission;

// Semantica del modelo de permisos: la implicacion por nombre con comodin, la implicacion por
// acciones, y la acumulacion que solo una PermissionCollection puede hacer.
public class PermTest {

    static PropertyPermission p(String n, String a) {
        return new PropertyPermission(n, a);
    }

    public static int run() {
        int r = 0;

        // ---- implicacion por nombre ------------------------------------------------------------
        r = r + (p("java.home", "read").implies(p("java.home", "read")) ? 1 : 0);
        r = r + (p("java.*", "read").implies(p("java.home", "read")) ? 10 : 0);
        r = r + (p("java.*", "read").implies(p("java.a.b", "read")) ? 100 : 0);
        // el comodin NO implica el nodo de arriba
        r = r + (p("java.*", "read").implies(p("java", "read")) ? 7777 : 0);
        // ni una rama hermana
        r = r + (p("java.*", "read").implies(p("os.name", "read")) ? 7777 : 0);
        // el comodin universal si
        r = r + (p("*", "read").implies(p("cualquier.cosa", "read")) ? 1000 : 0);

        // ---- implicacion por acciones ----------------------------------------------------------
        r = r + (p("x", "read,write").implies(p("x", "read")) ? 10000 : 0);
        r = r + (p("x", "read").implies(p("x", "read,write")) ? 7777 : 0);
        r = r + (p("java.*", "read").implies(p("java.home", "write")) ? 7777 : 0);

        // ---- acciones canonicas ----------------------------------------------------------------
        r = r + (p("x", "write,read").getActions().equals("read,write") ? 100000 : 0);
        r = r + (p("x", " READ ").getActions().equals("read") ? 1000000 : 0);
        // dos que dicen lo mismo son iguales y tienen el mismo hash
        r = r + (p("x", "write,read").equals(p("x", "read,write")) ? 10000000 : 0);
        r = r + (p("x", "write,read").hashCode() == p("x", "read,write").hashCode() ? 1 : 0);
        // el mismo nombre con distintas acciones NO es el mismo permiso
        r = r + (p("x", "read").equals(p("x", "write")) ? 7777 : 0);

        try {
            p("x", "borrar");
            r = r + 7777;
        } catch (IllegalArgumentException e) {
            r = r + 100000000;
        }

        // ---- la acumulacion de la coleccion ----------------------------------------------------
        //
        // Ninguno de los dos permisos implica solo el pedido; la union si. Es exactamente lo que
        // un bucle sobre Permission.implies no puede contestar.
        Permissions ps = new Permissions();
        ps.add(p("java.*", "read"));
        ps.add(p("java.home", "write"));
        r = r + (ps.implies(p("java.home", "read,write")) ? 2 : 0);
        r = r + (ps.implies(p("java.version", "read")) ? 20 : 0);
        r = r + (ps.implies(p("java.version", "write")) ? 7777 : 0);

        // ---- AllPermission ---------------------------------------------------------------------
        Permissions todos = new Permissions();
        todos.add(new AllPermission());
        r = r + (todos.implies(p("lo.que.sea", "read,write")) ? 200 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
