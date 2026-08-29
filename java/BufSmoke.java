import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/** Smoke test: do the nio buffers the charsets are built on actually run here? */
public class BufSmoke {
    public static int todo() {
        int bad = 0;
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put((byte) 1);
        bb.put((byte) 2);
        bb.flip();
        if (bb.remaining() != 2) { bad = bad + 1; }
        if (bb.get() != 1) { bad = bad + 1; }
        if (bb.position() != 1) { bad = bad + 1; }
        CharBuffer cb = CharBuffer.allocate(4);
        cb.put('h');
        cb.put('i');
        cb.flip();
        if (cb.remaining() != 2) { bad = bad + 1; }
        if (!cb.toString().equals("hi")) { bad = bad + 1; }
        CharBuffer w = CharBuffer.wrap("abc");
        if (w.remaining() != 3) { bad = bad + 1; }
        byte[] raw = new byte[3];
        raw[0] = 7;
        ByteBuffer wrapped = ByteBuffer.wrap(raw);
        if (wrapped.get() != 7) { bad = bad + 1; }
        return bad;
    }
    public static void main(String[] a) { System.out.println("todo " + BufSmoke.todo()); }
}
