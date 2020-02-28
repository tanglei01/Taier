package com.dtstack.engine.master.akka;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.dtstack.engine.common.JobClient;
import com.dtstack.engine.common.JobIdentifier;
import com.dtstack.engine.common.enums.RdosTaskStatus;
import com.dtstack.engine.common.exception.ExceptionUtil;
import com.dtstack.engine.common.message.*;
import com.dtstack.engine.common.pojo.JobResult;
import com.dtstack.engine.common.restart.RestartStrategyType;
import com.dtstack.engine.common.util.RandomUtils;
import com.dtstack.engine.common.message.WorkerInfo;
import com.dtstack.engine.master.env.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class WorkerOperator {

    private static final Logger logger = LoggerFactory.getLogger(WorkerOperator.class);

    @Autowired
    private EnvironmentContext env;

    @Autowired
    private ActorManager actorManager;

    private Object sendRequest(Object message) throws Exception {
        String path = RandomUtils.getRandomValueFromMap(actorManager.getWorkerInfoMap()).getPath();
        ActorSelection actorRef = actorManager.getSystem().actorSelection(path);
        Future<Object> future = Patterns.ask(actorRef, message, env.getAskResultTimeout());
        Object result = Await.result(future, Duration.create(env.getAskResultTimeout(), TimeUnit.SECONDS));
        return result;
    }

    public boolean judgeSlots(JobClient jobClient) {
        Object result = null;
        try {
            result = sendRequest(new MessageJudgeSlots(jobClient));
        } catch (Exception e) {
            logger.error("jobid:{} judgeSlots failed!", jobClient.getTaskId(), e);
            return false;
        }
        return (boolean) result;
    }

    public JobResult submitJob(JobClient jobClient) throws Exception {
        return (JobResult) sendRequest(new MessageSubmitJob(jobClient));
    }

    public RdosTaskStatus getJobStatus(String engineType, String pluginInfo, JobIdentifier jobIdentifier) throws Exception {
        return (RdosTaskStatus) sendRequest(new MessageGetJobStatus(engineType, pluginInfo, jobIdentifier));
    }

    public String getEngineMessageByHttp(String engineType, String path, String pluginInfo)  {
        String message;
        try {
            message = (String) sendRequest(new MessageGetEngineMessageByHttp(engineType, path, pluginInfo));
        } catch (Exception e) {
            message = ExceptionUtil.getErrorMessage(e);
        }
        return message;
    }

    public String getEngineLog(String engineType, String pluginInfo, JobIdentifier jobIdentifier) {
        String logInfo;
        try{
            logInfo = (String) sendRequest(new MessageGetEngineLog(engineType, pluginInfo, jobIdentifier));
        }catch (Exception e){
            logInfo = ExceptionUtil.getErrorMessage(e);
        }
        return logInfo;
    }

    public String getCheckpoints(String engineType, String pluginInfo, JobIdentifier jobIdentifier) {
        String checkpoints = null;
        try{
            checkpoints = (String) sendRequest(new MessageGetCheckpoints(engineType, pluginInfo, jobIdentifier));
        }catch (Exception e){
            logger.error("getCheckpoints failed!", e);
        }
        return checkpoints;
    }

    public String getJobMaster(String engineType, String pluginInfo, JobIdentifier jobIdentifier) throws Exception {
        return (String) sendRequest(new MessageGetJobMaster(engineType, pluginInfo, jobIdentifier));
    }

    public JobResult stopJob(JobClient jobClient) throws Exception {
        return (JobResult) sendRequest(new MessageStopJob(jobClient));
    }

    public List<String> containerInfos(JobClient jobClient) throws Exception {
        return (List<String>) sendRequest(new MessageContainerInfos(jobClient));
    }

    public RestartStrategyType getRestartStrategyType(String engineType, String pluginInfo, JobIdentifier jobIdentifier) {
        return RestartStrategyType.NONE;
    }

    public Map<String, WorkerInfo> getWorkerInfoMap() {
        return actorManager.getWorkerInfoMap();
    }
}
