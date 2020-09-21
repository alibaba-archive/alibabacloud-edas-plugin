package io.jenkins.plugins.alicloud.edas.enumeration;

public enum ReleaseType {
    AUTO("自动"),
    MANUAL("手动");

    private final String name;

    ReleaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static ReleaseType fromName(String releaseType) {
        ReleaseType[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ReleaseType region = var1[var3];
            if (region.getName().equals(releaseType)) {
                return region;
            }
        }

        throw new IllegalArgumentException("Cannot create enum from " + releaseType + " value!");
    }
}
