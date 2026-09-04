package p;

import java.util.ArrayList;
import java.util.List;

/** El import de un solo tipo tapa al homonimo del paquete (JLS 7.5.1). */
public class Uso {
    private final List<String> xs = new ArrayList<String>();
    public int run() { this.xs.add("a"); return this.xs.size(); }
}
