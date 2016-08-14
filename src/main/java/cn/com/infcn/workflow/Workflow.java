package cn.com.infcn.workflow;

import cn.com.infcn.workflow.activiti.ActivityUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Component
public class Workflow {

    private static final Logger LOGGER = LoggerFactory.getLogger(Workflow.class);

    private static final String DefaultNextAssigneeKey = "DefaultNextAssignee";

    private static final String WORKFLOW_CONDITION_NAME = "result";

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    /**
     * 通过业务Key启动流程
     * 流程启动后会自动生成相应的Task实例
     *
     * @param processDefinitionKey
     * @param businessKey
     * @return
     */
    public ProcessInstance startByBusinessKey(String processDefinitionKey, String businessKey) {
        return runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey);
    }

    /**
     * 通过业务Key取得流程实例
     *
     * @param key
     * @return
     */
    public ProcessInstance findProcessInstanceByBusinessKey(String key) {
        return runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(key).singleResult();
    }

    public ProcessInstance findProcessInstance(String id) {
        return runtimeService.createProcessInstanceQuery().processInstanceId(id).singleResult();
    }

    /**
     * 获取所有Activity
     *
     * @param pi
     * @return
     */
    public List<? extends PvmActivity> getActivities(ProcessInstance pi) {
        RepositoryServiceImpl rs = (RepositoryServiceImpl) repositoryService;
        ReadOnlyProcessDefinition pd = rs.getDeployedProcessDefinition(pi.getProcessDefinitionId());
        return pd.getActivities();
    }

    /**
     * 获取所有的UserTask
     *
     * @param pi
     * @return
     */
    public List<? extends PvmActivity> getUserTaskActivities(ProcessInstance pi) {
        List<PvmActivity> list = new ArrayList<PvmActivity>();
        RepositoryServiceImpl rs = (RepositoryServiceImpl) repositoryService;
        ReadOnlyProcessDefinition pd = rs.getDeployedProcessDefinition(pi.getProcessDefinitionId());
        List<? extends PvmActivity> activities = pd.getActivities();
        for (PvmActivity activity : activities) {
            if (ActivityUtils.isUserTask(activity)) {
                list.add(activity);
            }
        }

        return list;
    }

    /**
     * 查找指定用户的当前任务
     */
    public Task findTask(String taskId) {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }

    public List<Task> findTask(ProcessInstance processInstance) {
        return taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    }

    public Task findTask(ProcessInstance processInstance, String assignee) {
        return findTask(processInstance.getId(), assignee);
    }

    public Task findTask(String processInstanceId, String assignee) {
        return taskService.createTaskQuery().processInstanceId(processInstanceId).taskAssignee(assignee).singleResult();
    }

    public List<Task> findTaskByProcessInstance(String processInstanceId) {
        return taskService.createTaskQuery().processInstanceId(processInstanceId).list();
    }

    /**
     * 查找流程的所有流过的任务
     *
     * @param processInstance
     * @return
     */
    public List<HistoricTaskInstance> findHistoricTasks(ProcessInstance processInstance) {
        return findHistoricTasks(processInstance.getId());
    }

    /**
     * 查找流程的所有流过的任务
     *
     * @return
     */
    public List<HistoricTaskInstance> findHistoricTasks(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskId()
                .asc()
                .list();
    }

    /**
     * 根据传入的Execution获取全部历史UserTask
     *
     * @param executionId
     * @return
     */
    public List<HistoricActivityInstance> findHistoricTasksByExecution(String executionId) {
        return historyService.createHistoricActivityInstanceQuery()
                .executionId(executionId)
                .activityType("userTask")
                .orderByActivityId()
                .asc()
                .list();
    }

    public Task findUnassignedTask(String processInstance) {
        return taskService.createTaskQuery().processInstanceId(processInstance).taskUnassigned().singleResult();
    }

    /**
     * 获取下一个UserTaskActivity
     *
     * @param pi
     * @return
     * @throws Exception
     */
    public PvmActivity getNextUserTaskActivity(ProcessInstance pi, String assignee) throws Exception {
        return getNextUserTaskActivity(pi, findTask(pi, assignee));
    }

    /**
     * 取得下一个活动
     *
     * @param pi
     * @param task
     * @return
     * @throws Exception
     */
    public PvmActivity getActivity(ProcessInstance pi, Task task) throws Exception {
        List<? extends PvmActivity> activities = this.getActivities(pi);
        for (PvmActivity activity : activities) {
            if (task.getTaskDefinitionKey().equals(activity.getId())) {
                return activity;
            }
        }
        return null;
    }

    /**
     * 取得下一个活动
     *
     * @param pi
     * @return
     * @throws Exception
     */
    public PvmActivity getNextActivity(ProcessInstance pi, String assignee) throws Exception {
        return getNextActivity(pi, findTask(pi, assignee));
    }

    /**
     * 取得下一个活动
     *
     * @param pi
     * @param task
     * @return
     * @throws Exception
     */
    public PvmActivity getNextActivity(ProcessInstance pi, Task task) throws Exception {
        List<? extends PvmActivity> activities = this.getActivities(pi);
        for (PvmActivity activity : activities) {
            if (task.getTaskDefinitionKey().equals(activity.getId())) {
                return ActivityUtils.getNextActivity(activity);
            }
        }
        return null;
    }

    /**
     * 获得下一个用户任务
     *
     * @param pi
     * @param task
     * @return
     * @throws Exception
     */
    public PvmActivity getNextUserTaskActivity(ProcessInstance pi, Task task) throws Exception {
        List<? extends PvmActivity> activities = this.getUserTaskActivities(pi);
        for (PvmActivity activity : activities) {
            if (task.getTaskDefinitionKey().equals(activity.getId())) {
                return ActivityUtils.getNextUserTaskActivity(activity);
            }
        }
        return null;
    }

    public ProcessDefinition findProcessDefinition(ProcessInstance processInstance) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .singleResult();
    }

    public ProcessDefinition findProcessDefinition(String processDefinitionId) {
        return repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
    }

    /**
     * 认领任务
     *
     * @param processInstance
     * @param user
     * @return
     */
    public Task claimUnassignedTask(ProcessInstance processInstance, String user) {
        Task task = findUnassignedTask(processInstance.getProcessInstanceId());
        claim(task, user);
        task.setAssignee(user);
        return task;
    }

    public void claim(Task task, String user) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Claim {}[{}] to {}", task.getTaskDefinitionKey(), task.getId(), user);
        }
        // 领取任务
        taskService.claim(task.getId(), user);
    }

    private String firstTaskDefKey;

    /**
     * 完成任务
     *
     * @param task
     */
    public void complete(Task task) {
        // 完成任务
        taskService.complete(task.getId());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Complete {}[{}]", task.getTaskDefinitionKey(), task.getId());
        }
    }

    /**
     * 关闭流程
     *
     * @param processInstance
     */
    public void close(ProcessInstance processInstance) {
        close(processInstance, "");
    }

    public void close(ProcessInstance processInstance, String reason) {
        runtimeService.deleteProcessInstance(processInstance.getId(), reason);
    }

    public void setExecutionVariable(String executionId, String variableName, Object value) {
        runtimeService.setVariable(executionId, variableName, value);
    }

    public Object getExecutionVariable(String executionId, String variableName) {
        return runtimeService.getVariable(executionId, variableName);
    }

    public void setAction(String executionId, Object value) {
        setExecutionVariable(executionId, WORKFLOW_CONDITION_NAME, value);
    }

    /**
     * 设置当前任务默认的下一步办理人
     *
     * @param currentTaskId 当前任务ID
     * @param assignee
     */
    public void setDefaultNextAssignee(String currentTaskId, String assignee) {
        taskService.setVariableLocal(currentTaskId, DefaultNextAssigneeKey, assignee);
    }

    /**
     * 获取下一步默认办理人
     *
     * @param currentTaskId 当前任务ID
     * @return
     */
    public String getDefaultNextAssignee(String currentTaskId) {
        Object obj = taskService.getVariableLocal(currentTaskId, DefaultNextAssigneeKey);
        return obj != null ? (String) obj : "";
    }

    /**
     * 根据业务KEY查找当前任务
     *
     * @param businessKey
     * @return
     */
    public Task findCurrentTaskByBusinessKey(String businessKey) {
        List<Task> list = taskService.createTaskQuery().processInstanceBusinessKey(businessKey).list();
        if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() > 1) {
            LOGGER.warn("MULTI INSTANCE TASK");
            return list.get(0);
        }

        return null;
    }

    /**
     * 保存任务
     *
     * @param task
     */
    public void saveTask(Task task) {
        taskService.saveTask(task);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Save {}[{}]", task.getTaskDefinitionKey(), task.getId());
        }
    }

}
