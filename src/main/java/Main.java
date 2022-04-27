import jssc.SerialPort;
import jssc.SerialPortException;
import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import util.*;

import javax.usb.UsbException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Properties;

import static util.UVCDetector.getCams;

public class Main {
    public static LinkedList<StreamThread> currentStreams = new LinkedList<>();
    static LinkedList<String> currentCamDevices = new LinkedList<>();

    public static String primaryDomain = "triagecore.com";
    static String mainServer = "https://" + primaryDomain + ":8000/test";
    public static String serial;
    public static String authenticator;


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, UsbException, KeyManagementException, SerialPortException, MqttException {

        Properties properties = new Properties();
        try {
            properties.load(Main.class.getResourceAsStream("/base.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        serial = properties.getProperty("serial");
        authenticator = properties.getProperty("authenticator");

        //Initiate global MQTT env
        MqttConnectOptions options;
        options = new MqttConnectOptions();
        options.setUserName("DEFAULT");
        options.setPassword("DEFAULT".toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        MqttClient client = new MqttClient("tcp://" + primaryDomain + ":1883", serial );
        boolean hasNetwork = false;
        while (!hasNetwork){
            try {
                client.connect(options);
                hasNetwork = true;
            } catch (Exception e) {
                System.out.println("Could not connect to MQTT Service, waiting...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        System.out.println("MQTT system inititated");

        String cams = "";
        try {
            cams = getCams();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Cameras detected: " + cams);

        if (!(cams.length() == 0)) {
            String[] camSplit = cams.split("\n");

            if (camSplit.length > 0) {
                for (String cam : camSplit) {
                    if (!currentCamDevices.contains(cam)){
                        String number = "S" + currentCamDevices.size();
                        currentCamDevices.add(cam);
                        System.out.println("Starting new operation for '" + cam + "' at " + "rtsp://triagecore.com:8554/" + Main.serial + "/" + number);
                        StreamThread thread = new StreamThread("rtsp://triagecore.com:8554/" + Main.serial + "/" + number, cam);
                        thread.start();
                        currentStreams.add(thread);
                    }
                }
            }
        }

        PIRHandler handler = new PIRHandler(client, serial);
        handler.start();

        COMHandler comHandler = new COMHandler();

        WeatherReporter weatherReporter = null;
        comHandler.findValidSensors();
        if (!comHandler.weatherPorts.isEmpty()) {
            weatherReporter = new WeatherReporter(comHandler.weatherPorts.get(0), serial, authenticator, mainServer);
            weatherReporter.start();
        }
        AlarmHandler alarmHandler = null;
        if (!comHandler.alarmPorts.isEmpty()){
            alarmHandler = new AlarmHandler(comHandler.alarmPorts.get(0), client, serial);
            alarmHandler.start();
        }

        System.out.println("Target Reached: Alarm and weather reporter");
        StatusReporter reporter = new StatusReporter(mainServer, serial, authenticator, comHandler.weatherPorts, comHandler.alarmPorts, currentStreams);
        reporter.startTimer();

        // Display all the USB storage devices currently connected



        //Form a "teller" system to continuously report the temperature every 5 seconds, via HTTP
        //If a com device named weather sensor is connected, start a relay to the main server
        //detect via status, therefore com weather detected.
        //The client should ideally not display weather stats if the status they pull is not including a weather sensor

        /*
        Lets say we have a speaker set up and we want to play songs or whatever
        we need to be in constant contact with the server, so therefore we should
        make a UDP connection to the server in order to allow data to come back as well
        The response will come every time the server responds to our keepalive, which happens every 2 minutes
        The server may contact the device out of order in order to solicit a beep.
         */

        

        while(true){
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));

            // Reading data using readLine
            String name = "";
            try {
                name = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            switch (Objects.requireNonNull(name)) {
                case "stop" -> {
                    reporter.relayInformation("000A");
                    System.exit(0);
                }
                case "reloadSerial" -> {
                    properties = new Properties();
                    try {
                        properties.load(Main.class.getResourceAsStream("/base.properties"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    serial = properties.getProperty("serial");
                    authenticator = properties.getProperty("authenticator");
                }
                case "a" -> {
                    if (alarmHandler != null){
                    alarmHandler.activateAlarm();
                    System.out.println("Alarm activated");} else System.out.println("Alarm not installed");
                }
                case "w" -> {
                    if (weatherReporter != null)
                        System.out.println(weatherReporter.weatherInfo);
                    else System.out.println("Weather reporter not installed");
                }

                //case "update" -> reporter.performRelay();
                case "devices" -> {
                    try {
                        Process process = Runtime.getRuntime().exec("v4l2-ctl --list-devices");
                        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line = "";
                        StringBuilder buffer = new StringBuilder();
                        while ((line = output.readLine()) != null) {
                            buffer.append(line).append("\n");
                        }
                        String out = UVCDetector.readDevListing(buffer.toString());
                        System.out.println(out);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
