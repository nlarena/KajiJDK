package java.lang;

// KajiLibrary's java.lang.StringBuffer — StringBuilder's older, synchronized twin. Same buffer,
// same append overloads; every method takes the object's own monitor before touching the state.
//
// The pair is a lesson in what synchronization does and does not buy you:
//
//   * StringBuffer came first (1.0). StringBuilder was split out in 1.5 once it was clear that
//     the overwhelming majority of buffers never leave the method that created them — the one
//     the compiler creates for `a + b` is the extreme case — and were paying for a lock nobody
//     contended. The APIs are deliberately identical so the swap is mechanical.
//
//   * The locking here makes each individual call atomic, and nothing more. `if (sb.length() > 0)
//     sb.append(x)` is still a race: two locked operations with a gap in between. Per-method
//     locking cannot fix that, because the invariant spans calls. This is why "thread-safe class"
//     is a much weaker property than it sounds, and why StringBuffer's synchronization is usually
//     either unnecessary (single-threaded use) or insufficient (real sharing, compound updates).
//
// We lock on `this`, as the JDK does with its `synchronized` methods, so a caller that needs a
// compound operation to be atomic can wrap it in `synchronized (buf) { ... }` and use the same
// monitor. Every method is written single-exit — the result is computed inside the critical
// section, assigned to a local, and returned after it (finding #105).
public final class StringBuffer implements CharSequence, Appendable {

    private char[] value;

    private int count;

    public StringBuffer() {
        this.value = new char[16];
        this.count = 0;
    }

    // The capacity hint matters more here than for a short-lived StringBuilder: a shared buffer
    // that outlives one method is exactly the one worth sizing up front, since every growth copies
    // the whole array while holding the lock.
    public StringBuffer(int capacity) {
        if (capacity < 0) {
            throw new NegativeArraySizeException();
        }
        this.value = new char[capacity];
        this.count = 0;
    }

    public StringBuffer(String str) {
        int n = str.length();
        this.value = new char[n + 16];
        this.count = 0;
        appendChars(str, n);
    }

    // Grow the backing array (doubling) so it can hold at least `min` chars. Callers hold the
    // monitor already; this is private precisely so it cannot be called without it.
    private void ensureRoom(int min) {
        if (min > value.length) {
            int newCap = value.length * 2;
            if (newCap < min) {
                newCap = min;
            }
            char[] bigger = new char[newCap];
            System.arraycopy(value, 0, bigger, 0, count);
            value = bigger;
        }
    }

    // Unsynchronized bulk copy of a String's first `n` chars. Used by the constructor (where no
    // other thread can see `this` yet) and by the locked append below.
    private void appendChars(String s, int n) {
        ensureRoom(count + n);
        for (int i = 0; i < n; i++) {
            value[count + i] = s.charAt(i);
        }
        count = count + n;
    }

    public StringBuffer append(char c) {
        synchronized (this) {
            ensureRoom(count + 1);
            value[count] = c;
            count = count + 1;
        }
        return this;
    }

    public StringBuffer append(String s) {
        if (s == null) {
            s = "null";
        }
        int n = s.length();
        synchronized (this) {
            appendChars(s, n);
        }
        return this;
    }

    public StringBuffer append(boolean b) {
        String s;
        if (b) {
            s = "true";
        } else {
            s = "false";
        }
        return append(s);
    }

    // Appending a CharSequence reads someone else's object while we hold *our* monitor. The
    // JDK has the same hazard: the source is not locked, so a concurrently mutated argument can
    // be copied in a torn state. Locking it too would invite deadlock, so neither library does.
    public StringBuffer append(CharSequence s) {
        if (s == null) {
            return append("null");
        }
        int n = s.length();
        synchronized (this) {
            ensureRoom(count + n);
            for (int i = 0; i < n; i++) {
                value[count + i] = s.charAt(i);
            }
            count = count + n;
        }
        return this;
    }

    // Append the sub-range [start, end) of a CharSequence.
    public StringBuffer append(CharSequence s, int start, int end) {
        if (s == null) {
            String nul = "null";
            for (int i = start; i < end; i++) {
                append(nul.charAt(i));
            }
            return this;
        }
        synchronized (this) {
            ensureRoom(count + (end - start));
            for (int i = start; i < end; i++) {
                value[count] = s.charAt(i);
                count = count + 1;
            }
        }
        return this;
    }

    // int → its decimal digits, appended without an intermediate String. Works in negative space
    // so that Integer.MIN_VALUE (whose magnitude overflows a positive int) comes out right.
    public StringBuffer append(int i) {
        if (i == 0) {
            return append('0');
        }
        boolean neg = i < 0;
        if (i > 0) {
            i = -i;
        }
        char[] tmp = new char[11];
        int p = 11;
        while (i < 0) {
            int digit = -(i % 10);
            p = p - 1;
            tmp[p] = (char) ('0' + digit);
            i = i / 10;
        }
        // One critical section for the whole number: digits of a single append must not be
        // interleaved with another thread's.
        synchronized (this) {
            if (neg) {
                ensureRoom(count + 1);
                value[count] = '-';
                count = count + 1;
            }
            ensureRoom(count + (11 - p));
            for (int k = p; k < 11; k++) {
                value[count] = tmp[k];
                count = count + 1;
            }
        }
        return this;
    }

    // long → its decimal digits. Same negative-space trick; 20 chars is the widest result
    // ("-9223372036854775808").
    public StringBuffer append(long l) {
        if (l == 0L) {
            return append('0');
        }
        boolean neg = l < 0L;
        if (l > 0L) {
            l = -l;
        }
        char[] tmp = new char[20];
        int p = 20;
        while (l < 0L) {
            int digit = (int) -(l % 10L);
            p = p - 1;
            tmp[p] = (char) ('0' + digit);
            l = l / 10L;
        }
        synchronized (this) {
            if (neg) {
                ensureRoom(count + 1);
                value[count] = '-';
                count = count + 1;
            }
            ensureRoom(count + (20 - p));
            for (int k = p; k < 20; k++) {
                value[count] = tmp[k];
                count = count + 1;
            }
        }
        return this;
    }

    // Drop the last `count - newLength` chars, or pad with '\0' if lengthening — the JDK's
    // contract, and the usual way to reuse one buffer across iterations.
    public void setLength(int newLength) {
        if (newLength < 0) {
            throw new StringIndexOutOfBoundsException(newLength);
        }
        synchronized (this) {
            ensureRoom(newLength);
            for (int i = count; i < newLength; i++) {
                value[i] = (char) 0;
            }
            count = newLength;
        }
    }

    public int capacity() {
        int c;
        synchronized (this) {
            c = value.length;
        }
        return c;
    }

    // --- CharSequence ---

    // Locked, but the answer is stale the instant it is returned if anyone else is appending.
    // A lock makes a read consistent, not durable — see the class comment.
    public int length() {
        int n;
        synchronized (this) {
            n = count;
        }
        return n;
    }

    public char charAt(int index) {
        char c;
        synchronized (this) {
            if (index < 0 || index >= count) {
                throw new StringIndexOutOfBoundsException(index);
            }
            c = value[index];
        }
        return c;
    }

    public CharSequence subSequence(int start, int end) {
        CharSequence s;
        synchronized (this) {
            s = String.valueOf(value, start, end - start);
        }
        return s;
    }

    public String toString() {
        String s;
        synchronized (this) {
            s = String.valueOf(value, 0, count);
        }
        return s;
    }
}
