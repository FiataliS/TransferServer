package org.example.handler;



import com.fiatalis.model.*;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.fiatalis.model.MessageType.*;


@Slf4j
public class HandlerOperation {

    public static final Map<MessageType, BiConsumer<ChannelHandlerContext, CloudMessage>> HANDLER_MAP = new HashMap<>();

    static {
        HANDLER_MAP.put(FILE, (ctx, cloudMessage) -> {
            try {
                FileMessage fm = (FileMessage) cloudMessage;
                Files.write(CloudMessageHandler.getServerDir().resolve(fm.getName()), fm.getBytes());
                ctx.writeAndFlush(new ListMessage(CloudMessageHandler.getServerDir()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HANDLER_MAP.put(FILE_REQUEST, (ctx, cloudMessage) -> {
            try {
                FileRequest fr = (FileRequest) cloudMessage;
                ctx.write(new FileMessage(CloudMessageHandler.getServerDir().resolve(fr.getName())));
                if (fr.isDelete()) {
                    deleteFile(CloudMessageHandler.getServerDir().resolve(fr.getName()).toFile(), fr.getName(), fr.isDelete());
                }
                ctx.writeAndFlush(new ListMessage(CloudMessageHandler.getServerDir()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HANDLER_MAP.put(FILE_DELETE, (ctx, cloudMessage) -> {
            try {
                FileDelete fd = (FileDelete) cloudMessage;
                deleteFile(CloudMessageHandler.getServerDir().resolve(fd.getName()).toFile(), fd.getName(), true);
                ctx.writeAndFlush(new ListMessage(CloudMessageHandler.getServerDir()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HANDLER_MAP.put(LIST, (ctx, cloudMessage) -> {
            try {
                ctx.writeAndFlush(new ListMessage(CloudMessageHandler.getServerDir()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HANDLER_MAP.put(FILE_DIR, (ctx, cloudMessage) -> {
            try {
                FileDir fd = (FileDir) cloudMessage;
                Path fileDir = CloudMessageHandler.getServerDir();
                String item = fd.getItem();
                if (item.equals("...")) {
                    fileDir = fileDir.resolve("..").toAbsolutePath();
                } else {
                    File selected = fileDir.resolve(item).toFile();
                    if (selected.isDirectory()) {
                        fileDir = fileDir.resolve(item).normalize();
                    }
                }
                CloudMessageHandler.setServerDir(fileDir);
                ctx.writeAndFlush(new ListMessage(CloudMessageHandler.getServerDir()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HANDLER_MAP.put(AUTH_SERV, (ctx, cloudMessage) -> {
            try {
                AuthServ authServ = (AuthServ) cloudMessage;
                AuthService.connect();
                String id = "1"; //AuthService.getID(authServ.getNick(), authServ.getPass());
                if (id != null) {
                    CloudMessageHandler.setServerDir(CloudMessageHandler.getServerDir().resolve(id).toFile().toPath());
                    ctx.writeAndFlush(new AuthServ(authServ.getNick(), "_", true));
                } else {
                    System.out.println("Пороль не правильный");
                    ctx.writeAndFlush(new AuthServ(authServ.getNick(), "_", false));
                }
                AuthService.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        HANDLER_MAP.put(NEW_FOLDER, (ctx, cloudMessage) -> {
            NewFolder newFolder = (NewFolder) cloudMessage;
            Path path = CloudMessageHandler.getServerDir().resolve(newFolder.getItem());
            if (path.getFileName().toString().length() >= 1) {
                try {
                    Files.createDirectory(path);
                    ctx.writeAndFlush(new ListMessage(CloudMessageHandler.getServerDir()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    private static void deleteFile(File file, String name, boolean delete) {
        if (delete) {
            if (file.delete()) {
                System.out.println("File: " + name + " delete");
            }
        }
    }
}
