/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.device;

import com.sniffer.list.networkOperations.DataManager;
import com.sniffer.list.networkOperations.DeleteThings;
import com.sniffer.list.networkOperations.RegistDevice;
import com.sniffer.list.sniffer.BrokerInternal;
import com.sniffer.mqtt.GenericClient;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * Multiplexes incoming mqtt messages at a high level and manages connection losses to other brokers in the network. This class represents a mqtt broker on high level and therefore sets up all broker threads.
 * @author eliseu
 */
public class BrokerDevice extends GenericClient{
    private final String resourceCollectionPath;
    private final String snifferID;
    private final String deviceID;
    private Boolean firstRegist;
    private static final String TOPIC_REGIST_DEVICE = "/registdevice";

    //messages per topic buffer
    private static final int LIMIT_MESSAGES = 5;
    //expected message data to kill the broker thread
    private static final String THREAD_KILL = "threadkill";

    //the different broker threads seem to exchange information through this object.
    private final String[] deviceToken;
    
    private final RegistDevice reg;
    private final DataManager data;

    /**
     * Constructor of class. Sets up message buffering and device registration.
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID associated sniffer ID
     * @param deviceToken information about this broker. Structure [0] = operation, [1] = deviceID, [2] = deviceIP, [3] = devicePort
     * @throws JSONException thrown on read write error of resource collection
     */
    public BrokerDevice(String resourceCollectionPath, String snifferID, String[] deviceToken) throws JSONException {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        this.deviceID = deviceToken[1];
        
        this.deviceToken = deviceToken;
        
        reg = new RegistDevice(resourceCollectionPath, snifferID);
        data = new DataManager(resourceCollectionPath, snifferID);
    }

    /**
     * Sets this.firstRegist
     * @param startedBySniffer boolean that express if this broker instance was started by sniffer.
     */
    public void setBrokerType(Boolean startedBySniffer){
        firstRegist = startedBySniffer;
    }

    /**
     * Processes messages differently depending on if it's a device registration message or any other message. Register device or transmit it to local sniffer.
     * @param message the mqtt message
     * @param topic the topic the message was published to
     */
    @Override
    public void processMessage(String message, String topic) {
        try {
            switch (topic) {
                case TOPIC_REGIST_DEVICE:
                    //registration messages
                    System.out.println("\nNew device regist operation in the local device broker.");
                    reg.newDevice(message, this, firstRegist, deviceToken);
                    if (!firstRegist) {
                        firstRegist = true;
                    }
                    break;
                default:
                    //data or ping messages
                    data.newDataMessageDevice(message, topic, this.getTopicsMap(), LIMIT_MESSAGES);
                    break;
            }
            
        } catch (JSONException ex) {
            Logger.getLogger(BrokerDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method gets called on lost connection to any of the other brokers. TODO: Check this...
     * It removes the sniffer associated to to disconnected broker from resource collection and notifies its own associated sniffer.
     * It halts the thread listening for incoming messages on this broker.
     */
    @Override
    public void lostConnection() {
        try {
            //If device goes down
            DeleteThings delete = new DeleteThings(resourceCollectionPath, snifferID);
            delete.sendDeleteDevice(snifferID, deviceID);
            delete.deleteDevice(snifferID, deviceID);
            deviceToken[0] = THREAD_KILL;
            synchronized(deviceToken){
                deviceToken.notify();
            }
            
        } catch (JSONException ex) {
            System.err.println("Error deleting device from list.");
            Logger.getLogger(BrokerInternal.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
}
