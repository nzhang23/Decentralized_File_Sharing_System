/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decentralizedfilesharesystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zn
 */
public class PeertoPeer extends Thread {

    private int port;
    private boolean listening;
    private ServerSocket serverSocket;
    private String localDir;

    public PeertoPeer(int port, String localDir) {
        try {
            this.port = port;
            this.serverSocket = new ServerSocket(port);
            this.localDir = localDir;
        } catch (IOException ex) {
            System.err.println("Could not listen on port: ");
            //Logger.getLogger(SendManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            listening = true;
            while (listening) {
                try {
                    Socket socket = serverSocket.accept();
                    PeertoPeerSender sender = new PeertoPeerSender(socket, localDir);
                    sender.start();
                } catch (SocketException socketException) {
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PeertoPeer.class.getName()).log(Level.SEVERE, null, ex);
            this.listening = false;
        }
    }

    public void close() {
        try {
            listening = false;
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(PeertoPeer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

