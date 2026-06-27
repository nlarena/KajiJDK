// Base class. `population` is a STATIC field (one shared copy, in the Animal
// mirror). The constructor bumps it — population = population + 1 compiles to
// getstatic + iadd + putstatic — so every Animal (and Dog, via super()) counts.
class Animal implements Speaker {
    static int population;   // class-wide: a slot in the Animal Class<…> mirror
    int legs;                // per-instance: a slot in each object

    Animal() {
        legs = 4;                      // putfield (instance)
        population = population + 1;   // getstatic + putstatic (class)
    }

    public int sound() {     // implements Speaker.sound; virtual
        return 1;
    }
}
