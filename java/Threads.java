// Green-threads demo: `main` spawns two worker threads and waits for them by spinning
// on per-worker flags (Phase 0 has no join() yet). The cooperative scheduler interleaves
// main + both workers at opcode granularity, so the workers make progress during the
// spin. Each worker does a small loop (spanning many scheduler ticks → real interleaving)
// then writes its own flag — distinct fields, so the result is race-free and stable.
class Worker extends Thread {
    int id;

    Worker(int id) {
        this.id = id;
    }

    public void run() {
        int x = 0;
        for (int i = 0; i < 50; i++) {
            x = x + 1;
        }
        if (id == 1) {
            Threads.aDone = x;
        } else {
            Threads.bDone = x;
        }
    }
}

public class Threads {
    static int aDone = 0;
    static int bDone = 0;

    static int run() {
        Worker a = new Worker(1);
        Worker b = new Worker(2);
        a.start();
        b.start();
        // Spin until both workers have signalled completion. Round-robin scheduling
        // runs the workers during these idle iterations.
        while (aDone == 0 || bDone == 0) {
        }
        return aDone + bDone; // 50 + 50 = 100
    }
}
