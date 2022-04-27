package util;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.*;

public class StreamThread extends Thread{
    private final String outputUrl;
    private final String inputDevice;

    public StreamThread(String outputUrl, String inputDevice) {
        this.inputDevice = inputDevice;
        this.outputUrl = outputUrl;
    }

    @Override
    public void run() {
        super.run();
        try {
            System.out.println("Output url: " + this.outputUrl + ", Input Device:" + this.inputDevice);
            FFmpeg ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
            FFprobe ffprobe = new FFprobe("/usr/bin/ffprobe");

            String inputDesignator = this.inputDevice.split("/dev/video")[0];
            System.out.println(inputDesignator);

            FFmpegBuilder builder = new FFmpegBuilder()

                    .setInput(inputDevice)     // Filename, or a FFmpegProbeResult
                    .setFormat("v4l2")

                    .addOutput(outputUrl)   // Filename for the destination
                    .setVideoCodec("libx264")
                    .setPreset("veryfast")
                    .setFormat("rtsp")

                    .done();

            System.out.println("Created new builder");

            System.out.println(builder.build());

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            System.out.println("FFmpeg target reached: setup");
            executor.createJob(builder).run();
            //Code stops here, until the program dies.

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
