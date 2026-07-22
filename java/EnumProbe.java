// Enums ran even before `java.lang.Enum` existed: the constants are built by the class's
// <clinit> and the unresolvable superclass <init> no-opped, so identity comparison was
// already right. With a real java.lang.Enum present they also carry their state, which
// is what the last checks pin.
//
// The enum is `Tone`, not `Color`: `java/` already holds a `Color` fixture (public, with
// a third constant) that a same-named enum here would overwrite.
public class EnumProbe {
    public static int run() {
        Tone c = Tone.GREEN;
        if (c == Tone.RED) return -1;
        if (c != Tone.GREEN) return -2;

        // Only true if Enum's constructor actually ran.
        if (c.ordinal() != 1) return -3;
        if (Tone.RED.ordinal() != 0) return -4;
        if (!c.name().equals("GREEN")) return -5;

        return 42;
    }
}

enum Tone { RED, GREEN }
