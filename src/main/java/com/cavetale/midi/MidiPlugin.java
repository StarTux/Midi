package com.cavetale.midi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MidiPlugin extends JavaPlugin implements Runnable, Listener {
    List<MidiPlayer> midiPlayers;

    @Override
    public void onEnable() {
        loadPlayers();
        getServer().getScheduler().runTaskTimer(this, this, 1L, 1L);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        midiPlayers.clear();
    }

    void loadPlayers() {
        File dir = new File(getDataFolder(), "players");
        dir.mkdirs();
        Gson gson = new Gson();
        this.midiPlayers = new ArrayList<>();
        for (File file: dir.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;
            try (FileReader reader = new FileReader(file)) {
                final MidiPlayer mplayer = gson.fromJson(reader, MidiPlayer.class);
                if (mplayer.filename == null) {
                    getLogger().warning("Missing filename in " + file);
                }
                if (mplayer.world == null) {
                    getLogger().warning("Missing world name in " + file);
                }
                mplayer.name = file.getName();
                mplayer.name = mplayer.name.substring(0, mplayer.name.length() - 5);
                getLogger().info("Starting player: " + mplayer.name);
                mplayer.paused = true;
                this.midiPlayers.add(mplayer);
                final File mfile = new File(getDataFolder(), mplayer.filename + ".mid");
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                        Midi midi = Midi.load(mfile);
                        if (midi == null) {
                            mplayer.stopped = true;
                        } else {
                            mplayer.midi = midi;
                            mplayer.setup();
                            mplayer.paused = false;
                        }
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        getLogger().info("" + this.midiPlayers.size() + " midi players loaded.");
    }

    @Override
    public void run() {
        for (Iterator<MidiPlayer> iter = midiPlayers.iterator(); iter.hasNext();) {
            MidiPlayer mplayer = iter.next();
            if (mplayer.stopped) {
                iter.remove();
            } else if (!mplayer.paused) {
                mplayer.tick();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "play": {
            String fn = "test";
            if (args.length >= 2) fn = args[1];
            long speed = 20L;
            if (args.length >= 3) speed = Long.parseLong(args[2]);
            float volume = 1.0f;
            if (args.length >= 4) volume = Float.parseFloat(args[3]);
            MidiPlayer mplayer = new MidiPlayer();
            Player player = (Player)sender;
            mplayer.setLocation(player.getLocation());
            mplayer.volume = volume;
            mplayer.speed = speed;
            mplayer.name = fn;
            mplayer.paused = true;
            this.midiPlayers.add(mplayer);
            final File mfile = new File(getDataFolder(), fn + ".mid");
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    Midi midi = Midi.load(mfile);
                    if (midi == null) {
                        mplayer.stopped = true;
                    } else {
                        mplayer.midi = midi;
                        mplayer.paused = false;
                        mplayer.setup();
                    }
                });
            return true;
        }
        case "stop": {
            if (args.length != 2) return false;
            String fn = args[1];
            int count = 0;
            for (MidiPlayer mplayer: this.midiPlayers) {
                if (!mplayer.stopped && fn.equals(mplayer.name)) {
                    mplayer.stopped = true;
                    count += 1;
                }
            }
            sender.sendMessage("Stoppped " + count + " midi players.");
            return true;
        }
        case "reload": {
            loadPlayers();
            if (sender instanceof Player) sender.sendMessage("Midi players reloaded. See console.");
            return true;
        }
        case "list": {
            sender.sendMessage(this.midiPlayers.size() + " MIDI players");
            for (MidiPlayer mplayer: this.midiPlayers) {
                sender.sendMessage("" + mplayer.name + ") " + mplayer.world + ":" + (int)Math.floor(mplayer.x) + "," + (int)Math.floor(mplayer.y) + "," + (int)Math.floor(mplayer.z) + (mplayer.paused ? " PAUSED" : "") + " speed=" + mplayer.speed + " volume=" + String.format("%.02f", mplayer.volume));
            }
            return true;
        }
        case "create": {
            MidiPlayer mplayer = new MidiPlayer();
            Player player = (Player)sender;
            mplayer.setLocation(player.getLocation());
            String name = args[1];
            mplayer.filename = name;
            File dir = new File(getDataFolder(), "players");
            dir.mkdirs();
            File file = new File(dir, name + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(mplayer, writer);
                sender.sendMessage("MIDI Player file created: " + name);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                sender.sendMessage("Error creating MIDI Player file. See cosole.");
            }
        }
        default: return false;
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        for (MidiPlayer mplayer: midiPlayers) {
            if (mplayer.paused && world.equals(mplayer.world) && x == mplayer.cx && z == mplayer.cz) {
                mplayer.paused = false;
            }
        }
    }
}
