package com.cavetale.midi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import lombok.Value;
import org.bukkit.Sound;

@Value
public final class Midi {
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private final List<Blip> blips = new ArrayList<>();

    @Value
    static class Blip {
        public final long tick;
        public final Sound sound;
        public final float pitch;
    }

    static Sound pianoSoundOf(int octave) {
        switch (octave) {
        case 0: case 1: return Sound.BLOCK_NOTE_BLOCK_BASS;
        case 2: case 3: return Sound.BLOCK_NOTE_BLOCK_GUITAR;
        case 4: case 5: return Sound.BLOCK_NOTE_BLOCK_PLING;
        default: return Sound.BLOCK_NOTE_BLOCK_HARP;
        }
    }

    static Sound fluteSoundOf(int octave) {
        switch (octave) {
        case 0: case 1: return Sound.BLOCK_NOTE_BLOCK_FLUTE;
        case 2: case 3: return Sound.BLOCK_NOTE_BLOCK_BELL;
        case 4: case 5: return Sound.BLOCK_NOTE_BLOCK_CHIME;
        default: return Sound.BLOCK_NOTE_BLOCK_CHIME;
        }
    }

    static Sound percussionSoundOf(int octave) {
        switch (octave) {
        case 0: case 1: return Sound.BLOCK_NOTE_BLOCK_BASEDRUM;
        case 2: case 3: return Sound.BLOCK_NOTE_BLOCK_HAT;
        case 4: case 5: return Sound.BLOCK_NOTE_BLOCK_SNARE;
        case 6: case 7: return Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
        default: return Sound.BLOCK_NOTE_BLOCK_CHIME;
        }
    }

    static Sound soundOf(int in, int octave) {
        switch ((in - 1) / 8) {
        case 0: // Piano
            return pianoSoundOf(octave);
        case 1: // Chromatic Percussion
            return percussionSoundOf(octave);
        case 2: // Organ
            return fluteSoundOf(octave);
        case 3: // Guitar
            return pianoSoundOf(octave);
        case 4: // Bass
            return pianoSoundOf(octave);
        case 5: // String
            return pianoSoundOf(octave);
        case 6: // Ensemble
            return pianoSoundOf(octave);
        case 7: // Brass
            return fluteSoundOf(octave);
        case 8: // Reed
            return fluteSoundOf(octave);
        case 9: // Pipe
            return fluteSoundOf(octave);
        case 10: // Synth Lead
            return fluteSoundOf(octave);
        case 11: // Synth Pad
            return fluteSoundOf(octave);
        case 12: // Synth Effects
            return fluteSoundOf(octave);
        case 13: // Ethnic
            return pianoSoundOf(octave);
        case 14: // Sound Effects
            return percussionSoundOf(octave);
        default: // Sound Effects
            return percussionSoundOf(octave);
        }
    }

    enum Note {
        FS1(1, "F#", 0.5f),
        G1 (1, "G",  0.529732f),
        GS1(1, "G#", 0.561231f),
        A1 (1, "A",  0.594604f),
        AS1(1, "A#", 0.629961f),
        B1 (1, "B",  0.667420f),
        C1 (1, "C",  0.707107f),
        CS1(1, "C#", 0.749154f),
        D1 (1, "D",  0.793701f),
        DS1(1, "D#", 0.840896f),
        E1 (1, "E",  0.890899f),
        F1 (1, "F",  0.943874f),
        GM1(1, "F#", 1.0f),
        // Octave2
        FS2(2, "F#", 1.0f),
        G2 (2, "G",  1.059463f),
        GS2(2, "G#", 1.122462f),
        A2 (2, "A",  1.189207f),
        AS2(2, "A#", 1.259921f),
        B2 (2, "B",  1.334840f),
        C2 (2, "C",  1.414214f),
        CS2(2, "C#", 1.498307f),
        D2 (2, "D",  1.587401f),
        DS2(2, "D#", 1.681793f),
        E2 (2, "E",  1.781797f),
        F2 (2, "F",  1.887749f),
        GM2(2, "F#", 2.0f);

        public final int octave;
        public final String name;
        public final float pitch;

        Note(int octave, String name, float pitch) {
            this.octave = octave;
            this.name = name;
            this.pitch = pitch;
        }
    }

    static Midi combine(Collection<Midi> midis) {
        Midi result = new Midi();
        for (Midi midi: midis) result.blips.addAll(midi.blips);
        Collections.sort(result.blips, (a, b) -> Long.compare(a.tick, b.tick));
        return result;
    }

    static Collection<Midi> parse(InputStream in) throws Exception {
        List<Midi> result = new ArrayList<>();
        Sequence sequence = MidiSystem.getSequence(in);
        int trackNumber = 0;
        int currentSound = 0;
        for (Track track: sequence.getTracks()) {
            trackNumber++;
            Midi midi = new Midi();
            result.add(midi);
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    if (sm.getCommand() == ShortMessage.NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int noteIndex = key % 12;
                        String noteName = NOTE_NAMES[noteIndex];
                        Note note = null;
                        for (Note n: Note.values()) {
                            if (n.name.equals(noteName) && n.octave == (octave % 2) + 1) {
                                note = n;
                                break;
                            }
                        }
                        if (note == null) continue;
                        int velocity = sm.getData2();
                        Sound sound = soundOf(currentSound, octave);
                        if (note != null) midi.blips.add(new Blip(event.getTick(), sound, note.pitch));
                    } else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                    } else if ((sm.getCommand() >> 4) == 12) { // PROGRAM_CHANGE
                        int channel = sm.getCommand() & 0xF;
                        int instrument = sm.getData1();
                        int some = sm.getData2();
                        currentSound = instrument;
                    }
                }
            }
        }
        return result;
    }

    static Midi load(File file) {
        try {
            return combine(parse(new FileInputStream(file)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static String commandNameOf(int in) {
        switch (in) {
        case ShortMessage.CONTROL_CHANGE: return "CONTROL_CHANGE";
        case ShortMessage.PROGRAM_CHANGE: return "PROGRAM_CHANGE";
        case ShortMessage.CHANNEL_PRESSURE: return "CHANNEL_PRESSURE";
        default: return "Unknown";
        }
    }
}
