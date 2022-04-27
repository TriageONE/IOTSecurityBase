package util;

import jssc.SerialPort;

import javax.swing.*;
import javax.usb.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class StatusReporter extends Thread{

    //The status reporter object reports the status of the device periodically
    /**
     * <b>String STATUS</b>
     * <p>The status string contains multiple datapoints represented as characters in the string.
     * status datum are only one character long, and never longer.
     * The placement of these characters and how they are interpreted are to never be async from the standard described. </p>
     * <p>Characters below are defined by their positional placement within the string, where 1 is the 0th element</p>
     * <ol>
     *     <li>Cameras connected [1, 2, 3]</li>
     *     <li>Intercoms connected [1, 2, 3]</li>
     *     <li>Temperature sensors connected [1, 2, 3]</li>
     *     <li>Disk status, regarding space consumed
     *     <ul>
     *         <li>[A ... Z] where A=1 and Z=24, multiplied by 4 to gain the disk space used</li>
     *         <li>1 indicates the disk is full</li>
     *     </ul></li>
     *     <li>Temperature in Centigrade of the system's CPU [0-9]</li>
     * </ol>
     */
    String sharedStatus, serial, authenticator;
    String ip;
    Timer timer;
    LinkedList<DeviceType> devices;
    LinkedList<SerialPort> weatherSensors;
    LinkedList<SerialPort> alarms;
    LinkedList<StreamThread> cameras;

    public StatusReporter(String ip, String serial, String authenticator, LinkedList<SerialPort> weatherSensors, LinkedList<SerialPort> alarms, LinkedList<StreamThread> cameras) throws UsbException, IOException, NoSuchAlgorithmException, KeyManagementException {
        this.serial = serial;
        this.authenticator = authenticator;
        this.ip = ip;
        this.weatherSensors = weatherSensors;
        this.alarms = alarms;
        this.cameras = cameras;


        ActionListener listener = e -> {
            try {
                //Report status Info
                gatherStatusInfo();
                System.out.println(System.currentTimeMillis() / 1000 + " Checking in: " + sharedStatus );
                relayInformation();
            } catch (UsbException | IOException | NoSuchAlgorithmException | KeyManagementException ex) {
                ex.printStackTrace();
            }
        };
        this.timer = new Timer(60000, listener);


        gatherStatusInfo();
        relayInformation();

    }

    @Override
    public void run(){
    }

    public String getSharedStatus(){
        return sharedStatus;
    }

    public int getTempSensors(){
        return Integer.parseInt(sharedStatus.split("")[2]);
    }

    public String performRelay(){
        try {
            String s = gatherStatusInfo();
            relayInformation();
            return s;
        } catch (UsbException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
        return "";

    }

    public void startTimer(){ timer.start(); }
    public void stopTimer(){ timer.stop(); }

    private String gatherStatusInfo() throws UsbException {
        StringBuilder status = new StringBuilder();
        this.devices = getValidDevices(getAllAttachedDevices());
        status.append(this.cameras.size());
        status.append(this.weatherSensors.size());
        status.append(this.alarms.size());

        //Get the total space used and total space
        File[] files = File.listRoots();
        long freeSpace = 0;
        long totalSpace = 0;
        for  (File file : files) {
            freeSpace += file.getFreeSpace();
            totalSpace += file.getTotalSpace();
        }


        //The disk space will never reach over the unit 24. therefore, the max character is 'Z' for this permutation.
        double percentUsed = ((totalSpace - freeSpace) / (double) (totalSpace) * 100f) / 4.175;
        char ascii =  (char) ((int)(65 + percentUsed));

        status.append(ascii);

        //Get the CPU temperature

        sharedStatus = status.toString();
        return String.valueOf(status);
    }

    private void relayInformation() throws UsbException, IOException, NoSuchAlgorithmException, KeyManagementException {
        HttpsOperations https = new HttpsOperations();
        HashMap<String,String> headers = new HashMap<>();
        headers.put("request", "checkin");

        https.post(ip, serial + "|" + sharedStatus + "|" + authenticator, headers, false);
    }

    public void relayInformation(String customStatus) throws UsbException, IOException, NoSuchAlgorithmException, KeyManagementException {
        HttpsOperations https = new HttpsOperations();
        HashMap<String,String> headers = new HashMap<>();
        headers.put("request", "checkin");

        https.post(ip, serial + "|" + customStatus + "|" + authenticator, headers, false);
    }

    //Methods for handling USB devices

    //get all devices from the system
    public static LinkedList<DeviceType> getAllAttachedDevices() throws UsbException {
        UsbServices services = UsbHostManager.getUsbServices();
        UsbHub hub = services.getRootUsbHub();
        List allDevices = hub.getAttachedUsbDevices();
        LinkedList<DeviceType> deviceTypes = new LinkedList<>();
        for (Object o : allDevices) {
            deviceTypes.add(new DeviceType((UsbDevice) o));
        }
        return deviceTypes;
    }

    //Filter valid devices into a linked list
    public static LinkedList<DeviceType> getValidDevices(LinkedList<DeviceType> allDevices) {
        LinkedList<DeviceType> validDevices = new LinkedList<>();
        for (DeviceType type : allDevices){
            if (type.getType() != DeviceType.DeviceTypes.INVALID) {
                validDevices.add(type);
            }
        }
        return validDevices;
    }

    public LinkedList<DeviceType> getValidDevices() {
        LinkedList<DeviceType> validDevices = new LinkedList<>();
        for (DeviceType type : this.devices){
            if (type.getType() != DeviceType.DeviceTypes.INVALID) {
                validDevices.add(type);
            }
        }
        return validDevices;
    }

    public static int getNumDevices(DeviceType.DeviceTypes type, LinkedList<DeviceType> devices) {
        int i = 0;
        for (DeviceType device : devices) {
            if (device.getType() == type) i++;
        }
        return i;
    }


}
