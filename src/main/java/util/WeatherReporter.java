package util;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class WeatherReporter extends Thread{
    SerialPort device;
    public String weatherInfo;
    String serial, authenticator, ip;
    Timer timer;
    boolean stop;

    public WeatherReporter(SerialPort device, String serial, String authenticator, String ip) throws SerialPortException {
        if (device != null){
            this.ip = ip;
            this.device = device;
            this.authenticator = authenticator;
            this.serial = serial;
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        ActionListener listener = e -> {
            try {
                //Report status Info
                weatherInfo = getWeatherInfo();
                relayInformation();
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException | SerialPortException ex) {
                ex.printStackTrace();
            }
        };
        this.timer = new Timer(5000, listener);
        this.timer.start();
    }

    @Override
    public void run() {
        super.run();
    }

    private String getWeatherInfo() throws SerialPortException {
        byte b;
        int c = 0;
        StringBuilder builder = new StringBuilder();
        device.openPort();
        device.setParams(SerialPort.BAUDRATE_9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        while(true) {
            try {
                b = device.readBytes(1, 5000)[0];
                builder.append((char) (b & 0xFF));
                if (b == 10 || c >= 64) {
                    device.closePort();
                    return String.valueOf(builder);
                }
                c++;
            } catch (SerialPortTimeoutException | SerialPortException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSystem(){
        timer.stop();
        stop = true;
    }

    private void relayInformation() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        HttpsOperations https = new HttpsOperations();
        HashMap<String,String> headers = new HashMap<>();
        headers.put("solicit", "weather");

        https.post(ip, serial + "|" + authenticator + "\n" + weatherInfo, headers, false);
    }



}
