// Marcado transitivo por ESTÁTICO: el Animal cuelga solo de StatBox.shared (un static).
// El GC lo alcanza vía el mirror Class<StatBox> → su slot estático.
class Stat {
    static int run() {
        StatBox.shared = new Animal();  // alcanzable solo por el campo estático
        return StatBox.shared.legs;     // 4
    }
}
