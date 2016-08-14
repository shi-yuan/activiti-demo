<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>activiti-demo</title>
    <meta name="description" content="">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel="stylesheet" href="../../resources/css/common.css">
    <link rel="stylesheet" href="../../resources/css/index.css">
    <link rel="stylesheet" href="../../resources/css/workflow.css">
    <link rel="stylesheet" href="../../resources/css/jquery.plugin.css">

    <script src="../../resources/jquery/jquery-3.1.0.min.js"></script>
</head>
<body>
<form name="leaveForm">
    <div class="pop_box" style="background:#f6fbfd;width: 99%">
        <div class="title weight">请假申请</div>
        <div class="pop_holder">
            <div class="forms_box">
                <p class="top">请假单基本信息</p>
                <p class="bg">
                    <label>单据号</label>
                    <span class="text">
					<input type="text" class="txt" name="inputCode" id="inputCode" value="${inputCode}"
                           readonly="readonly"/>
					</span>
                    <label>申请人</label>
                    <span class="text">
					<input type="text" class="txt" value="AAA" readonly="readonly"/>
					</span></p>
                <p class="bg">
                    <label>申请时间</label>
                    <span class="text">
					<input type="text" class="txt" name="applyTime" id="applyTime" value="${applyTime}"
                           readonly="readonly"/>
					</span></p>
                <p style="background-color: #F4F9FB;">
                    <label>原因</label>
                    <span class="area" style="width:70.8%;">
					<textarea cols="20" rows="3">临时有事</textarea>
					</span></p>
            </div>
        </div>
    </div>
</form>
<tags:workflow businessKey="${param['inputCode']}" definitionKey="${definitionKey}">
    <script>
        workflow = $.extend(workflow, {
            load: function () {

            }
        });
    </script>
</tags:workflow>


<c:if test="${isNew}">
    <div class="pop_box" style="width: 99%">
        <div class="pop_holder">
            <div class="forms_box">
                <p class="shop1 shop1_btn" style="background:none;" id="operateBtnSpan">
                    <input type="button" id="btnSubmit" class="submit_btn" value="提交"/>
                </p>
            </div>
        </div>
    </div>
    <script type="text/javascript">
        $(document).ready(function () {
            $('#btnSubmit').on('click', function () {
                var params = $('form[name="leaveForm"]').serialize();
                $.post('/submit', params, function (data) {
                    if (data.success) {
                        alert("提交成功!");
                    } else {
                        alert("提交失败!");
                        window.opener = null;
                        window.open('', '_self', '');
                        window.close();
                    }
                });
            });
        });
    </script>
</c:if>

</body>
</html>