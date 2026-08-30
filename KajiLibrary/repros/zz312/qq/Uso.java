// La otra mitad del repro del #312: nombra `Tipo` corto, por el import.
package qq;

import pp.Tipo;

public class Uso {
    public static int prueba() {
        return Tipo.uno();
    }
}
