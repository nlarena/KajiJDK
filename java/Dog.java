// Subclass: inherits Animal's `legs` (and its Speaker implementation), adds
// `tailLength`, and overrides sound() — at the same vtable slot it inherited.
class Dog extends Animal {
    int tailLength;

    Dog() {
        tailLength = 5;      // putfield Dog.tailLength
    }

    @Override
    public int sound() {     // overrides Animal.sound() at the same slot
        return 2;
    }
}
