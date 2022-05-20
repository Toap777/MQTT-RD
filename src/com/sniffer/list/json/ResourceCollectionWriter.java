/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.json;

import java.io.FileWriter;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This executes and caches write operations to resource collection for device, sniffer and the whole resource collection.
 * @author eliseu
 */
public class ResourceCollectionWriter {
    //cache object
    private JSONObject resourceCollectionJSON;
    private final String resourceCollectionPath;

    /**
     * Constructor of class
     * @param resourceCollectionPath resourceCollectionPath
     */
    public ResourceCollectionWriter(String resourceCollectionPath) {
        this.resourceCollectionPath = resourceCollectionPath;
    }

    /**
     * Setter of resource collection cache object.
     * @param resourceCollectionJSON resourceCollectionJSON
     */
    public void setList(JSONObject resourceCollectionJSON){
         this.resourceCollectionJSON = resourceCollectionJSON;
    }

    /**
     * Writes the current state of the resource collection cache object to the resource collection file at {@see resourceCollectionPath}.
     */
    public synchronized void saveListFile(){
        FileWriter file;
        try {
            file = new FileWriter(resourceCollectionPath);
            file.write(resourceCollectionJSON.toString(2));
            file.flush();
        } catch (IOException | JSONException ex) {
            System.err.println("Can not save the list in a file.");
        }
    }

    /**
     * Clears and saves current resource collection file.
     * @throws JSONException thrown on reading / write error
     */
    public void resetList() throws JSONException{
        JSONObject resetList = new JSONObject();
        JSONArray sniffersArray = new JSONArray();
        resetList.put("sniffers", sniffersArray);
        setList(resetList);
        saveListFile();
    }

    /**
     * Adds a sniffer to resource collection cache.
     * @param snifferJSON sniffer data object representation.
     * @throws JSONException thrown on read error
     */
    public void addSniffer(JSONObject snifferJSON) throws JSONException{
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        sniffersArray.put(snifferJSON);
        resourceCollectionJSON.put("sniffers", sniffersArray);
    }

    /**
     * Adds a device to resource collection cache if its not already contained in resource collection file.
     * @param snifferID snifferID of sniffer the device is associated to.
     * @param deviceJSON device data JSON object
     * @throws JSONException thrown on read error
     */
    public void addDevice(String snifferID, JSONObject deviceJSON) throws JSONException {
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
        
        int positionSniffer = reader.findSniffer(snifferID);
        int positionDevice = reader.findDevice(positionSniffer, deviceJSON.getString("device_id"));
        if (positionDevice < 0) {

            JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
            JSONObject snifferJSON = (JSONObject) sniffersArray.get(positionSniffer);
            JSONArray devicesArray = snifferJSON.getJSONArray("devices");
            devicesArray.put(deviceJSON);
            snifferJSON.put("devices", devicesArray);
            sniffersArray.put(positionSniffer, snifferJSON);
            resourceCollectionJSON.put("sniffers", sniffersArray);
        }
    }

    /**
     * Finds device associated to sniffer from resource collection and remove it from cache if found.
     * @param snifferID snifferID of sniffer the device is associated to.
     * @param deviceID deviceId to search for
     * @throws JSONException thrown on read error
     */
    public void removeDevice(String snifferID, String deviceID) throws JSONException{
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
        
        int positionSniffer = reader.findSniffer(snifferID);
        int positionDevice = reader.findDevice(positionSniffer, deviceID);
        if(positionDevice >= 0){
            JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
            JSONObject snifferJSON = (JSONObject) sniffersArray.get(positionSniffer);
            JSONArray devicesArray = snifferJSON.getJSONArray("devices");
            
            devicesArray.remove(positionDevice);
            
            snifferJSON.put("devices", devicesArray);
            sniffersArray.put(positionSniffer, snifferJSON);
            resourceCollectionJSON.put("sniffers", sniffersArray);
        }
    }

    /**
     * Finds sniffer having snifferID from resource collection and remove it from cache if found.
     * @param snifferID sniffer id to search for
     * @throws JSONException thrown on reading / write error
     */
    public void removeSniffer(String snifferID) throws JSONException{
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
        
        int positionSniffer = reader.findSniffer(snifferID);
        if(positionSniffer >= 0){
            JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
            
            sniffersArray.remove(positionSniffer);
            
            resourceCollectionJSON.put("sniffers", sniffersArray);
        }
    }
}
