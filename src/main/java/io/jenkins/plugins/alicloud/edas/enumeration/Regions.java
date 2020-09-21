package io.jenkins.plugins.alicloud.edas.enumeration;

public enum Regions {
    CN_QINGDAO("cn-qingdao"),
    CN_BEIJING("cn-beijing"),
    CN_ZHANGJIAKOU("cn-zhangjiakou"),
    CN_HANGZHOU("cn-hangzhou"),
    CN_SHANGHAI("cn-shanghai"),
    CN_SHENZHEN("cn-shenzhen"),
    CN_HONGKONG("cn-hongkong"),
    AP_SINGAPORT("ap-southeast-1");

    public static final Regions DEFAULT_REGION = CN_HANGZHOU;
    private final String name;

    Regions(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Regions fromName(String regionName) {
        Regions[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            Regions region = var1[var3];
            if (region.getName().equals(regionName)) {
                return region;
            }
        }

        throw new IllegalArgumentException("Cannot create enum from " + regionName + " value!");
    }
}
