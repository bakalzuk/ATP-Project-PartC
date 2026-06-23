package ViewModel;

import Model.IModel;
import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import algorithms.search.Solution;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

/**
 * The ViewModel layer of the application.
 * Connects the View to the Model. Implements Observer interface to listen to
 * Model state modifications, and extends Observable to emit notifications up to the View.
 */
public class MyViewModel extends Observable implements Observer {

    private final IModel model;

    private final SimpleObjectProperty<Maze> maze = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Position> characterPosition = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Solution> solution = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty mazeSolvedByPlayer = new SimpleBooleanProperty(false);
    private final SimpleStringProperty errorMessage = new SimpleStringProperty();

    public MyViewModel(IModel model) {
        this.model = model;
        if (this.model instanceof Observable) {
            ((Observable) this.model).addObserver(this);
        }
    }

    /**
     * Receives event notifications emitted from the functional underlying Model layer
     * and safely dispatches synchronous notifications to the attached View layer.
     */
    @Override
    public void update(Observable o, Object arg) {
        if (o == model) {
            String changeType = (String) arg;

            if (changeType.startsWith("error:")) {
                Platform.runLater(() -> {
                    errorMessage.set(changeType.replace("error: ", ""));
                    setChanged();
                    notifyObservers("error");
                });
                return;
            }

            switch (changeType) {
                case "maze_generated":
                case "maze_loaded":
                    Platform.runLater(() -> {
                        maze.set(model.getCurrentMaze());
                        characterPosition.set(model.getCharacterPosition());
                        solution.set(null);
                        mazeSolvedByPlayer.set(false);
                        setChanged();
                        notifyObservers("maze_changed");
                    });
                    break;

                case "character_moved":
                    Platform.runLater(() -> {
                        characterPosition.set(model.getCharacterPosition());
                        if (model.isCharacterAtGoal()) {
                            mazeSolvedByPlayer.set(true);
                            setChanged();
                            notifyObservers("victory");
                        } else {
                            setChanged();
                            notifyObservers("position_changed");
                        }
                    });
                    break;

                case "maze_solved":
                    Platform.runLater(() -> {
                        solution.set(model.getLastSolution());
                        setChanged();
                        notifyObservers("solution_ready");
                    });
                    break;
            }
        }
    }

    public void startServers() {
        model.startServers();
    }

    public void stopServers() {
        model.stopServers();
    }

    public void generateMaze(int rows, int cols) {
        model.generateMaze(rows, cols);
    }

    public void solveCurrentMaze() {
        model.solveCurrentMaze();
    }

    public boolean moveCharacter(int rowDelta, int colDelta) {
        return model.moveCharacter(rowDelta, colDelta);
    }

    public void saveMazeToFile(File file) {
        try {
            model.saveMazeToFile(file);
        } catch (IOException e) {
            errorMessage.set("Could not save maze: " + e.getMessage());
            setChanged();
            notifyObservers("error");
        }
    }

    public void loadMazeFromFile(File file) {
        try {
            model.loadMazeFromFile(file);
        } catch (IOException e) {
            errorMessage.set("Could not load maze: " + e.getMessage());
            setChanged();
            notifyObservers("error");
        }
    }

    public int getThreadPoolSize() {
        return model.getThreadPoolSize();
    }

    public String getMazeGeneratingAlgorithmName() {
        return model.getMazeGeneratingAlgorithmName();
    }

    public String getMazeSearchingAlgorithmName() {
        return model.getMazeSearchingAlgorithmName();
    }

    public SimpleObjectProperty<Maze> mazeProperty() {
        return maze;
    }

    public SimpleObjectProperty<Position> characterPositionProperty() {
        return characterPosition;
    }

    public SimpleObjectProperty<Solution> solutionProperty() {
        return solution;
    }

    public SimpleBooleanProperty mazeSolvedByPlayerProperty() {
        return mazeSolvedByPlayer;
    }

    public SimpleStringProperty errorMessageProperty() {
        return errorMessage;
    }
}