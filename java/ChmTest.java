import java.util.concurrent.ConcurrentHashMap;

// H6 volume: ConcurrentHashMap. Three workers each put 100 DISTINCT keys concurrently (300 entries
// total across the lock stripes); then a value-based get() with a *rebuilt* key (same fields, new
// object) must find its entry — proving the map dispatches key.hashCode()/equals() correctly.
// Result = size(300) + found(1) = 301, deterministically → green ≡ os-gil ≡ os.
class Key {
    int a;
    int b;

    Key(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public int hashCode() {
        return a * 31 + b;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Key)) {
            return false;
        }
        Key k = (Key) o;
        return a == k.a && b == k.b;
    }
}

class MapWorker extends Thread {
    ConcurrentHashMap<Key, Object> map;
    int id;
    Object val;

    public void run() {
        for (int j = 0; j < 100; j++) {
            map.put(new Key(id, j), val);
        }
    }
}

public class ChmTest {
    static int run() {
        ConcurrentHashMap<Key, Object> map = new ConcurrentHashMap<Key, Object>();
        Object val = new Object();
        MapWorker[] ws = new MapWorker[3];
        for (int i = 0; i < 3; i++) {
            ws[i] = new MapWorker();
            ws[i].map = map;
            ws[i].id = i;
            ws[i].val = val;
            ws[i].start();
        }
        try {
            for (int i = 0; i < 3; i++) {
                ws[i].join();
            }
        } catch (InterruptedException e) {
        }
        int size = map.size(); // 300 distinct keys
        int found = 0;
        if (map.get(new Key(1, 50)) != null) { // rebuilt key → value-equality lookup
            found = 1;
        }
        return size + found; // 301
    }
}
