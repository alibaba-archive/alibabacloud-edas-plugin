package io.jenkins.plugins.alicloud.edas.enumeration;

public enum Status {
    UNKNOW(-100),
    PREPARE(0),
    EXECUTING(1),
    SUCCESS(2),
    FAIL(3),
    ABORT(6),
    EXCEPTION(10);

    private int val;
    Status(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static Status getByVal(Integer val) {
        if (val == null) {
            return UNKNOW;
        }

        for (Status status: Status.values()) {
            if (status.getVal() == val) {
                return status;
            }
        }

        return UNKNOW;
    }
}
