# Midi

Midi player plugin.  Load MIDI files, translate them into Minecraft sounds, and play them at an arbitray location.

## Commands
- `/midi play <name> <speed> <volume>` Play a filename at the current location. *(Player required)*
- `/midi stop <name>` Stop a midi playback.
- `/midi list` List current midi playbacks.
- `/midi reload` Reload configurations.
- `/midi create <name>` Create a midi player file. *(Player required)*

## File structure
MIDI files are stored in the plugin folder, `plugins/Midi`.  The `players` subfolder contains a list of MIDI players, one JSON file per "player".  A new file can be created with the `create` command and then edited via text editor, then loaded via the `reload command.  Their strcuture looks as follows.  The settings should be self-explanatory.

```json
{
  "world": "world",
  "x": 128.0,
  "y": 65.0,
  "z": 128.0,
  "speed": 20,
  "volume": 1.0,
  "filename": "Nachtmusik",
  "loop": false
}
```

