package boot.spring.controller;

import boot.spring.pagemodel.*;
import boot.spring.po.PurchaseApply;
import boot.spring.po.WorkApply;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Api(value = "加班申请流程接口")
@Controller
public class WorkApplyController {

    @Autowired
    RuntimeService runservice;

    @Autowired
    FormService formservice;

    @Autowired
    TaskService taskservice;

    @Autowired
    HistoryService historyservice;

    @Autowired
    IdentityService identityservice;

    @RequestMapping(value="/workapply",method=RequestMethod.GET)
    String mypurchase(){
        return "work/workapply";
    }

    @RequestMapping(value="/pmcheck",method=RequestMethod.GET)
    String pmcheck(){
        return "work/pmcheck";
    }

    @RequestMapping(value="/submitwork",method=RequestMethod.GET)
    String submitwork(){
        return "work/submitwork";
    }

    @RequestMapping(value="/myworkapply",method=RequestMethod.GET)
    String myworkapply(){
        return "work/myworkapply";
    }

    @ApiOperation("发起一个加班流程")
    @RequestMapping(value="startWorkApply",method= RequestMethod.POST)
    @ResponseBody
    public MSG startWorkApply(WorkApply apply, HttpSession session){
        Map<String,Object> variables=new HashMap<String, Object>();
        variables.put("applyer", session.getAttribute("username"));
        // 写入流程的发起人
        identityservice.setAuthenticatedUserId((String)session.getAttribute("username"));
        ProcessInstance process = runservice.startProcessInstanceByKey("workapply", variables);
        Task t =  taskservice.createTaskQuery().processInstanceId(process.getId()).singleResult();
        taskservice.claim(t.getId(), apply.getApplyer());
        Map<String,String> variables2=new HashMap<String, String>();
        variables2.put("projectName", apply.getProjectName());
        variables2.put("startTime", apply.getStartTime());
        variables2.put("endTime", apply.getEndTime());
        variables2.put("pm", apply.getPm());
        variables2.put("content", apply.getContent());
        // 提交表单数据并完成用户任务
        formservice.submitTaskFormData(t.getId(), variables2);
        return new MSG("success");
    }

    @ApiOperation("查看加班流程的表单数据")
    @RequestMapping(value="getApplyInfo/{taskId}",method= RequestMethod.GET)
    @ResponseBody
    public List<FormInfo> getApplyInfo(@PathVariable String taskId){
        TaskFormData formData = formservice.getTaskFormData(taskId);
        List<FormProperty> form = formData.getFormProperties();
        List<FormInfo> formList = new ArrayList<>();
        form.stream().forEach(f -> {
            String key = f.getId();
            String name = f.getName();
            String value;
            if (f.getType().getName().equals("date")) {
                Date v = (Date) taskservice.getVariable(taskId, key);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                value = sdf.format(v);
            } else {
                value = (String) taskservice.getVariable(taskId, key);
            }
            FormInfo info = new FormInfo();
            info.setKey(key);
            info.setName(name);
            info.setWriteable(f.isWritable());
            info.setValue(value);
            formList.add(info);
        });
        return formList;
    }

    @ApiOperation("项目经理审批")
    @RequestMapping(value="pmcheck/{taskId}",method= RequestMethod.POST)
    @ResponseBody
    public MSG pmcheck(@RequestParam String approve, @PathVariable String taskId, HttpSession session){
        Map<String,String> variables=new HashMap<String, String>();
        variables.put("approve", approve);
        formservice.submitTaskFormData(taskId, variables);
        return new MSG("success");
    }

    @ApiOperation("获取项目经理待办列表")
    @RequestMapping(value="/pmtasklist",method=RequestMethod.POST)
    @ResponseBody
    DataGrid<WorkTask> puchasemanagertasklist(HttpSession session, @RequestParam("current") int current, @RequestParam("rowCount") int rowCount){
        DataGrid<WorkTask> grid = new DataGrid<WorkTask>();
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(0);
        grid.setRows(new ArrayList<WorkTask>());
        int firstrow = (current - 1) * rowCount;
        List<WorkTask> results = new ArrayList<WorkTask>();
        List<Task> tasks=taskservice.createTaskQuery().taskAssignee((String) session.getAttribute("username")).taskName("项目经理审批").listPage(firstrow, rowCount);
        Long totaltask = taskservice.createTaskQuery().taskAssignee((String) session.getAttribute("username")).taskName("项目经理审批").count();
        for (Task task : tasks) {
            WorkTask vo = new WorkTask();
            String instanceid = task.getProcessInstanceId();
            ProcessInstance ins = runservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
            vo.setProcessinstanceid(task.getProcessInstanceId());
            vo.setTaskid(task.getId());
            vo.setTaskname(task.getName());
            // 获取表单数据
            TaskFormData formData = formservice.getTaskFormData(task.getId());
            String pname = (String) taskservice.getVariable(task.getId(), "projectName");
            Date v = (Date) taskservice.getVariable(task.getId(), "startTime");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String startTime = sdf.format(v);
            Date end = (Date) taskservice.getVariable(task.getId(), "endTime");
            String endTime = sdf.format(end);
            String pm = (String) taskservice.getVariable(task.getId(), "pm");
            String content = (String) taskservice.getVariable(task.getId(), "content");
            String applyer = (String) taskservice.getVariable(task.getId(), "applyer");
            vo.setApplyer(applyer);
            vo.setContent(content);
            vo.setEndTime(endTime);
            vo.setStartTime(startTime);
            vo.setPm(pm);
            vo.setProjectName(pname);
            results.add(vo);
        }
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(totaltask.intValue());
        grid.setRows(results);
        return grid;
    }

    @ApiOperation("获取重新提交加班的待办列表")
    @RequestMapping(value="/submitwork",method=RequestMethod.POST)
    @ResponseBody
    DataGrid<WorkTask> submitwork(HttpSession session, @RequestParam("current") int current, @RequestParam("rowCount") int rowCount){
        DataGrid<WorkTask> grid = new DataGrid<WorkTask>();
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(0);
        grid.setRows(new ArrayList<WorkTask>());
        int firstrow = (current - 1) * rowCount;
        List<WorkTask> results = new ArrayList<WorkTask>();
        List<Task> tasks=taskservice.createTaskQuery().taskAssignee((String) session.getAttribute("username")).taskName("提交加班申请").listPage(firstrow, rowCount);
        Long totaltask = taskservice.createTaskQuery().taskAssignee((String) session.getAttribute("username")).taskName("提交加班申请").count();
        for (Task task : tasks) {
            WorkTask vo = new WorkTask();
            String instanceid = task.getProcessInstanceId();
            ProcessInstance ins = runservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
            vo.setProcessinstanceid(task.getProcessInstanceId());
            vo.setTaskid(task.getId());
            vo.setTaskname(task.getName());
            // 获取表单数据
            TaskFormData formData = formservice.getTaskFormData(task.getId());
            String pname = (String) taskservice.getVariable(task.getId(), "projectName");
            Date v = (Date) taskservice.getVariable(task.getId(), "startTime");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String startTime = sdf.format(v);
            Date end = (Date) taskservice.getVariable(task.getId(), "endTime");
            String endTime = sdf.format(end);
            String pm = (String) taskservice.getVariable(task.getId(), "pm");
            String content = (String) taskservice.getVariable(task.getId(), "content");
            String applyer = (String) taskservice.getVariable(task.getId(), "applyer");
            vo.setApplyer(applyer);
            vo.setContent(content);
            vo.setEndTime(endTime);
            vo.setStartTime(startTime);
            vo.setPm(pm);
            vo.setProjectName(pname);
            results.add(vo);
        }
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(totaltask.intValue());
        grid.setRows(results);
        return grid;
    }

    @ApiOperation("重新提交加班信息")
    @RequestMapping(value="submitWork/{taskId}",method= RequestMethod.POST)
    @ResponseBody
    public MSG submitWork( WorkApply apply, @PathVariable String taskId, HttpSession session){
        Map<String,String> variables=new HashMap<String, String>();
        variables.put("projectName", apply.getProjectName());
        variables.put("startTime", apply.getStartTime());
        variables.put("endTime", apply.getEndTime());
        variables.put("pm", apply.getPm());
        variables.put("content", apply.getContent());
        formservice.submitTaskFormData(taskId, variables);
        return new MSG("success");
    }

    @ApiOperation("我发起的加班流程")
    @RequestMapping(value="myworkapplyprocess",method=RequestMethod.POST)
    @ResponseBody
    public DataGrid<WorkTask> myworkapplyprocess(HttpSession session,@RequestParam("current") int current,@RequestParam("rowCount") int rowCount){
        String username=(String) session.getAttribute("username");
        DataGrid<WorkTask> grid=new DataGrid<WorkTask>();
        grid.setCurrent(current);
        grid.setRowCount(rowCount);
        List<ProcessInstance> list = runservice.createProcessInstanceQuery().processDefinitionKey("workapply").variableValueEquals("applyer",username).list();
        List<WorkTask> tasks = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        list.stream().forEach(process->{
            WorkTask t = new WorkTask();
            t.setApplyer(username);
            t.setTaskname(taskservice.createTaskQuery().processInstanceId(process.getId()).singleResult().getName());
            t.setProcessinstanceid(process.getId());
            t.setStartTime(sdf.format(process.getStartTime()));
            t.setState("运行中");
            tasks.add(t);
        });
        List<HistoricProcessInstance> endList = historyservice.createHistoricProcessInstanceQuery().processDefinitionKey("workapply").variableValueEquals("applyer",username).list();
        endList.stream().forEach(process->{
        	WorkTask t = new WorkTask();
        	t.setApplyer(username);
        	t.setTaskname("无");
        	t.setProcessinstanceid(process.getId());
        	t.setStartTime(sdf.format(process.getStartTime()));
        	t.setState("已结束");
        	tasks.add(t);
        });
        int from = (current-1)*rowCount;
        int to = (from + rowCount) > tasks.size()? tasks.size() : from + rowCount;
        grid.setTotal(tasks.size());
        grid.setRows(tasks.subList(from, to));
        return grid;
    }
    
}
