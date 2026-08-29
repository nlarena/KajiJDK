// Regression probe for #269: System.arraycopy has to use the array's REAL element width, and it
// has to behave as if the source were copied to a temporary first when the two ranges overlap.
//
// Before the fix the native assumed four bytes per element for every array, so a char[] was
// copied with twice the stride: the destination got garbage, nothing landed where it belonged,
// and the read ran off the end of the array -- which is where the "range end index N out of range
// for slice of length N-4" panics came from. Overlap was not considered at all.
//
// One bit per property, so a partial failure names itself. All seven → 127.
public class CopyProbe {

    static int run() {
        int score = 0;

        // char[], two bytes per element, overlapping DOWNWARD: "abcdefg" with [4,7) moved to 2
        // must read 'e' at index 2.
        char[] down = new char[7];
        down[0] = 'a';
        down[1] = 'b';
        down[2] = 'c';
        down[3] = 'd';
        down[4] = 'e';
        down[5] = 'f';
        down[6] = 'g';
        System.arraycopy(down, 4, down, 2, 3);
        if (down[2] == 'e' && down[3] == 'f' && down[4] == 'g' && down[0] == 'a') {
            score = score + 1;
        }

        // ...and UPWARD, the direction a naive forward loop destroys: [0,3) moved to 2 must leave
        // 'a','b','c' at 2,3,4 and not 'a','a','a'.
        char[] up = new char[7];
        up[0] = 'a';
        up[1] = 'b';
        up[2] = 'c';
        up[3] = 'd';
        up[4] = 'e';
        up[5] = 'f';
        up[6] = 'g';
        System.arraycopy(up, 0, up, 2, 3);
        if (up[2] == 'a' && up[3] == 'b' && up[4] == 'c') {
            score = score + 2;
        }

        // Two different char[]s: the plain case, which was ALSO broken.
        char[] src = new char[7];
        char[] dst = new char[7];
        src[4] = 'e';
        src[5] = 'f';
        src[6] = 'g';
        System.arraycopy(src, 4, dst, 2, 3);
        if (dst[2] == 'e' && dst[3] == 'f' && dst[4] == 'g' && dst[0] == 0) {
            score = score + 4;
        }

        // byte[], one byte per element.
        byte[] bytes = new byte[6];
        bytes[0] = 10;
        bytes[1] = 20;
        bytes[2] = 30;
        byte[] bytesOut = new byte[6];
        System.arraycopy(bytes, 0, bytesOut, 3, 3);
        if (bytesOut[3] == 10 && bytesOut[4] == 20 && bytesOut[5] == 30 && bytesOut[0] == 0) {
            score = score + 8;
        }

        // long[], eight bytes per element -- the other side of the wrong assumption.
        long[] longs = new long[4];
        longs[0] = 1234567890123L;
        longs[1] = -1L;
        long[] longsOut = new long[4];
        System.arraycopy(longs, 0, longsOut, 1, 2);
        if (longsOut[1] == 1234567890123L && longsOut[2] == -1L && longsOut[0] == 0L) {
            score = score + 16;
        }

        // int[], the width the old code assumed: this one always worked and must keep working.
        int[] ints = new int[4];
        ints[0] = 7;
        ints[1] = 8;
        int[] intsOut = new int[4];
        System.arraycopy(ints, 0, intsOut, 2, 2);
        if (intsOut[2] == 7 && intsOut[3] == 8 && intsOut[0] == 0) {
            score = score + 32;
        }

        // References: the copy has to go through the write barrier, and the values have to arrive.
        String[] refs = new String[3];
        refs[0] = "uno";
        refs[1] = "dos";
        String[] refsOut = new String[3];
        System.arraycopy(refs, 0, refsOut, 1, 2);
        if (refsOut[1] == refs[0] && refsOut[2] == refs[1] && refsOut[0] == null) {
            score = score + 64;
        }

        return score; // 127
    }
}
