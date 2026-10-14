package app.ui;

/**
 * Entry point for the fat JAR. It must not extend Application: launching a
 * JavaFX Application subclass straight from a classpath JAR fails with
 * "JavaFX runtime components are missing".
 */
public final class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
