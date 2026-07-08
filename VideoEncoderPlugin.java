package com.godot.game.encoder;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;

public class VideoEncoderPlugin extends GodotPlugin {

    public VideoEncoderPlugin(Godot godot) {
        super(godot);
    }

    @Override
    public String getPluginName() {
        return "VideoEncoderPlugin";
    }

    public void encodeVideo(String[] paths, String output, int w, int h, int fps) {
        VideoEncoder.encodeJPEGsToMP4(paths, output, w, h, fps);
    }
}