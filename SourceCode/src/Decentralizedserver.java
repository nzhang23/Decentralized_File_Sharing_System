/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decentralizedfilesharesystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author zn
 */
public class Decentralizedserver {
    
     private Distributehashtable hashserver;
     
     public Decentralizedserver(int port){
         this.hashserver = new Distributehashtable(port);
         this.hashserver.start();
          if (this.hashserver.isAlive()) {
            System.out.println("Server is established!");
        }
     }
    public static void main(String[] args) throws IOException{

        FileParser IDextracter = new FileParser();
        String configurefile=new String("config.txt");
        ArrayList<String> ServerList = IDextracter.configure(configurefile);//storing the server ip and port being deploied
        int ServerNum = IDextracter.getNumItems(ServerList);
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));       
        int portnumber=IDextracter.getserverid(ServerNum);
        int port = Integer.parseInt(IDextracter.get_port_number(ServerList.get(portnumber-1)));
        Decentralizedserver server= new Decentralizedserver(port);   
        String fromUser;
        while ((fromUser = stdIn.readLine()) != null) {
            if(fromUser.equalsIgnoreCase("EXIT"))
            {
                server.hashserver.close();
                stdIn.close();
                break;
            }
            else 
            {
                System.out.println("Only EXIT suppport, please try again");
            }
        }
    }
}
