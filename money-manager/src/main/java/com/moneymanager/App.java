package com.moneymanager;

import com.moneymanager.config.ApplicationContext;
import com.moneymanager.config.DatabaseInitializer;
import com.moneymanager.config.LoggingConfig;
import com.moneymanager.ui.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        LoggingConfig.init();
        DatabaseInitializer.init();
        var context = new ApplicationContext();

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);

        LoginController ctrl = loader.getController();
        ctrl.init(context.authService(), context.transactionService(), context.budgetService(),
                  context.goalService(), context.noteService(), context.dashboardService(),
                  context.monthlyIncomeService(), stage);

        stage.setTitle("Money Manager");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
