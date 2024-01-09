package com.yilmaznaslan.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SoundNotifier implements NotificationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SoundNotifier.class);

    private static final String ALERT_MESSAGE = "Alert! Attention required! Aksahin Hukuk Bürosu!";

    @Override
    public void triggerNotification(String message) {
        String osName = System.getProperty("os.name").toLowerCase();
        String osVersion = System.getProperty("os.version").toLowerCase();

        try {
            int i = 0;
            while (i < 10) {
                seslendir(osName, osVersion);
                i++;
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void seslendir(String osName, String osVersion) {
        try {
            if (osName.contains("win")) {
                String[] cmd = {
                        "powershell",
                        "Add-Type -AssemblyName System.Speech; $speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; $speak.Speak('" + ALERT_MESSAGE + "');"
                };
                Runtime.getRuntime().exec(cmd);
            } else if (osName.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"say", ALERT_MESSAGE});
            } else if (osVersion.contains("wsl")) {
                Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "powershell.exe -c \"(New-Object -ComObject SAPI.SpVoice).Speak('" + ALERT_MESSAGE + "')\"\n"});
            } else {
                // Handle other operating systems
            }
        } catch (IOException e) {
            logger.error("Failed to notify. Reason: ", e);
        }
    }

}
