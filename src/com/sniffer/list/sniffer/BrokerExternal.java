/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.sniffer;

import com.sniffer.mqtt.GenericClient;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;

/**
 * This class represents an external broker connection (?). It does not process device registration messages unlike internal broker.
 * TODO: is this true? Could also represent the implementation of an external broker ?
 * @author eliseu
 */
public class BrokerExternal extends GenericClient{
    private static final String TOPIC_SNIFFER = "/sniffercommunication";

    //sniffer representation
    private final SnifferOperations ope;

    /**
     * Constructor of class.
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID associated sniffer's id
     * @param confBroker broker type
     * @throws JSONException thrown on reading or writing error of resource collection
     */
    public BrokerExternal(String resourceCollectionPath, String snifferID, String confBroker) throws JSONException {
        ope = new SnifferOperations(resourceCollectionPath, snifferID, confBroker);
    }

    /**
     * IF its a sniffer message demultiplex and process it with associated sniffer. I don't react to device registration messages.
     * @param message message
     * @param topic the topic the message was published to
     */
    @Override
    public void processMessage(String message, String topic) {
        try {
            switch (topic) {
                case TOPIC_SNIFFER:
                    ope.demultiplexAndProcess(message);
                    break;
                default:
                    break;
            }
            
        } catch (JSONException ex) {
            Logger.getLogger(BrokerExternal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Logs an error message to the console on lost mqtt connection to a sniffer.
     */
    @Override
    public void lostConnection() {
        System.err.println("Error at external broker connection.");
    }
    
}
