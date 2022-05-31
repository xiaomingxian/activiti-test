package boot.spring.controller;

import boot.spring.pagemodel.AjaxResult;
import boot.spring.pagemodel.DataGrid;
import boot.spring.po.MeetingTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

@Api(value = "发起会议流程接口")
@Controller
public class MeetingController {

    @Autowired
    RuntimeService runservice;

    @Autowired
    FormService formservice;

    @Autowired
    TaskService taskservice;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    IdentityService identityservice;

    @Autowired
    HistoryService historyservice;


    @RequestMapping(value = "/startmeeting", method = RequestMethod.GET)
    String startmeeting() {
        return "meeting/startmeeting";
    }


    @RequestMapping(value = "/meetingtask", method = RequestMethod.GET)
    String meetingtask() {
        return "meeting/meetingtask";
    }

    @RequestMapping(value = "/mymeeting", method = RequestMethod.GET)
    String mymeeting() {
        return "meeting/mymeeting";
    }

    @ApiOperation("发起一个会议流程")
    @RequestMapping(value = "/startMeeting", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult startMeeting(String topic, String place, String startTime, @RequestParam("people[]") List<String> people, HttpSession session) {
        String username = (String) session.getAttribute("username");
        identityservice.setAuthenticatedUserId(username);
        Map<String, Object> variables = new HashMap<>();
        variables.put("topic", topic);
        variables.put("place", place);
        variables.put("startTime", startTime);
        variables.put("people", people);
        variables.put("host", username);
        String peoplelist = "";
        for (int i = 0; i < people.size(); i++) {
            peoplelist += people.get(i)+",";
        }
        peoplelist = peoplelist.substring(0, peoplelist.length() -1);
        // 该变量用于外置表单回显
        variables.put("peoplelist", peoplelist);
        runservice.startProcessInstanceByKey("meeting", variables);
        return AjaxResult.success();
    }

    @ApiOperation("完成会议签到")
    @RequestMapping(value = "/completeMeetingTask/{taskId}", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult completeSignature(@PathVariable String taskId) {
        formservice.submitTaskFormData(taskId, new HashMap<String, String>());
        return AjaxResult.success();
    }

    @ApiOperation("完成会议纪要填报")
    @RequestMapping(value = "/completeMeetingContent/{taskId}", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResult completeMeetingContent(@PathVariable String taskId, @RequestParam String content) {
        Map<String, String> variables = new HashMap<>();
        variables.put("content", content);
        formservice.submitTaskFormData(taskId, variables);
        return AjaxResult.success();
    }

    @ApiOperation("获取外置表单数据")
    @RequestMapping(value = "/getForm/{taskId}", method = RequestMethod.GET)
    @ResponseBody
    public AjaxResult getFormInfo(@PathVariable("taskId") String taskId) {
        Object form = formservice.getRenderedTaskForm(taskId);
        return AjaxResult.success(form);
    }

    @ApiOperation("获取参会待办列表")
    @RequestMapping(value="/meetingtasklist",method=RequestMethod.POST)
    @ResponseBody
    DataGrid<MeetingTask> meetingtasklist(HttpSession session, @RequestParam("current") int current, @RequestParam("rowCount") int rowCount){
        DataGrid<MeetingTask> grid = new DataGrid<MeetingTask>();
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(0);
        grid.setRows(new ArrayList<MeetingTask>());
        int firstrow = (current - 1) * rowCount;
        List<MeetingTask> results = new ArrayList<MeetingTask>();
        List<Task> tasks=taskservice.createTaskQuery().taskAssignee((String) session.getAttribute("username")).processDefinitionKey("meeting").listPage(firstrow, rowCount);
        Long totaltask = taskservice.createTaskQuery().taskAssignee((String) session.getAttribute("username")).processDefinitionKey("meeting").count();
        for (Task task : tasks) {
            MeetingTask vo = new MeetingTask();
            vo.setProcessinstanceid(task.getProcessInstanceId());
            vo.setTaskid(task.getId());
            vo.setTaskname(task.getName());
            // 获取表单数据
            String topic = (String) taskservice.getVariable(task.getId(), "topic");
            String startTime = (String) taskservice.getVariable(task.getId(), "startTime");
            String place = (String) taskservice.getVariable(task.getId(), "place");
            String host = (String) taskservice.getVariable(task.getId(), "host");
            vo.setStartTime(startTime);
            vo.setTopic(topic);
            vo.setPlace(place);
            vo.setHost(host);
            results.add(vo);
        }
        grid.setRowCount(rowCount);
        grid.setCurrent(current);
        grid.setTotal(totaltask.intValue());
        grid.setRows(results);
        return grid;
    }

    @ApiOperation("我发起的参会流程")
    @RequestMapping(value="mymeetingprocess",method=RequestMethod.POST)
    @ResponseBody
    public DataGrid<MeetingTask> mymeetingprocess(HttpSession session,@RequestParam("current") int current,@RequestParam("rowCount") int rowCount){
        String username=(String) session.getAttribute("username");
        DataGrid<MeetingTask> grid=new DataGrid<MeetingTask>();
        grid.setCurrent(current);
        grid.setRowCount(rowCount);
        List<ProcessInstance> list = runservice.createProcessInstanceQuery().processDefinitionKey("meeting").variableValueEquals("host",username).list();
        List<MeetingTask> tasks = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        list.stream().forEach(process->{
            MeetingTask t = new MeetingTask();
            t.setHost(username);
            t.setTaskname(taskservice.createTaskQuery().processInstanceId(process.getId()).list().get(0).getName());
            t.setProcessinstanceid(process.getId());
            t.setStartTime(sdf.format(process.getStartTime()));
            t.setState("运行中");
            tasks.add(t);
        });
        List<HistoricProcessInstance> endList = historyservice.createHistoricProcessInstanceQuery().processDefinitionKey("meeting").variableValueEquals("host",username).list();
        endList.stream().forEach(process->{
            MeetingTask t = new MeetingTask();
            t.setHost(username);
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
