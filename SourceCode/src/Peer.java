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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;

/**
 *
 * @author zn
 */
public class Peer {

    private String peerId;
    private int fileserverport;
    public int ServerNum;//the number of servers
    public int PeerNum;//the number of peers
    private SimpleMD5 convert;
    public ArrayList<String> ServerList;//storing the server ip and port being deploied
    public ArrayList<String> PeerList;//storing the peer ip and port being deploied
    public Map<String, Socket> SocketList;//storing the socket connection
    public Map<String, ObjectOutputStream> outList;//storing the outputstream of the socket connection
    public Map<String, ObjectInputStream> inList;//storing the inputstream of the socket connection
    public Map<String, Socket> FileSocketList;//storing the download file socket connection
    public Map<String, DataOutputStream> dataoutList;//storing the outputstream of the socket connection
    public Map<String, DataInputStream> datainList;//storing the inputstream of the socket connection
    public FileParser IDextracter;
    private PeertoPeer PeertoPeerOperation;
    public String FileServerInformation;
    private String localDir;
    private String localDirBackup;
    public Set<String> fileaddress;
    private String serverconfigurefile;
    private String peerconfigurefile;
    private int fileservernum;

    public Peer() {
        try {
            this.IDextracter = new FileParser();
            this.serverconfigurefile = new String("config.txt");
            this.peerconfigurefile = new String("peerconfig.txt");
            this.PeerList = this.IDextracter.configure(this.peerconfigurefile);
            this.PeerNum = this.IDextracter.getNumItems(this.PeerList);
            this.ServerList = this.IDextracter.configure(this.serverconfigurefile);
            this.ServerNum = this.IDextracter.getNumItems(this.ServerList);
            this.convert = new SimpleMD5();
            this.SocketList = new HashMap<String, Socket>();
            this.outList = new HashMap<String, ObjectOutputStream>();
            this.inList = new HashMap<String, ObjectInputStream>();
            this.FileSocketList = new HashMap<String, Socket>();
            this.dataoutList = new HashMap<String, DataOutputStream>();
            this.datainList = new HashMap<String, DataInputStream>();

            int n = this.IDextracter.getnodeid(this.PeerNum);
            this.fileservernum = n;
            this.fileserverport = Integer.parseInt(this.IDextracter.get_port_number(PeerList.get(n - 1)));

            this.localDir = Integer.toString(this.fileserverport);
            this.localDirBackup = this.localDir + "backup";

            File dir = new File(this.localDir);
            if (!dir.exists()) {
                dir.mkdir();
            }// create directory for this peer
            File dir1 = new File(this.localDirBackup);
            if (!dir1.exists()) {
                dir1.mkdir();
            }// create backup directory for this peer
            //System.out.println(dir.getAbsolutePath());
            this.PeertoPeerOperation = new PeertoPeer(this.fileserverport, this.localDir);
            this.peerId = "/" + this.IDextracter.get_ip_string(PeerList.get(n - 1)) + "/" + this.fileserverport;
            this.FileServerInformation = this.peerId;
        } catch (UnknownHostException ex) {
            System.err.println("Cannot access the server");
            //Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     * registry function, will be called when peer starts up or monitored folder
     * has changed
     */
    public void registry(String peerId, ArrayList<String> fileNames) throws NoSuchAlgorithmException, ClassNotFoundException {
        try {

            Iterator iter = fileNames.iterator();
            Set<String> id1 = new HashSet<String>();
            id1.add(peerId);
            while (iter.hasNext()) {
                String key1 = new String();
                String key2 = new String();
                key2 = iter.next().toString();
                key1 = convert.MD5(key2);
                String value = new String();
                BigInteger keyInteger = new BigInteger(key1, 16);
                String num = Integer.toString(this.ServerNum);
                BigInteger Bignum = new BigInteger(num);
                BigInteger id;

                id = keyInteger.mod(Bignum);
                int SelectedId = id.intValue();
                int SelectedPort = Integer.parseInt(this.IDextracter.get_port_number(this.ServerList.get(SelectedId)));
                String SelectedIP = this.IDextracter.get_ip_string(this.ServerList.get(SelectedId));
                String serverID = this.ServerList.get(SelectedId);
                HashMap<String, Object> fileList = new HashMap<String, Object>();

                fileList.put(key2, id1);
                if (SocketList.get(serverID) == null) {//if not making the connection to this server, make a new connection to this server
                    Socket PeertoServerSocket = new Socket(SelectedIP, SelectedPort);//making a new connection to this server
                    SocketList.put(serverID, PeertoServerSocket);//putting the socket connection into SocketList
                    ObjectOutputStream out = new ObjectOutputStream(PeertoServerSocket.getOutputStream());
                    outList.put(serverID, out);
                    ObjectInputStream in = new ObjectInputStream(PeertoServerSocket.getInputStream());
                    inList.put(serverID, in);
                    MessageCommand msg = new MessageCommand(Command.REGISTER, fileList);
                    out.writeObject(msg);//sending the put message to the server
                    out.flush();
                    msg = (MessageCommand) in.readObject();//receiving the message from server

                } else {

                    MessageCommand msg = new MessageCommand(Command.REGISTER, fileList);
                    outList.get(serverID).writeObject(msg);//the connection to this server exists in the SocketList, just use this connection
                    outList.get(serverID).flush();
                    msg = (MessageCommand) inList.get(serverID).readObject();

                }
            }

        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*
     get a list of names of all the files in a directory
     */

    public ArrayList<String> getFileNames(String dir) {
        ArrayList fileNames = new ArrayList<String>();
        File directory = new File(dir);
        Iterator<File> iter = FileUtils.iterateFiles(directory, null, false);
        while (iter.hasNext()) {
            fileNames.add(iter.next().getName());
        }
        return fileNames;
    }

    public String getRandItem(Set<String> set) {
        int size = set.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for (String obj : set) {
            if (i == item) {
                return obj;
            }
            i = i + 1;
        }

        return null;
    }
    /*
     obtain function, will be called when user typed obtain in the command line
     */

    public void obtain(String fileName, String ip, String port, String FileServerID) {
       // System.out.println("Downloading from " + ip + ":" + port + " ...");
        // create a Receiver to get file from another peer
        // PeertoPeerReceiver recvr = new PeertoPeerReceiver(ip, port, fileName, localDir);
        //recvr.start(); 
        try {
            
            int serverport = Integer.parseInt(port);
            if (FileSocketList.get(FileServerID) == null) {//if not making the connection to this server, make a new connection to this server
                Socket socket;

                socket = new Socket(ip, serverport); //making a new connection to this server

                FileSocketList.put(FileServerID, socket);//putting the socket connection into SocketList
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                dataoutList.put(FileServerID, os);
                DataInputStream is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                datainList.put(FileServerID, is);
                DataOutputStream fileout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(localDir + "/" + fileName)));
                os.writeUTF("obtain");
                os.flush();
                os.writeUTF(fileName);
                os.flush();
                long len = is.readLong();
                //System.out.println("len is" + len);
                byte[] buf = new byte[1024];
                long len1 = 0;
                while (true) {
                    int read = 0;
                    if (is != null) {
                        read = is.read(buf);
                    }
                    len1 += read;
                    //  System.out.println("read is" + read);

                    fileout.write(buf, 0, read);
                    if (len1 == len) {
                        break;
                    } else
                        {
                            if(len1>len)
                            {
                                System.out.println("len1>len");
                            }
                        }
                }
                fileout.flush();
                fileout.close();
                os.writeUTF("ok");
                os.flush();
            } else {
                DataOutputStream os = dataoutList.get(FileServerID);
                DataInputStream is = datainList.get(FileServerID);
                DataOutputStream fileout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(localDir + "/" + fileName)));
                os.writeUTF("obtain");
                os.flush();
                os.writeUTF(fileName);
                os.flush();
                long len = is.readLong();
                // System.out.println("len is" + len);
                byte[] buf = new byte[1024];
                long len1 = 0;
                while (true) {
                    int read = 0;
                    if (is != null) {
                        read = is.read(buf);
                    }
                    len1 += read;
                    //  System.out.println("read is" + read);

                    fileout.write(buf, 0, read);
                    if (len1 == len) {
                        break;
                    } else
                        {
                            if(len1>len)
                            {
                                System.out.println("len1>len");
                            }
                        }
                }
                fileout.flush();
                fileout.close();
                os.writeUTF("ok");
                os.flush();
            }
        } catch (IOException ex) {
            //Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("fileserver is closed! choosing the backup!");
            try {
                int n=this.PeerList.indexOf(FileServerID)+1;
                String FileServerID1=new String();
                if(n!=this.PeerNum)
                {
                     FileServerID1=this.PeerList.get(n);
                }
                else
                {
                    FileServerID1=this.PeerList.get(0);
                }
                  ip=this.IDextracter.get_ip_string(FileServerID1);
                int serverport = Integer.parseInt(this.IDextracter.get_port_number(FileServerID1));
                if (FileSocketList.get(FileServerID1) == null) {//if not making the connection to this server, make a new connection to this server
                    Socket socket;

                    socket = new Socket(ip, serverport); //making a new connection to this server

                    FileSocketList.put(FileServerID1, socket);//putting the socket connection into SocketList
                    DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                    dataoutList.put(FileServerID1, os);
                    DataInputStream is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    datainList.put(FileServerID1, is);
                    DataOutputStream fileout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(localDir + "/" + fileName)));
                    os.writeUTF("obtainbackup");
                    os.flush();
                    os.writeUTF(fileName);
                    os.flush();
                    long len = is.readLong();
                    //System.out.println("len is" + len);
                    byte[] buf = new byte[1024];
                    long len1 = 0;
                    while (true) {
                        int read = 0;
                        if (is != null) {
                            read = is.read(buf);
                        }
                        len1 += read;
                        //  System.out.println("read is" + read);

                        fileout.write(buf, 0, read);
                        if (len1 == len) {
                            break;
                        }
                        else
                        {
                            if(len1>len)
                            {
                                System.out.println("len1>len");
                            }
                        }
                    }
                    fileout.flush();
                    fileout.close();
                    os.writeUTF("ok");
                    os.flush();
                } else {
                    DataOutputStream os = dataoutList.get(FileServerID1);
                    DataInputStream is = datainList.get(FileServerID1);
                    DataOutputStream fileout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(localDir + "/" + fileName)));
                    os.writeUTF("obtainbackup");
                    os.flush();
                    os.writeUTF(fileName);
                    os.flush();
                    long len = is.readLong();
                    // System.out.println("len is" + len);
                    byte[] buf = new byte[1024];
                    long len1 = 0;
                    while (true) {
                        int read = 0;
                        if (is != null) {
                            read = is.read(buf);
                        }
                        len1 += read;
                        //  System.out.println("read is" + read);

                        fileout.write(buf, 0, read);
                        if (len1 == len) {
                            break;
                        }
                         else
                        {
                            if(len1>len)
                            {
                                System.out.println("len1>len");
                            }
                        }
                    }
                    fileout.flush();
                    fileout.close();
                    os.writeUTF("ok");
                    os.flush();
                }
            } catch (IOException ex1) {
                //Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex1);
                            System.out.println("backup fileserver is closed,too! Can not obtain this file!");

            }
        }
//            os.close();
//            is.close();
//            socket.close();
            /*if (FileSocketList.get(FileServerID) == null) {//if not making the connection to this server, make a new connection to this server
             Socket socket = new Socket(ip, serverport);//making a new connection to this server
             FileSocketList.put(FileServerID, socket);//putting the socket connection into SocketList
             OutputStream os = socket.getOutputStream(); 
             InputStream is = socket.getInputStream();
            
             // send file name out first
             System.out.println("Ask for " + fileName);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
             out.write(fileName);
             out.newLine();
             out.flush();
             // save content to file
             FileUtils.copyInputStreamToFile(is, new File(localDir + "/" + fileName));
             System.out.println("Done");
             }
             else
             {
             OutputStream os = FileSocketList.get(FileServerID).getOutputStream(); 
             InputStream is = FileSocketList.get(FileServerID).getInputStream();
            
             // send file name out first
             System.out.println("Ask for " + fileName);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
             out.write(fileName);
             out.newLine();
             out.flush();
             // save content to file
             // Path path=
             FileUtils.copyInputStreamToFile(is, new File(localDir + "/" + fileName));
             is.close();
             System.out.println("Done");
             }*/

        
    }
        /*
         lookup function, will be called when user typed lookup in the command line
         */

    public Set<String> lookup(String fileName) throws NoSuchAlgorithmException, IOException, ClassNotFoundException {

        String key1 = new String();
        key1 = convert.MD5(fileName);
        String value = new String();
        BigInteger keyInteger = new BigInteger(key1, 16);
        String num = Integer.toString(this.ServerNum);
        BigInteger Bignum = new BigInteger(num);
        BigInteger id;
        id = keyInteger.mod(Bignum);
        int SelectedId = id.intValue();
        int SelectedPort = Integer.parseInt(this.IDextracter.get_port_number(this.ServerList.get(SelectedId)));
        String SelectedIP = this.IDextracter.get_ip_string(this.ServerList.get(SelectedId));
        String serverID = this.ServerList.get(SelectedId);
        if (SocketList.get(serverID) == null) {
            Socket PeertoServerSocket = new Socket(SelectedIP, SelectedPort);
            SocketList.put(serverID, PeertoServerSocket);

            ObjectOutputStream out = new ObjectOutputStream(PeertoServerSocket.getOutputStream());
            outList.put(serverID, out);
            ObjectInputStream in = new ObjectInputStream(PeertoServerSocket.getInputStream());
            inList.put(serverID, in);
            MessageCommand msg = new MessageCommand(Command.SEARCH, fileName);
            out.writeObject(msg);//sending the get message to the server
            out.flush();
            msg = (MessageCommand) in.readObject();//receiving the  message from the server
            Map<String, Object> map = (Map<String, Object>) msg.getBody();
            Set<String> keySet = map.keySet();
            Set<String> addresses = new HashSet<String>();
            for (String key : keySet) {
                addresses.addAll((Set<String>) map.get(key));
            }
            if (addresses.isEmpty()) {
                System.out.println(fileName + " is not found");
                return null;
            }
            System.out.println(fileName + " is found on " + addresses);
            return addresses;

        } else {
            MessageCommand msg = new MessageCommand(Command.SEARCH, fileName);
            outList.get(serverID).writeObject(msg);//sending the get message to the server
            outList.get(serverID).flush();
            msg = (MessageCommand) inList.get(serverID).readObject();//receiving the  message from the server
            Map<String, Object> map = (Map<String, Object>) msg.getBody();
            Set<String> keySet = map.keySet();
            Set<String> addresses = new HashSet<String>();
            for (String key : keySet) {
                addresses.addAll((Set<String>) map.get(key));
            }
            if (addresses.isEmpty()) {
                System.out.println(fileName + " is not found");
                return null;
            }
           System.out.println(fileName + " is found on " + addresses);
            return addresses;
        }
    }

    public void replicatefiles(String ip, int serverport, String FileServerID, ArrayList<String> fileNames) {
        int num = fileNames.size();
        byte[] buf = new byte[1024];
        if (FileSocketList.get(FileServerID) == null) {//if not making the connection to this server, make a new connection to this server
            Socket socket;
            try {
                
                socket = new Socket(ip, serverport); //making a new connection to this server
                FileSocketList.put(FileServerID, socket);//putting the socket connection into SocketList
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                dataoutList.put(FileServerID, os);
                DataInputStream is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                datainList.put(FileServerID, is);
                
                os.writeUTF("replication");
                os.flush();
                os.writeInt(num);
                os.flush();
                for (int i = 0; i < num; i++) {
                    String filename = fileNames.get(i);
                   // System.out.println("filename is" + filename);
                    os.writeUTF(filename);
                    os.flush();
                    File file = new File(localDir + "/" + filename);
                    os.writeLong((long) file.length());
                    os.flush();
                    DataInputStream fileout = new DataInputStream(new BufferedInputStream(new FileInputStream(localDir + "/" + filename)));
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
                        //os.flush();
                    }
                    os.flush();
                   is.readUTF();
                }
            } catch (IOException ex) {
                System.out.println("the backup server is closed!");
               // Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            DataOutputStream os = dataoutList.get(FileServerID);
            DataInputStream is = datainList.get(FileServerID);
            try {
                os.writeUTF("replication");
                os.flush();
                os.writeInt(num);
                os.flush();
                for (int i = 0; i < num; i++) {
                    String filename = fileNames.get(i);
                    //System.out.println("filename is" + filename);
                    os.writeUTF(filename);
                    os.flush();
                    File file = new File(localDir + "/" + filename);
                    os.writeLong((long) file.length());
                    os.flush();
                    DataInputStream fileout = new DataInputStream(new BufferedInputStream(new FileInputStream(localDir + "/" + filename)));
                    while (true) {
                        int read = 0;
                        if (fileout != null) {
                            read = fileout.read(buf);
                        }
                    //  System.out.println("read is"+read);

                        if (read == -1) {
                            break;
                        }
                        else
                        {
                            os.write(buf, 0, read);
                           // os.flush();
                        }
                        
                    }
                    os.flush();
                   is.readUTF();
                   
                }
            } catch (IOException ex) {
                 System.out.println("the backup server is closed!");
              // Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void replication(ArrayList<String> fileNames) {
        int num = fileNames.size();
        if(num>0)
        {
        if (this.fileservernum != this.PeerNum) {
            int serverport = Integer.parseInt(this.IDextracter.get_port_number(PeerList.get(this.fileservernum)));
            String ip = this.IDextracter.get_ip_string(PeerList.get(this.fileservernum));
            String FileServerID = "/" + ip + "/" + serverport;
            replicatefiles(ip, serverport, FileServerID, fileNames);
        } else {
            int serverport = Integer.parseInt(this.IDextracter.get_port_number(PeerList.get(0)));
            String ip = this.IDextracter.get_ip_string(PeerList.get(0));
            String FileServerID = "/" + ip + "/" + serverport;
            replicatefiles(ip, serverport, FileServerID, fileNames);
        }
        }
    }

    /*
     Initiate the peer, start folder monitoring
     */
    public void init() throws NoSuchAlgorithmException, ClassNotFoundException {
        try { // start PeertoPeer thread
            PeertoPeerOperation.start();
            // registry when starts up
            registry(this.FileServerInformation, getFileNames(localDir));
           new Timer().schedule(new TimerTask(){ public void run(){
                replication(getFileNames(localDir));
            }}, 120000);
           
            // setup file monitor
            FileSystemManager fsManager = VFS.getManager();
            FileObject listenDir = fsManager.resolveFile(new File(localDir).getAbsolutePath());
            DefaultFileMonitor defFileMonitor = new DefaultFileMonitor(new FileListener() {
                @Override
                public void fileCreated(FileChangeEvent fce) throws Exception {
                    String fileName = fce.getFile().toString();
                    // System.out.println(fileName + " created");
                    ArrayList<String> filelist = new ArrayList<String>();
                    filelist.add(fileName.substring(fileName.lastIndexOf("/") + 1));
                    registry(FileServerInformation, filelist);
                    replication(filelist);

                }

                @Override
                public void fileDeleted(FileChangeEvent fce) throws Exception {
                    String fileName = fce.getFile().toString();
                    // System.out.println(fileName + " deleted");
                    // delete(fileName.substring(fileName.lastIndexOf("/") + 1));
                }

                @Override
                public void fileChanged(FileChangeEvent fce) throws Exception {
                    //System.out.println(fce.getFile() + " changed");
                    // registry(peerId, getFileNames(localDir));
                }
            });
            defFileMonitor.setRecursive(false);
            defFileMonitor.addFile(listenDir);
            defFileMonitor.start();
        } catch (FileSystemException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void generatingfile() throws FileNotFoundException, IOException {
        char[] b = new char[1024];
        for (int j = 0; j < 1024; j++) {
            b[j] = 'a';
        }
       /* File dir1 = new File("testingfile1K");
        if (!dir1.exists()) {
            dir1.mkdir();
        }*/
        for (int i = 0; i < 100; i++) {
           // File file = new File(dir1.getAbsolutePath() + "/file1K" + i);
            File file = new File(localDir + "/" + "/file" + i);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            for (int j = 0; j < 1; j++) {
                pw.write(b);
            }
            pw.flush();
            pw.close();
            fos.close();
        }
    }

    @Override
    public String toString() {
        return this.peerId;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        Peer peer = new Peer();
        System.out.println("*******************************************************************");
        System.out.println("*                    Peer Operation Command                       *");
        System.out.println("*                                                                 *");
        System.out.println("* 1.SEARCH      (lookup the file position)                        *");
        System.out.println("* 2.OBTAIN      (download the file)                               *");
        System.out.println("* 2.TESTOTHER   (test the register and search performance)        *");
        System.out.println("* 3.TESTOBTAIN  (test the obtain performance)                     *");
        System.out.println("* 4.FILING      (generate test file)                              *");
        System.out.println("* 5.EXIT        (exit the peer)                                   *");
        System.out.println("*******************************************************************");
        System.out.println("Peer ID:" + peer.toString());
        peer.init();

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        String fromUser;
        Set<String> peers;

        System.out.println("Input the command: ");
        while ((fromUser = stdIn.readLine()) != null) {
            if (fromUser.equalsIgnoreCase("OBTAIN")) {
                System.out.println("Filename: ");
                String fileName = stdIn.readLine();
                peers = peer.lookup(fileName);
                if (peers != null) {
                    peer.fileaddress = peers;
                    Iterator it = peer.fileaddress.iterator();
                    String tmp = it.next().toString();
                    String ip = peer.IDextracter.get_ip_string(tmp);
                    String port = peer.IDextracter.get_port_number(tmp);
                    System.out.println("Download " + fileName + " from " + ip + ":" + port + " ...");
                    peer.obtain(fileName, ip, port, tmp);
                }
                System.out.println("Input the command: ");
            } else if (fromUser.equalsIgnoreCase("SEARCH")) {
                System.out.println("Filename: ");
                String fileName = stdIn.readLine();
                peers = peer.lookup(fileName);
                System.out.println("Input the command: ");
            } else if (fromUser.equalsIgnoreCase("EXIT")) {
                File dir = new File(peer.localDir);
                dir.delete();
                int i;
                for (i = 0; i < peer.ServerNum; i++) {//release the socket connection
                    ObjectInputStream tmp2 = peer.inList.get(peer.ServerList.get(i));
                    if (tmp2 != null) {
                        tmp2.close();
                    }
                    ObjectOutputStream tmp1 = peer.outList.get(peer.ServerList.get(i));
                    if (tmp1 != null) {
                        tmp1.close();
                    }
                    Socket tmp = peer.SocketList.get(peer.ServerList.get(i));
                    if (tmp != null) {
                        tmp.close();
                    }
                }
                Set<String> map1KeySet = peer.FileSocketList.keySet();
                for (String key : map1KeySet) {
                    peer.datainList.get(key).close();
                    peer.dataoutList.get(key).close();
                    peer.FileSocketList.get(key).close();
                }
                peer.PeertoPeerOperation.close();
                stdIn.close();
                break;
            } else if (fromUser.equalsIgnoreCase("TESTOBTAIN")) {
                System.out.println("Number of requests: ");
                int num = Integer.parseInt(stdIn.readLine());
                peer.testobtain(num);
                System.out.println("Input the command: ");
            } 
            else if (fromUser.equalsIgnoreCase("TESTFILES")) {
                
                peer.testfiles();
                System.out.println("Input the command: ");
            }else if (fromUser.equalsIgnoreCase("TESTOTHER")) {
                System.out.println("Number of requests: ");
                int num = Integer.parseInt(stdIn.readLine());
                peer.testother(num);
                System.out.println("Input the command: ");
            }else if (fromUser.equalsIgnoreCase("FILING")) {
                peer.generatingfile();
                System.out.println("Input the command: ");
            } else {
                System.out.println("Only LOOKUP, DOWNLOAD ,TEST and EXIT suppport, please try again");
                System.out.println("Input the command: ");
            }
        }
    }
    /*
     Called when user type in test to test
     */
    
    /**
     * implement the TEST operation to respectively test 10K register requests, 10K
     * search requests
     **/
    public void testother(int num) throws NoSuchAlgorithmException, ClassNotFoundException, IOException {
        System.out.println("Start performance test, please be patient ...");
        //int num = 100000;
        long startTime, endTime,time,sum=0;
        startTime = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
             Random rand=new Random();
            int n = (rand.nextInt(num));
              String filename=new String("file"+n);
              ArrayList<String> filelist = new ArrayList<String>();
                    filelist.add(filename);
                    registry(this.FileServerInformation, filelist);
        }
        endTime = System.currentTimeMillis();
        System.out.println("register test takes: " + (double) (endTime - startTime) / num * 1000 + " us ");
        startTime = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
              Random rand=new Random();
            int n = (rand.nextInt(num));
              String filename=new String("file"+n);
              Set<String> tmp=this.lookup(filename);
        }
        endTime = System.currentTimeMillis();
        System.out.println("search test takes: " + (double) (endTime - startTime) / num * 1000 + " us ");
        
       
      
    }
    /**
     * implement the TEST operation to  test 10K obtain requests
     **/
    public void testobtain(int num) {
        System.out.println("Start performance test, please be patient ...");
        //int num = 100000;
        long startTime, endTime,time,sum=0;
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
          //  time = System.currentTimeMillis();
            Random rand=new Random();
            int n = (rand.nextInt(800));
          //  System.out.println("random is "+n);
            String filename=new String("file"+n);
            String ip = this.IDextracter.get_ip_string(this.PeerList.get(n/100));
            String port=this.IDextracter.get_port_number(this.PeerList.get(n/100));
            String FileServerID=this.PeerList.get(n/100);
            startTime = System.currentTimeMillis();
            if(FileServerID.compareTo(this.peerId)!=0)
            {
                obtain(filename,ip,port,FileServerID);
            }
            endTime=System.currentTimeMillis();
            sum+=(endTime - startTime);
            //flag = delete(key2);
        }
        endTime = System.currentTimeMillis();
        System.out.println("10k obtain test takes: " +  (double)(sum) / num * 1000 + " us ");
    }
    /**
     * implement the TEST operation to  test 1min obtain requests
     **/
    public void testfiles() {
        System.out.println("Start performance test, please be patient ...");
        long startTime, endTime,time,sum=0;
        ArrayList<String> filenames= new ArrayList<String>();
        double []a=new double[7];
        a[0]=1.0;
        a[1]=10.0;
        a[2]=100.0;
        a[3]=1000.0;
                a[4]=10000.0;
                a[5]=100000.0;
                        a[6]=1000000.0;
        filenames.add("file1K1");
        filenames.add("file10K1");
        filenames.add("file100K1");
         filenames.add("file1M1");
          filenames.add("file10M1");
           filenames.add("file100M1");
            filenames.add("file1G1");
            String ip="127.0.0.1";
            String port="5555";
            String FileServerID="/"+ip+"/"+port;
        for(int i=0;i<7;i++)
        {
            startTime=System.currentTimeMillis();
            obtain(filenames.get(i), ip,  port,  FileServerID);
            endTime = System.currentTimeMillis();
            System.out.println(" obtain "+filenames.get(i)+ "test takes: " +  a[i]/((double)(endTime - startTime)/(1000)) + " KB/s ");
        }
    }


}
