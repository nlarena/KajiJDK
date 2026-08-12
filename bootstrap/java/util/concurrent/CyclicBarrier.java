package java.util.concurrent;

// Minimal java.util.concurrent.CyclicBarrier — N parties wait for each other; when the last
// arrives, all are released and the barrier **resets** for reuse. Monitor-based: the last party
// bumps a generation counter and `notifyAll`s; the others wait until their generation changes (so
// a spurious wake, or the next round, doesn't slip anyone through early).
public class CyclicBarrier {
    private final int parties;
    private int count;
    private int generation;

    public CyclicBarrier(int parties) {
        this.parties = parties;
        this.count = parties;
    }

    public synchronized int await() throws InterruptedException {
        int gen = this.generation;
        count = count - 1;
        int index = count; // parties-1 for the first to arrive, 0 for the last
        if (index == 0) {
            count = parties;     // reset for the next round
            generation = generation + 1;
            notifyAll();
            return 0;
        }
        while (gen == this.generation) {
            wait();
        }
        return index;
    }

    public int getParties() {
        return parties;
    }
}
