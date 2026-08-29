/**
 * `ACC_SYNCHRONIZED` is dropped from a method's flags, so `wait`/`notifyAll` inside a
 * `synchronized` method run without the monitor and throw IllegalMonitorStateException.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_255.java
 *   bin/jvm.exe -v KajiLibrary/repros/finding_255.class
 *   bin/run-headless.exe KajiLibrary/repros/finding_255.class avisa
 *
 * Expected of `avisa`: `flags: (0x0021) ACC_PUBLIC, ACC_SYNCHRONIZED` and a run returning 1.
 * Actual: `flags: (0x0001) ACC_PUBLIC` and the run returns nothing.
 *
 * This is what makes every wait/notify design in KajiLibrary unrunnable, and it also means the
 * classes that rely on synchronized METHODS for atomicity are not actually atomic.
 */
public class finding_255 {

    private int n;

    /** A synchronized method that signals: it needs the monitor the flag was supposed to take. */
    public synchronized int avisa() {
        this.n = this.n + 1;
        this.notifyAll();
        return this.n;
    }

    /** The control: the same method without the signal runs fine, flag or no flag. */
    public synchronized int cuenta() {
        this.n = this.n + 1;
        return this.n;
    }

    public static int llamaAvisa() {
        finding_255 it = new finding_255();
        return it.avisa();
    }

    public static int llamaCuenta() {
        finding_255 it = new finding_255();
        return it.cuenta();
    }
}
