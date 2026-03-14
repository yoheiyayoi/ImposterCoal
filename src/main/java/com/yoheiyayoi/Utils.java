package com.yoheiyayoi;

public class Utils {
    public static int convertSecondToTick(int second) {
        return second * 20;
    }

    public static int convertMinuteToTick(int minute) {
        return convertSecondToTick(minute * 60);
    }
}
