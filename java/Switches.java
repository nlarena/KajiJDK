public class Switches {
    int t(int x) { switch (x) { case 1: return 10; case 2: return 20; case 3: return 30; default: return 0; } }
    int l(int x) { switch (x) { case 1: return 10; case 100: return 20; case 1000: return 30; default: return 0; } }
    int[] arr() { return new int[5]; }
    byte[] b() { return new byte[3]; }
}
