// Ver el encabezado de OvLib3.java. Este archivo NO COMPILA a proposito: es la mitad que falla
// del repro. No dejar .class.
import java.util.ArrayList;
public class OvUse3 {
    public static int run() {
        String[] arr = new String[2];
        arr[0] = "a"; arr[1] = "b";
        ArrayList<String> l = OvLib3.of(arr);
        if (l.size() != 2) { return 0; }
        return -1;
    }
}
