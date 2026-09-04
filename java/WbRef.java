// Differential workload for **F3-H3: the write barrier** — the block that kept every reference
// *store* out of the compiled subset.
//
// Reading a reference has been in the subset for a while; writing one owes the collector a record
// of the old→young pointer it may have just created, and no instruction stream can make that
// record. So the compiled store writes the `(holder, value)` pair into a log in the caller's buffer
// and the trampoline replays it. Everything here exercises one half of that:
//
//  - `chainSum`  — `putfield` of a reference, in a loop, read back through the chain;
//  - `oldToYoung` — the case the remembered set exists for: a **promoted** holder given a **fresh**
//    young object, with enough allocation between the store and the read that a minor collection
//    happens in between. Without the barrier the young object is freed while still reachable, and
//    the read that follows sees something that is not there;
//  - `spine`     — `aaload`, chained, which is the read half and owes nothing.
//
// The answer is the whole point: run it under `java` and under this VM, with the JIT off and on,
// and all three must agree.
public class WbRef {
    static int chainSum(int n) {
        WbCell head = new WbCell();
        head.tag = 1;
        for (int i = 0; i < n; i++) {
            WbCell node = new WbCell();
            node.tag = i & 31;
            node.next = head; // putfield of a reference — the opcode under test
            head = node;
        }
        int acc = 0;
        WbCell at = head;
        while (at != null) {
            acc = acc + at.tag;
            at = at.next;
        }
        return acc;
    }

    // The old→young case, spelled out so that a missing barrier record cannot survive it.
    //
    // `holder` outlives many minor collections and is promoted to Old. Every round then stores a
    // *fresh* object into its reference field and drops every other root to it, allocates enough to
    // provoke a collection, and only then reads the field back. A store that was not remembered
    // leaves `holder.next` pointing at a slot the collector reused.
    static int oldToYoung(int rounds) {
        WbCell holder = new WbCell();
        holder.tag = 7;
        int acc = 0;
        for (int i = 0; i < rounds; i++) {
            WbCell fresh = new WbCell();
            fresh.tag = i & 255;
            holder.next = fresh;
            fresh = null; // the field is now the only root
            for (int j = 0; j < 40; j++) {
                WbCell junk = new WbCell();
                junk.tag = j;
                acc = acc + (junk.tag & 1);
            }
            acc = acc + holder.next.tag; // must still be `i & 255`
        }
        return acc & 0xFFFF;
    }

    // `aaload`, and then `aaload` on what it produced: the read half of the pair, which owes the
    // collector nothing and needs no type check — only the bounds check `iaload` already had.
    static int spine(int n) {
        WbCell[][] grid = new WbCell[4][4];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                WbCell cell = new WbCell();
                cell.tag = r * 4 + c;
                grid[r][c] = cell;
            }
        }
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = acc + grid[i & 3][(i + 1) & 3].tag; // aaload; aaload; getfield
        }
        return acc;
    }

    public static int run() {
        int acc = 0;
        acc = acc * 31 + chainSum(400);
        acc = acc * 31 + oldToYoung(600);
        acc = acc * 31 + spine(500);
        return acc & 0x3FFFFFF;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}

class WbCell {
    WbCell next;
    int tag;
}
