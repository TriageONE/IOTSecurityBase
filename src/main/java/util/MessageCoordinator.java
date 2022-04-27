package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MessageCoordinator extends Thread{


    //create a process in which the rolling output of DMESG is attached, where every line is read and checked to detect
    //a change in USB devices and find mount points

    /*
    if dmesg outputs a message saying
     */
    MessageCoordinator(){
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("dmesg -Wt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()  ));

        String output;


        while (true) {
            try {
                output = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}
