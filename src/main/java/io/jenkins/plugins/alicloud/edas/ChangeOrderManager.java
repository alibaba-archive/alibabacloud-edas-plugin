package io.jenkins.plugins.alicloud.edas;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.GetChangeOrderInfoRequest;
import com.aliyuncs.edas.model.v20170801.GetChangeOrderInfoResponse;
import com.aliyuncs.edas.model.v20170801.GetChangeOrderInfoResponse.ChangeOrderInfo;
import com.aliyuncs.edas.model.v20170801.GetChangeOrderInfoResponse.ChangeOrderInfo.PipelineInfo;
import com.aliyuncs.edas.model.v20170801.GetChangeOrderInfoResponse.ChangeOrderInfo.PipelineInfo.StageInfoDTO;
import com.aliyuncs.edas.model.v20170801.GetChangeOrderInfoResponse.ChangeOrderInfo.PipelineInfo.StageInfoDTO.StageResultDTO;
import com.aliyuncs.exceptions.ClientException;
import io.jenkins.plugins.alicloud.edas.enumeration.Status;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangeOrderManager {

    private static final Logger logger = Logger.getLogger(ChangeOrderManager.class.getName());

    private ChangeOrderTraceState curTraceState;

    public ChangeOrderManager() {
        this.curTraceState = new ChangeOrderTraceState();
    }

    public boolean trace(DefaultAcsClient defaultAcsClient, String changeOrderId) {
        logger.log(Level.INFO,"Begin to trace change order: " + changeOrderId);
        for (;;) {
            boolean finish = false;
            try {
                finish = trace0(defaultAcsClient, changeOrderId);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            if (finish) {
                break;
            }
            sleep(1000);
        }
        return true;
    }

    private boolean trace0(DefaultAcsClient defaultAcsClient, String changeOrderId) throws Exception {
        GetChangeOrderInfoRequest request = new GetChangeOrderInfoRequest();
        request.setChangeOrderId(changeOrderId);

        ChangeOrderTraceState beforeState = curTraceState.clone();
        try {
            GetChangeOrderInfoResponse response = defaultAcsClient.getAcsResponse(request);
            if (response.getCode() == 200) {
                boolean finish = handleSuccessResponse(response);
                if (finish) {
                    return true;
                }
            } else {
                String msg = String.format("Failed to get change order info, code:%d, message:%s",
                        response.getCode(), response.getMessage());
                logger.log(Level.SEVERE, msg);
            }
        } catch (ClientException ex) {
            String msg = "Failed to get change order info: " + ex.getMessage();
            logger.log(Level.SEVERE, msg);
        }

        if (beforeState.equals(curTraceState)) {
            logger.log(Level.INFO, "Waiting...");
        }

        return false;
    }

    private boolean handleSuccessResponse(GetChangeOrderInfoResponse response) throws Exception {
        ChangeOrderInfo orderInfo = response.getChangeOrderInfo();
        List<PipelineInfo> pipelineInfos = orderInfo.getPipelineInfoList();

        while (curTraceState.getPipelineCounter() < pipelineInfos.size()) {
            int beforePipelineCounter = curTraceState.getPipelineCounter();
            showPipeline(pipelineInfos.get(curTraceState.getPipelineCounter()));
            if (beforePipelineCounter == curTraceState.getPipelineCounter()) {
                break;
            }
        }

        if (isEndStatus(orderInfo.getStatus())) {
            Status status = Status.getByVal(orderInfo.getStatus());
            switch (status) {
            case SUCCESS:
                logger.log(Level.INFO,"Deploy application successfully!");
                return true;
            case EXCEPTION:
                throw new Exception("Deploy failed due to exception");
            case FAIL:
                throw new Exception("Deploy failed");
            case ABORT:
                throw new Exception("Deploy failed due to abort");
            default:
                throw new Exception("Deploy failed for unknown reason.");
            }
        }

        return false;
    }

    private void showPipeline(PipelineInfo pipelineInfo) throws Exception {
        if (!curTraceState.isHadPrintPipelineInfo()) {
            String pipelineId = pipelineInfo.getPipelineId();
            String pipelineName = pipelineInfo.getPipelineName();
            logger.log(Level.INFO,String.format("PipelineName:%s, PipelineId:%s", pipelineName, pipelineId));
            curTraceState.setHadPrintPipelineInfo(true);
        }

        List<StageInfoDTO> stageInfoDTOS = pipelineInfo.getStageList();
        if (curTraceState.getStageCounter() < stageInfoDTOS.size()) {
            while (curTraceState.getStageCounter() < stageInfoDTOS.size()) {
                int beforeStageCounter = curTraceState.getStageCounter();
                showStage(stageInfoDTOS.get(curTraceState.getStageCounter()));
                if (beforeStageCounter == curTraceState.getStageCounter()) {
                    break;
                }
            }
        }

        if (isSuccessStatus(pipelineInfo.getPipelineStatus())) {
            curTraceState.setPipelineCounter(curTraceState.getPipelineCounter() + 1);
            curTraceState.resetPipelineState();
        }
    }

    private void showStage(StageInfoDTO stageInfo) throws Exception {
        if (!curTraceState.isHadPrintStageInfo()) {
            String stageId = stageInfo.getStageId();
            String stageName = stageInfo.getStageName();
            logger.log(Level.INFO,String.format("StageName:%s, StageId:%s", stageName, stageId));
            curTraceState.setHadPrintStageInfo(true);
        }

        StageResultDTO stageResult = stageInfo.getStageResultDTO();
        showStageResult(stageResult);

        if (isSuccessStatus(stageInfo.getStatus())) {
            curTraceState.setStageCounter(curTraceState.getStageCounter() + 1);
            curTraceState.resetStageState();
        }
    }

    private void showStageResult(StageResultDTO stageResult) throws Exception {
        StageResultDTO.ServiceStage serviceStage = stageResult.getServiceStage();
        List<StageResultDTO.InstanceDTO> instances = stageResult.getInstanceDTOList();
        if (serviceStage != null && serviceStage.getStageId() != null) {
            if (!curTraceState.isHadPrintServiceStageInfo()) {
                String stageName = serviceStage.getStageName();
                String stageId = serviceStage.getStageId();
                logger.log(Level.INFO,String.format("ServiceStageName:%s, ServiceStageId:%s", stageName, stageId));
                curTraceState.setHadPrintServiceStageInfo(true);
                curTraceState.getTimeoutManager().setServiceStageStartTime(System.currentTimeMillis());
            }

            if (isSuccessStatus(serviceStage.getStatus())) {
                curTraceState.resetServiceStage();
            }

            checkBeginEndTimeout(
                    curTraceState.getTimeoutManager().getServiceStageStartTime(),
                    System.currentTimeMillis(),
                    300 * 1000,
                    "Time out for waiting serviceStage: " + serviceStage.getStageName());
        }

        if (instances != null && instances.size() > 0) {
            while (curTraceState.getInstanceCounter() < instances.size()) {
                int beforeInstanceCounter = curTraceState.getInstanceCounter();
                showInstance(instances.get(curTraceState.getInstanceCounter()));
                if (beforeInstanceCounter == curTraceState.getInstanceCounter()) {
                    break;
                }
            }
        }
    }

    private void checkBeginEndTimeout(long begin, long end, long timeout, String msg) throws Exception {
        if (begin != -1 && end != -1 && end - begin > timeout) {
            throw new Exception(msg);
        }
    }

    private void showInstance(StageResultDTO.InstanceDTO instance) throws Exception {
        if (!curTraceState.isHadPrintInstanceInfo()) {
            String instanceName = instance.getInstanceName();
            String instanceIp = instance.getInstanceIp();
            logger.log(Level.INFO,String.format("InstanceName:%s, InstanceIp:%s", instanceName, instanceIp));
            curTraceState.setHadPrintInstanceInfo(true);
        }

        List<StageResultDTO.InstanceDTO.InstanceStageDTO> instanceStages = instance.getInstanceStageDTOList();
        if (curTraceState.getInstanceStageCounter() < instanceStages.size()) {
            while (curTraceState.getInstanceStageCounter() < instanceStages.size()) {
                int beforeInstanceStageCounter = curTraceState.getInstanceStageCounter();
                showInstanceStage(instanceStages.get(curTraceState.getInstanceStageCounter()));
                if (beforeInstanceStageCounter == curTraceState.getInstanceStageCounter()) {
                    break;
                }
            }
        }

        if (isSuccessStatus(instance.getStatus())) {
            curTraceState.setInstanceCounter(curTraceState.getInstanceCounter() + 1);
            curTraceState.resetInstanceState();
        }
    }

    private void showInstanceStage(StageResultDTO.InstanceDTO.InstanceStageDTO instanceStage)
            throws Exception {
        if (!curTraceState.isHadPrintInstanceStageInfo()) {
            String stageName = instanceStage.getStageName();
            String stageId = instanceStage.getStageId();
            logger.log(Level.INFO,String.format("InstanceStageName:%s, InstanceStageId:%s", stageName, stageId));
            curTraceState.setHadPrintInstanceStageInfo(true);
            curTraceState.getTimeoutManager().setInstanceStageStartTime(System.currentTimeMillis());
        }

        if (isSuccessStatus(instanceStage.getStatus())) {
            curTraceState.setInstanceStageCounter(curTraceState.getInstanceStageCounter() + 1);
            curTraceState.resetInstanceStageState();
        }

        checkBeginEndTimeout(
                curTraceState.getTimeoutManager().getInstanceStageStartTime(),
                System.currentTimeMillis(),
                300 * 1000,
                "Time out for waiting instanceStage: " + instanceStage.getStageName());
    }

    private boolean isEndStatus(Integer s) {
        Status status = Status.getByVal(s);
        switch (status) {
            case FAIL:
            case ABORT:
            case SUCCESS:
            case EXCEPTION:
                return true;
            default:
                return false;
        }
    }

    private boolean isSuccessStatus(Integer s) {
        return s != null && s == Status.SUCCESS.getVal();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
            //ignore
        }
    }
}
