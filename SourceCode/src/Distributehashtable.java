/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decentralizedfilesharesystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author zn
 */

public class Distributehashtable extends Thread {

    private int port;
    private boolean listening;
    private ServerSocket serverSocket;
 

    public  Distributehashtable (int port) {
        try {
            this.port = port;
            this.serverSocket = new ServerSocket(port);
            
        } catch (IOException ex) {
            System.err.println("Could not listen on port: ");
        }
    }




    @Override
    public void run() {
        try {
            listening = true;
            while (listening) {
                try {
                   new Thread(new IndexServer(serverSocket.accept())).start();
                } catch (SocketException socketException) {
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Distributehashtable.class.getName()).log(Level.SEVERE, null, ex);
            this.listening = false;
        }
    }

    public void close() {
        try {
            listening = false;
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(Distributehashtable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
