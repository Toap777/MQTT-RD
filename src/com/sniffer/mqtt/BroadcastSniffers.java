/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.mqtt;

import com.sniffer.list.json.ResourceCollectionReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class delivers broadcast messages to all local OR network sniffers using the given snifferID.
 * @author eliseu
 */
public class BroadcastSniffers {
    private final String resourceCollectionPath;
    private final String snifferID;
    private final String snifferType;
    private static final String TYPE_INTERNET = "internet";
    private static final String TYPE_LOCAL = "local";
    private final ResourceCollectionReader reader;

    /**
     * Constructor of the class. Accesses the resourceCollection internal
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID associated sniffer
     * @throws JSONException thrown on resource collection read write error
     */
    public BroadcastSniffers(String resourceCollectionPath, String snifferID) throws JSONException {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        reader = new ResourceCollectionReader(resourceCollectionPath);
        this.snifferType = reader.getSnifferType(snifferID);
    }

    /**
     * Sends a broadcast message of operation name operation and data objectJSON. IF snifferType = TYPE_INTERNET send to all local AND internet sniffers ELSE send only to local sniffers.
     * @param operation event name that should be broadcast to the network. values: PING_ALIVE, REGIST_OPERATION, DELETE_DEVICE_OPERATION
     * @param objectJSON event data
     * @throws JSONException thrown on read / write error
     */
    public void sendObject(String operation, JSONObject objectJSON) throws JSONException {
        switch (snifferType) {
            case TYPE_INTERNET:
                sendLocal(operation, objectJSON);
                sendInternet(operation, objectJSON);
                break;
            case TYPE_LOCAL:
                sendLocal(operation, objectJSON);
                break;
            default:
                break;
        }

    }

    /**
     * Sends a broadcast message of operation name operation and data objectJSON to all local sniffers int hte network of sniffer having snifferID
     * @param operation operation name
     * @param objectJSON operation data
     * @throws JSONException thrown on read exception for resource collection
     */
    public synchronized void sendLocal(String operation, JSONObject objectJSON) throws JSONException{
        JSONObject message = new JSONObject();
        message.put("operation", operation);
        message.put("object", objectJSON);
        
        reader.readFile();
        List<String[]> sniffersList = reader.getSniffers(snifferID, TYPE_LOCAL);
        //loop through all local sniffers
        for (String[] snifferMetaData : sniffersList) {
            if (!snifferMetaData[0].equals(snifferID)) {
                
                SendThread send = new SendThread(snifferID,resourceCollectionPath, snifferMetaData[0], snifferMetaData[1], snifferMetaData[2]);
                send.setMessage(message.toString());
                Thread t = new Thread(send);
                t.start();

                //block until sending is finished
                synchronized (t) {
                    try {
                        t.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(BroadcastSniffers.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }        
    }

    /**
     * Sends a broadcast message of operation x and data objectJSON to all local sniffers int the network of sniffer having snifferID
     * @param operation operation name
     * @param objectJSON operation data
     * @throws JSONException thrown on read exception for resource collection
     */
    public synchronized void sendInternet(String operation, JSONObject objectJSON) throws JSONException{
        JSONObject message = new JSONObject();
        message.put("operation", operation);
        message.put("object", objectJSON);    
        
        reader.readFile();
        List<String[]> sniffersList = reader.getSniffers(snifferID, TYPE_INTERNET);
        for (String[] iterator : sniffersList) {
            if (!iterator[0].equals(snifferID)) {
               
                SendThread send = new SendThread(snifferID,resourceCollectionPath, iterator[0], iterator[1], iterator[2]);
                String[] auth = reader.getSnifferRemoteBroker(iterator[0]);
                send.setLoginAndPasswd(auth[2], auth[3]);
                send.setMessage(message.toString());
                Thread t = new Thread(send);
                t.start();
                
                synchronized (t) {
                    try {
                        t.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(BroadcastSniffers.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }         
                
            }
        }
    }
}
