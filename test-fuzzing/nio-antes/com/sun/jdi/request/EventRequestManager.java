package com.sun.jdi.request;

import com.sun.jdi.Field;
import com.sun.jdi.Location;
import com.sun.jdi.Mirror;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import java.util.List;

/**
 * Where every request comes from, and who keeps the list of those there are.
 *
 * <p>It is obtained from {@code VirtualMachine.eventRequestManager()}. There is one
 * {@code createXxx} per event type and one {@code xxxRequests()} for enumerating the live
 * ones.
 *
 * <p>{@link #deleteEventRequest} is not optional: the requests live in the other VM and cost
 * there. A debugger that creates requests and does not delete them degrades the debugged
 * program.
 *
 * @since 1.3
 */
public interface EventRequestManager extends Mirror {

    /**
     * It creates a class prepare request.
     *
     * @return the result
     */
    ClassPrepareRequest createClassPrepareRequest();

    /**
     * It creates a class unload request.
     *
     * @return the result
     */
    ClassUnloadRequest createClassUnloadRequest();

    /**
     * It creates a thread start request.
     *
     * @return the result
     */
    ThreadStartRequest createThreadStartRequest();

    /**
     * It creates a thread death request.
     *
     * @return the result
     */
    ThreadDeathRequest createThreadDeathRequest();

    /**
     * It creates a exception request.
     *
     * @param type the ReferenceType
     * @param flag the boolean
     * @param flag2 the boolean
     * @return the result
     */
    ExceptionRequest createExceptionRequest(ReferenceType type, boolean flag, boolean flag2);

    /**
     * It creates a method entry request.
     *
     * @return the result
     */
    MethodEntryRequest createMethodEntryRequest();

    /**
     * It creates a method exit request.
     *
     * @return the result
     */
    MethodExitRequest createMethodExitRequest();

    /**
     * It creates a monitor contended enter request.
     *
     * @return the result
     */
    MonitorContendedEnterRequest createMonitorContendedEnterRequest();

    /**
     * It creates a monitor contended entered request.
     *
     * @return the result
     */
    MonitorContendedEnteredRequest createMonitorContendedEnteredRequest();

    /**
     * It creates a monitor wait request.
     *
     * @return the result
     */
    MonitorWaitRequest createMonitorWaitRequest();

    /**
     * It creates a monitor waited request.
     *
     * @return the result
     */
    MonitorWaitedRequest createMonitorWaitedRequest();

    /**
     * It creates a step request.
     *
     * @param thread the ThreadReference
     * @param index the int
     * @param index2 the int
     * @return the result
     */
    StepRequest createStepRequest(ThreadReference thread, int index, int index2);

    /**
     * It creates a breakpoint request.
     *
     * @param location the Location
     * @return the result
     */
    BreakpointRequest createBreakpointRequest(Location location);

    /**
     * It creates a access watchpoint request.
     *
     * @param field the Field
     * @return the result
     */
    AccessWatchpointRequest createAccessWatchpointRequest(Field field);

    /**
     * It creates a modification watchpoint request.
     *
     * @param field the Field
     * @return the result
     */
    ModificationWatchpointRequest createModificationWatchpointRequest(Field field);

    /**
     * It creates a v m death request.
     *
     * @return the result
     */
    VMDeathRequest createVMDeathRequest();

    /**
     * The delete event request.
     *
     * @param request the EventRequest
     */
    void deleteEventRequest(EventRequest request);

    /**
     * The delete event requests.
     *
     * @param values the List<? extends EventRequest>
     */
    void deleteEventRequests(List<? extends EventRequest> values);

    /**
     * The delete all breakpoints.
     */
    void deleteAllBreakpoints();

    /**
     * The step requests.
     *
     * @return the result
     */
    List<StepRequest> stepRequests();

    /**
     * The class prepare requests.
     *
     * @return the result
     */
    List<ClassPrepareRequest> classPrepareRequests();

    /**
     * The class unload requests.
     *
     * @return the result
     */
    List<ClassUnloadRequest> classUnloadRequests();

    /**
     * The thread start requests.
     *
     * @return the result
     */
    List<ThreadStartRequest> threadStartRequests();

    /**
     * The thread death requests.
     *
     * @return the result
     */
    List<ThreadDeathRequest> threadDeathRequests();

    /**
     * The exception requests.
     *
     * @return the result
     */
    List<ExceptionRequest> exceptionRequests();

    /**
     * The breakpoint requests.
     *
     * @return the result
     */
    List<BreakpointRequest> breakpointRequests();

    /**
     * The access watchpoint requests.
     *
     * @return the result
     */
    List<AccessWatchpointRequest> accessWatchpointRequests();

    /**
     * The modification watchpoint requests.
     *
     * @return the result
     */
    List<ModificationWatchpointRequest> modificationWatchpointRequests();

    /**
     * The method entry requests.
     *
     * @return the result
     */
    List<MethodEntryRequest> methodEntryRequests();

    /**
     * The method exit requests.
     *
     * @return the result
     */
    List<MethodExitRequest> methodExitRequests();

    /**
     * The monitor contended enter requests.
     *
     * @return the result
     */
    List<MonitorContendedEnterRequest> monitorContendedEnterRequests();

    /**
     * The monitor contended entered requests.
     *
     * @return the result
     */
    List<MonitorContendedEnteredRequest> monitorContendedEnteredRequests();

    /**
     * The monitor wait requests.
     *
     * @return the result
     */
    List<MonitorWaitRequest> monitorWaitRequests();

    /**
     * The monitor waited requests.
     *
     * @return the result
     */
    List<MonitorWaitedRequest> monitorWaitedRequests();

    /**
     * The vm death requests.
     *
     * @return the result
     */
    List<VMDeathRequest> vmDeathRequests();
}
