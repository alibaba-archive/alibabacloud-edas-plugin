package io.jenkins.plugins.alicloud.edas;

import java.util.Objects;

public class ChangeOrderTraceState {
    private int pipelineCounter = 0;
    private int stageCounter = 0;
    private int instanceCounter = 0;
    private int instanceStageCounter = 0;
    private boolean hadPrintPipelineInfo = false;
    private boolean hadPrintStageInfo = false;
    private boolean hadPrintServiceStageInfo = false;
    private boolean hadPrintInstanceInfo = false;
    private boolean hadPrintInstanceStageInfo = false;

    private TimeoutManager timeoutManager = new TimeoutManager();

    public static class TimeoutManager {
        private long serviceStageStartTime = -1;
        private long instanceStageStartTime = -1;

        public long getServiceStageStartTime() {
            return serviceStageStartTime;
        }

        public void setServiceStageStartTime(long serviceStageStartTime) {
            this.serviceStageStartTime = serviceStageStartTime;
        }

        public long getInstanceStageStartTime() {
            return instanceStageStartTime;
        }

        public void setInstanceStageStartTime(long instanceStageStartTime) {
            this.instanceStageStartTime = instanceStageStartTime;
        }
    }

    public TimeoutManager getTimeoutManager() {
        return timeoutManager;
    }

    public void setTimeoutManager(TimeoutManager timeoutManager) {
        this.timeoutManager = timeoutManager;
    }

    public int getPipelineCounter() {
        return pipelineCounter;
    }

    public void setPipelineCounter(int pipelineCounter) {
        this.pipelineCounter = pipelineCounter;
    }

    public int getStageCounter() {
        return stageCounter;
    }

    public void setStageCounter(int stageCounter) {
        this.stageCounter = stageCounter;
    }

    public int getInstanceCounter() {
        return instanceCounter;
    }

    public void setInstanceCounter(int instanceCounter) {
        this.instanceCounter = instanceCounter;
    }

    public int getInstanceStageCounter() {
        return instanceStageCounter;
    }

    public void setInstanceStageCounter(int instanceStageCounter) {
        this.instanceStageCounter = instanceStageCounter;
    }

    public boolean isHadPrintPipelineInfo() {
        return hadPrintPipelineInfo;
    }

    public void setHadPrintPipelineInfo(boolean hadPrintPipelineInfo) {
        this.hadPrintPipelineInfo = hadPrintPipelineInfo;
    }

    public boolean isHadPrintStageInfo() {
        return hadPrintStageInfo;
    }

    public void setHadPrintStageInfo(boolean hadPrintStageInfo) {
        this.hadPrintStageInfo = hadPrintStageInfo;
    }

    public boolean isHadPrintServiceStageInfo() {
        return hadPrintServiceStageInfo;
    }

    public void setHadPrintServiceStageInfo(boolean hadPrintServiceStageInfo) {
        this.hadPrintServiceStageInfo = hadPrintServiceStageInfo;
    }

    public boolean isHadPrintInstanceInfo() {
        return hadPrintInstanceInfo;
    }

    public void setHadPrintInstanceInfo(boolean hadPrintInstanceInfo) {
        this.hadPrintInstanceInfo = hadPrintInstanceInfo;
    }

    public boolean isHadPrintInstanceStageInfo() {
        return hadPrintInstanceStageInfo;
    }

    public void setHadPrintInstanceStageInfo(boolean hadPrintInstanceStageInfo) {
        this.hadPrintInstanceStageInfo = hadPrintInstanceStageInfo;
    }

    public void resetPipelineState() {
        hadPrintPipelineInfo = false;
        stageCounter = 0;
        resetStageState();
    }

    public void resetStageState() {
        hadPrintStageInfo = false;
        resetServiceStage();
        instanceCounter=0;
        resetInstanceState();
    }

    public void resetServiceStage() {
        hadPrintServiceStageInfo = false;
        timeoutManager.setServiceStageStartTime(-1);
    }

    public void resetInstanceState() {
        hadPrintInstanceInfo = false;
        instanceStageCounter = 0;
        resetInstanceStageState();
    }

    public void resetInstanceStageState() {
        hadPrintInstanceStageInfo = false;
        timeoutManager.setInstanceStageStartTime(-1);
    }

    public ChangeOrderTraceState clone() {
        ChangeOrderTraceState state = new ChangeOrderTraceState();
        state.setPipelineCounter(pipelineCounter);
        state.setStageCounter(stageCounter);
        state.setInstanceCounter(instanceCounter);
        state.setInstanceStageCounter(instanceStageCounter);
        state.setHadPrintPipelineInfo(hadPrintPipelineInfo);
        state.setHadPrintStageInfo(hadPrintStageInfo);
        state.setHadPrintServiceStageInfo(hadPrintServiceStageInfo);
        state.setHadPrintInstanceInfo(hadPrintInstanceInfo);
        state.setHadPrintInstanceStageInfo(hadPrintInstanceStageInfo);
        state.setTimeoutManager(timeoutManager);
        return state;
    }



    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChangeOrderTraceState state = (ChangeOrderTraceState) o;
        return pipelineCounter == state.pipelineCounter &&
                stageCounter == state.stageCounter &&
                instanceCounter == state.instanceCounter &&
                instanceStageCounter == state.instanceStageCounter &&
                hadPrintPipelineInfo == state.hadPrintPipelineInfo &&
                hadPrintStageInfo == state.hadPrintStageInfo &&
                hadPrintServiceStageInfo == state.hadPrintServiceStageInfo &&
                hadPrintInstanceInfo == state.hadPrintInstanceInfo &&
                hadPrintInstanceStageInfo == state.hadPrintInstanceStageInfo &&
                Objects.equals(timeoutManager, state.timeoutManager);
    }

    @Override public int hashCode() {

        return Objects.hash(pipelineCounter, stageCounter, instanceCounter, instanceStageCounter, hadPrintPipelineInfo,
                hadPrintStageInfo, hadPrintServiceStageInfo, hadPrintInstanceInfo, hadPrintInstanceStageInfo,
                timeoutManager);
    }
}
