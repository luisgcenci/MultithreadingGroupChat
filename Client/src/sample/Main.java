package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Main extends Application {

    Stage window;
    String address;
    int port;

    @Override
    public void start(Stage primaryStage) throws Exception{

        address = super.getParameters().getRaw().get(0);
        port = Integer.parseInt(super.getParameters().getRaw().get(1));

        //Window
        this.window = primaryStage;
        this.window.setTitle("Client");

        //Grid
        GridPane grid = new GridPane();
        grid.setVgap(8);
        grid.setHgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));

        //set up columns
        ColumnConstraints column0 = new ColumnConstraints();
        column0.setPercentWidth(80);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(20);

        //set up rows
        RowConstraints row0 = new RowConstraints();
        row0.setPercentHeight(75);
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(20);
        RowConstraints row2 = new RowConstraints();
        row1.setPercentHeight(20);

        //add columns and rows to the grid
        grid.getRowConstraints().addAll(row0, row1, row2);
        grid.getColumnConstraints().addAll(column0, column1);

        //Scene
        Scene scene = new Scene(grid, 600, 520);
        scene.getStylesheets().add("css/main.css");

        //Server Stuff
        new Client(address, port, grid);

        //Finish Window
        this.window.setScene(scene);
        this.window.show();
    }

    public static class Client {

        //Network Communication
        private Socket socket = null;
        private DataInputStream in = null;
        private DataOutputStream out = null;
        private final LinkedBlockingQueue<String> MESSAGES;
        private final LinkedBlockingQueue<String> MESSAGES_BY_CLIENT;

        //UI Stuff
        private final TextFlow CHAT = new TextFlow();
        private final TextArea CHAT_INPUT = new TextArea();
        private final Button BUTTON_SEND = new Button("SEND");
        private String username = "";


        public Client(String address, int port, GridPane grid) {

            this.MESSAGES_BY_CLIENT = new LinkedBlockingQueue<>();
            this.MESSAGES = new LinkedBlockingQueue<>();

            try {
                socket = new Socket(address, port);

                in = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(socket.getOutputStream());
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

                //Server sents "Enter Username: " message, Client reads it
                String serverMessage = in.readUTF();
                System.out.println(serverMessage);

                //Client inputs username
                username = input.readLine();
                setUsername(username);
                out.writeUTF(username);

                //connection accepted and welcome
                serverMessage = in.readUTF();
                Text text = new Text(serverMessage);
                CHAT.getChildren().add(text);

                //set up chat to UI
                CHAT.getStyleClass().add("server-chat");
                CHAT.setPadding(new Insets(5, 5, 5, 5));
                GridPane.setHgrow(CHAT, Priority.ALWAYS);
                GridPane.setVgrow(CHAT, Priority.ALWAYS);
                ScrollPane sp = new ScrollPane();
                sp.setContent(CHAT);
                GridPane.setConstraints(sp, 0, 0);

                //Client Status (Circle)
                Circle statusCircle = new Circle(0, 0, 10);
                statusCircle.getStyleClass().add("status-circle");
                GridPane.setValignment(statusCircle, VPos.TOP);
                GridPane.setHgrow(statusCircle, Priority.ALWAYS);
                GridPane.setVgrow(statusCircle, Priority.ALWAYS);
                GridPane.setConstraints(statusCircle, 1, 0);

                //Client Status (Label)
                Label statusLabel = new Label("Online");
                statusLabel.getStyleClass().add("status-label");
                GridPane.setValignment(statusLabel, VPos.TOP);
                GridPane.setHalignment(statusLabel, HPos.CENTER);
                GridPane.setHgrow(statusLabel, Priority.ALWAYS);
                GridPane.setVgrow(statusLabel, Priority.ALWAYS);
                GridPane.setConstraints(statusLabel,1, 0 );

                //UserName
                Label usernameLabel = new Label("User: " + username);
                usernameLabel.getStyleClass().add("status-username");
                GridPane.setValignment(usernameLabel, VPos.TOP);
                GridPane.setHalignment(usernameLabel, HPos.LEFT);
                GridPane.setHgrow(usernameLabel, Priority.ALWAYS);
                GridPane.setVgrow(usernameLabel, Priority.ALWAYS);
                usernameLabel.setPadding(new Insets(30, 0, 0, 0));
                GridPane.setConstraints(usernameLabel,1, 0 );

                //set up user input area to UI
                CHAT_INPUT.getStyleClass().add("chat-input");
                CHAT_INPUT.setPromptText("Enter message...");
                GridPane.setHgrow(CHAT_INPUT, Priority.ALWAYS);
                GridPane.setVgrow(CHAT_INPUT, Priority.ALWAYS);
                GridPane.setConstraints(CHAT_INPUT, 0, 1);

                //set button to send message to UI
                BUTTON_SEND.getStyleClass().add("button-send");
                GridPane.setHgrow(BUTTON_SEND, Priority.ALWAYS);
                GridPane.setVgrow(BUTTON_SEND, Priority.ALWAYS);
                GridPane.setHalignment(BUTTON_SEND, HPos.RIGHT);
                GridPane.setValignment(BUTTON_SEND, VPos.TOP);
                GridPane.setConstraints(BUTTON_SEND, 0, 2);


                grid.getChildren().addAll(sp, CHAT_INPUT, BUTTON_SEND, statusCircle, statusLabel, usernameLabel);
                //set up BUTTON to send message to UI
                setUpButtonSend();

            } catch (Exception e) {
                System.out.println("error " + e.getMessage());
            }

            Thread userInput = new Thread(){

                public void run(){

                    try {
                        String message = "";
                        while (!message.equals("bye")) {
                            try {
                                message = MESSAGES_BY_CLIENT.take();
                                out.writeUTF(message);
                            } catch (IOException | InterruptedException e) {
                                System.out.println(e);
                            }
                        }

                        try{
                            Thread.sleep(2000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }

                        in.close();
                        out.close();
                        socket.close();
                        System.exit(0);
                    }
                    catch (IOException e){
                        System.out.println("Error here " + e.getMessage());
                    }
                }
            };
            userInput.start();

            Thread readMessagesToClient = new Thread(){

                public void run(){
                    String message = "";
                    while(true){
                        try{
                            message = MESSAGES.take();
                            addMessageToChat(message);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            readMessagesToClient.start();

            ReadMessagesFromServer server = new ReadMessagesFromServer(socket);
            new Thread(server).start();
        }

        public void setUsername(String username){
            this.username = username;
        }

        public void addMessageToChat(String message){
            Platform.runLater(new Runnable() {
                public void run(){
                    Text text = new Text("\n" + message);

                    if (message.contains(username + ": ") && (!username.equals(""))){
                        text.setStyle("-fx-fill:#004AFF;-fx-font-weight:bold");
                    }
                    CHAT.getChildren().add(text);
                }
            });
        }

        private class ReadMessagesFromServer implements Runnable{
            DataInputStream in = null;
            DataOutputStream out = null;
            Socket socket;

            ReadMessagesFromServer(Socket socket){
                this.socket = socket;
            }

            public void run(){
                try{
                    in = new DataInputStream(
                            new BufferedInputStream(socket.getInputStream()));
                    out = new DataOutputStream(socket.getOutputStream());

                    while(true){
                        try{
                            String line = in.readUTF();
                            MESSAGES.put(line);
                        }catch (IOException | InterruptedException e){
//                            e.printStackTrace();
                        }
                    }
                }catch(IOException e){
                    System.out.println(e.getMessage());
                }
            }
        }

        public void setUpButtonSend(){
            BUTTON_SEND.setOnAction(e -> {
                String message = CHAT_INPUT.getText();
                MESSAGES_BY_CLIENT.add(message);
                CHAT_INPUT.setText("");
            });
        }
    }

    public static void main(String[] args) {

        launch(args);
    }
}
