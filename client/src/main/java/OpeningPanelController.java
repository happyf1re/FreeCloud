import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OpeningPanelController implements Initializable {

    @FXML
    public AnchorPane anchorPane;

    @FXML
    public VBox registerBox;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label registerLabel;

    @FXML
    private TextField loginRegisterField;

    @FXML
    private PasswordField passwordRegisterField;

    @FXML
    private ImageView loginPanelImage;

    @FXML
    private Button registerButtonFinal;

    @FXML
    Label messageToUser;
    @FXML
    Label registrationMessage;
    @FXML
    Label registrationSuccessfullMessage;

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        ConnectionWithServer.startConnection();
        OpeningPanelServerListener.setDaemon(true);
        OpeningPanelServerListener.start();
    }

    public void showRegistrationForms() {
        registerBox.setVisible(true);
        registerButtonFinal.setVisible(true);
        registerButton.setVisible(false);
    }

    public void cancelRegistration() {
        registerButton.setVisible(true);
        loginRegisterField.clear();
        passwordRegisterField.clear();
        registerBox.setVisible(false);
        registerButtonFinal.setVisible(false);
        registrationMessage.setText("");
        registrationSuccessfullMessage.setVisible(false);

    }

    public void sendAuthMessage() {
        if (!loginField.getText().isEmpty() && !passwordField.getText().isEmpty()) {
            ConnectionWithServer.sendAuthMessageToServer(loginField.getText(), passwordField.getText());
            loginField.clear();
            passwordField.clear();
        }
    }

    public void sendRegMessageToServer() {
        if (!loginRegisterField.getText().isEmpty() && !passwordRegisterField.getText().isEmpty()) {
            ConnectionWithServer.sendRegMessageToServer(loginRegisterField.getText(), passwordRegisterField.getText());
        } else {
            registrationMessage.setText("Enter login and password");
            passwordRegisterField.clear();
        }
    }


    public void switchScenes(String login) throws IOException {
        Stage stage;
        Parent root;
        stage = (Stage) loginButton.getScene().getWindow();
        root = FXMLLoader.load(getClass().getResource("/mainwindow.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setTitle("Cloud Storage" + File.separator + "" + login);
        stage.show();
    }

    public void authorizeAndSwitchToMainPanel(String login) {
        Platform.runLater(() -> {
            try {
                switchScenes(login);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    Thread OpeningPanelServerListener = new Thread(() -> {
        while (true) {
            Object serverMessage = null;
            try {
                serverMessage = ConnectionWithServer.readIncomingObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (serverMessage.toString().startsWith("userIsValid/")) {
                String[] receivedWords = serverMessage.toString().split("/");
                String login = receivedWords[1];
                CurrentLogin.setCurrentLogin(login);
                authorizeAndSwitchToMainPanel(login);
            } else if (serverMessage.toString().startsWith("wrongPassword")) {
                Platform.runLater(() -> {
                    messageToUser.setText("Wrong password");
                });
            } else if (serverMessage.toString().startsWith("userDoesNotExist")) {
                Platform.runLater(() -> {
                    messageToUser.setText("Such user does not exist");
                    messageToUser.setLayoutX(567.5);
                });
            } else if (serverMessage.toString().equals("userAlreadyExists")) {
                Platform.runLater(() -> {
                    registrationMessage.setText("Such user already exists");
                    loginRegisterField.clear();
                });

            } else if (serverMessage.toString().equals("registrationIsSuccessful")) {
                Platform.runLater(() -> {
                    registerBox.setVisible(false);
                    registerButtonFinal.setVisible(true);
                });
            }
        }
    });
}
