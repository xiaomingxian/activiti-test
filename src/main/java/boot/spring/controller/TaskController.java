package boot.spring.controller;

import boot.spring.pagemodel.AjaxResult;
import boot.spring.pagemodel.DataGrid;
import boot.spring.pagemodel.MSG;
import boot.spring.pagemodel.TaskInfo;
import boot.spring.po.LeaveApply;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(value = "所有流程待办通用接口")
@Controller
public class TaskController {

    @Autowired
    RuntimeService runservice;

    @Autowired
    HistoryService historyservice;

    @Autowired
    TaskService taskservice;

    @Autowired
    FormService formservice;

    @RequestMapping(value = "/alltasks", method = RequestMethod.GET)
	String process() {
		return "activiti/alltasks";
	}
    
    @ApiOperation("查询所有的流程待办")
    @RequestMapping(value = "/listAllTasks", method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<TaskInfo> listAllTasks(@RequestParam("current") int current, @RequestParam("rowCount") int rowCount) {
        int firstrow = (current - 1) * rowCount;
        List<Task> taskList = taskservice.createTaskQuery().listPage(firstrow, rowCount);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<TaskInfo> tasks = new ArrayList<>();
        taskList.stream().forEach(t -> {
        	ProcessInstance process = runservice.createProcessInstanceQuery().processInstanceId(t.getProcessInstanceId()).singleResult();
        	TaskInfo info = new TaskInfo();
        	info.setAssignee(t.getAssignee());
        	info.setBusinessKey(process.getBusinessKey());
        	info.setCreateTime(sdf.format(t.getCreateTime()));
        	info.setCurrentTask(t.getName());
        	info.setExecutionId(t.getExecutionId());
        	info.setProcessInstanceId(t.getProcessInstanceId());
        	info.setProcessName(process.getProcessDefinitionName());
        	info.setStarter(process.getStartUserId());
        	info.setStartTime(sdf.format(process.getStartTime()));
        	info.setTaskId(t.getId());
        	tasks.add(info);
        });
        int total = taskservice.createTaskQuery().list().size();
        DataGrid<TaskInfo> grid = new DataGrid<TaskInfo>();
        grid.setCurrent(current);
        grid.setRowCount(rowCount);
        grid.setRows(tasks);
        grid.setTotal(total);
        return grid;
    }

    @ApiOperation("办理一个用户任务")
    @RequestMapping(value = "/completeTask/{taskId}", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult completeTask(@PathVariable("taskId") String taskId, @RequestParam(required=false) Map<String, Object> variables, HttpSession session) {
    	String username = (String) session.getAttribute("username");
        taskservice.setAssignee(taskId, username);
    	taskservice.complete(taskId, variables);
    	return AjaxResult.success();
    }

    @ApiOperation("获取formkey数据")
    @RequestMapping(value = "/getFormInfo/{taskId}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult getFormInfo(@PathVariable("taskId") String taskId) {
        String formKey = formservice.getTaskFormData(taskId).getFormKey();
        return AjaxResult.success(formKey);
    }

}