public class AsGcProbe {
    static int run() {
        long[][] holder = new long[64][];
        int ok = 0;
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 50; j++) {
                Object garbage = new long[64]; // churn: fuerza minor GCs entre stores
            }
            holder[i] = new long[8]; // aastore de [J en [[J — nunca debe lanzar ASE
            ok++;
        }
        return ok; // 64
    }
}
