package com.cloud.network.nicira;

public class SwitchingProfileTypeIdEntry {

    public static final String QOS = "QosSwitchingProfile";
    public static final String PORT_MIRRORING = "PortMirroringSwitchingProfile";
    public static final String IP_DISCOVERY = "IpDiscoverySwitchingProfile";
    public static final String SPOOF_GUARD = "SpoofGuardSwitchingProfile";
    public static final String SWITCH_SECURITY = "SwitchSecuritySwitchingProfile";

    private String key;
    private String value;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

}
