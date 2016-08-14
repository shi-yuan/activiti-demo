package cn.com.infcn.controller;

import cn.com.infcn.workflow.Workflow;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    private static final String CURRENT_USER_ID = "1";
    private static final String PROCESS_DEFINITION_KEY = "leave";

    @Autowired
    private Workflow workflow;

    @RequestMapping("/")
    public String view(@RequestParam(value = "inputCode", required = false) String inputCode, Model model) throws Exception {
        Date date = new Date();
        if (StringUtils.isEmpty(inputCode)) {
            inputCode = "KQ" + FastDateFormat.getInstance("yyyyMMddHHmmss").format(date);
            model.addAttribute("isNew", true);
        }

        model.addAttribute("inputCode", inputCode);
        model.addAttribute("applyTime", DateFormatUtils.ISO_DATE_FORMAT.format(date));
        model.addAttribute("definitionKey", PROCESS_DEFINITION_KEY);

        return "demo";
    }

    /**
     * 处理提交的请求
     */
    @ResponseBody
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @Transactional
    public Object submit(String inputCode) throws Exception {
        // 业务主键
        if (StringUtils.isEmpty(inputCode)) {
            throw new Exception("Invalid BusinessKey: " + inputCode);
        }

        // 查询流程实例
        ProcessInstance processInstance = workflow.findProcessInstanceByBusinessKey(inputCode);

        // 如果该单据尚未启动流程, 则新开启一个
        if (processInstance == null) {
            processInstance = workflow.startByBusinessKey(PROCESS_DEFINITION_KEY, inputCode);
            if (logger.isDebugEnabled()) {
                logger.debug("Start new ProcessInstance[{}:{}]", processInstance.getBusinessKey(), processInstance.getId());
            }
            // 自己直接领取任务
            workflow.claimUnassignedTask(processInstance, CURRENT_USER_ID);

            // 当前任务
            Task task = workflow.findTask(processInstance, CURRENT_USER_ID);
            if (task == null) {
                throw new Exception(String.format("Can't find task. processInstanceId=%s, userId=%s", processInstance, CURRENT_USER_ID));
            }

            // 完成任务
            workflow.complete(task);

            // 分配审核任务
            workflow.claimUnassignedTask(processInstance, CURRENT_USER_ID);
        }
        return new ModelMap().addAttribute("success", true);
    }
}
