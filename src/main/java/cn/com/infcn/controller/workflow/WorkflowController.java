package cn.com.infcn.controller.workflow;

import cn.com.infcn.workflow.Workflow;
import cn.com.infcn.workflow.activiti.ActivityUtils;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    private static final String TEST_USER_ID = "1";

    @Autowired
    private Workflow workflow;

    /**
     * 获取流程实例ID
     *
     * @param businessKey
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/processInstanceId", method = RequestMethod.GET)
    public Object getProcessInstanceId(String businessKey) throws Exception {
        ProcessInstance pi = workflow.findProcessInstanceByBusinessKey(businessKey);
        ModelMap map = new ModelMap();
        map.put("processInstanceId", pi != null ? pi.getProcessInstanceId() : "");
        return map;
    }

    /**
     * 获取流程信息
     *
     * @return
     * @throws Exception
     * @throws IOException
     */
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public Object workflow(
            @RequestParam(required = false) String definitionKey,
            @RequestParam(required = false) String businessKey) throws Exception {

        ModelMap response = new ModelMap();

        //
        response.put("success", true);

        //
        ProcessInstance pi = workflow.findProcessInstanceByBusinessKey(businessKey);

        response.put("processInstanceId", pi != null ? pi.getProcessInstanceId() : "");

        // 没有当前流程实例
        if (pi == null) {
            if (StringUtils.isEmpty(definitionKey)) {
                throw new Exception("DefinitionKey can't be null");
            }
            RepositoryServiceImpl repositoryService = (RepositoryServiceImpl) workflow.getRepositoryService();
            ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService
                    .createProcessDefinitionQuery()
                    .latestVersion()
                    .processDefinitionKey(definitionKey)
                    .singleResult();
            ReadOnlyProcessDefinition pd = repositoryService.getDeployedProcessDefinition(processDefinition.getId());
            PvmActivity firstUserTaskActivity = pd.getActivities().get(0);
            PvmActivity secondUserTaskActivity = ActivityUtils.getNextUserTaskActivity(firstUserTaskActivity);

            if (logger.isDebugEnabled()) {
                logger.debug("Current user task: {}", firstUserTaskActivity.getId());
                logger.debug("Next user task: {}", secondUserTaskActivity.getId());
            }

            // 取得所有的用户任务活动
            List<ModelMap> activityModels = new ArrayList<ModelMap>();
            ModelMap map = new ModelMap();
            map.put("name", secondUserTaskActivity.getProperty("name"));
            map.put("type", secondUserTaskActivity.getProperty("type"));
            map.put("id", secondUserTaskActivity.getId());
            activityModels.add(map);

            //
            response.put("task", firstUserTaskActivity.getId());
            response.put("taskName", firstUserTaskActivity.getProperty("name"));
            response.put("nextTask", secondUserTaskActivity.getId());
            response.put("nextTaskName", secondUserTaskActivity.getProperty("name"));
            response.put("activities", activityModels);
        } else {
            String currUserName = TEST_USER_ID;
            // 得到当前任务
            Task task = workflow.findTask(pi, currUserName);

            // 处理当前登录人不拥有流程任务的情况
            if (task == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Can't find task for specified user. BusinessKey={}, UserId={}", pi.getBusinessKey(), currUserName);
                }
                // 不显示流程面板
                response.put("success", false);
                return response;
            }

            // 取得所有的用户任务活动
            List<? extends PvmActivity> activities = workflow.getUserTaskActivities(pi);
            List<ModelMap> activityModels = new ArrayList<>();
            if (activities != null && !activities.isEmpty()) {
                for (PvmActivity item : activities) {
                    ModelMap map = new ModelMap();
                    map.put("name", item.getProperty("name"));
                    map.put("type", item.getProperty("type"));
                    map.put("id", item.getId());
                    activityModels.add(map);
                }
            }

            // 取得下一个用户任务
            PvmActivity next = workflow.getNextUserTaskActivity(pi, task);
            String nextTaskName = (String) (next != null ? next.getProperty("name") : "");

            // 取得下一个活动
            if (next == null) {
                next = workflow.getNextActivity(pi, currUserName);
            }

            // 当下面就是EndEvent时也需要在前台的任务列表中显示
            if (ActivityUtils.isEndEvent(next)) {
                ModelMap map = new ModelMap();
                map.put("name", next.getProperty("name"));
                map.put("type", next.getProperty("type"));
                map.put("id", next.getId());
                activityModels.add(map);
            }

            // export vars
            Map<String, Object> vars = workflow.getRuntimeService().getVariables(task.getExecutionId());

            // 返回
            response.put("task", task.getTaskDefinitionKey());
            response.put("taskName", task.getName());
            response.put("nextTask", next.getId());
            response.put("nextTaskName", nextTaskName);
            response.put("activities", activityModels);
            response.put("vars", vars);
        }
        return response;
    }

    /**
     * 处理流程
     *
     * @param comment       办理意见
     * @param businessKey   businessKey
     * @param action        action
     * @param nextOperators 下一步办理人ID
     * @throws Exception
     */
    @RequestMapping(value = "/process", method = RequestMethod.POST)
    @Transactional
    public Object process(
            @RequestParam(required = false) String comment,
            String businessKey,
            String action,
            String nextOperators) throws Exception {

        // 通过业务KEY找到流程实例
        ProcessInstance processInstance = workflow.findProcessInstanceByBusinessKey(businessKey);
        if (processInstance == null) {
            throw new Exception(String.format("Invalid ProcessInstance ID: %s", businessKey));
        }

        //
        Task task = workflow.findTask(processInstance.getProcessInstanceId(), nextOperators);

        // 设置ACTION
        workflow.setAction(task.getExecutionId(), action);

        // 设置Comment
        if (StringUtils.isNotEmpty(comment)) {
            workflow.getTaskService().addComment(task.getId(), processInstance.getId(), comment);
        }

        // 完成任务
        workflow.complete(task);

        //
        ModelMap response = new ModelMap().addAttribute("success", true);
        List<Task> tasks = workflow.findTask(processInstance);
        if (tasks.size() == 0) {
            return response;
        }

        Task curTask = tasks.get(0);

        // Clear Action
        workflow.setAction(curTask.getExecutionId(), " ");

        if (logger.isDebugEnabled()) {
            logger.debug("{} {}", task.getId(), action);
        }

        //
        workflow.claim(curTask, nextOperators);

        return response;
    }
}
