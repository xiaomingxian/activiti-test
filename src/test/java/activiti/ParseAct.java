package activiti;

import activiti.model.FormProperty;
import activiti.model.PvmTransitionModel;
import boot.spring.Application;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.FormService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.ExclusiveGatewayActivityBehavior;
import org.activiti.engine.impl.form.DefaultTaskFormHandler;
import org.activiti.engine.impl.form.FormPropertyHandler;
import org.activiti.engine.impl.form.FormPropertyImpl;
import org.activiti.engine.impl.form.TaskFormDataImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 解析流程
 *
 * @author 仙晓明
 * @date 2022/5/31 14:21
 */
@SpringBootTest(classes = Application.class)
@RunWith(SpringRunner.class)
@Slf4j
public class ParseAct {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FormService formService;


    static final String AND = "and";
    static final String OR = "or";
    static final String AND_EXP = "&&";
    static final String OR_EXP = "||";
    static final String EQUAL = "==";
    static final String LT = "<";
    static final String LT_EQ = "<=";
    static final String GT = ">";
    static final String GT_EQ = ">=";
    static final String CONDITION_TEXT = "conditionText";

    String KEY = "lizhiShenqing_kehcg";
    String DEF_ID = "shichangbukehufenfa:8:112517";

    @Test
    public void depoloy() {
        repositoryService.createDeployment()
                .addClasspathResource("processes/test.bpmn")
                .deploy();

        List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery().processDefinitionKey(KEY).orderByProcessDefinitionId().desc().list();
        log.info("res:{}",list);
    }

    @Test
    public void start() {

        HashMap<String, Object> var = new HashMap<>();
//        var.put("k1", "2  ");
//        var.put("k2", "3");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(KEY, var);

        log.info("res:{}", processInstance);
    }

    @Test
    public void  getForm(){
//        115004
        TaskFormData taskFormData = formService.getTaskFormData("115004");
        ArrayList<FormPropertyImpl> formProperties = (ArrayList) ((TaskFormDataImpl) taskFormData).getFormProperties();


        log.info("taskForm:{}",taskFormData);
    }
    @Test
    public void  completeTask() {
//135008
//135011
//135014
        HashMap<String, Object> var = new HashMap<>();
        var.put("new_property_1","无未归档合同");

        taskService.complete("142503",var);
//        taskService.complete("135008");
//        taskService.complete("135014");
    }

    @Test
    public void parse() {
        ProcessDefinitionEntity deployedProcessDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition("shichangbukehufenfa:4:105017");
        String nodeId = "usertask1";
        PvmTransitionModel pvmTransitionModel = getPvmTransitionModel(deployedProcessDefinition, nodeId);

        log.info("test:{}", pvmTransitionModel);
    }
    @Test
    public void parseCurrent() {


        String nodeId = "usertask1";

        ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) repositoryService
                .getProcessDefinition(DEF_ID);


        ActivityImpl activityImpl = processDefinitionEntity.findActivity(nodeId);

        TaskDefinition taskDef = (TaskDefinition) activityImpl.getProperties().get("taskDefinition");

        DefaultTaskFormHandler fh = (DefaultTaskFormHandler) taskDef.getTaskFormHandler();

        List<FormPropertyHandler> flList = fh == null ? null : (List<FormPropertyHandler>) fh.getFormPropertyHandlers();
        //获取表单
        if (flList != null) {
            for (FormPropertyHandler formPropertyHandler : flList) {
                //id
                String id = formPropertyHandler.getId();
                //name
                String name = formPropertyHandler.getName();
                //表达式
                String variableExpression = formPropertyHandler.getVariableExpression().toString();
                //角色组
                String variableName = formPropertyHandler.getVariableName();
                FormProperty formProperty = new FormProperty();
                formProperty.setVariableExpression(variableExpression);
                BeanUtils.copyProperties(formPropertyHandler,formProperty);
                log.info("form信息:{}", JSON.toJSONString(formPropertyHandler));
                log.info("formProperty:{}", JSON.toJSONString(formProperty));
            }
        }
    }

    public PvmTransitionModel getPvmTransitionModel(ProcessDefinitionEntity deployedProcessDefinition, String nodeId) {
        List<ActivityImpl> activities = deployedProcessDefinition.getActivities();
        ActivityImpl currentNode = null;
        for (ActivityImpl act : activities) {
            String id = act.getId();
            if (nodeId.equals(id)) {
                currentNode = act;
            }
        }
        List<PvmTransition> outgoingTransitions = currentNode.getOutgoingTransitions();
        PvmTransitionModel pvmTransitionModel = null;
        if (outgoingTransitions.size() == 1) {
            PvmActivity destination = outgoingTransitions.get(0).getDestination();
            pvmTransitionModel = getAllControlKeys(destination);
        }
        return pvmTransitionModel;
    }


    /**
     * 收集userTask后的所有条件key
     *
     * @param destinationl
     * @return
     */
    private PvmTransitionModel getAllControlKeys(PvmActivity destinationl) {
        PvmTransitionModel pvmTransitionModel = PvmTransitionModel.builder().keys(new ArrayList<>()).otherLine(new ArrayList<>()).build();
        getAllControlKeys(destinationl, pvmTransitionModel);
        return pvmTransitionModel;
    }


    /**
     * 收集userTask后的所有条件key
     *
     * @param destination
     */
    private void getAllControlKeys(PvmActivity destination, PvmTransitionModel pvmTransitionModel) {
        if (destination instanceof ActivityImpl) {
            ActivityImpl dest = (ActivityImpl) destination;
            ActivityBehavior activityBehavior = dest.getActivityBehavior();
            //如果是排他网关
            if (activityBehavior instanceof ExclusiveGatewayActivityBehavior) {
                //获取发出的连线
                List<PvmTransition> outgoingTransitions = dest.getOutgoingTransitions();
                for (PvmTransition outgoingTransition : outgoingTransitions) {
                    String conditionText = outgoingTransition.getProperty(CONDITION_TEXT).toString();
                    parseConditionKeys(Arrays.asList(conditionText), pvmTransitionModel.getKeys());
                    PvmTransitionModel pvmTransitionModelOther = PvmTransitionModel.builder().keys(new ArrayList<>()).otherLine(new ArrayList<>()).build();
                    pvmTransitionModel.getOtherLine().add(pvmTransitionModelOther);
                    PvmActivity destinationOther = outgoingTransition.getDestination();
                    getAllControlKeys(destinationOther, pvmTransitionModelOther);
                }
            }
        }
    }

    private static void parseConditionKeys(List<String> els, List list) {


        for (String el : els) {
            el = el.replaceAll("\\$\\{", "")
                    .replaceAll("}", "")
                    .replaceAll("\\(", "")
                    .replaceAll("\\)", "")
            ;
            String[] elSplit = {};
            if (el.contains(AND)) {
                elSplit = el.split(AND);
            } else if (el.contains(AND_EXP)) {
                elSplit = el.split(AND_EXP);
            } else if (el.contains(OR)) {
                elSplit = el.split(OR);
            } else if (el.contains(OR_EXP)) {
                elSplit = el.split("\\|\\|");
            } else {
                String keyTrim = null;
                if (el.contains(EQUAL)) {
                    keyTrim = el.split(EQUAL)[0].trim();
                } else if (el.contains(GT_EQ)) {
                    keyTrim = el.split(GT_EQ)[0].trim();
                } else if (el.contains(LT_EQ)) {
                    keyTrim = el.split(LT_EQ)[0].trim();
                } else if (el.contains(LT) && !el.contains("=")) {
                    keyTrim = el.split(LT)[0].trim();
                } else if (el.contains(GT) && !el.contains("=")) {
                    keyTrim = el.split(GT)[0].trim();
                }
                if (keyTrim != null && !list.contains(keyTrim) && StringUtils.isNotEmpty(keyTrim)) {
                    list.add(keyTrim);
                }
            }
            parseConditionKeys(Arrays.asList(elSplit), list);
        }
    }

    public static List<String> parseConditionKeys(String el) {
        List<String> list = new ArrayList<>();
        parseConditionKeys(Arrays.asList(el), list);
        return list;
    }

}
