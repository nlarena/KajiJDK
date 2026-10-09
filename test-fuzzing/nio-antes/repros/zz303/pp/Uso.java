// La otra mitad del repro del #303: nombra `Zzz` a secas, por el `import java.lang.*` implicito.
package pp;

public class Uso {
    public static int prueba() {
        return Zzz.uno();
    }
}
