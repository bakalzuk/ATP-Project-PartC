package View;

import Model.MyModel;
import ViewModel.MyViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application container entry point.
 */
public class Main extends Application {

    private MyViewController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MyView.fxml"));
            Parent root = loader.load();
            controller = loader.getController();

            // Architectural initialization and wire up based on MVVM specifications
            MyModel model = new MyModel();
            MyViewModel viewModel = new MyViewModel(model);

            // Register the View to actively observe state dispatches from the ViewModel
            viewModel.addObserver(controller);
            controller.setViewModel(viewModel);

            Scene scene = new Scene(root, 900, 650);

            primaryStage.setTitle("ATP Maze Game");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(500);
            primaryStage.setMinHeight(400);
            primaryStage.show();

            // Safe server initialization wrapper to prevent JavaFX launch block
            if (controller != null) {
                try {
                    controller.startServers();
                } catch (Exception e) {
                    System.err.println("Error during server initialization: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Throwable t) {
            System.err.println("CRITICAL ERROR DURING APPLICATION START:");
            t.printStackTrace(); // זה ידפיס הכל לטרמינל בטוח!
        }
    }

    @Override
    public void stop() {
        try {
            if (controller != null) {
                controller.shutdown();
            }
        } catch (Exception e) {
            System.err.println("Error during server shutdown: " + e.getMessage());
        } finally {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}