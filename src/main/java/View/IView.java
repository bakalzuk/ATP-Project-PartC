package View;

import algorithms.mazeGenerators.Position;

import java.util.List;

/**
 * Defines the contract for the View layer of the application.
 * Most of the live data flow (the maze itself, the character position, the solution)
 * is handled automatically through property bindings to the ViewModel, but these methods
 * cover the explicit, one-off display actions the View needs to support.
 */
public interface IView {

    /**
     * Displays an error message to the user.
     *
     * @param message The error message to display.
     */
    void showError(String message);

    /**
     * Displays an informational message to the user.
     *
     * @param title   The title of the message window.
     * @param message The message body to display.
     */
    void showInfo(String title, String message);

    /**
     * Displays the given solution path on top of the maze board.
     *
     * @param path The ordered list of positions making up the solution.
     */
    void displaySolution(List<Position> path);

    /** Clears any solution path currently displayed on the maze board. */
    void clearSolution();

    /** Called when the player's character reaches the goal position. */
    void onMazeSolved();
}
