<%@ tag language="java" pageEncoding="utf-8"
        import="cn.com.infcn.Application,cn.com.infcn.ApplicationHolder,cn.com.infcn.workflow.Workflow,org.activiti.engine.runtime.ProcessInstance" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!-- 业务单据号 -->
<%@ attribute name="businessKey" %>
<!-- 流程定义, 在没有具体实例时使用 -->
<%@ attribute name="definitionKey" %>

<!-- JAVA -->
<%
    Application app = ApplicationHolder.get();
    Workflow workflow = app.getWorkflow();
    ProcessInstance pi = workflow.findProcessInstanceByBusinessKey(businessKey);
    request.setAttribute("processInstanceId", pi != null ? pi.getProcessInstanceId() : "");
%>

<!-- WORKFLOW HTML -->
<div class="workflow">
    <div class="workflow_panel hide">
        <form name="workflow">
            <input type="hidden" name="businessKey" value="${businessKey}"/>
            <input type="hidden" name="definitionKey" value="${definitionKey}"/>
            <input type="hidden" name="action" value=""/>
            <div>
                <!-- 基本信息 -->
                <p class="bg">
                    <label>当前步骤</label>
                    <span class="text">
						<input type="text" class="txt task" value="" readonly="readonly">
					</span>
                </p>
            </div>
            <div>
                <p class="bg">
                    <label>下一步名称</label>
                    <span class="text text_flow">
						<select name="nextTask" class="activities disabled" disabled="disabled"></select>
					</span>
                    <label style="margin-left:2.5%;width:10%;">下一步办理人 </label>
                    <span class="text">
						<input type="text" class="txt nextOperatorNames" value="AAA" style="width:80%; float:left;"
                               readonly="readonly">
                        <input type="hidden" name="nextOperators" value="1"/>
						<a class="selectOperator" href="javascript:void(0);"
                           style="width:20px; height:20px; float:right; overflow:hidden;">
                            <img src="../../resources/image/ico1.gif" class="jing"></a>
					</span>
                </p>
            </div>
            <c:if test="${not empty processInstanceId}">
                <!-- 已有流程实例时显示 -->
                <div>
                    <p class="bg">
                        <label style="">快速意见</label>
                        <span class="text text_flow">
						<select id="quickIdea">
							<option value="">请选择</option>
							<option value="同意。">同意</option>
							<option value="已阅。">已阅</option>
							<option value="已核。">已核</option>
							<option value="不同意。">不同意</option>
						</select>
					</span>
                        <label class="message">流程意见</label>
                        <a href="javascript:void(0);" id="auditIdea"><img src="../../resources/image/his.gif"
                                                                          style="float:left;"/></a>
                    </p>
                    <p class="bg">
                        <label class="lblComment">意见</label>
                        <span class="area">
						<textarea cols="20" rows="3" name="comment" id="comment"></textarea>
					</span>
                    </p>
                </div>
                <div class="actions1 view_button" style="margin:10px 0px; width:100%;">
                    <p class="shop1 shop1_btn" style="background:none;">
                        <input type="button" class="approve submit_btn" value="提交"/>
                        <input type="button" class="reject back_btn" value="退回"/>
                    </p>
                </div>
                <div class="actions2 hide">
                    <p class="shop1" style="text-align:center;">
                        <input type="button" class="submit agree_btn" style="margin:20px 5px;" value="确定">
                    </p>
                </div>
            </c:if>
            <jsp:doBody/>
        </form>
    </div>
</div>

<!-- WORKFLOW SCRIPT -->
<script>
    var workflow = {
        init: function () {
            //
            var me = this, activitiesSel = $('select.activities');

            var nextDefaultTask = '';

            // 提交表单
            $('.workflow_panel .approve').on('click', function (e) {
                me._post('approve');
            });
            $('.workflow_panel .reject').on('click', function (e) {
                me._post('reject');
            });

            // 初始化
            $.get('/workflow/info', {
                businessKey: '${businessKey}',
                definitionKey: '${definitionKey}',
                processInstanceId: '${processInstanceId}'
            }, function (response) {
                // set runtime info
                me = $.extend(me, {
                    runtime: {
                        processInstanceId: response.processInstanceId,
                        variables: response.vars,
                        task: response.task,
                        taskName: response.taskName,
                        nextTask: response.nextTask,
                        nextTaskName: response.nextTaskName
                    }
                }, {
                    processDefinitionKey: response.processDefinitionKey
                });
                //
                $("select.activities").empty();
                // 当前任务
                $('.task').val(response.taskName);

                $.each(response.activities, function () {
                    var html = '<option value=' + this.id + '>' + this.name + '</option>';
                    var option = $(html).appendTo(activitiesSel);
                    // 选中下一个任务
                    if (response.nextTask === this.id) {
                        option.attr('selected', 'selected');
                    }
                });

                //
                if ($.isFunction(me.load)) {
                    me.load.call(me);
                }

                nextDefaultTask = response.nextTask;

                //
                if (response.success) {
                    $('.workflow_panel').removeClass('hide');
                }
            });

            //快速填写意见
            $("#quickIdea").change(function () {
                $("#comment").val($(this).val());
            });
        },
        submit: null,
        msgPrompt: true,
        _post: function (action) {
            $(':hidden[name="action"]').val(action);

            //
            var isReject = (action == 'reject');

            if (!isReject) {
                if ($('.workflow_panel .nextOperatorNames').val() == "") {
                    alert('下一步办理人不能为空，请选择');
                    return;
                }
            }

            //
            if (!isReject && $.isFunction(this.submit)) {
                this.submit(this._submit);
            } else {
                this._submit();
            }
        },
        _submit: function () {
            //
            var data = $('.workflow_panel form').serialize();

            //
            var isApprove = ($(':hidden[name="action"]').val() == 'approve');
            $.post('/workflow/process', data, function (resp) {
                if (isApprove) {
                    alert('提交成功!');
                } else {
                    alert('退回成功!');
                }
            });
        }
    };

    workflow.init();
</script>
