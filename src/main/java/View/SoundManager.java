package View;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Manages background music and sound effects using JavaFX MediaPlayer (supports MP3).
 */
public class SoundManager {

    private MediaPlayer backgroundPlayer;
    private MediaPlayer oneShotPlayer;

    public void playBackgroundMusic(String resourcePath) {
        try {
            stopAll();
            String url = getClass().getResource(resourcePath).toExternalForm();
            Media media = new Media(url);
            backgroundPlayer = new MediaPlayer(media);
            backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundPlayer.play();
        } catch (Exception e) {
            System.err.println("Could not play background music: " + e.getMessage());
        }
    }

    public void stopBackgroundMusic() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
            backgroundPlayer.dispose();
            backgroundPlayer = null;
        }
    }

    public void playOneShotSound(String resourcePath) {
        try {
            // עצור one shot קודם אם עדיין מתנגן
            if (oneShotPlayer != null) {
                oneShotPlayer.stop();
                oneShotPlayer.dispose();
                oneShotPlayer = null;
            }
            String url = getClass().getResource(resourcePath).toExternalForm();
            Media media = new Media(url);
            oneShotPlayer = new MediaPlayer(media);
            oneShotPlayer.setOnEndOfMedia(() -> {
                oneShotPlayer.dispose();
                oneShotPlayer = null;
            });
            oneShotPlayer.play();
        } catch (Exception e) {
            System.err.println("Could not play sound effect: " + e.getMessage());
        }
    }

    /** עוצר הכל - רקע וone shot */
    public void stopAll() {
        stopBackgroundMusic();
        if (oneShotPlayer != null) {
            oneShotPlayer.stop();
            oneShotPlayer.dispose();
            oneShotPlayer = null;
        }
    }
}