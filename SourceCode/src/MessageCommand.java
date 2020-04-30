/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package decentralizedfilesharesystem;

import java.io.Serializable;

/**
 *
 * @author zn
 */
public class MessageCommand implements Serializable{
    
    
    protected Command cmd;
    protected Object body;

    public MessageCommand() {
    }

    public MessageCommand(Command cmd, Object body) {
        this.cmd = cmd;
        this.body = body;
    }

    public Command getCmd() {
        return cmd;
    }

    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }  

}

