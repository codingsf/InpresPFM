package be.hepl.benbear.trafficapplication;

import be.hepl.benbear.commons.protocol.Packet;
import be.hepl.benbear.protocol.tramap.LoginPacket;
import be.hepl.benbear.protocol.tramap.LoginResponsePacket;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private final MainApplication app;
    private MainController mainController;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    public LoginController(MainApplication app) {
        this.app = app;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loginButton.setOnAction(e -> {
            if(!preLogin()) {
                onLogin();
            }
        });
    }

    /**
     * Checks for basic error before handling login.
     *
     * @return true if there were any error, false otherwise
     */
    private boolean preLogin() {
        boolean error = false;
        resetError();
        if(usernameField.getText().isEmpty()) {
            error("You need to provide your username.", usernameField);
            error = true;
        }
        if(passwordField.getText().isEmpty()) {
            error("You need to provide your password.", passwordField);
            error = true;
        }
        return error;
    }

    private void onLogin() {
        LoginResponsePacket p = null;

        try {
            app.write(new LoginPacket(usernameField.getText(), passwordField.getText()));
            p = (LoginResponsePacket) app.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (p.isOk()) {
            app.setConnected(true);
            mainController.setDisable(false);
            app.getStage(this).close();
        } else {
            //todo display wrong login and ask again
        }
    }

    private void error(String error, Control control) {
        control.getStyleClass().add("error");
        errorLabel.setText(errorLabel.getText() + '\n' + error);
    }

    private void resetError() {
        errorLabel.setText("");
        usernameField.getStyleClass().remove("error");
        passwordField.getStyleClass().remove("error");
    }

    public void setMainController(MainController ctrl) {
        this.mainController = ctrl;
    }

}
