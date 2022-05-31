package activiti;

import activiti.model.PvmTransitionModel;
import boot.spring.Application;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.ExclusiveGatewayActivityBehavior;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private ProcessEngineConfiguration processEngineConfiguration;

    String KEY = "varTest3";

    @Test
    public void depoloy() {
        repositoryService.createDeployment()
                .addClasspathResource("processes/varTest3.bpmn")
                .deploy();
    }

    @Test
    public void start() {

        HashMap<String, Object> var = new HashMap<>();
        var.put("k1", "2  ");
        var.put("k2", "3");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(KEY, var);

        log.info("res:{}", processInstance);
    }

    public static void main(String[] args) {
//        || k7==8
        String s = "${k1==1 and k2=='two' or k3==1.0 and (k4==9 or k5==10) && k6==5 || k7==8}";
        ArrayList<String> keysCol = new ArrayList<>();
        parseConditionKeys(Arrays.asList(s), keysCol);
        log.info("res:{}", keysCol);
    }

    private static void parseConditionKeys(List<String> els, List list) {
        for (String el : els) {
            el = el.replaceAll("\\$\\{", "")
                    .replaceAll("}", "")
                    .replaceAll("\\(", "")
                    .replaceAll("\\)", "")
            ;
            String[] elSplit = {};
            if (el.contains("and")) {
                elSplit = el.split("and");
            } else if (el.contains("&&")) {
                elSplit = el.split("&&");
            } else if (el.contains("or")) {
                elSplit = el.split("or");
            } else if (el.contains("||")) {
                elSplit = el.split("\\|\\|");
            } else {
                //todo > < >= <= 判断
                String key = el.split("==")[0];
                String keyTrim = key.trim();
                if (!list.contains(key) && StringUtils.isNotEmpty(keyTrim)) {
                    list.add(keyTrim);
                }

            }
            parseConditionKeys(Arrays.asList(elSplit), list);
        }
    }


    @Test
    public void parse() {
        ProcessDefinitionEntity deployedProcessDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition("myProcess:2:8");

        List<ActivityImpl> activities = deployedProcessDefinition.getActivities();

        String nodeId = "usertask1";
        ActivityImpl currentNode = null;

        for (ActivityImpl act : activities) {
            String id = act.getId();
            if (nodeId.equals(id)) {
                currentNode = act;
            }
        }

        List<PvmTransition> outgoingTransitions = currentNode.getOutgoingTransitions();
        PvmTransitionModel pvmTransitionModel = PvmTransitionModel.builder().keys(new ArrayList<>()).otherLine(new ArrayList<>()).build();
        if (outgoingTransitions.size() == 1) {
            PvmActivity destination = outgoingTransitions.get(0).getDestination();
            getAllControlKeys(destination, pvmTransitionModel);
        }

        log.info("test:{}", pvmTransitionModel);


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
                    String conditionText = outgoingTransition.getProperty("conditionText").toString();
                    pvmTransitionModel.getKeys().add(conditionText);
                    PvmTransitionModel pvmTransitionModelOther = PvmTransitionModel.builder().keys(new ArrayList<>()).otherLine(new ArrayList<>()).build();
                    pvmTransitionModel.getOtherLine().add(pvmTransitionModelOther);
                    PvmActivity destinationOther = outgoingTransition.getDestination();
                    getAllControlKeys(destinationOther, pvmTransitionModelOther);
                }
            }
        }
    }

}
