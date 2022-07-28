/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sniffer.udp;

import com.sniffer.list.device.BrokerDeviceThread;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides methods to receive register messages via udp for device and sniffers.
 * @author eliseu
 */
public class CheckNetworkThread implements Runnable {
    private final String resourceCollectionPath;
    private final String snifferID;
    //expected package content for sniffer ping request
    private static final String REQUEST_SNIFFER = "anyonesniffer?";
    //expected package content for sniffer ping response
    private static final String RESPONSE_SNIFFER = "yes";
    //expected package content for device ping request
    private static final String REQUEST_DEVICE = "newdevice";
    //expected package content for device ping response
    private static final String RESPONSE_DEVICE = "startregist";
    //max size of response packages in Bytes
    private static final int PACKET_SIZE = 300;

    private final String multicastAddress;
    private final int port;

    /**
     * Constructor of the class
     * @param multicastAddress multicast address to bind service to
     * @param port port to bind service to
     * @param resourceCollectionPath path to resourceCollection file
     * @param snifferID ID of sniffer that is added to resourceCollection as devices sniffer on ping of these device. ?
     */
    public CheckNetworkThread(String multicastAddress, int port, String resourceCollectionPath, String snifferID) {
        this.multicastAddress = multicastAddress;
        this.port = port;
        this.resourceCollectionPath = resourceCollectionPath;
        this.snifferID = snifferID;
    }

    /**
     *
     */
    @Override
    public void run() {
        try {
            System.out.println("\nStart checking new devices and sniffers in the network...");

            InetAddress addr = InetAddress.getByName(multicastAddress);
            MulticastSocket mcSock = new MulticastSocket(port);
            mcSock.joinGroup(addr);

            DatagramPacket rcPacket = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);

            //receiving loop
            while (true) {
                mcSock.receive(rcPacket);
                System.out.println("\nNew message.");
                String request = new String(rcPacket.getData()).trim();

                String deviceIP = rcPacket.getAddress().getHostAddress();

                String[] splitRequest = request.split(":");
                //expectation: the message contains the request type after :
                switch (splitRequest[0]) {
                   // Receiving a ping message (used as discovery) from a sniffer newly joined the network
                    case REQUEST_SNIFFER:
                        System.out.println("New sniffer in the network.\nSniffer ip: " + deviceIP);
                        sendResponse(deviceIP, RESPONSE_SNIFFER);
                        break;
                    // Receiving a ping from a device newly joined the network. This is done one time on join.
                    case REQUEST_DEVICE:
                        System.out.println("New device in the network.\nDevice ID: " + splitRequest[1]);
                        System.out.println("Device IP: " + deviceIP + ", Port: 1883");
                        sendResponse(deviceIP, RESPONSE_DEVICE);

                        (new Thread(new BrokerDeviceThread(resourceCollectionPath, snifferID, splitRequest[1], deviceIP, "1883", false), "DeviceThread")).start();
                        break;
                    default:
                        System.out.println("Message doesn't corresponds to the expected.");
                        break;
                }
            }

        } catch (IOException ex) {
            System.err.println("Error checking new network sniffers and devices.");
        }
    }

    /**
     * This method sends a connectionless network package with content {@see responseString} to {@see host} address.
     * @param host resolves a hostname to IP address or reads IP address string as an object
     * @param responseString sends a packet containing this string data
     */
    private void sendResponse(String host, String responseString) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            byte[] response = responseString.getBytes();

            DatagramSocket sendSOCK = new DatagramSocket();
            DatagramPacket responsePacket = new DatagramPacket(response, response.length);
            responsePacket.setAddress(addr);
            responsePacket.setPort(port + 1);
            sendSOCK.send(responsePacket);

            System.out.println("Sent packet to address: " + responsePacket.getAddress().getHostAddress());

        } catch (SocketException ex) {
            Logger.getLogger(CheckNetworkThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CheckNetworkThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
