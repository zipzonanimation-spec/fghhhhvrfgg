package com.godot.game.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;

public class VideoEncoder {
    public static boolean encodeJPEGsToMP4(String[] jpegPaths, String outputPath, int width, int height, int fps) {
        try {
            MediaMuxer muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 4000000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            
            MediaCodec codec = MediaCodec.createEncoderByType("video/avc");
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
            
            int trackIndex = -1;
            boolean muxerStarted = false;
            
            for (String path : jpegPaths) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                int[] colors = new int[width * height];
                bmp.getPixels(colors, 0, width, 0, 0, width, height);
                byte[] yuv420 = new byte[width * height * 3 / 2];
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        int idx = i * width + j;
                        int r = (colors[idx] >> 16) & 0xFF;
                        int g = (colors[idx] >> 8) & 0xFF;
                        int b = colors[idx] & 0xFF;
                        int y = (66 * r + 129 * g + 25 * b + 128) >> 8 + 16;
                        int u = (-38 * r - 74 * g + 112 * b + 128) >> 8 + 128;
                        int v = (112 * r - 94 * g - 18 * b + 128) >> 8 + 128;
                        yuv420[idx] = (byte) Math.min(255, Math.max(0, y));
                        int uvIdx = width * height + (i / 2) * width + (j / 2) * 2;
                        yuv420[uvIdx] = (byte) Math.min(255, Math.max(0, u));
                        yuv420[uvIdx + 1] = (byte) Math.min(255, Math.max(0, v));
                    }
                }
                
                int inputBufferIndex = codec.dequeueInputBuffer(10000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(yuv420);
                    codec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, fps, 0);
                }
                
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        codec.releaseOutputBuffer(outputBufferIndex, false);
                    } else {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.getOutputFormat());
                            muxer.start();
                            muxerStarted = true;
                        }
                        outputBuffer.position(info.offset);
                        outputBuffer.limit(info.offset + info.size);
                        muxer.writeSampleData(trackIndex, outputBuffer, info);
                        codec.releaseOutputBuffer(outputBufferIndex, false);
                    }
                    outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                }
                bmp.recycle();
            }
            
            codec.stop();
            codec.release();
            muxer.stop();
            muxer.release();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}