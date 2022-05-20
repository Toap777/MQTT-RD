/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.sniffer.publish;

import com.sniffer.mqtt.BroadcastSniffers;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A thread runnable that sends keep alive ping messages to all sniffers at a static time interval.
 * @author eliseu
 */
public class PingThread implements Runnable{
    private final String resourceCollectionPath;
    private final String snifferID;  
    private static final int PUBLISH_INTERVAL = 1000;
    //expected data for ping message
    private static final String PING_ALIVE = "pingalive";

    /**
     * Constructor of PingThread
     * @param resourceCollectionPath resourceCollectionPath
     * @param snifferID snifferID of sniffer that want to send a ping.
     */
    public PingThread(String resourceCollectionPath, String snifferID){
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
    }

    /**
     * Ping loop. Waits for {@value PUBLISH_INTERVAL} ms and then sends a broadcast ping to all sniffers.
     */
    @Override
    public void run() {
        try {
            System.out.println("Starting the thread responsible to ping the others sniffers.");
            JSONObject objectJSON = new JSONObject();
            objectJSON.put("sniffer_id", snifferID);
            
            BroadcastSniffers send = new BroadcastSniffers(resourceCollectionPath, snifferID);
            
            while (true) {
                send.sendObject(PING_ALIVE, objectJSON);
                
                //sleep between publications
                Thread.sleep(PUBLISH_INTERVAL);
            }
        } catch (JSONException | InterruptedException ex) {
            System.err.println("Stop pinging the other sniffers.");
        }
    }
    
}
