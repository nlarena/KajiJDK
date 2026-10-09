package jdk.management.jfr;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.util.List;
import java.util.Map;

/**
 * JFR handled by JMX: recording in one VM from another process.
 *
 * <h2>Why it exists if {@code jdk.jfr} is already there</h2>
 *
 * <p>Because {@code jdk.jfr} can only be used from inside the VM one wants to observe. A monitoring
 * console is outside, and the only thing that crosses is JMX.
 *
 * <p>It is also what makes it possible to record in a process that was not written for that:
 * nothing has to be added to it, connecting is enough.
 *
 * <h2>Why everything is handled with a {@code long}</h2>
 *
 * <p>{@link #newRecording} returns a number and every other method receives it. It is so because a
 * {@link jdk.jfr.Recording} object cannot travel through JMX: the only thing that crosses are open
 * types. The number is the identifier of the recording on the other side, and this interface is a
 * remote control over it.
 *
 * <p>The same with {@link #openStream}, which returns another number: the data are brought in
 * pieces with {@link #readStream}, until it returns {@code null}. A recording may weigh hundreds of
 * megabytes and cannot be sent in one go.
 *
 * <h2>The two ways of taking the data away</h2>
 *
 * <p>{@link #copyTo} asks the remote VM to write the file <strong>on its own disk</strong>; the
 * stream brings it over the network. The first one is much faster and leaves the file over there,
 * which is of use when somebody is going to fetch it later.
 *
 * @since 9
 */
public interface FlightRecorderMXBean extends PlatformManagedObject {

    /** The name of the MBean in the platform server. */
    String MXBEAN_NAME = "jdk.management.jfr:type=FlightRecorder";

    /**
     * It creates a recording and returns its identifier.
     *
     * @return the identifier
     * @throws IllegalStateException if JFR is not available
     */
    long newRecording() throws IllegalStateException;

    /**
     * A recording with whatever is in the buffers at this moment.
     *
     * @return the identifier of the snapshot
     */
    long takeSnapshot();

    /**
     * It copies a recording.
     *
     * @param recordingId the identifier of the original
     * @param stop whether the copy is left stopped
     * @return the identifier of the copy
     * @throws IllegalArgumentException if that recording does not exist
     */
    long cloneRecording(long recordingId, boolean stop) throws IllegalArgumentException;

    /**
     * It starts a recording.
     *
     * @param recordingId the identifier
     * @throws IllegalStateException if it already started or was closed
     */
    void startRecording(long recordingId) throws IllegalStateException;

    /**
     * It stops a recording.
     *
     * @param recordingId the identifier
     * @return whether it was recording
     * @throws IllegalArgumentException if that recording does not exist
     * @throws IllegalStateException if it cannot be stopped
     */
    boolean stopRecording(long recordingId) throws IllegalArgumentException, IllegalStateException;

    /**
     * It closes a recording and releases its data.
     *
     * @param recordingId the identifier
     * @throws IOException if it could not be closed
     */
    void closeRecording(long recordingId) throws IOException;

    /**
     * It opens a stream in order to bring over the data of a recording.
     *
     * @param recordingId the identifier of the recording
     * @param streamOptions options of the stream, such as the interval of time
     * @return the identifier of the stream
     * @throws IOException if it could not be opened
     */
    long openStream(long recordingId, Map<String, String> streamOptions) throws IOException;

    /**
     * It closes a stream.
     *
     * @param streamId the identifier of the stream
     * @throws IOException if it could not be closed
     */
    void closeStream(long streamId) throws IOException;

    /**
     * The next piece of a stream.
     *
     * @param streamId the identifier of the stream
     * @return the bytes, or {@code null} when nothing is left
     * @throws IOException if it could not be read
     */
    byte[] readStream(long streamId) throws IOException;

    /**
     * The options of a recording: name, duration, destination, limits.
     *
     * @param recordingId the identifier
     * @return the options
     * @throws IllegalArgumentException if that recording does not exist
     */
    Map<String, String> getRecordingOptions(long recordingId) throws IllegalArgumentException;

    /**
     * The event settings of a recording.
     *
     * <p>Different from {@link #getRecordingOptions}: the settings say what to record, the options
     * say how.
     *
     * @param recordingId the identifier
     * @return the settings
     * @throws IllegalArgumentException if that recording does not exist
     */
    Map<String, String> getRecordingSettings(long recordingId) throws IllegalArgumentException;

    /**
     * It sets the settings from the text of a {@code .jfc} file.
     *
     * @param recordingId the identifier
     * @param contents the contents of the file
     * @throws IllegalArgumentException if that recording does not exist or the contents do not
     *     serve
     */
    void setConfiguration(long recordingId, String contents) throws IllegalArgumentException;

    /**
     * It sets the settings from an installed configuration, by name.
     *
     * @param recordingId the identifier
     * @param name the name, for example {@code "default"}
     * @throws IllegalArgumentException if that recording or that configuration does not exist
     */
    void setPredefinedConfiguration(long recordingId, String name) throws IllegalArgumentException;

    /**
     * It sets the event settings.
     *
     * @param recordingId the identifier
     * @param settings the settings
     * @throws IllegalArgumentException if that recording does not exist
     */
    void setRecordingSettings(long recordingId, Map<String, String> settings)
            throws IllegalArgumentException;

    /**
     * It sets the options of the recording.
     *
     * @param recordingId the identifier
     * @param options the options
     * @throws IllegalArgumentException if that recording does not exist
     */
    void setRecordingOptions(long recordingId, Map<String, String> options)
            throws IllegalArgumentException;

    /**
     * The recordings there are in the remote VM.
     *
     * @return the recordings
     */
    List<RecordingInfo> getRecordings();

    /**
     * The configurations installed in the remote VM.
     *
     * @return the configurations
     */
    List<ConfigurationInfo> getConfigurations();

    /**
     * The types of event the remote VM knows.
     *
     * @return the types
     */
    List<EventTypeInfo> getEventTypes();

    /**
     * It asks the remote VM to write the recording on its own disk.
     *
     * @param recordingId the identifier
     * @param outputFile the path, interpreted on the remote machine
     * @throws IOException if it could not be written
     */
    void copyTo(long recordingId, String outputFile) throws IOException;
}
