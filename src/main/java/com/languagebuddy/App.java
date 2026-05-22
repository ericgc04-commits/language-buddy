package com.languagebuddy;

import com.languagebuddy.database.DatabaseManager;
import com.languagebuddy.view.MainView;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Language Buddy — Java-Powered Vocabulary Learning Chatbot
 * York St John University | Eric González Ceballos | 250158429
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.getInstance().initialize();
        new MainView().show(primaryStage);
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
