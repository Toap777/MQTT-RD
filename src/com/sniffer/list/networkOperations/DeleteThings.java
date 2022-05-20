/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.networkOperations;

import com.sniffer.mqtt.BroadcastSniffers;
import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.list.json.ResourceCollectionWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class bundles all network and ressource collection I/O operations on deleting devices and sniffers.
 * @author eliseu
 */
public class DeleteThings {
    //expected operation name  of DELETE_DEVICE_BROADCAST message
    private static final String DELETE_DEVICE_OPERATION = "deletedevice";
    //expected operation name  of DELETE_SNIFFER_OPERATION message
    private static final String DELETE_SNIFFER_OPERATION = "deletesniffer";
    private final String resourceCollectionPath;
    private final String snifferID;
    private final ResourceCollectionReader reader;

    /**
     * Constructor of class
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID associated sniffer
     */
    public DeleteThings(String resourceCollectionPath, String snifferID) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
        this.reader = new ResourceCollectionReader(resourceCollectionPath);
    }

    /**
     * Removes sniffer with snifferID from resourceCollection file.
     * @param rSnifferID snifferID
     * @throws JSONException thrown on read / write error
     */
    public void deleteSniffer(String rSnifferID) throws JSONException{
        //remove device from list
        System.out.println("Deleting the sniffer: " + rSnifferID);
        ResourceCollectionWriter writer = new ResourceCollectionWriter(resourceCollectionPath);
        writer.setList(reader.getList());
        writer.removeSniffer(rSnifferID);
        writer.saveListFile();     
    }

    /**
     * Deletes device with deviceID of sniffer with snifferID from resourceCollection.
     * @param rSnifferID snifferID
     * @param rDeviceID deviceID
     * @throws JSONException thrown on read / write error
     */
    public void deleteDevice(String rSnifferID, String rDeviceID) throws JSONException{
        //remove device from list
        System.out.println("Deleting the device: " + rDeviceID + ", that corresponds to sniffer: " + rSnifferID);
        ResourceCollectionWriter writer = new ResourceCollectionWriter(resourceCollectionPath);
        writer.setList(reader.getList());
        writer.removeDevice(rSnifferID, rDeviceID);
        writer.saveListFile();        
    }

    /**
     * Gets device with rdeviceID from sniffer with rSnifferID and sends a delete notification to sniffer having rsnifferID.
     * @param rSnifferID sniffer the device is associated to
     * @param rDeviceID device to notify about
     * @throws JSONException thrown on resource collection read error
     */
    public void sendDeleteDevice(String rSnifferID, String rDeviceID) throws JSONException{
        JSONArray sniffersArray = reader.getList().getJSONArray("sniffers");
        int positionSniffer = reader.findSniffer(rSnifferID);
        JSONObject snifferJSON = sniffersArray.getJSONObject(positionSniffer);
        
        JSONArray devicesArray = snifferJSON.getJSONArray("devices");
        int positionDevice = reader.findDevice(positionSniffer, rDeviceID);
        JSONObject deviceJSON = devicesArray.getJSONObject(positionDevice);
        
        deviceJSON.put("sniffer_id", rSnifferID);
        
        System.out.println("Sending a delete device operation to the rest of the network.");
        //sends a message to only one sniffer with rSnifferID over /sniffercommunciation channel.
        BroadcastSniffers broad = new BroadcastSniffers(resourceCollectionPath, snifferID);
        broad.sendObject(DELETE_DEVICE_OPERATION, deviceJSON);
    }

    /**
     * Sends a delete notification of rSnifferID to sniffer having this.snifferID.
     * @param rSnifferID the snifferID of sniffer that gets broadcasted to be deleted
     * @throws JSONException thrown on read / write error
     */
    public void sendDeleteSniffer(String rSnifferID) throws JSONException{
        JSONObject snifferJSON = new JSONObject();
        snifferJSON.put("sniffer_id", rSnifferID);
        
        System.out.println("Sending a delete sniffer operation to the rest of the network.");
        BroadcastSniffers broad = new BroadcastSniffers(resourceCollectionPath, snifferID);
        broad.sendObject(DELETE_SNIFFER_OPERATION, snifferJSON);        
    }
}
