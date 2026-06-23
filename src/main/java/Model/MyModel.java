package Model;

import Client.Client;
import Client.IClientStrategy;
import IO.MyCompressorOutputStream;
import IO.MyDecompressorInputStream;
import Server.Configurations;
import Server.Server;
import Server.ServerStrategyGenerateMaze;
import Server.ServerStrategySolveSearchProblem;
import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.AState;
import algorithms.search.MazeState;
import algorithms.search.SearchableMaze;
import algorithms.search.Solution;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Observable;

/**
 * Concrete implementation of {@link IModel}.
 * Extends {@link Observable} to fully integrate with the architectural Observer pattern.
 * Manages background network communication tasks with generation and solving servers.
 */
public class MyModel extends Observable implements IModel {

    private static final int GENERATE_MAZE_PORT = 5400;
    private static final int SOLVE_MAZE_PORT = 5401;
    private static final int LISTENING_INTERVAL_MS = 1000;

    private final Server mazeGeneratingServer;
    private final Server solveSearchProblemServer;

    private Maze currentMaze;
    private Position characterPosition;
    private Solution lastSolution;

    /**
     * Constructs the Model and prepares the two internal infrastructure execution servers.
     */
    public MyModel() {
        mazeGeneratingServer = new Server(GENERATE_MAZE_PORT, LISTENING_INTERVAL_MS, new ServerStrategyGenerateMaze());
        solveSearchProblemServer = new Server(SOLVE_MAZE_PORT, LISTENING_INTERVAL_MS, new ServerStrategySolveSearchProblem());
    }

    @Override
    public void startServers() {
        mazeGeneratingServer.start();
        solveSearchProblemServer.start();
    }

    @Override
    public void stopServers() {
        mazeGeneratingServer.stop();
        solveSearchProblemServer.stop();
    }

    @Override
    public void generateMaze(int rows, int cols) {
        new Thread(() -> {
            try {
                Client client = new Client(InetAddress.getLocalHost(), GENERATE_MAZE_PORT, new IClientStrategy() {
                    @Override
                    public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {
                        try {
                            ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                            ObjectInputStream fromServer = new ObjectInputStream(inFromServer);
                            toServer.flush();

                            toServer.writeObject(new int[]{rows, cols});
                            toServer.flush();

                            byte[] compressedMaze = (byte[]) fromServer.readObject();
                            InputStream decompressor = new MyDecompressorInputStream(new ByteArrayInputStream(compressedMaze));
                            byte[] decompressedMazeBytes = readAllBytes(decompressor);

                            currentMaze = new Maze(decompressedMazeBytes);
                            characterPosition = currentMaze.getStartPosition();
                            lastSolution = null;

                            setChanged();
                            notifyObservers("maze_generated");

                        } catch (Exception e) {
                            setChanged();
                            notifyObservers("error: Failed to generate maze: " + e.getMessage());
                        }
                    }
                });
                client.communicateWithServer();
            } catch (IOException e) {
                setChanged();
                notifyObservers("error: Could not connect to the maze generating server: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void solveCurrentMaze() {
        if (currentMaze == null) {
            setChanged();
            notifyObservers("error: There is no maze to solve yet.");
            return;
        }
        final Maze mazeToSolve = currentMaze;

        new Thread(() -> {
            try {
                Client client = new Client(InetAddress.getLocalHost(), SOLVE_MAZE_PORT, new IClientStrategy() {
                    @Override
                    public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {
                        try {
                            ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                            ObjectInputStream fromServer = new ObjectInputStream(inFromServer);
                            toServer.flush();

                            toServer.writeObject(mazeToSolve);
                            toServer.flush();

                            lastSolution = (Solution) fromServer.readObject();

                            setChanged();
                            notifyObservers("maze_solved");

                        } catch (Exception e) {
                            setChanged();
                            notifyObservers("error: Failed to solve maze: " + e.getMessage());
                        }
                    }
                });
                client.communicateWithServer();
            } catch (IOException e) {
                setChanged();
                notifyObservers("error: Could not connect to the maze solving server: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public Maze getCurrentMaze() {
        return currentMaze;
    }

    @Override
    public Solution getLastSolution() {
        return lastSolution;
    }

    @Override
    public Position getCharacterPosition() {
        return characterPosition;
    }

    @Override
    public boolean moveCharacter(int rowDelta, int colDelta) {
        if (currentMaze == null || characterPosition == null) {
            return false;
        }

        int targetRow = characterPosition.getRowIndex() + rowDelta;
        int targetCol = characterPosition.getColumnIndex() + colDelta;

        SearchableMaze searchableMaze = new SearchableMaze(currentMaze);
        MazeState currentState = new MazeState(characterPosition);
        ArrayList<AState> neighbors = searchableMaze.getAllPossibleStates(currentState);

        for (AState neighbor : neighbors) {
            Position neighborPos = ((MazeState) neighbor).getPosition();
            if (neighborPos.getRowIndex() == targetRow && neighborPos.getColumnIndex() == targetCol) {
                characterPosition = neighborPos;

                setChanged();
                notifyObservers("character_moved");
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCharacterAtGoal() {
        return currentMaze != null
                && characterPosition != null
                && characterPosition.equals(currentMaze.getGoalPosition());
    }

    @Override
    public void saveMazeToFile(File file) throws IOException {
        if (currentMaze == null) {
            throw new IOException("There is no maze to save.");
        }
        OutputStream out = new MyCompressorOutputStream(new FileOutputStream(file));
        out.write(currentMaze.toByteArray());
        out.flush();
        out.close();
    }

    @Override
    public void loadMazeFromFile(File file) throws IOException {
        InputStream in = new MyDecompressorInputStream(new FileInputStream(file));
        byte[] mazeBytes = readAllBytes(in);
        in.close();

        currentMaze = new Maze(mazeBytes);
        characterPosition = currentMaze.getStartPosition();
        lastSolution = null;

        setChanged();
        notifyObservers("maze_loaded");
    }

    @Override
    public int getThreadPoolSize() {
        return Configurations.getInstance().getThreadPoolSize();
    }

    @Override
    public String getMazeGeneratingAlgorithmName() {
        return Configurations.getInstance().getMazeGeneratingAlgorithm();
    }

    @Override
    public String getMazeSearchingAlgorithmName() {
        return Configurations.getInstance().getMazeSearchingAlgorithm();
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
}