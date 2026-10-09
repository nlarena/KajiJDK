package com.sun.jdi;

/**
 * A mirror of something that lives in the virtual machine **on the other side**.
 *
 * <p>It is the root of all JDI, and the word "mirror" is not decoration: a debugger does not
 * have the debugged program's objects, it has representatives of them. An `ObjectReference` is
 * not *the* object: it is an identifier that, every time it is consulted, crosses the JDWP wire
 * and asks.
 *
 * <p>From there comes this interface's only operation: given any mirror, knowing **which VM**
 * it belongs to. Two mirrors of different VMs cannot be mixed, and without this there would be
 * no way of checking it.
 */
public interface Mirror {

    /** The virtual machine this mirror is a mirror of. */
    VirtualMachine virtualMachine();

    /** A readable description; the exact form depends on the implementation. */
    String toString();
}
