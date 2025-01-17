# MQTT-RD

The repository owner suggests the usage of hiveMQTT Broker instead of Eclipse moquetto, shown  in [this branch](https://github.com/Toap777/MQTT-RD/tree/hivemqbroker). After intensive testing during a thesis an exchange of the MQTT broker librray provided the needed speed ans stability to process messages of more than 40 devices.
TODO: Anyways there is still a issue with processing the ressourceColletion json file thtough mqtt devices, which is stored at the sniffers. When a mqtt device process this file to find new topics to subscribe it takes a growing and huge amount of time, which leads to delays in delivering messages...
~ Toap777

### Prerequisites

* **JDK** - Java Development Kit
* **Ant** - Java compiler used to compile sniffer project. [Linux Instalation](https://docs.wso2.com/display/ESB450/Installing+Apache+Ant+on+Linux) or [Windows Instalation](https://www.mkyong.com/ant/how-to-install-apache-ant-on-windows/)

### Installing

After clonning the repository, in the folder of the project, you need to build the sniffer.
After building the jar you can run the program.

```
ant -Dnb.internal.action.name=rebuild clean jar

java -jar dist/Sniffer.jar
```

### Execution
While devleopment IntelliJ IDE was used to run this system. A "Run/Debug Configuration" with main class com.sniffer.Main was created and executed.

#### JVM Arguments
The logging of heap usage of this software shows an very rapid growth of stored bytes. This is perhaps during the TCP connections to manage new subscribtions and to buffer some messages. The repository owner suggests to increase the Heap to atleast 500MB for 40 devices at start of the system to prevent frequenttly resizing at the Heap on system startup. Use -Xms500m as JVM option in IntelliJ or as parameter to "java -jar" command.

### Sniffer Configuration File

In the path [/src/com/sniffer/resources/config1.json](/src/com/sniffer/resources/config1.json) you can find the configuration file were you can change the configuration settings.

The configuration settings that you can change are:

* ***sniffer id*** - unnique id that you want to use.
* ***sniffer type*** - local or internet (if the broker was in a internet address).
* ***start broker*** - if you want to start automatically a broker (recomended yes).
* ***remote broker settings*** - IP address, port, username and password.
* ***local broker settings*** - 'autodetected' if you want ta the Sniffer detects the broker automatically, otherwise you must specify the IP. You need also to define the port.
* ***network settings*** - multicast address that you want to use for the device/sniffer discovery. You need also to define the port and the network id.

## Device Usage

If your device doesn´t know the IP of the Sniffer, you can discovert sending a multicast package to the multicast IP 224.0.1.1 and the port 9876, with the message 'newdevice'.

This way, the device needs to communicate with the Sniffer using multicast sockets (discovering the IP address) ([example file](/src/com/sniffer/udp/CheckNetworkThread.java)) and over MQTT (normal communication) ([example files](/src/com/sniffer/mqtt)). 

### MQTT Topics

The topics that the new devices uses for the the communication with the Sniffer are the following:

```
# topic to regist a new device in the sniffer responsible for the local network.
/registdevice

# topic to get the list off entities and devices from the Sniffer-MQTT.
/getlist

# topic used by the device, to publish the data from one attribute:
# device_id: ID used to register the device in the platform.  
# attribute_id: ID used to register the attribute of that device in the platform.
/device_id/attrs/attribute_id
```

## Citation

```
@inproceedings{MQTTRD,
  title={MQTT-RD: A MQTT based Resource Discovery for Machine to Machine Communication},
  author={Eliseu Pereira and Rui Pinto and Jo{\~a}o Reis and Gil Gonçalves},
  booktitle={IoTBDS},
  year={2019}
}
```
