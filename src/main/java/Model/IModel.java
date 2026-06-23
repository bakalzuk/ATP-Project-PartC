package Model;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.Solution;

import java.io.File;
import java.io.IOException;

/**
 * Defines the contract for the Model layer of the application.
 * The Model is responsible for:
 * - Communicating with the maze generating and maze solving servers.
 * - Holding the maze the user is currently playing.
 * - Holding the current position of the character inside that maze.
 * - Saving and loading mazes to and from disk.
 */
public interface IModel {

    /**
     * Starts the internal maze generating and maze solving servers.
     * Should be called once, when the application launches.
     */
    void startServers();

    /**
     * Stops the internal servers gracefully.
     * Should be called once, when the application shuts down.
     */
    void stopServers();

    /**
     * Requests a new maze from the maze generating server.
     * Runs asynchronously on a background thread and fires notifications when completed.
     *
     * @param rows Number of rows for the new maze.
     * @param cols Number of columns for the new maze.
     */
    void generateMaze(int rows, int cols);

    /**
     * Requests a solution for the maze currently being played from the maze solving server.
     * Runs asynchronously on a background thread and fires notifications when completed.
     */
    void solveCurrentMaze();

    /**
     * @return The maze currently being played, or null if no maze has been generated/loaded yet.
     */
    Maze getCurrentMaze();

    /**
     * @return The last solution received from the solving server, or null if the current maze
     * has not been solved yet.
     */
    Solution getLastSolution();

    /**
     * @return The current position of the character inside the current maze.
     */
    Position getCharacterPosition();

    /**
     * Attempts to move the character by the given row/column offsets (supports diagonal moves).
     *
     * @param rowDelta -1, 0 or 1 - the row offset of the requested move.
     * @param colDelta -1, 0 or 1 - the column offset of the requested move.
     * @return true if the move was valid and the character position was updated, false otherwise.
     */
    boolean moveCharacter(int rowDelta, int colDelta);

    /**
     * @return true if the character is currently standing on the goal position.
     */
    boolean isCharacterAtGoal();

    /**
     * Saves the current maze to the given file, compressed on disk.
     *
     * @param file The destination file.
     * @throws IOException if there is no current maze, or the file could not be written.
     */
    void saveMazeToFile(File file) throws IOException;

    /**
     * Loads a maze previously saved from the given file.
     * Replaces the current maze and resets the character to the loaded maze's start position.
     *
     * @param file The source file.
     * @throws IOException if the file could not be read or parsed.
     */
    void loadMazeFromFile(File file) throws IOException;

    /**
     * @return The thread pool size currently configured for the servers.
     */
    int getThreadPoolSize();

    /**
     * @return The name of the maze generating algorithm currently configured.
     */
    String getMazeGeneratingAlgorithmName();

    /**
     * @return The name of the maze searching algorithm currently configured.
     */
    String getMazeSearchingAlgorithmName();
}