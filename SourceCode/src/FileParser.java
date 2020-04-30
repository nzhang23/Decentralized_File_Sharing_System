/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decentralizedfilesharesystem;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 * @author zn
 */
public class FileParser {

    public  ArrayList<String> configure(String filename) {

        BufferedReader br = null;
        ArrayList<String> configInfo = new ArrayList();
        String sCurrentLine = null;

        try {
            
            
                br = new BufferedReader(new FileReader("src/"+filename));
            

            while ((sCurrentLine = br.readLine()) != null) {
                configInfo.add(sCurrentLine);
                //System.out.println(sCurrentLine);
            }

         //   System.out.println(new File(".").getAbsolutePath());

        } catch (IOException e) {
            System.out.println("Read file mistake!");
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                System.out.println("Close file mistake!");
            }
        }
        return configInfo;
    }

    public  String get_ip_string(String ID) {
        int start = ID.indexOf("/");
        int end = ID.lastIndexOf("/");
        return ID.substring(start + 1, end);
    }

    public  String get_port_number(String ID) {
        int start = ID.lastIndexOf("/");

        return ID.substring(start + 1);
    }

    public int getNumItems(ArrayList<String> A) {
        return (A.size());
    }

    public int getnodeid(int num) throws FileNotFoundException, IOException
    {
        BufferedReader br = null;
        br = new BufferedReader(new FileReader("src/id.txt"));
    
        String sCurrentLine = null;
        sCurrentLine = br.readLine();
       // sCurrentLine.replaceAll("\n", " ");
        
       // System.out.println("peerid num is "+ Integer.parseInt(sCurrentLine));

        PrintWriter writer = new PrintWriter("src/id.txt", "UTF-8");
        if(Integer.parseInt(sCurrentLine) >= num)
        {
            writer.println(1);
        }
        else
        {
            writer.println(Integer.parseInt(sCurrentLine)+1);
        }
        writer.close();
        return Integer.parseInt(sCurrentLine);
    }
   public int getserverid(int num) throws FileNotFoundException, IOException
    {
        BufferedReader br = null;
        br = new BufferedReader(new FileReader("src/serverid.txt"));
    
        String sCurrentLine = null;
        sCurrentLine = br.readLine();
       // sCurrentLine.replaceAll("\n", " ");
        
       // System.out.println("Serverid num is "+ Integer.parseInt(sCurrentLine));

        PrintWriter writer = new PrintWriter("src/serverid.txt", "UTF-8");
        if(Integer.parseInt(sCurrentLine) >= num)
        {
            writer.println(1);
        }
        else
        {
            writer.println(Integer.parseInt(sCurrentLine)+1);
        }
        writer.close();
        return Integer.parseInt(sCurrentLine);
    }
    
}

