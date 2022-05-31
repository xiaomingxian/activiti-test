package boot.spring;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

@Order(value=1)
@Component
public class ZipDeployStarter implements ApplicationRunner {

    @Autowired
    RepositoryService repositoryService;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        List<ProcessDefinition> meeting = repositoryService.createProcessDefinitionQuery().processDefinitionKey("meeting").list();
        if (meeting.size() == 0) {
            InputStream is = new ClassPathResource("processes/meeting.zip").getInputStream();
            repositoryService.createDeployment().name("meeting.zip").addZipInputStream(new ZipInputStream(is)).deploy();
        }
    }

}
