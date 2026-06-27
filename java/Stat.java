// Marcado transitivo por ESTÁTICO: el Animal cuelga solo de Box.shared (un static).
// El GC lo alcanza vía el mirror Class<Box> → su slot estático.
class Stat {
    static int run() {
        Box.shared = new Animal();  // alcanzable solo por el campo estático
        return Box.shared.legs;     // 4
    }
}
