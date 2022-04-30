package com.example.imu;

public class TiltBuffer {
    private float accTilt;
    private float gyrTilt;
    private float complementaryTilt;
    public double complementaryPitch;
    public double complementaryRoll;
    private boolean accUpdated;
    private boolean gyrUpdated;

    private boolean shouldUpdate() {
        if (accUpdated && gyrUpdated) {
            accUpdated = false;
            gyrUpdated = false;
            return true;
        }
        return false;
    }

    public boolean updateAccTilt(float tilt) {
        accTilt = tilt;
        accUpdated = true;
        return shouldUpdate();
    }

    public boolean updateGyrTilt(float tilt, float compTilt) {
        gyrTilt = tilt;
        gyrUpdated = true;
        complementaryTilt = compTilt;
        return shouldUpdate();
    }

    public float[] getGraphData() {
        Constants.tiltAcc.add(accTilt);
        Constants.tiltGyr.add(gyrTilt);
        Constants.tiltComp.add(complementaryTilt);
        return new float[] {
                accTilt, gyrTilt, complementaryTilt
        };
    }

    public void clear() {
        accTilt = 0;
        gyrTilt = 0;
        complementaryTilt = 0;
        complementaryPitch = 0;
        complementaryRoll = 0;
        accUpdated = false;
        gyrUpdated = false;
    }
}
