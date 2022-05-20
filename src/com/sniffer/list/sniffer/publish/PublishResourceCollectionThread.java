/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.list.sniffer.publish;

import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.mqtt.GenericClient;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread runnable that broadcasts the current state of local resource collection to all sniffers periodically.
 * @author eliseu
 */
public class PublishResourceCollectionThread implements Runnable{
    private final String resourceCollectionPath;
    private static final String TOPIC_LIST = "/getlist";
    private static final int PUBLISH_INTERVAL = 6000;
    private final GenericClient broker;

    /**
     * Constructor of class.
     * @param resourceCollectionPath resourceCollectionPath
     * @param broker mqtt client representing a broker
     */
    public PublishResourceCollectionThread(String resourceCollectionPath, GenericClient broker) {
        this.resourceCollectionPath = resourceCollectionPath;
        this.broker = broker;
    }
    /**
     * Publish loop. Waits for {@value PUBLISH_INTERVAL} ms and then broadcasts the current resource collection to all sniffers.
     */
    @Override
    public void run() {
        System.out.println("Publishing the list in this broker: " + broker.getClient().getCurrentServerURI());
        ResourceCollectionReader reader = new ResourceCollectionReader(resourceCollectionPath);
        try {
            while (true) {                
                reader.readFile();
                broker.publish(reader.getList().toString(), TOPIC_LIST);
                Thread.sleep(PUBLISH_INTERVAL);
            }
            
        } catch (InterruptedException ex) {
            Logger.getLogger(PublishResourceCollectionThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
