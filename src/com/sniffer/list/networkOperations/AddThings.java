/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.networkOperations;

import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.list.json.ResourceCollectionWriter;
import com.sniffer.mqtt.GenericClient;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class adds a sniffer or device to local resource collection.
 * @author eliseu
 */
public class AddThings {
    private final String resourceCollectionPath;
    private final String snifferID;
    private final ResourceCollectionReader reader;
    
    private GenericClient internetBroker;
    private Map<String,GenericClient> dataBrokers;

    /**
     * Constructor of the class
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID snifferID of sniffer to attach devices to
     */
    public AddThings(String resourceCollectionPath, String snifferID) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        reader = new ResourceCollectionReader(resourceCollectionPath);
    }

    /**
     * Set internal internet broker and replaces the list of active mqtt clients.
     * @param internetBroker internet broker
     * @param dataBrokers dictionary of sniffer-broker associations. Key is snifferID and value is mqtt client.
     */
    public void setSubscriptionActive(GenericClient internetBroker, Map<String,GenericClient> dataBrokers){
        this.internetBroker = internetBroker;
        this.dataBrokers = dataBrokers;
    }

    /**
     * This method checks the resource collection if a sniffer with these data is already registered. If not register it, if yes overwrite it.
     * It creates a DataManager object for device or sniffer in each cases, but don't saves it.
     * @param newSnifferJSON sniffer data json object
     * @throws JSONException thrown on read / write error
     */
    public void addThing(JSONObject newSnifferJSON) throws JSONException {
        String newSnifferID = newSnifferJSON.getString("sniffer_id");
        int positionSniffer = reader.findSniffer(newSnifferID);
        //check if sniffer is in the list IF not ADD ist ELSE overwrite it
        if (positionSniffer < 0) {
            System.out.println("Sniffer wasn't registered in the list.");
            addSniffer(newSnifferJSON);
            DataManager manager = new DataManager(resourceCollectionPath, snifferID);
            manager.addDataSniffer(newSnifferID, internetBroker, dataBrokers);

        } else {
            System.out.println("Sniffer was registered in the list.");

            //check new devices in that sniffer
            JSONArray newDevices = newSnifferJSON.getJSONArray("devices");

            JSONObject device;
            for (int i = 0; i < newDevices.length(); i++) {
                device = (JSONObject) newDevices.get(i);

                //check if new device are registered on list
                String newDeviceID = device.getString("device_id");
                //query resource collection for each device
                int positionDevice = reader.findDevice(positionSniffer, newDeviceID);
                // if device dont exists create DataManager for device and add it to the collection
                if (positionDevice < 0) {
                    addDevice(newSnifferID, device);
                    //instantiate new device object
                    DataManager manager = new DataManager(resourceCollectionPath, snifferID);
                    manager.addDataDevice(newDeviceID, newSnifferID, dataBrokers);
                }
            }
        }
    }

    /**
     * Writes device to resource collection
     * @param snifferJSON sniffer data JSON object
     * @throws JSONException thrown on read / write error
     */
    public void addSniffer(JSONObject snifferJSON) throws JSONException {
        //add sniffer to list
        System.out.println("Adding the new sniffer to the list.");
        ResourceCollectionWriter writer = new ResourceCollectionWriter(resourceCollectionPath);
        writer.setList(reader.getList());
        writer.addSniffer(snifferJSON);
        writer.saveListFile();       
    }

    /**
     * Writes device to resource collection
     * @param snifferID sniffer to associate the device to
     * @param device device data JSON object
     * @throws JSONException thrown on read / write error
     */
    public void addDevice(String snifferID, JSONObject device) throws JSONException{
        System.out.println("Adding the new device to the list.");
        ResourceCollectionWriter writer = new ResourceCollectionWriter(resourceCollectionPath);
        writer.setList(reader.getList());
        writer.addDevice(snifferID, device);
        writer.saveListFile();    
    }
}
