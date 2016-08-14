package cn.com.infcn;

import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationHolder {

    private static final ApplicationHolder me = new ApplicationHolder();

    private ApplicationHolder() {
    }

    public static ApplicationHolder createInstance() {
        return me;
    }

    private Application application;

    @Autowired
    public void setApplication(Application application) {
        this.application = application;
    }

    public static Application get() {
        return me.application;
    }
}
