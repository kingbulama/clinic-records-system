package app.ui;

import app.service.Services;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Services.startup();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/ui/main.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 700);
        UiAnimations.install(scene);
        scene.getStylesheets().add(getClass().getResource("/app/ui/app.css").toExternalForm());
        stage.setTitle("Clinic Records System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
