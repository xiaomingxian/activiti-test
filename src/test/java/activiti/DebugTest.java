package activiti;

import boot.spring.Application;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;

/**
 * @author 仙晓明
 * @date 2022/5/31 14:21
 */
@SpringBootTest(classes = Application.class)
@RunWith(SpringRunner.class)
@Slf4j
public class DebugTest {
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;


    @Test
    public  void  depoloy(){
        repositoryService.createDeployment()
                .addClasspathResource("processes/jiqiren.bpmn")
                .deploy();

    }
    @Test
    public void show() {
        String key = "applyNoneStoreSell_zidong";

        List<Deployment> list = repositoryService.createDeploymentQuery()
                .deploymentKey(key)
                //.deploymentId(depId)
                .list();
        System.out.println(list);
    }
    @Test
    public void satrt(){
        String key = "applyNoneStoreSell_zidong";

        HashMap<String, Object> var = new HashMap<>();
        var.put("new_property_3","全新机");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key,var);
        log.info("res:{}",processInstance);

    }

    @Test
    public void runTaskFind(){

        ProcessDefinitionEntity deployedProcessDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
                .getDeployedProcessDefinition("applyNoneStoreSell_zidong:2:5017");

        log.info("debug");
    }

}
