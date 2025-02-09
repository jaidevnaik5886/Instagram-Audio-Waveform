Instagram-Style Audio Recorder Waveform in Android

Introduction

This project demonstrates how to create an Instagram-style audio recorder waveform in Android using a custom View and Fragment. The AudioRecorderWaveformView dynamically visualizes real-time audio amplitude changes while recording.

Features

🎵 Real-time waveform visualization

🎨 Customizable bar width, spacing, and color

🔄 Automatic scaling and memory management

🛠 Easy reset functionality

📲 Compatible with modern Android versions

Usage
```
        <com.jaidev.instagramstyleaudiorecorder.AudioRecorderWaveformView
            android:id="@+id/waveformView"
            android:layout_width="0dp"
            android:layout_height="32dp"
            app:wave_width="4dp"
            app:wave_corner_radius="3dp"
            app:wave_gap="3dp"
            app:wave_progress_color="@color/white"
          />
```

![Screen_Recording_20250209_170143_InstagramStyleAudioRecorder](https://github.com/user-attachments/assets/031a0054-d562-4853-9712-91b3a21c658b)
