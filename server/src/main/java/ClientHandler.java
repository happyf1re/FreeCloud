import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedList;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static String pathOfDirectories = "";
    private static String pathsOfDirectories = "";
    private static LinkedList<File> list = new LinkedList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        } else {
            if (msg instanceof UpdateMessage) {
                UpdateMessage updateMessage = (UpdateMessage) msg;
                String receivedLogin = updateMessage.getLogin();
                ctx.writeAndFlush(new UpdateMessage(getContentsOfCloudStorage(receivedLogin)));
            } else if (msg instanceof DeletionMessage) {
                DeletionMessage deletionMessage = (DeletionMessage) msg;
                for (int i = 0; i < deletionMessage.getFilesToDelete().size(); i++) {
                    File fileToDelete = new File(deletionMessage.getFilesToDelete().get(i).getAbsolutePath());
                    if (fileToDelete.isDirectory()) {
                        ClientHandler.deleteRecursively(fileToDelete);
                    } else {
                        fileToDelete.delete();
                    }
                }
                deletionMessage.getFilesToDelete().clear();
                if (deletionMessage.getFilesToDelete().isEmpty()) {
                    ctx.writeAndFlush(new UpdateMessage(getContentsOfCloudStorage(deletionMessage.getLogin())));
                } else {
                    ctx.writeAndFlush("DeletionFailure");
                }
            } else if (msg instanceof FileRequest) {
                FileRequest fileRequest = (FileRequest) msg;
                for (int i = 0; i < fileRequest.getFilesToRequest().size(); i++) {
                    File file = new File(fileRequest.getFilesToRequest().get(i).getAbsolutePath());
                    Path fileToRequest = Paths.get(fileRequest.getFilesToRequest().get(i).getAbsolutePath());
                    try {
                        if (file.isDirectory()) {
                            if (file.listFiles().length == 0) {
                                ctx.writeAndFlush(new FileMessage(file.getName(), true, true));
                            } else {
                                ctx.writeAndFlush(new FileMessage(file.getName(), true, false));
                            }
                        } else {
                            try {
                                ctx.writeAndFlush(new FileMessage(fileToRequest));
                            } catch (AccessDeniedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (msg instanceof FileMessage) {
                FileMessage fileMessage = (FileMessage) msg;
                Path pathToNewFile = Paths.get("server/ServerStorage/" + fileMessage.getLogin() + File.separator + fileMessage.getFileName());
                if (fileMessage.isDirectory() && fileMessage.isEmpty()) {
                    if (Files.exists(pathToNewFile)) {
                        System.out.println("???????? ?? ?????????? ???????????? ?????? ????????????????????");
                    } else {
                        Files.createDirectory(pathToNewFile);
                    }
                } else {
                    if (Files.exists(pathToNewFile)) {
                        System.out.println("???????? ?? ?????????? ???????????? ?????? ????????????????????");
                    } else {
                        Files.write(Paths.get("server/ServerStorage/" + fileMessage.getLogin() + File.separator + fileMessage.getFileName()), fileMessage.getData(), StandardOpenOption.CREATE);
                    }
                }
                ctx.writeAndFlush(new UpdateMessage(getContentsOfCloudStorage(fileMessage.getLogin())));
            } else if (msg instanceof AuthMessage) {
                AuthMessage authMessage = (AuthMessage) msg;
                DatabaseHandler.getConnectionWithDB();
                if (DatabaseHandler.checkIfUserExistsForAuthorization(authMessage.getLogin())) {
                    if (DatabaseHandler.checkIfPasswordIsRight(authMessage.getLogin(), authMessage.getPassword())) {
                        ctx.writeAndFlush("userIsValid/" + authMessage.getLogin());
                    } else {
                        ctx.writeAndFlush("wrongPassword");
                    }
                } else {
                    ctx.writeAndFlush("userDoesNotExist");
                }
                DatabaseHandler.disconnectDB();
            } else if (msg instanceof RegistrationMessage) {
                RegistrationMessage registrationMessage = (RegistrationMessage) msg;
                DatabaseHandler.getConnectionWithDB();
                if (DatabaseHandler.checkIfUserExistsForAuthorization(registrationMessage.getLogin())) {
                    ctx.writeAndFlush("userAlreadyExists");
                } else {
                    if (DatabaseHandler.registerNewUser(registrationMessage.getLogin(), registrationMessage.getPassword())) {
                        File newDirectory = new File("server/ServerStorage/" + registrationMessage.getLogin());
                        newDirectory.mkdir();
                        ctx.writeAndFlush("registrationIsSuccessful");
                    }
                }
                DatabaseHandler.disconnectDB();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        DatabaseHandler.disconnectDB();
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        DatabaseHandler.disconnectDB();
        ctx.close();
    }

    public static void deleteRecursively(File f) throws Exception {
        try {
            if (f.isDirectory()) {
                for (File c : f.listFiles()) {
                    deleteRecursively(c);
                }
            }
            if (!f.delete()) {
                throw new Exception("Delete command returned false for file: " + f);
            }
        } catch (Exception e) {
            throw new Exception("Failed to delete the folder: " + f, e);
        }
    }

    public static HashMap<Integer, LinkedList<File>> getContentsOfCloudStorage(String login) {
        HashMap<Integer, LinkedList<File>> cloudStorageContents;
        LinkedList<File> listCloudStorageFiles = new LinkedList<>();
        File path = new File("server/ServerStorage/" + login);
        File[] files = path.listFiles();
        cloudStorageContents = new HashMap<>();
        if (files.length == 0) {
            cloudStorageContents.clear();
        } else {
            listCloudStorageFiles.clear();
            for (int i = 0; i < files.length; i++) {
                listCloudStorageFiles.add(files[i]);
            }
            cloudStorageContents.clear();
            cloudStorageContents.put(0, listCloudStorageFiles);
        }
        return cloudStorageContents;
    }
}
