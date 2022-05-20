/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.sniffer;

import com.sniffer.list.networkOperations.RegistDevice;
import com.sniffer.mqtt.GenericClient;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * This class represents a connection to an internal broker that is directly associated to a sniffer. (local broker)
 * @author eliseu
 */
public class BrokerInternal extends GenericClient{
    private static final String TOPIC_SNIFFER = "/sniffercommunication";
    private static final String TOPIC_REGIST_DEVICE = "/registdevice";
    
    private final String[] deviceToken;
    
    private final SnifferOperations ope;
    private final RegistDevice dev;

    /**
     * Class constructor.
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID associated sniffer's id
     * @param confBroker broker type
     * @param deviceToken information about this broker. Structure [0] = operation, [1] = deviceID, [2] = deviceIP, [3] = devicePort
     * @throws JSONException thrown on reading or writing error of resource collection
     */
    public BrokerInternal(String resourceCollectionPath, String snifferID, String confBroker, String[] deviceToken) throws JSONException {
        this.deviceToken = deviceToken;
        
        ope = new SnifferOperations(resourceCollectionPath, snifferID, confBroker);
        dev = new RegistDevice(resourceCollectionPath, snifferID);
    }

    /**
     * This method activates all processing functions on this.dataBrokers broker topics.
     * @param internetBroker represents a connection to an internet broker TODO:  Check if this is true.
     * @throws JSONException thrown on reading or writing error of resource collection
     */
    protected void setSubscriptionActive(GenericClient internetBroker) throws JSONException{
        Map<String,GenericClient> dataBrokers = new HashMap<>();
        ope.setSubscriptionsActive(internetBroker, dataBrokers);
        dev.setSubscriptionsActive(dataBrokers);
    }

    /**
     * IF its a sniffer message demultiplex and process it. IF its an register message from device create a new device.
     * @param message message
     * @param topic the topic the message was published to
     */
    @Override
    public void processMessage(String message, String topic) {
        try {
            switch (topic) {
                case TOPIC_SNIFFER:
                    //everything and registration notification between sniffers
                    ope.demultiplexAndProcess(message);
                    break;
                case TOPIC_REGIST_DEVICE:
                    //device to broker registration message
                    System.out.println("\nNew device regist operation in the local broker.");
                    dev.newDevice(message, this, Boolean.TRUE, deviceToken);
                    break;
                default:
                    break;
            }
            
        } catch (JSONException ex) {
            Logger.getLogger(BrokerInternal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Logs an error message to the console on lost mqtt connection to a sniffer.
     */
    @Override
    public void lostConnection() {
        System.err.println("Error at internal broker connection.");
    }
}
