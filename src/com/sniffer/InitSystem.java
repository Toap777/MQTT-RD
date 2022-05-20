/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer;

import com.sniffer.list.networkOperations.ResourceCollectionSubscriber;
import com.sniffer.list.networkOperations.RegistSniffer;
import com.sniffer.list.sniffer.SnifferThread;
import com.sniffer.udp.CheckNetworkThread;
import com.sniffer.list.json.ResourceCollectionReader;
import com.sniffer.list.json.ResourceCollectionWriter;
import com.sniffer.udp.NetworkOperations;
import java.io.File;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import io.moquette.server.Server;
import java.io.FileWriter;
import java.util.Properties;

/**
 * Fassade class. Initialize the system by starting at least one local broker and sniffer. Could be used to retrieve the systems config.
 * @author eliseu
 */
public class InitSystem {
    //port that will be used to open a mqtt broker on, clients connect to this
    private static final String MQTT_PORT = "1883";
    private String configPath = "/src/com/sniffer/resources/config1.json";
    private String logPath = "/src/com/sniffer/resources/logMessages.csv";

    //configures the system to use local hosts ip-address to start the mqtt broker on; possible values: autodetect, "" else uses ip: attribute of local broker in config file.
    private static final String IP_AUTO_DETECTED = "autodetected";
    private static final String TYPE_INTERNET = "internet";
    //path of the "resourceCollection" file
    private String resourceCollectionPath;

    /**
     * This method does the following steps to start the MQTT-RD system:
     * 1. read configuration file
     * 2. reset ResourceCollection / create Writer object
     * 3. send multicast sniffer discovery request
     * 4. add external sniffer to ResourceCollection
     * 5. start own Sniffer thread.
     * @throws JSONException exception on parsing JSON file
     * @throws InterruptedException exception on interrupt of program (ex. strg +c)
     * @throws IOException exception on dealing with read and write operations to the file system
     */
    public void startSniffer() throws JSONException, InterruptedException, IOException{
        String rootPath = new File("").getAbsolutePath();

        //Intialize logfile, Writer and reset the logfile
        logPath = rootPath.concat(logPath);
        File file = new File(logPath);
        FileWriter fw = new FileWriter(file,false);
        fw.append("###### SYSTEM LOG ######\n");
        fw.close();
        System.out.println("Logging started. Logging messages to: " + logPath);

        //Thread to monitor the machine resources
        (new Thread(new MonitorResources())).start();


        //Configuration file
        configPath = rootPath.concat(configPath);
        JSONObject config = parseConfig(configPath);

        //Initialize ListWriter and reset resourceCollectionlist
        resourceCollectionPath = rootPath.concat(config.getString("list_path"));
        ResourceCollectionWriter writer = new ResourceCollectionWriter(resourceCollectionPath);
        writer.resetList();


        //start local mosquitto broker
        NetworkOperations net = new NetworkOperations();
        String ShouldStartLocalBroker = config.getString("start_broker");
        //starts a local broker if JSON property "start_broker" is "yes", expects local_broker-object to be described in config file
        if ("yes".equals(ShouldStartLocalBroker)) {
            JSONObject configBroker = config.getJSONObject("local_broker");
            String port = configBroker.getString("port");
            String ip = configBroker.getString("ip");

            // get local hosts IP
            if (ip.equals(IP_AUTO_DETECTED)) {
                ip = net.getLocalIP();
            }
            startMQTTBroker(ip, port);
        }


        //Get the network parameters
        JSONObject networkJSON = config.getJSONObject("network");
        String networkMulticastIP = networkJSON.getString("ip");
        String networkPort = networkJSON.getString("port");

        //Initialize the remote/local sniffer properties:
        //Try to discover at least one available network sniffer
        System.out.println("Discovery of (local/ internet) sniffers via udp started ...");
        String meshSnifferIP = net.sendPacketNetwork(networkMulticastIP, Integer.parseInt(networkPort));
        //read from config file
        String meshSnifferID = config.getString("sniffer_id");
        String meshSnifferType = config.getString("sniffer_type");

        //if sniffer_type = TYPE_INTERNET register remote sniffer else register local sniffer.
        //Update local "resourceCollection"
        if (meshSnifferIP != null) {
            //Get the list from other sniffer
            ResourceCollectionSubscriber subList = new ResourceCollectionSubscriber(resourceCollectionPath);
            subList.getLocalresourceCollection(meshSnifferID, meshSnifferIP, MQTT_PORT); //TODO: Check if meshSnifferID is wrong naming scheme, cause it will be used as clientID with MQTTClient () class.

        }else if (meshSnifferType.equals(TYPE_INTERNET)) {
            ResourceCollectionSubscriber subList = new ResourceCollectionSubscriber(resourceCollectionPath);
            subList.getRemoteResourceCollection(meshSnifferID, config);
        }
        System.out.println("Done.");

        System.out.println("Register (local/ internet) sniffer to resourceCollection");
        //Register the local sniffer if necessary by copying config file parameter to resource Collection
        RegistSniffer serviceReg = new RegistSniffer(resourceCollectionPath);
        //Write to resourceCollection
        serviceReg.createSnifferRegist(meshSnifferID, meshSnifferType, config.getString("transport"), networkJSON);
        serviceReg.setLocalBroker(config);
        if (meshSnifferType.equals(TYPE_INTERNET)) {
            serviceReg.setRemoteBroker(config);
            serviceReg.registSniffer();
        } else {
            //standard: localBroker
            serviceReg.registSniffer();
        }
        System.out.println("Done.");

        System.out.println("Starting new local sniffer...");
        //Create thread for the devices registration by connecting with a new MQTTClient to currently started broker and connect a lot of callbacks to it.
        (new Thread(new SnifferThread(resourceCollectionPath, meshSnifferID), "SnifferThread")).start();

        //Create sniffer thread for responses on device and sniffer discovery messages
        (new Thread(new CheckNetworkThread(networkMulticastIP, Integer.parseInt(networkPort), resourceCollectionPath, meshSnifferID), "NetworkChecker")).start();
        System.out.println("Done.");
    }

    /**
     * Starts a broker by using a Mosquetto server.
     * @param ip broker address
     * @param port broker port
     */
    private void startMQTTBroker(String ip, String port){
        try {
            Properties prop = new Properties();
            prop.setProperty("port", port);
            prop.setProperty("host", ip);
            prop.setProperty("allow_anonymous", "true");
            
            Server mqttBroker = new Server();

            System.out.println("\nStarting the MQTT (local network) broker...");
            mqttBroker.startServer(prop);
            
            System.out.println("Moquette MQTT broker started, press ctrl-c to shutdown..");
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("\nStopping moquette MQTT broker..");
                    mqttBroker.stopServer();
                    System.out.println("Moquette MQTT broker stopped");
                }
            });
        } catch (IOException ex) {
            System.err.println("Can't execute the MQTT broker.");
        }
    }

    /**
     * This method reads a JSONobject representing the systems config file from {configPathAbsolute};
     * @param configPathAbsolute absolute path of config file
     * @return config JSON object if parsing was successful
     * @throws JSONException generic exception on parsing errors
     */
    private JSONObject parseConfig(String configPathAbsolute) throws JSONException {
        System.out.println("Parsing configuration file at path: " + configPath);
        ResourceCollectionReader reader = new ResourceCollectionReader(configPathAbsolute);
        try{
            JSONObject config = reader.getList();
            return config;
        }
        catch (Exception e) {
            System.out.println("An error occured while parsing: " +  e.getMessage() + "\n" + e.getCause() + "\n" + e.fillInStackTrace());
            throw new JSONException(e);
        }
    }
}
