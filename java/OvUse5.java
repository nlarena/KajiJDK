// Ver OvLib5.java / OvLib3.java.
import java.util.ArrayList;
public class OvUse5 {
    public static int run() {
        String[] arr = new String[2];
        arr[0] = "a"; arr[1] = "b";
        ArrayList<String> l = OvLib5.of(arr);
        if (l.size() != 2) { return 0; }
        return -1;
    }
}
