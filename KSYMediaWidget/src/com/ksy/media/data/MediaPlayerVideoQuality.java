package com.ksy.media.data;

public enum MediaPlayerVideoQuality {

    UNKNOWN(MediaPlayerVideoQuality.VIDEO_QUALITY_UNKNOWN,"未知"),SD(MediaPlayerVideoQuality.VIDEO_QUALITY_SD,"标清"),HD(MediaPlayerVideoQuality.VIDEO_QUALITY_HD,"高清");
    
    public static final int VIDEO_QUALITY_UNKNOWN = -1;
    public static final int VIDEO_QUALITY_SD = 1;
    public static final int VIDEO_QUALITY_HD = 2;
    
    private MediaPlayerVideoQuality(int flag, String name) {
        this.flag = flag;
        this.name = name;
    }
    
    private int flag = VIDEO_QUALITY_UNKNOWN;
    private String name;
    public int getFlag() {
        return flag;
    }
    public String getName() {
        return name;
    }
    
    public static MediaPlayerVideoQuality getQualityNameByFlag(int flag){
        switch (flag) {
		case VIDEO_QUALITY_UNKNOWN:
			return UNKNOWN;
		case VIDEO_QUALITY_SD:
			return SD;
		case VIDEO_QUALITY_HD:
			return HD;
		}
        return null;
        
    }
    
}
