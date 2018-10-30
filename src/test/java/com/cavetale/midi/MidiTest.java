package com.cavetale.midi;

import java.io.File;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import org.junit.Test;

public final class MidiTest {
    public void playback() throws Exception {
        Sequence sequence = MidiSystem.getSequence(new File("test.mid"));
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.setSequence(sequence);
        sequencer.open();
        sequencer.setTempoFactor(sequencer.getTempoFactor() * 1.2f);
        sequencer.start();
        while (sequencer.isOpen()) Thread.sleep(1000);
        sequencer.close();
    }

    @Test
    public void test() throws Exception {
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        Instrument[] instruments = synthesizer.getAvailableInstruments();
        // for (int i = 0; i < instruments.length; i += 1) System.out.println("Instr #" + i + " = " + instruments[i]);

        final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        File file = new File("test.mid");
        if (!file.exists()) return;
        Sequence sequence = MidiSystem.getSequence(file);
        int trackNumber = 0;
        for (Track track: sequence.getTracks()) {
            trackNumber++;
            System.out.println("Track " + trackNumber + ": size = " + track.size());
            System.out.println();
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                System.out.print("@" + event.getTick() + " ");
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    System.out.print("Channel: " + sm.getChannel() + " ");
                    if (sm.getCommand() == ShortMessage.NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else if (sm.getCommand() == ShortMessage.NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else if (sm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                        System.out.print("Control change (176) ");
                        switch (sm.getData1()) {
                        case 0: System.out.println("Bank select " + sm.getData2()); break;
                        case 7: System.out.println("Channel volume " + sm.getData2()); break;
                        case 10: System.out.println("Pan " + sm.getData2()); break;
                        case 11: System.out.println("Expression Controller " + sm.getData2()); break;
                        case 32: System.out.println("LSB for Bank select " + sm.getData2()); break;
                        case 91: System.out.println("Effects 1 Depth " + sm.getData2()); break;
                        case 121: System.out.println("Reset all Controllers " + sm.getData2()); break;
                        default: System.out.println("Unknown (" + sm.getData1() + ") " + sm.getData2()); break;
                        }
                    } else if ((sm.getCommand() >> 4) == 12) { // PROGRAM_CHANGE
                        int channel = sm.getCommand() & 0xF;
                        int instrument = sm.getData1();
                        int some = sm.getData2();
                        System.out.println("Program change: channel=" + channel + " instrument[" + instrument + "]=" + instruments[instrument].getName() + " some=" + some);
                    } else {
                        System.out.println("Command: " + commandNameOf(sm.getCommand()) + "(" + sm.getCommand()  + ") " + sm.getData1() + "," + sm.getData2());
                    }
                } else {
                    System.out.println("Other message: " + message.getClass());
                }
            }

                System.out.println();
            }
    }

    String commandNameOf(int in) {
        switch (in) {
        case ShortMessage.CONTROL_CHANGE: return "CONTROL_CHANGE";
        case ShortMessage.PROGRAM_CHANGE: return "PROGRAM_CHANGE";
        case ShortMessage.CHANNEL_PRESSURE: return "CHANNEL_PRESSURE";
        default: return "Unknown";
        }
    }
}
