/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decentralizedfilesharesystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zn
 */
public class IndexServer implements Runnable {

    private Socket ClientSocket;
    private static Map<String, Object> fileMap = new ConcurrentHashMap<String, Object>();
    private static Map<String, Object> fileMapCopy = new ConcurrentHashMap<String, Object>();

    public IndexServer(Socket connection) {

        this.ClientSocket = connection;

    }

    public static Map<String, Object> getMap() {
        return fileMap;
    }

    public static void setMap(Map<String, Object> fileMap1) {
        fileMapCopy = fileMap1;
    }

    public void run() {

        try {

            ObjectInputStream in = new ObjectInputStream(this.ClientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(this.ClientSocket.getOutputStream());
            Map<String, Object> receivedMap = new HashMap<String, Object>();
            Map<String, Object> sendMap = null;
            MessageCommand message = new MessageCommand(Command.OK, receivedMap);
            Command cmd;
            while (this.ClientSocket.isConnected()) {

                message = (MessageCommand) in.readObject();

                cmd = message.getCmd();

                switch (cmd) {
                    case REGISTER:
                        System.out.println("Register from " + ClientSocket.getRemoteSocketAddress());
                        receivedMap = (HashMap<String, Object>) message.getBody();
                        MapOperator.mergeTwoMaps(receivedMap, fileMap);
                        MapOperator.printMap(fileMap);
                        message.setCmd(Command.OK);
                        
                        message.setBody(sendMap);
                        out.writeObject(message);
                        out.flush();
                        // System.out.println(key+"  "+value);
                        break;
                    case SEARCH:
                        System.out.println("Search from " + ClientSocket.getRemoteSocketAddress());
                        String fileSearched = (String) message.getBody();
                        HashMap<String, Object> searchMap = new HashMap<String, Object>();
                        searchMap.put(fileSearched, null);
                        MapOperator.queryMap(fileMap, searchMap);
                        System.out.println(searchMap);
                        if(searchMap.get(fileSearched)==null)
                             searchMap.remove(fileSearched);
                        message.setCmd(Command.RESULT);
                        message.setBody(searchMap);
                        out.writeObject(message);//sent query result back
                        out.flush();

                        break;
                    case SETCOPY:
                        // System.out.println("Setcopy from " + ClientSocket.getRemoteSocketAddress());
                        receivedMap = (Map<String, Object>) message.getBody();
                        IndexServer.setMap(receivedMap);

                        break;
                }
            }
     

        } catch (IOException ex) {
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(IndexServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {

                ClientSocket.close();
            } catch (IOException ex) {
                System.out.println("Close failed!");
            }
        }
    }
}
