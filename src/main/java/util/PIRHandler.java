package util;

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.platform.Platforms;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class PIRHandler extends Thread{

    private static final int PIN_BUTTON = 24; // PIN 18 = BCM 24
    private final MqttClient client;
    private final String serial;

    public PIRHandler(MqttClient client, String serial) {
        this.client = client;
        this.serial = serial;
    }

    @Override
    public void run() {

        var pi4j = Pi4J.newAutoContext();

        Platforms platforms = pi4j.platforms();

        System.out.println("Pi4J PLATFORMS");
        platforms.describe().print(System.out);

        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("button")
                .name("Press button")
                .address(PIN_BUTTON)
                .debounce(300L)
                .provider("pigpio-digital-input");

        var button = pi4j.create(buttonConfig);

        button.addListener(e -> {
            if (e.state() == DigitalState.LOW) {
                try {
                    client.publish(serial + "-M", new MqttMessage());
                    System.out.println("Motion detected");
                } catch (MqttException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
