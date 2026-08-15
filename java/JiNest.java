// F3 step 8 — **nested inlining, and the frames in the middle of the chain**.
//
// `outer` expands `middle`, which expands `inner`: three method bodies in one compiled function,
// which is the depth limit. A guard inside `inner` therefore has to hand back three interpreter
// frames, and the interesting one is the middle: a frame that is simultaneously a *callee* (it was
// inlined) and a *caller* (it is parked at an invoke of its own).
//
// Two things about such a frame are easy to get wrong and are both wired into the answer here.
//
// **Its operand stack stops short of the arguments.** Each of these methods leaves something on its
// stack *below* the call's arguments — the `7` and the `100` — so at each invoke the live operands
// are one deep and the arguments above them belong to the frame below now, as its locals. A rebuilt
// frame that kept them would push every argument twice and the arithmetic after the call would read
// the wrong operands.
//
// **The deopt has to be survivable to be observable.** `inner` catches its own exception, so the
// guard fails every fourth call, the interpreter re-executes the array read in the rebuilt frame,
// `inner`'s handler catches it, and control then flows back *through* the rebuilt `middle` and
// `outer` frames — doing the multiply and the adds that read exactly the operands this test is
// about.
public class JiNest {
    static int inner(int[] arr, int i) {
        try {
            return arr[i];
        } catch (ArrayIndexOutOfBoundsException e) {
            return 9;
        }
    }

    static int middle(int[] arr, int i) {
        return 100 + inner(arr, i) * 2;
    }

    static int outer(int[] arr, int i) {
        return 7 + middle(arr, i) * 3;
    }

    static int run() {
        int[] arr = { 2, 3, 5 };
        int acc = 0;
        for (int i = 0; i < 400; i++) {
            acc = (acc + outer(arr, i % 4)) & 0xFFFFF;
        }
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
