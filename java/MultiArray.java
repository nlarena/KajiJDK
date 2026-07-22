// Exercises `multianewarray` (0xc5): the multidimensional allocation. Each negative
// return value pins a distinct failure, so a red test says *which* invariant broke.
public class MultiArray {
    public static int run() {
        // --- two dimensions: 3 rows of 4 ---------------------------------------
        int[][] grid = new int[3][4];
        if (grid.length != 3) return -1;
        if (grid[0].length != 4) return -2;

        grid[1][2] = 7;
        grid[2][3] = 35;

        // Rows must be *distinct objects*, not many references to one row — the
        // classic multianewarray bug (allocate the child once, store it N times).
        grid[0][0] = 1;
        if (grid[1][0] != 0) return -3;
        if (grid[2][0] != 0) return -4;

        // --- three dimensions: the recursion has to go all the way down ---------
        int[][][] cube = new int[2][3][4];
        if (cube.length != 2) return -5;
        if (cube[1].length != 3) return -6;
        if (cube[1][2].length != 4) return -7;
        cube[1][2][3] = 9;
        if (cube[0][0][0] != 0) return -8;

        // --- a byte grid: elements are 1 byte wide, not 4 ----------------------
        // If the innermost level were sized as int slots the rows would overlap and
        // this write would be readable from the wrong place.
        byte[][] flags = new byte[2][5];
        flags[1][4] = 3;
        if (flags[1][4] != 3) return -9;
        if (flags[0][4] != 0) return -10;

        return grid[1][2] + grid[2][3]; // 7 + 35 = 42
    }
}
