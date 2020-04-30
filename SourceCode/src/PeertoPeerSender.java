/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decentralizedfilesharesystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author zn
 */
public class PeertoPeerSender extends Thread {

    private Socket clientSocket;
    private String localDir;

    public PeertoPeerSender(Socket sock, String localDir) {
        this.clientSocket = sock;
        this.localDir = localDir;
    }

    @Override
    public void run() {
        try {
            DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream is = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            byte[] buf = new byte[1024];
            // get file name first
            while (this.clientSocket.isConnected()) {
                String cmd = is.readUTF();
                if (cmd.compareTo("obtain") == 0) {
                    String fileName = is.readUTF();
                    //System.out.println(fileName);
                    File file = new File(localDir + "/" + fileName);
                    os.writeLong((long) file.length());
                    os.flush();
                    DataInputStream fileout = new DataInputStream(new BufferedInputStream(new FileInputStream(localDir + "/" + fileName)));
                    while (true) {
                        int read = 0;
                        if (fileout != null) {
                            read = fileout.read(buf);
                        }
                        // System.out.println("read is"+read);

                        if (read == -1) {
                            break;
                        }
                        os.write(buf, 0, read);
                    }
                    os.flush();
                    fileout.close();
                    is.readUTF();
                } else {
                    if (cmd.compareTo("obtainbackup") == 0) {
                        String fileName = is.readUTF();
                        //System.out.println(fileName);
                        File file = new File(localDir+"backup" + "/" + fileName);
                        os.writeLong((long) file.length());
                        os.flush();
                        DataInputStream fileout = new DataInputStream(new BufferedInputStream(new FileInputStream(localDir+"backup" + "/" + fileName)));
                        while (true) {
                            int read = 0;
                            if (fileout != null) {
                                read = fileout.read(buf);
                            }
                            // System.out.println("read is"+read);

                            if (read == -1) {
                                break;
                            }
                            os.write(buf, 0, read);
                        }
                        os.flush();
                          fileout.close();
                       is.readUTF();
                    } else {
                        if (cmd.compareTo("replication") == 0) {
                            int num = is.readInt();
                           // System.out.println("num is " + num);
                            for (int i = 0; i < num; i++) {
                                String filename = is.readUTF();
                               // System.out.println("filename is" + filename);
                                DataOutputStream fileout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(localDir + "backup" + "/" + filename)));
                                long len = is.readLong();
                                //System.out.println("len is" + len);
                                long len1 = 0;
                                while (true) {
                                    int read = 0;
                                    if (is != null) {
                                        read = is.read(buf);
                                    }
                                    len1 += read;
                                     //System.out.println("read is" + read);
                                    
                                    fileout.write(buf, 0, read);
                                     //fileout.flush();
                                    if (len1 == len) {
                                        break;
                                    }
                                }
                                fileout.flush();
                                fileout.close();
                                os.writeUTF("ok");
                                os.flush();
                            }
                        }
                    }

                }

//            OutputStream os = clientSocket.getOutputStream(); 
//            InputStream is = clientSocket.getInputStream();
//            
//            BufferedReader in = new BufferedReader(new InputStreamReader(is));
//            String fileName =in.readLine();
                // if(fileName!=null)
                //{
                //System.out.println("Sending " + fileName);
                // send file content
                //  FileUtils.copyFile(new File(localDir + "/" + fileName), os);
                //os.flush();
                //           clientSocket.shutdownOutput();
                // is.close();
                // os.close();
//            clientSocket.close();
                // System.out.println("Done");
                //}
            }

        } catch (IOException ex) {
            System.out.println("One client is lost!");
           // Logger.getLogger(PeertoPeerSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
