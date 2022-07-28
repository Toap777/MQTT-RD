/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.mqtt;

import com.sniffer.list.networkOperations.DeleteThings;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * This class is responsible for sending a message on /sniffercommunication channel. It also manages the absent notification of all sniffers, if a sniffer could not be reached.
 * @author eliseu
 */
public class SendThread implements Runnable{
    private final String snifferID;
    private final String url;
    private static final String TOPIC_SNIFFER = "/sniffercommunication";
    private final BasicClientMQTT mqtt;
    private String message;
    private final String resourceCollectionPath;
    private final String otherSniffer;

    /**
     * Constructor of class. Initializes a new internal mqttClient connecting to ip and port.
     * @param snifferID the id of the associated sniffer instance
     * @param resourceCollectionPath resourceCollectionPath
     * @param otherSniffer snifferID of sniffer to send to
     * @param ip sniffer address to send to
     * @param port sniffer port to send to
     */
    public SendThread(String snifferID, String resourceCollectionPath, String otherSniffer, String ip, String port) {
        this.snifferID = snifferID;
        this.url = "tcp://" + ip + ":" + port;
        this.mqtt = new BasicClientMQTT();
        this.otherSniffer = otherSniffer;
        this.resourceCollectionPath = resourceCollectionPath;
        
    }

    /**
     * Sets username and password for internal mqtt connection.
     * @param login username
     * @param passwd password
     */
    public void setLoginAndPasswd(String login, String passwd){
        mqtt.setUserPass(login, passwd);
    }

    /**
     * Set message for sending.
     * @param message mqtt message
     */
    public void setMessage(String message){
        this.message = message;
    }

    /**
     * Open an mqtt connection to url and sen message on topic. On error delete otherSniffer from resoureCollection and notify via braodcast all other sniffers.
     * Runs one time. TODO: Be more detailed.
     */
    @Override
    public void run() {
        // build connection
        int success = mqtt.connect(url, snifferID + "BroadcastPublisher" + UUID.randomUUID());
        if (success < 0) {
            try {
                // self healing the sniffer network entries..
                DeleteThings delete = new DeleteThings(resourceCollectionPath, snifferID);
                delete.deleteSniffer(otherSniffer);
                delete.sendDeleteSniffer(otherSniffer);
            } catch (JSONException ex) {
                Logger.getLogger(SendThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            mqtt.publish(message, TOPIC_SNIFFER);
            mqtt.disconnect();
        }   
        //wakeup objects that are waiting for this instance.
        synchronized (this) {
            notify();
        }
    }
    
}
