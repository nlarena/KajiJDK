// An interface: declares a method with no body. A call through a Speaker-typed
// reference compiles to invokeinterface, dispatched on the receiver's real class.
interface Speaker {
    int sound();
}
