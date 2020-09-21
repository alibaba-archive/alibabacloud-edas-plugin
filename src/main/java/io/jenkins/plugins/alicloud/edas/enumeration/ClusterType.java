package io.jenkins.plugins.alicloud.edas.enumeration;

public enum ClusterType {
    K8S_CLUSTER_TYPE(5),
    ECS_CLUSTER_TYPE(2),
    ALL_KINDS(0);
    private int value;
    ClusterType(int type) {
        value = type;
    }

    public static ClusterType getType(int type) {
        ClusterType[] sources = ClusterType.values();
        for (ClusterType source : sources) {
            if (type == source.value()) {
                return source;
            }
        }
        return null;
    }

    public int value() {
        return value;
    }
}
