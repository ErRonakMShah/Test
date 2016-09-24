package com.almalence.plugins.capture.video;

import android.util.Log;

/**
 * Created by root on 17/9/16.
 */
public final class Mp4Editor {


    public static synchronized native String appendFds(int[] inputFilesDescriptors, int newFileDescriptor);

    static {
        System.loadLibrary("almalence-mp4editor");
    }

}
