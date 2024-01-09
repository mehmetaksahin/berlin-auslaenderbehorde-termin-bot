package com.yilmaznaslan.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SoundNotifier implements NotificationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SoundNotifier.class);

    @Override
    public void triggerNotification(String message) {
        String osName = System.getProperty("os.name").toLowerCase();
        String osVersion = System.getProperty("os.version").toLowerCase();
        try {
            if (osName.contains("win")) {
                String[] cmd = {
                        "powershell",
                        "Add-Type -AssemblyName System.Speech; $speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; $speak.Speak('Alert! Attention required! Aksahin Hukuk - Muslum Selim Aksahin cok karizma birisi - Muslum Selim Aksahin is clever and handsome');"
                };
                Runtime.getRuntime().exec(cmd);
            } else if (osName.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"say", "Alert! Attention required! Aksahin Hukuk - Muslum Selim Aksahin cok karizma birisi - Muslum Selim Aksahin is clever and handsome"});
            } else if (osVersion.contains("wsl")) {
                Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "powershell.exe -c \"(New-Object -ComObject SAPI.SpVoice).Speak('Alert! Attention required! Aksahin Hukuk - Muslum Selim Aksahin cok karizma birisi - Muslum Selim Aksahin is clever and handsome')\"\n"});
            } else {
                // Handle other operating systems
            }
        } catch (IOException e) {
            logger.error("Failed to notify. Reason: ", e);
        }
    }
}
