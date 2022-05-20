/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides multiple methods to read devices and teier attributes, sniffers, brokers and network from resource collection.
 * @author eliseu
 */
public class ResourceCollectionReader {
    private JSONObject resourceCollectionJSON;
    private final String resourceCollectionPath;
    private static final String TYPE_INTERNET = "internet";
    private static final String TYPE_LOCAL = "local";

    /**
     * Constructor of class. Reads in the current resource collection from disk and saves it as a JSON representation to memory.
     * @param resourceCollectionPath resourceCollectionPath
     */
    public ResourceCollectionReader(String resourceCollectionPath) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.readFile();
        
    }

    /**
     * Reads in the file at resourceCollectionPath and save it as internal JSON representation.
     */
    public final synchronized void readFile() {
        File f = new File(resourceCollectionPath);
        FileReader file;
        JSONObject json = null;
        try {
            file = new FileReader(f);

            char[] cbuf = new char[(int) f.length()];
            file.read(cbuf);
            String list = new String(cbuf);
            json = new JSONObject(list);
            
        } catch (FileNotFoundException ex) {
            System.err.println("File not found in this path: " + resourceCollectionPath);
        } catch (IOException | JSONException ex) {
            Logger.getLogger(ResourceCollectionReader.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Can not read the list from the file.");
        }
        
        this.resourceCollectionJSON = json;
    }

    /**
     * Finds the sniffer with specified snifferID
     * @param snifferID snifferID
     * @return position of sniffer in sniffers list OR -1 if it doesnt exist
     * @throws JSONException thrown on error on reading resource collection
     */
    public int findSniffer(String snifferID) throws JSONException{
        //access the highest hierarchy level of the resource collection file
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        
        JSONObject iterator;
        for (int i = 0; i < sniffersArray.length(); i++) {
            iterator = sniffersArray.getJSONObject(i);
            
            if(iterator.getString("sniffer_id").equals(snifferID)){
                return i;
            }
        }

        return -1;
    }

    /**
     * Reads sniffer having snifferID from resource collection and then read device having deviceID.
     * @param positionSniffer position of sniffer with associated device at resource collection
     * @param deviceID deviceID of searched device
     * @return position of searched device
     * @throws JSONException if error on reading resource collection
     */
    public int findDevice(int positionSniffer, String deviceID) throws JSONException{
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        JSONObject snifferJSON = (JSONObject) sniffersArray.get(positionSniffer);
        JSONArray devicesArray = snifferJSON.getJSONArray("devices");
        
        JSONObject iterator;
        for (int i = 0; i < devicesArray.length(); i++) {
            iterator = devicesArray.getJSONObject(i);
            
            if(iterator.getString("device_id").equals(deviceID)){
                return i;
            }
        }
        return -1;
    }

    /**
     * Getter for resource collection JSON representation
     * @return resource collection
     */
    public JSONObject getList(){
        return resourceCollectionJSON;
    }

    /**
     * Find sniffer having snifferID in resource collection and read type.
     * @param snifferID snifferID
     * @return the sniffers type
     * @throws JSONException thrown read write error
     */
    public String getSnifferType(String snifferID) throws JSONException{
        int position = findSniffer(snifferID);
        if(position < 0){
            System.err.println("Sniffer doesn't exist");
            return null;
        }
        
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        JSONObject snifferJSON = (JSONObject) sniffersArray.get(position);
        return snifferJSON.getString("sniffer_type");
    }

    /**
     * This method finds the sniffer with snifferID and reads it "local_broker" object. Expects a sniffer of type TYPE_LOCAL
     * @param snifferID snifferID to search for
     * @return IF sniffer is found: String array with network information of sniffer's broker [ip, port] ELSE null
     * @throws JSONException thrown on error reading resource collection
     */
    public String[] getSnifferLocalBroker(String snifferID) throws JSONException{
        int position = findSniffer(snifferID);
        if(position < 0){
            System.err.println("Sniffer doesn't exist");
            return null;
        }
        
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        JSONObject snifferJSON = (JSONObject) sniffersArray.get(position);
        JSONObject broker = snifferJSON.getJSONObject("local_broker");
        String url[] = new String[2];
        url[0] = broker.getString("ip");
        url[1] = broker.getString("port");
        return url;
    }

    /**
     * This method finds the sniffer with snifferID and reads it "remote_broker" object. Expects a sniffer of type TYPE_INTERNET
     * @param snifferID snifferID to search for
     * @return IF sniffer is found: String array with network information of sniffer's broker [ip, port, user, password] ELSE NULL
     * @throws JSONException thrown on error reading resource collection
     */
    public String[] getSnifferRemoteBroker(String snifferID) throws JSONException{
        int positionSniffer = findSniffer(snifferID);
        if(positionSniffer < 0){
            System.err.println("Sniffer doesn't exist");
            return null;
        }
        
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        JSONObject snifferJSON = (JSONObject) sniffersArray.get(positionSniffer);
        JSONObject rBroker = snifferJSON.getJSONObject("remote_broker");
        
        String[] auth = new String[4];
        auth[0] = rBroker.getString("ip");
        auth[1] = rBroker.getString("port");
        auth[2] = rBroker.getString("user");
        auth[3] = rBroker.getString("pass");
        
        return auth;
    }

    /**
     * This method accesses sniffer with snifferID from resource collection and reads the sniffers associated network information
     * @param snifferID snifferID
     * @return networkID of sniffers network OR null if the snifferID was not found in resource collection
     * @throws JSONException thrown on read error
     */
    public String getSnifferNetwork(String snifferID) throws JSONException{
        int position = findSniffer(snifferID);
        if (position < 0) {
            System.err.println("Sniffer doesn't exist");
            return null;
        }

        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        JSONObject snifferJSON = (JSONObject) sniffersArray.get(position); //TODO: Check if there could be a more dircet call without reading the collection twice..
        JSONObject networkJSON = snifferJSON.getJSONObject("network");

        return networkJSON.getString("network_id");
    }    

    /**
     * Reads all the devices of sniffer with specified sniffer id from resource collection and returns them into 2D array.
     * @param snifferID snifferID to search for
     * @return IF sniffer exists a 2D String array [i][j] with [i] is device position in resource collection and [j] is a Sting array [deviceId, device address] ELSE Null
     * @throws JSONException thrown on read error of resource collection
     */
    public String[][] getSnifferDevices(String snifferID) throws JSONException{
        String prefix = "tcp://";
        int position = findSniffer(snifferID);
        if(position < 0){
            System.err.println("Sniffer doesn't exist");
            return null;
        }
        
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        JSONObject snifferJSON = (JSONObject) sniffersArray.get(position);
        JSONArray devicesArray = snifferJSON.getJSONArray("devices");
        
        String[][] matrix = new String[devicesArray.length()][2];
        JSONObject iterator;
        JSONObject localBroker;
        for (int i = 0; i < devicesArray.length(); i++) {
            iterator = (JSONObject) devicesArray.get(i);
            localBroker = snifferJSON.getJSONObject("local_broker");            
            matrix[i][0] = iterator.getString("device_id"); //ID
            matrix[i][1] = prefix + localBroker.getString("ip") + ":" + localBroker.getString("port"); //e.g. tcp://192.168.2.1:2393
        }
        
        return matrix;
    }

    /**
     * Queries the resource collection for specified device of specified sniffer. IF anything is found it reads all device attributes.
     * @param snifferID snifferID
     * @param deviceID deviceID od device associated to a sniffer
     * @return String array containing all attributes_object_ID's of specified device OR null if sniffer doesn't exist
     * @throws JSONException thrown on read error of resource collection
     */
    public String[] getDeviceAttributes(String snifferID, String deviceID) throws JSONException{
        int positionSniffer = findSniffer(snifferID);
        if(positionSniffer < 0){
            System.err.println("Sniffer doesn't exist");
            return null;
        }
        
        int positionDevice = findDevice(positionSniffer, deviceID);
        if(positionDevice < 0){
            System.err.println("Device doesn't exist");
            return null;
        }
        
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        JSONObject snifferJSON = (JSONObject) sniffersArray.get(positionSniffer);
        JSONArray devicesArray = snifferJSON.getJSONArray("devices");
        JSONObject deviceJSON = (JSONObject) devicesArray.get(positionDevice);
        JSONArray attributes = deviceJSON.getJSONArray("attributes");
        
        String[] vector = new String[attributes.length()];
        JSONObject iterator;
        for (int i = 0; i < attributes.length(); i++) {
            iterator = (JSONObject) attributes.get(i);
            vector[i] = iterator.getString("object_id");
        }
        
        return vector;
    }

    /**
     * Returns a filtered list of sniffers depending on confBroker.
     * @param snifferID If confBroker = TYPE_local only sniffers in the same network as sniffer with snifferID are included in the list ELSE it is ignored.
     * @param confBroker Determine which type of sniffers should be returned. Value is whether TYPE_INTERNET or TYPE_LOCAL
     * @return A list of sniffer entries contained in resource collection. Entry is represented by String array with the defined format: [snifferID,ip,port]
     *@throws JSONException thrown on read error of resource collection
     */
    public List<String[]> getSniffers(String snifferID, String confBroker) throws JSONException{
        List<String[]> sniffersList = null;
        
        switch (confBroker) {
            case TYPE_INTERNET:
                // get all internet sniffers
                sniffersList = getInternetSniffers();
                break;
            case TYPE_LOCAL:
                // get the sniffers in the same network
                sniffersList = getNetworkSniffers(snifferID);
                break;
            default:
                break;

        }
        return sniffersList;
    }

    /**
     * This method compares every sniffers network to that associated to sniffer with snifferID. If its equal it generates an list entry.
     * @param snifferID snifferID
     * @return A list of sniffer entries contained in resource collection. Entry is represented by String array with the defined format: [snifferID,ip,port]
     * @throws JSONException thrown on read error
     */
    private List<String[]> getNetworkSniffers(String snifferID) throws JSONException{
        String snifferNetwork = getSnifferNetwork(snifferID);
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        List<String[]> sniffersList = new LinkedList<>();
        //loop through every persisted sniffer entry
        for (int i = 0; i < sniffersArray.length(); i++) {
            JSONObject iteratorJSON = sniffersArray.getJSONObject(i);
            String iteratorID = iteratorJSON.getString("sniffer_id");
            String iteratorNetwork = getSnifferNetwork(iteratorID);

            //save entry if snifferNetwork is the same as the that associated to sniffer with snifferID
            if(iteratorNetwork.equals(snifferNetwork)){
                String[] listItem = new String[3];
                listItem[0] = iteratorID;
                JSONObject lBroker = iteratorJSON.getJSONObject("local_broker");
                listItem[1] = lBroker.getString("ip");
                listItem[2] = lBroker.getString("port");
                sniffersList.add(listItem);
            }
        }
        
        return sniffersList;
    }

    /**
     * This method loops through all sniffer entries of ressource collection and writes the sniffers of sniffer_type: "TYPE_INTERNET" to the list.
     * @return A list of sniffer entries contained in resource collection. Entry is represented by String array with the defined format: [snifferID,ip,port]
     * @throws JSONException thrown on read error
     */
    private List<String[]> getInternetSniffers() throws JSONException{
        List<String[]> sniffersList = new LinkedList<>();
        JSONArray sniffersArray = resourceCollectionJSON.getJSONArray("sniffers");
        
        for (int i = 0; i < sniffersArray.length(); i++) {
            JSONObject iteratorJSON = sniffersArray.getJSONObject(i);
            String iteratorID = iteratorJSON.getString("sniffer_id");
            String iteratorType = getSnifferType(iteratorID);
            
            if (iteratorType.equals(TYPE_INTERNET)) {
                String[] listItem = new String[3];
                listItem[0] = iteratorID;
                JSONObject rBroker = iteratorJSON.getJSONObject("remote_broker");
                listItem[1] = rBroker.getString("ip");
                listItem[2] = rBroker.getString("port");
                sniffersList.add(listItem);
            }
        }
        
        return sniffersList;
    }
}
