/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.sniffer;

import com.sniffer.list.networkOperations.DataManager;
import com.sniffer.mqtt.GenericClient;
import org.json.JSONException;

/**
 * This class deals with messages and connections from an associated MQTT-Client. Should represent the internal processes of a broker.
 * @author eliseu
 */
public class BrokerData extends GenericClient{
    private final GenericClient internetBroker;
    
    private final DataManager manager;

    /**
     * Constructor of BrokerData. Creates a new associated DataManager.
     * @param resourceCollectionPath resourceCollectionPath
     * @param subSnifferID snifferID that is associated with this instance and the new dataManager
     * @param internetBroker MQTT-Client
     * @throws JSONException thrown on error reading resource collection path
     */
    public BrokerData(String resourceCollectionPath, String subSnifferID, GenericClient internetBroker) throws JSONException {
        this.internetBroker = internetBroker;
        manager = new DataManager(resourceCollectionPath, subSnifferID);
    }

    /**
     * Forwards and buffers incoming messages to this broker.
     * @param message MQTT message
     * @param topic message's topic
     */
    @Override
    public void processMessage(String message, String topic) {
        manager.newDataMessageSniffer(message, topic, internetBroker, this.getTopicsMap(), 3);
    }

    /**
     * Print an error message to console on lost connection of the internal MQTT-client.
     */
    @Override
    public void lostConnection() {
        System.err.println("Error at data broker sniffer subscriber.");
    }
    
}
