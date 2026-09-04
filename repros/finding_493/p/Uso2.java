package p;

import java.util.ArrayList;

/** Con el nombre entero no hay ambiguedad posible. */
public class Uso2 {
    private final java.util.List<String> xs = new ArrayList<String>();
    public int run() { this.xs.add("a"); return this.xs.size(); }
}
