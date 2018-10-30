package com.cavetale.midi;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;

@Getter @Setter
final class MidiPlayer {
    String world;
    double x, y, z;
    long speed = 20L;
    float volume = 1.0f;
    String filename;
    boolean loop;
    transient String name;
    transient Midi midi;
    transient boolean paused;
    transient boolean stopped;
    transient int index;
    transient long tick;
    transient int cx, cz;

    void setup() {
        this.index = 0;
        if (this.midi.getBlips().isEmpty()) return;
        this.tick = this.midi.getBlips().get(0).tick;
        this.cx = (int)Math.floor(this.x) >> 4;
        this.cz = (int)Math.floor(this.z) >> 4;
    }

    void stop() {
        this.stopped = true;
    }

    void setLocation(Location location) {
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.cx = (int)Math.floor(this.x) >> 4;
        this.cz = (int)Math.floor(this.z) >> 4;
    }

    Location getLocation() {
        World bworld = Bukkit.getWorld(this.world);
        if (bworld == null || !bworld.isChunkLoaded(cx, cz)) {
            this.paused = true;
            return null;
        }
        return new Location(bworld, this.x, this.y, this.z);
    }

    public void tick() {
        if (stopped) return;
        if (paused) return;
        if (this.midi == null) {
            paused = true;
            return;
        }
        List<Midi.Blip> blips = this.midi.getBlips();
        if (blips.size() <= this.index) {
            if (this.loop) {
                setup();
            } else {
                stopped = true;
                return;
            }
        }
        Location location = null;
        while (blips.size() > this.index && blips.get(index).tick <= this.tick) {
            Midi.Blip blip = blips.get(index);
            if (location == null) location = getLocation();
            if (location == null) return;
            location.getWorld().playSound(location, blip.sound, SoundCategory.MASTER, this.volume, blip.pitch);
            this.index += 1;
        }
        this.tick += speed;
    }
}
