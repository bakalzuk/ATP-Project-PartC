package View;

import algorithms.mazeGenerators.Maze;
import algorithms.mazeGenerators.Position;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * A self-contained, reusable maze board widget.
 * Extends {@link Canvas} so it behaves like any other JavaFX control.
 */
public class MazeDisplayer extends Canvas {

    private Maze maze;
    private Position characterPosition;
    private List<Position> solutionPath;

    private double zoomFactor = 1.0;

    private double boardOffsetX;
    private double boardOffsetY;
    private double currentCellSize;

    private Image characterImage;
    private Image wallImage;
    private Image backgroundImage;
    private Image goalImage;

    private double btnX, btnY, btnW, btnH;

    // Pink color theme
    private Color wallColor     = Color.web("#c9184a");   // deep pink walls
    private Color passageColor  = Color.web("#ffe0eb");   // light pink passages
    private Color goalColor     = Color.web("#ff6b9d");   // hot pink goal
    private Color characterColor= Color.web("#c9184a");
    private Color solutionColor = Color.web("#ff85a1");   // soft pink solution dots

    private IMoveRequestListener moveRequestListener;
    private Position lastDragCell;

    public MazeDisplayer() {
        setFocusTraversable(true);

        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        setOnMousePressed(event -> {
            requestFocus();
            if (maze == null && moveRequestListener != null) {
                double mx = event.getX();
                double my = event.getY();
                if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                    moveRequestListener.onMoveRequested(0, 0);
                    return;
                }
            }
            lastDragCell = pixelToCell(event.getX(), event.getY());
        });

        setOnMouseDragged(event -> {
            Position hovered = pixelToCell(event.getX(), event.getY());
            if (hovered == null || characterPosition == null) return;
            if (hovered.equals(lastDragCell)) return;
            lastDragCell = hovered;

            int rowDelta = hovered.getRowIndex() - characterPosition.getRowIndex();
            int colDelta = hovered.getColumnIndex() - characterPosition.getColumnIndex();

            boolean isSingleStep = Math.abs(rowDelta) <= 1 && Math.abs(colDelta) <= 1
                    && (rowDelta != 0 || colDelta != 0);

            if (isSingleStep && moveRequestListener != null) {
                moveRequestListener.onMoveRequested(rowDelta, colDelta);
            }
        });

        setOnKeyPressed(event -> {
            int rowDelta = 0;
            int colDelta = 0;
            switch (event.getCode()) {
                case NUMPAD8: case UP:    rowDelta = -1; break;
                case NUMPAD2: case DOWN:  rowDelta =  1; break;
                case NUMPAD4: case LEFT:  colDelta = -1; break;
                case NUMPAD6: case RIGHT: colDelta =  1; break;
                case NUMPAD7: rowDelta = -1; colDelta = -1; break;
                case NUMPAD9: rowDelta = -1; colDelta =  1; break;
                case NUMPAD1: rowDelta =  1; colDelta = -1; break;
                case NUMPAD3: rowDelta =  1; colDelta =  1; break;
                default: return;
            }
            if (moveRequestListener != null) {
                moveRequestListener.onMoveRequested(rowDelta, colDelta);
            }
            event.consume();
        });

        setOnScroll(event -> {
            if (event.isControlDown()) {
                double factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
                zoomFactor = Math.max(0.2, Math.min(5.0, zoomFactor * factor));
                redraw();
                event.consume();
            }
        });
    }

    public void setMaze(Maze maze) {
        this.maze = maze;
        this.zoomFactor = 1.0;
        redraw();
    }

    public void setCharacterPosition(Position position) {
        this.characterPosition = position;
        redraw();
    }

    public void setSolutionPath(List<Position> path) {
        this.solutionPath = path;
        redraw();
    }

    public void clearSolutionPath() {
        this.solutionPath = null;
        redraw();
    }

    public void setOnMoveRequestListener(IMoveRequestListener listener) {
        this.moveRequestListener = listener;
    }

    public void setCharacterImage(Image image)    { this.characterImage  = image; redraw(); }
    public void setWallImage(Image image)         { this.wallImage       = image; redraw(); }
    public void setBackgroundImage(Image image)   { this.backgroundImage = image; redraw(); }
    public void setGoalImage(Image image)         { this.goalImage       = image; redraw(); }

    private Position pixelToCell(double pixelX, double pixelY) {
        if (maze == null || currentCellSize <= 0) return null;
        int col = (int) ((pixelX - boardOffsetX) / currentCellSize);
        int row = (int) ((pixelY - boardOffsetY) / currentCellSize);
        if (row < 0 || row >= maze.getRows() || col < 0 || col >= maze.getCols()) return null;
        return new Position(row, col);
    }

    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // 1. WELCOME SCREEN
        if (maze == null) {
            if (backgroundImage != null) {
                gc.drawImage(backgroundImage, 0, 0, getWidth(), getHeight());
            } else {
                // Pink gradient background fallback
                LinearGradient backdrop = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#ff85a1")),
                        new Stop(1, Color.web("#c9184a")));
                gc.setFill(backdrop);
                gc.fillRect(0, 0, getWidth(), getHeight());
            }

            // Welcome text - big and centered
            double centerX = getWidth() / 2;
            double centerY = getHeight() / 2 - 60;

            // Shadow effect
            gc.setFill(Color.web("#c9184a", 0.4));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(Font.font("System", FontWeight.BOLD, 28));
            gc.fillText("תעזרו להדס ורותם לצאת מהמבוך! :)", centerX + 2, centerY + 2);

            // Main welcome text
            gc.setFill(Color.WHITE);
            gc.fillText("תעזרו להדס ורותם לצאת מהמבוך! :)", centerX, centerY);

            // Sub text
            gc.setFont(Font.font("System", FontWeight.NORMAL, 16));
            gc.setFill(Color.web("#fff0f5"));
            gc.fillText("לחצו על הכפתור למטה כדי להתחיל", centerX, centerY + 45);

            // Pink play button
            btnW = 300;
            btnH = 55;
            btnX = (getWidth() - btnW) / 2;
            btnY = (getHeight() - btnH) / 1.35;

            // Button shadow
            gc.setFill(Color.web("#c9184a", 0.5));
            gc.fillRoundRect(btnX + 3, btnY + 3, btnW, btnH, 30, 30);

            // Button fill
            LinearGradient btnGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#ff85a1")),
                    new Stop(1, Color.web("#c9184a")));
            gc.setFill(btnGradient);
            gc.fillRoundRect(btnX, btnY, btnW, btnH, 30, 30);

            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRoundRect(btnX, btnY, btnW, btnH, 30, 30);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 20));
            gc.fillText("💕 בואו נשחק!", getWidth() / 2, btnY + btnH / 2);

            return;
        }

        // 2. ACTIVE MAZE
        int rows = maze.getRows();
        int cols = maze.getCols();

        double rawCellSize = Math.min(getWidth() / cols, getHeight() / rows);
        currentCellSize = rawCellSize * zoomFactor;

        double boardWidth  = currentCellSize * cols;
        double boardHeight = currentCellSize * rows;

        boardOffsetX = (getWidth()  - boardWidth)  / 2;
        boardOffsetY = (getHeight() - boardHeight) / 2;

        // Pink background behind board
        gc.setFill(Color.web("#ffe0eb"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        gc.setFill(wallColor);
        gc.fillRect(boardOffsetX, boardOffsetY, boardWidth, boardHeight);

        // 3. TILES
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = boardOffsetX + c * currentCellSize;
                double y = boardOffsetY + r * currentCellSize;

                if (maze.getCellValue(r, c) == 1) {
                    if (wallImage != null) {
                        gc.drawImage(wallImage, x, y, currentCellSize, currentCellSize);
                    } else {
                        gc.setFill(wallColor);
                        gc.fillRect(x, y, currentCellSize, currentCellSize);
                    }
                } else {
                    gc.setFill(passageColor);
                    gc.fillRect(x, y, currentCellSize, currentCellSize);
                }
            }
        }

        // 4. SOLUTION TRAIL
        if (solutionPath != null) {
            gc.setFill(solutionColor);
            double pad = currentCellSize * 0.3;
            for (Position p : solutionPath) {
                double x = boardOffsetX + p.getColumnIndex() * currentCellSize;
                double y = boardOffsetY + p.getRowIndex() * currentCellSize;
                gc.fillOval(x + pad, y + pad, currentCellSize - 2 * pad, currentCellSize - 2 * pad);
            }
        }

        // 5. GOAL
        if (maze.getGoalPosition() != null) {
            Position goal = maze.getGoalPosition();
            double x = boardOffsetX + goal.getColumnIndex() * currentCellSize;
            double y = boardOffsetY + goal.getRowIndex() * currentCellSize;
            if (goalImage != null) {
                gc.drawImage(goalImage, x, y, currentCellSize, currentCellSize);
            } else {
                gc.setFill(goalColor);
                gc.fillRect(x, y, currentCellSize, currentCellSize);
            }
        }

        // 6. CHARACTER - bigger (1.4x cell size, centered)
        if (characterPosition != null) {
            double cellX = boardOffsetX + characterPosition.getColumnIndex() * currentCellSize;
            double cellY = boardOffsetY + characterPosition.getRowIndex() * currentCellSize;
            if (characterImage != null) {
                double imgSize = currentCellSize * 1.4;
                double offset  = (currentCellSize - imgSize) / 2;
                gc.drawImage(characterImage, cellX + offset, cellY + offset, imgSize, imgSize);
            } else {
                gc.setFill(characterColor);
                double pad = currentCellSize * 0.1;
                gc.fillOval(cellX + pad, cellY + pad, currentCellSize - 2 * pad, currentCellSize - 2 * pad);
            }
        }
    }

    @Override public boolean isResizable() { return true; }
    @Override public double prefWidth(double height) { return getWidth(); }
    @Override public double prefHeight(double width) { return getHeight(); }
    @Override public void resize(double width, double height) { setWidth(width); setHeight(height); }
}