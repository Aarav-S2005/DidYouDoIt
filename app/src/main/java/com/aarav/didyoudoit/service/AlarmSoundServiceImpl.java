package com.aarav.didyoudoit.service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native implementation of {@link AlarmSoundService} using Java Sound API (javax.sound.sampled).
 * Synthesizes a crisp, melodious multi-tone completion chime asynchronously with zero external dependencies.
 */
public class AlarmSoundServiceImpl implements AlarmSoundService {

    private static final Logger LOGGER = Logger.getLogger(AlarmSoundServiceImpl.class.getName());
    private static final int SAMPLE_RATE = 44100;

    @Override
    public void playTimerFinishedAlarm() {
        CompletableFuture.runAsync(() -> {
            try {
                byte[] audioData = generateChimeWaveform();
                playAudioPcm(audioData);
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Direct audio playback failed, falling back to system beep: " + e.getMessage());
                try {
                    Toolkit.getDefaultToolkit().beep();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private void playAudioPcm(byte[] pcmData) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, pcmData.length);
            line.start();
            line.write(pcmData, 0, pcmData.length);
            line.drain();
            line.stop();
        }
    }

    /**
     * Synthesizes an energetic, pleasant completion chime composed of two ascending chords:
     * Part 1: C5 (523Hz) -> E5 (659Hz) -> G5 (784Hz) -> C6 (1046Hz)
     * Short pause
     * Part 2: E5 (659Hz) -> G5 (784Hz) -> C6 (1046Hz, sustained)
     */
    private byte[] generateChimeWaveform() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Burst 1
        appendTone(out, 523.25, 0.12, 0.7);  // C5
        appendTone(out, 659.25, 0.12, 0.75); // E5
        appendTone(out, 783.99, 0.12, 0.8);  // G5
        appendTone(out, 1046.50, 0.28, 0.9); // C6

        // Silence pause (80ms)
        appendSilence(out, 0.08);

        // Burst 2
        appendTone(out, 659.25, 0.12, 0.75); // E5
        appendTone(out, 783.99, 0.12, 0.85); // G5
        appendTone(out, 1046.50, 0.45, 0.95);// C6 (ringing finale)

        return out.toByteArray();
    }

    private void appendTone(ByteArrayOutputStream out, double frequencyHz, double durationSec, double volume) {
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        for (int i = 0; i < totalSamples; i++) {
            double time = (double) i / SAMPLE_RATE;
            // Harmonic synthesis: fundamental + subtle 2nd harmonic for a rich bell chime
            double wave = Math.sin(2.0 * Math.PI * frequencyHz * time) * 0.75
                        + Math.sin(2.0 * Math.PI * (frequencyHz * 2) * time) * 0.25;

            // Amplitude envelope: quick attack (5ms), smooth exponential decay
            double progress = (double) i / totalSamples;
            double attack = Math.min(1.0, (double) i / (SAMPLE_RATE * 0.008));
            double decay = Math.exp(-3.5 * progress);
            double envelope = attack * decay * volume;

            short sample = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, wave * envelope * Short.MAX_VALUE));
            // 16-bit little-endian
            out.write(sample & 0xFF);
            out.write((sample >> 8) & 0xFF);
        }
    }

    private void appendSilence(ByteArrayOutputStream out, double durationSec) {
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        for (int i = 0; i < totalSamples; i++) {
            out.write(0);
            out.write(0);
        }
    }
}
