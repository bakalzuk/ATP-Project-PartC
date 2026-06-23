package View;

import ViewModel.MyViewModel;
import algorithms.mazeGenerators.Position;
import algorithms.search.AState;
import algorithms.search.MazeState;
import algorithms.search.Solution;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller class interacting directly with the presentation FXML layer.
 * Implements Observer interface to capture custom decoupled continuous state broadcasts.
 */
public class MyViewController implements IView, Observer {

    @FXML private MazeDisplayer mazeDisplayer;
    @FXML private MenuItem newMenuItem;
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem loadMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private MenuItem propertiesMenuItem;
    @FXML private MenuItem helpMenuItem;
    @FXML private MenuItem aboutMenuItem;
    @FXML private Button solveButton;
    @FXML private Button generateButton;
    @FXML private Label statusLabel;

    private MyViewModel viewModel;
    private final SoundManager soundManager = new SoundManager();

    public void setViewModel(MyViewModel viewModel) {
        this.viewModel = viewModel;
        bindProperties();
    }

    @FXML
    private void initialize() {
        solveButton.setDisable(true);

        mazeDisplayer.setCharacterImage(new Image(getClass().getResourceAsStream("/images/character.png")));
        mazeDisplayer.setGoalImage(new Image(getClass().getResourceAsStream("/images/goal.png")));
        mazeDisplayer.setBackgroundImage(new Image(getClass().getResourceAsStream("/images/welcome_maze.png")));

        soundManager.playBackgroundMusic("/sounds/game_song.mp3");

        mazeDisplayer.setOnMoveRequestListener((rowDelta, colDelta) -> {
            if (rowDelta == 0 && colDelta == 0) {
                handleNewMaze();
            } else {
                if (viewModel != null) {
                    viewModel.moveCharacter(rowDelta, colDelta);
                }
            }
        });

        newMenuItem.setOnAction(e -> handleNewMaze());
        saveMenuItem.setOnAction(e -> handleSaveMaze());
        loadMenuItem.setOnAction(e -> handleLoadMaze());
        exitMenuItem.setOnAction(e -> Platform.exit());
        propertiesMenuItem.setOnAction(e -> handleShowProperties());
        helpMenuItem.setOnAction(e -> handleShowHelp());
        aboutMenuItem.setOnAction(e -> handleShowAbout());
        solveButton.setOnAction(e -> {
            if (viewModel != null) {
                viewModel.solveCurrentMaze();
            }
        });

        if (generateButton != null) {
            generateButton.setOnAction(e -> handleNewMaze());
        }
    }

    private void bindProperties() {
        if (viewModel == null) return;

        viewModel.mazeProperty().addListener((obs, oldMaze, newMaze) -> {
            if (newMaze != null) {
                mazeDisplayer.setMaze(newMaze);
                mazeDisplayer.requestFocus();
                solveButton.setDisable(false);
                statusLabel.setText("Maze ready - use the NumPad to move, or drag the character.");
            }
        });

        viewModel.characterPositionProperty().addListener((obs, oldPos, newPos) ->
                mazeDisplayer.setCharacterPosition(newPos));
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o == viewModel) {
            String event = (String) arg;
            switch (event) {
                case "maze_changed":
                case "position_changed":
                    break;
                case "solution_ready":
                    if (viewModel.solutionProperty().get() != null) {
                        displaySolution(extractPositions(viewModel.solutionProperty().get()));
                    }
                    break;
                case "victory":
                    onMazeSolved();
                    break;
                case "error":
                    if (viewModel.errorMessageProperty().get() != null) {
                        showError(viewModel.errorMessageProperty().get());
                    }
                    break;
            }
        }
    }

    public void startServers() { if (viewModel != null) viewModel.startServers(); }
    public void stopServers()  { if (viewModel != null) viewModel.stopServers(); }

    public void shutdown() {
        stopServers();
        soundManager.stopBackgroundMusic();
    }

    private void handleNewMaze() {
        Alert welcomeAlert = new Alert(Alert.AlertType.CONFIRMATION);
        welcomeAlert.setTitle("Welcome to ATP Maze");
        welcomeAlert.setHeaderText("Ready to start a new adventure?");
        welcomeAlert.setContentText("Click 'Start Game' to configure your custom maze board.");

        ButtonType startBtnType = new ButtonType("🎮 Start Game", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtnType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        welcomeAlert.getButtonTypes().setAll(startBtnType, cancelBtnType);

        java.util.Optional<ButtonType> welcomeResult = welcomeAlert.showAndWait();
        if (!welcomeResult.isPresent() || welcomeResult.get() != startBtnType) return;

        Dialog<Pair<Integer, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Configure Maze Size");
        dialog.setHeaderText("Please enter your preferred maze dimensions below:");

        ButtonType generateButtonType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 60, 10, 10));

        TextField rowsField = new TextField();
        rowsField.setPromptText("e.g. 30");
        TextField colsField = new TextField();
        colsField.setPromptText("e.g. 30");

        grid.add(new Label("Rows:"), 0, 0);
        grid.add(rowsField, 1, 0);
        grid.add(new Label("Columns:"), 0, 1);
        grid.add(colsField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == generateButtonType) {
                try {
                    int rows = Integer.parseInt(rowsField.getText().trim());
                    int cols = Integer.parseInt(colsField.getText().trim());
                    return new Pair<>(rows, cols);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dimensions -> {
            if (dimensions.getKey() < 3 || dimensions.getValue() < 3) {
                showError("Maze dimensions must be at least 3x3.");
            } else {
                statusLabel.setText("Generating maze...");
                solveButton.setDisable(true);
                clearSolution();
                if (viewModel != null) {
                    viewModel.generateMaze(dimensions.getKey(), dimensions.getValue());
                }
            }
        });
    }

    private void handleSaveMaze() {
        if (viewModel == null || viewModel.mazeProperty().get() == null) {
            showError("There is no maze to save yet.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Maze");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Maze Files", "*.maze"));
        fileChooser.setInitialFileName("myMaze.maze");

        File file = fileChooser.showSaveDialog(mazeDisplayer.getScene().getWindow());
        if (file != null) {
            viewModel.saveMazeToFile(file);
            statusLabel.setText("Maze saved to " + file.getName());
        }
    }

    private void handleLoadMaze() {
        if (viewModel == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Maze");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Maze Files", "*.maze"));

        File file = fileChooser.showOpenDialog(mazeDisplayer.getScene().getWindow());
        if (file != null) {
            clearSolution();
            viewModel.loadMazeFromFile(file);
            mazeDisplayer.requestFocus();
            statusLabel.setText("Maze loaded from " + file.getName());
        }
    }

    private void handleShowProperties() {
        if (viewModel == null) return;
        String message = "Thread pool size: " + viewModel.getThreadPoolSize() + "\n"
                + "Maze generating algorithm: " + viewModel.getMazeGeneratingAlgorithmName() + "\n"
                + "Maze searching algorithm: " + viewModel.getMazeSearchingAlgorithmName();
        showInfo("Properties", message);
    }

    private void handleShowHelp() {
        String message = "Move the character using the NumPad:\n"
                + "8 = Up, 2 = Down, 4 = Left, 6 = Right\n"
                + "7 = Up-Left, 9 = Up-Right, 1 = Down-Left, 3 = Down-Right\n\n"
                + "You can also drag the character with the mouse.\n"
                + "Hold Ctrl and scroll the mouse wheel to zoom in/out.\n"
                + "Reach the gold goal cell to win the maze!";
        showInfo("How To Play", message);
    }

    private void handleShowAbout() {
        String message = "ATP Maze Game\n"
                + "A JavaFX desktop application built with an MVVM architecture.\n\n"
                + "Maze generation runs on a dedicated server using a randomized "
                + "version of Prim's algorithm.\n"
                + "Maze solving runs on a separate server, using a configurable "
                + "search algorithm (Breadth First / Depth First / Best First Search).";
        showInfo("About", message);
    }

    private List<Position> extractPositions(Solution solution) {
        List<Position> positions = new ArrayList<>();
        for (AState state : solution.getSolutionPath()) {
            positions.add(((MazeState) state).getPosition());
        }
        return positions;
    }

    @Override
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Error");
        alert.showAndWait();
    }

    @Override
    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    @Override
    public void displaySolution(List<Position> path) {
        mazeDisplayer.setSolutionPath(path);
    }

    @Override
    public void clearSolution() {
        mazeDisplayer.clearSolutionPath();
    }

    @Override
    public void onMazeSolved() {
        statusLabel.setText("You solved the maze! 🎉");

        soundManager.stopBackgroundMusic();
        soundManager.playOneShotSound("/sounds/win_sound.mp3");

        Alert winAlert = new Alert(Alert.AlertType.CONFIRMATION);
        winAlert.setTitle("Victory!");
        winAlert.setHeaderText("Congratulations! You reached the goal!");
        winAlert.setContentText("What would you like to do next?");

        ButtonType newGameBtn = new ButtonType("🎮 New Game");
        ButtonType exitBtn = new ButtonType("❌ Exit Game");
        winAlert.getButtonTypes().setAll(newGameBtn, exitBtn);

        java.util.Optional<ButtonType> result = winAlert.showAndWait();
        if (result.isPresent() && result.get() == newGameBtn) {
            soundManager.stopAll();
            soundManager.playBackgroundMusic("/sounds/game_song.mp3");
            handleNewMaze();
        } else if (result.isPresent() && result.get() == exitBtn) {
            shutdown();
            Platform.exit();
        }
    }
}