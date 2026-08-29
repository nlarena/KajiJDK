public class Labeled {
  public int find(int[][] m, int t) {
    int r = -1;
    outer:
    for (int i = 0; i < m.length; i++) {
      for (int j = 0; j < m[i].length; j++) {
        if (m[i][j] == t) { r = i * 100 + j; break outer; }
      }
    }
    return r;
  }
  public int skip(int n) {
    int c = 0;
    loop:
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (j == i) continue loop;
        c++;
      }
    }
    return c;
  }
}
