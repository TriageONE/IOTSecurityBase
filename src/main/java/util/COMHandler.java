package util;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Timer;

public class COMHandler {
    public LinkedList<SerialPort> ports = new LinkedList<>();
    public LinkedList<SerialPort> weatherPorts = new LinkedList<>();
    public LinkedList<SerialPort> alarmPorts = new LinkedList<>();

    public COMHandler(){
        String[] names = SerialPortList.getPortNames();
        for (String name : names){
            ports.add(new SerialPort(name));
        }
    }

    public void findValidSensors(){

        for (SerialPort port : ports) {
            char code = 0;
            try {

                port.openPort();//Open serial port
                port.setParams(SerialPort.BAUDRATE_9600,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE,
                        true, true);//Set params. Also you can set params by this string: serialPort.setParams(9600, 8, 1, 0);
                System.out.println("Sending 't' to " + port.getPortName());
                port.writeBytes("t".getBytes());//Write data to port
                //If this does what i think, this will hang when nothing comes through
                boolean hasNext = true;
                byte b;
                port.setDTR(true);

                while (hasNext){
                    try {
                        b = port.readBytes(1, 500)[0];
                        if (((char) (b & 0xFF)) == '$') {
                            b = port.readBytes(1, 500)[0];
                            code = (char) (b & 0xFF);
                            hasNext = false;
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        hasNext = false;
                    }
                }
                //code = Arrays.toString(port.readBytes(10));

                System.out.println("got " + code + " from " + port.getPortName());
                if (code == '1'){
                    weatherPorts.add(port);

                    System.out.println("added port " + port.getPortName() + " as weather sensor");
                }
                if (code == '2') {
                    alarmPorts.add(port);

                    System.out.println("added port " + port.getPortName() + " as alarm");
                }
                port.closePort();//Close serial port
            } catch (SerialPortException e) {
                e.printStackTrace();
            }
        }
    }
}
