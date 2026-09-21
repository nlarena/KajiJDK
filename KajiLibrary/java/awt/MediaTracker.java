package java.awt;

import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Waits for a group of images to finish loading.
 *
 * <p>It solves a concrete problem of AWT: {@link Toolkit#getImage} returns on the spot an {@link
 * Image} that **has no pixels yet**, and the loading goes on in another thread. Without this, a
 * program that wants to draw three images together would have to implement {@link ImageObserver}
 * and keep count by hand.
 *
 * <p>The images are grouped by an **identifier** chosen by whoever adds them, and everything can be
 * asked by group or about the whole set. The idea is to load by priorities: wait for group 0 —what
 * is needed to show something— and leave the rest loading.
 *
 * <p>The distinction between `checkID` and `statusID` is the one that confuses most: `check` asks
 * **whether it finished**, `status` returns **what it is up to** as a combination of the four
 * flags. And both have a variant that starts the loading and one that does not —the `boolean
 * load`—, because asking about an image should not force downloading it.
 */
public class MediaTracker implements Serializable {

    private static final long serialVersionUID = -483174189758638095L;

    /** The image is loading. */
    public static final int LOADING = 1;

    /** The loading was aborted. */
    public static final int ABORTED = 2;

    /** The loading failed. */
    public static final int ERRORED = 4;

    /** The image finished loading fine. */
    public static final int COMPLETE = 8;

    /** The component they are going to be drawn on; it is the one that watches the loading. */
    Component target;

    /** A tracked image, with its group and the size asked for. */
    private static final class Tracked implements ImageObserver {
        private final Image image;
        private final int id;
        private final int width;
        private final int height;
        private int status;
        private boolean started;

        private Tracked(Image image, int id, int width, int height) {
            this.image = image;
            this.id = id;
            this.width = width;
            this.height = height;
        }

        /**
         * Receives the notices of the loading.
         *
         * @return `true` while something is still missing, which is how {@link ImageObserver} says
         *     "keep telling me"
         */
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
            synchronized (this) {
                if ((infoflags & ImageObserver.ERROR) != 0) {
                    this.status = ERRORED;
                } else if ((infoflags & ImageObserver.ABORT) != 0) {
                    this.status = ABORTED;
                } else if ((infoflags & ImageObserver.ALLBITS) != 0) {
                    this.status = COMPLETE;
                }
                this.notifyAll();
            }
            return (this.status & (COMPLETE | ERRORED | ABORTED)) == 0;
        }
    }

    /** The tracked images, in the order they were added. */
    private final ArrayList<Tracked> tracked = new ArrayList<Tracked>();

    /**
     * A tracker for the images that are going to be drawn on that component.
     *
     * @param comp the component; it is the one that will receive the repaint notices
     */
    public MediaTracker(Component comp) {
        this.target = comp;
    }

    /** Adds an image to that group, without asking for any particular size. */
    public void addImage(Image image, int id) {
        this.addImage(image, id, -1, -1);
    }

    /**
     * Adds an image to that group, asking that it be scaled to that size.
     *
     * <p>An image can be added **more than once** with different sizes: each entry is tracked
     * separately, which is what makes it possible to wait for the thumbnail without waiting for the
     * big one.
     */
    public synchronized void addImage(Image image, int id, int w, int h) {
        this.tracked.add(new Tracked(image, id, w, h));
    }

    /**
     * Whether **all** the images finished.
     *
     * <p>It starts no loading: it asks and leaves.
     */
    public boolean checkAll() {
        return this.checkAll(false);
    }

    /**
     * Whether they all finished, starting the loading of those that have not begun if `load` is
     * `true`.
     *
     * @return `true` if none was left loading; one that failed or was aborted also counts as
     *     finished, because it is not going to change any more
     */
    public boolean checkAll(boolean load) {
        return this.check(-1, false, load);
    }

    /** Whether any failed. */
    public synchronized boolean isErrorAny() {
        return this.errored(-1).length > 0;
    }

    /**
     * The images that failed.
     *
     * @return the images, or `null` if none failed
     */
    public synchronized Object[] getErrorsAny() {
        Object[] r = this.errored(-1);
        return r.length == 0 ? null : r;
    }

    /**
     * Waits for them all to finish.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void waitForAll() throws InterruptedException {
        this.waitForAll(0);
    }

    /**
     * Waits for them all to finish, up to that time.
     *
     * <p>It returns `true` only if they all finished **and finished well**. An empty tracker
     * returns `false`, which surprises until it is read the right way round: the question is not
     * "did I stop waiting?" but "are they all loaded?", and with no image the answer is no. One
     * that failed or was aborted also gives `false`, even though it is no longer loading.
     *
     * @param ms how long to wait at most; 0 means no limit, and a negative one is treated the same
     *     way —the JDK takes it as time already up and answers `false`, this one waits
     * @return `true` if they all finished well
     * @throws InterruptedException if the thread is interrupted
     */
    public synchronized boolean waitForAll(long ms) throws InterruptedException {
        return this.awaitGroup(-1, ms);
    }

    /**
     * What they are all up to, as a combination of the four flags.
     *
     * @param load whether to start the loading of those that have not begun
     */
    public int statusAll(boolean load) {
        return this.status(-1, load);
    }

    /** Whether that group finished. */
    public boolean checkID(int id) {
        return this.checkID(id, false);
    }

    /** Whether that group finished, starting the loading if `load` is `true`. */
    public boolean checkID(int id, boolean load) {
        return this.check(id, true, load);
    }

    /** Whether any of that group failed. */
    public synchronized boolean isErrorID(int id) {
        return this.errored(id).length > 0;
    }

    /**
     * The ones of that group that failed.
     *
     * @return the images, or `null` if none failed
     */
    public synchronized Object[] getErrorsID(int id) {
        Object[] r = this.errored(id);
        return r.length == 0 ? null : r;
    }

    /**
     * Waits for that group to finish.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public void waitForID(int id) throws InterruptedException {
        this.waitForID(id, 0);
    }

    /**
     * Waits for that group to finish, up to that time.
     *
     * @return `true` only if all of the group finished well; see {@link #waitForAll(long)}
     * @throws InterruptedException if the thread is interrupted
     */
    public synchronized boolean waitForID(int id, long ms) throws InterruptedException {
        return this.awaitGroup(id, ms);
    }

    /** What that group is up to. */
    public int statusID(int id, boolean load) {
        return this.status(id, load);
    }

    /** Stops tracking that image, in every group and size. */
    public synchronized void removeImage(Image image) {
        int i = 0;
        while (i < this.tracked.size()) {
            if (this.tracked.get(i).image == image) {
                this.tracked.remove(i);
            } else {
                i = i + 1;
            }
        }
        this.notifyAll();
    }

    /** Stops tracking it in that group. */
    public synchronized void removeImage(Image image, int id) {
        int i = 0;
        while (i < this.tracked.size()) {
            Tracked a = this.tracked.get(i);
            if (a.image == image && a.id == id) {
                this.tracked.remove(i);
            } else {
                i = i + 1;
            }
        }
        this.notifyAll();
    }

    /** Stops tracking it in that group and with that exact size. */
    public synchronized void removeImage(Image image, int id, int width, int height) {
        int i = 0;
        while (i < this.tracked.size()) {
            Tracked a = this.tracked.get(i);
            if (a.image == image && a.id == id && a.width == width && a.height == height) {
                this.tracked.remove(i);
            } else {
                i = i + 1;
            }
        }
        this.notifyAll();
    }

    /**
     * Wakes up whoever is waiting.
     *
     * <p>It marks nothing as finished, against what this note used to say: it only notifies. It is
     * package-private and nothing in the library calls it.
     */
    synchronized void setDone() {
        this.notifyAll();
    }

    /**
     * Starts the loading of a tracked image.
     *
     * <p>Asking it for its width with the observer set is what makes the image's source begin to
     * produce pixels: it is the way to start a load in AWT, and it is so far from obvious that it
     * is worth saying.
     */
    private void start(Tracked a) {
        if (a.started) {
            return;
        }
        a.started = true;
        a.status = LOADING;
        int width = a.image.getWidth(a);
        if (width >= 0) {
            // The image already had its dimensions, so it is already whole in memory.
            a.status = COMPLETE;
        }
    }

    /** The tracked ones of that group, or all of them if `id` is -1 and `byGroup` is `false`. */
    private ArrayList<Tracked> select(int id, boolean byGroup) {
        ArrayList<Tracked> out = new ArrayList<Tracked>();
        for (int i = 0; i < this.tracked.size(); i++) {
            Tracked a = this.tracked.get(i);
            if (!byGroup || a.id == id) {
                out.add(a);
            }
        }
        return out;
    }

    /** Whether the ones of the group asked for finished. */
    private boolean check(int id, boolean byGroup, boolean load) {
        ArrayList<Tracked> as;
        synchronized (this) {
            as = this.select(id, byGroup);
        }
        boolean all = true;
        for (int i = 0; i < as.size(); i++) {
            Tracked a = as.get(i);
            if (load) {
                synchronized (this) {
                    this.start(a);
                }
            }
            if ((a.status & (COMPLETE | ERRORED | ABORTED)) == 0) {
                all = false;
            }
        }
        return all;
    }

    /** The combination of flags of the group asked for. */
    private int status(int id, boolean load) {
        ArrayList<Tracked> as;
        synchronized (this) {
            as = this.select(id, id != -1);
        }
        int r = 0;
        for (int i = 0; i < as.size(); i++) {
            Tracked a = as.get(i);
            if (load) {
                synchronized (this) {
                    this.start(a);
                }
            }
            r = r | a.status;
        }
        return r;
    }

    /** The images of the group asked for that failed. */
    private Object[] errored(int id) {
        ArrayList<Object> out = new ArrayList<Object>();
        ArrayList<Tracked> as = this.select(id, id != -1);
        for (int i = 0; i < as.size(); i++) {
            if ((as.get(i).status & ERRORED) != 0) {
                out.add(as.get(i).image);
            }
        }
        return out.toArray();
    }

    /**
     * Waits for the group asked for, with or without a time limit.
     *
     * <p>Only the **first** round starts the loads. The following ones are pure waiting: starting
     * again what is already started would do nothing and would walk the whole list on every notice.
     */
    private boolean awaitGroup(int id, long ms) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ms;
        boolean first = true;
        while (true) {
            int st = this.status(id, first);
            first = false;
            if ((st & LOADING) == 0) {
                return st == COMPLETE;
            }
            if (ms > 0) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    return false;
                }
                this.wait(left);
            } else {
                this.wait();
            }
        }
    }
}
