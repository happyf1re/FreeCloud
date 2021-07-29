import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class ConnectionWithServer {

    private static Socket socket;
    private static ObjectEncoderOutputStream out;
    private static ObjectDecoderInputStream in;

    static void startConnection () {
        try {
            socket = new Socket("localhost", 1111);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object readIncomingObject() throws IOException, ClassNotFoundException {
        Object object = in.readObject();
        return  object;
    }

    public static boolean sendAuthMessageToServer(String login, String password) {
        try {
            out.writeObject(new AuthMessage(login,password));
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean sendDeletionMessage(String login, LinkedList<File> filesToDelete) {
        try {
            if (!filesToDelete.isEmpty()) {
                out.writeObject(new DeletionMessage(login, filesToDelete));
                out.flush();
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean transferFilesToCloudStorage(String login, LinkedList<File> filesToSendToCloud) {
        try {
            if (!filesToSendToCloud.isEmpty()) {
                for (int i = 0; i < filesToSendToCloud.size(); i++) {
                    Path path = Paths.get(filesToSendToCloud.get(i).getAbsolutePath());
                    out.writeObject(new FileMessage(login, path));
                    out.flush();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean sendUpdateMessageToServer(String login){
        try {
            out.writeObject(new UpdateMessage(login));
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean sendFileRequest(LinkedList<File> filesToRequest){
        try {
            if (!filesToRequest.isEmpty()){
                out.writeObject(new FileRequest(filesToRequest));
                out.flush();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    public static boolean sendRegMessageToServer(String login, String password) {
        try {
            out.writeObject(new RegistrationMessage(login,password));
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }



}
