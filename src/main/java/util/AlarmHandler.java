package util;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.commons.lang3.text.translate.NumericEntityUnescaper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe;

public class AlarmHandler extends Thread{

    SerialPort device;
    MqttClient client;
    String UUID;

    IMqttMessageListener listener = (topic, message) -> {
        System.out.println("Topic: " + topic + ", Message: " + message);
        activateAlarm();
        System.out.println("Activated alarm");
    };

    public AlarmHandler(SerialPort device, MqttClient client, String serial) throws MqttException {
        this.device = device;
        this.UUID = serial;
        this.client = client;
        /*
        MqttConnectOptions options;
        options = new MqttConnectOptions();
        options.setUserName("DEFAULT");
        options.setPassword("DEFAULT".toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        this.client = new MqttClient("tcp://" + serverAddress + ":1883", serial );
        this.client.connect(options);

         */
    }

    //I know this works
    public void activateAlarm() throws SerialPortException {
        System.out.println("Alarm Triggering");
        //Will trigger the alarm to beep once
        device.openPort();
        device.setParams(
                SerialPort.BAUDRATE_9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE
        );
        byte[] c = {'A'};
        device.writeBytes(c);
        device.closePort();
    }

    @Override
    public void run() {
        try {
            this.client.subscribe(this.UUID + "-A", 0, listener);
            System.out.println("MQTT Client subscribed with topic " + this.UUID + "-A");
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }
}
