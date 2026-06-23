package View;

/**
 * Listener interface implemented by whoever wants to react to move requests
 * coming from a {@link MazeDisplayer} (keyboard presses or mouse dragging).
 * The widget itself never decides whether a move is legal - it only reports
 * "the user is asking to move by this offset", and leaves the decision
 * (and the actual position update) to the listener.
 */
public interface IMoveRequestListener {

    /**
     * Called when the user requested to move the character by the given offsets.
     *
     * @param rowDelta -1, 0 or 1 - requested row offset.
     * @param colDelta -1, 0 or 1 - requested column offset.
     */
    void onMoveRequested(int rowDelta, int colDelta);
}
