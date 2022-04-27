package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UVCDetector {
    /*
    It is said that UVCVIDEO will push a message to dmesg every time a new webcam comes up under [TIME] 'uvcvideo'
    therefore, it is possible to establish a reader that takes the output from 'dmesg -wt' where -w is the rolling input stream
    of dmesg that continually displays messages, and -t is the deletion of the time category that leaves only the descriptor and the message

    every time we see uvcvideo, the following important messages should be read:

        'uvcvideo: Found UVC' shows a new camera has been located
        'uvcvideo: Failed' shows that a camera was disconnected and we should scan 'v4l2-ctl --list-devices' for changes

    reading 'v4l2-ctl --list-devices' is as easy as delimiting by \n\n to get the sections, then splitting that by \n for the rows,
    then reading the second element to get the right video location, excluding the first two and limiting to three cameras

     */

    /**
     * Parses the output of 'v4l2-ctl --list-devices' into a format where the only thing that we are interested in is
     * the existence of the video objects from the devices listed
     *
     * @param output The output of the command 'v4l2-ctl --list-devices'
     * @return a <code>String</code> of the locations of valid cameras delimited by a comma but no space
     */
    public static String readDevListing(String output) {

        String[] sections = output.split("\n\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 2; i < sections.length; i++){
            String[] devices = sections[i].split("\n");
            builder.append(devices[1].trim()).append('\n');
        }
        return builder.toString();
    }

    public static String getCams() throws IOException {
        Process process = Runtime.getRuntime().exec("v4l2-ctl --list-devices");
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        StringBuilder buffer = new StringBuilder();
        while ((line = output.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        System.out.println(buffer);
        return UVCDetector.readDevListing(buffer.toString());
    }

}
