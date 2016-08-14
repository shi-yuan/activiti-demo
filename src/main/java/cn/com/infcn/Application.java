package cn.com.infcn;

import cn.com.infcn.workflow.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private Workflow workflow;

    public Workflow getWorkflow() {
        return workflow;
    }

    public Application() {
    }
}
