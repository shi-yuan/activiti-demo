package cn.com.infcn.workflow.activiti;

import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ActivityUtils {

    public static boolean isUserTask(PvmActivity activity) {
        return "userTask".equals(activity.getProperty("type"));
    }

    public static boolean isExclusiveGateway(PvmActivity activity) {
        return "exclusiveGateway".equals(activity.getProperty("type"));
    }

    public static boolean isEndEvent(PvmActivity activity) {
        return "endEvent".equals(activity.getProperty("type"));
    }

    /**
     * 取得下一个UserTask
     *
     * @param activity
     * @return
     * @throws Exception
     */
    public static PvmActivity getNextUserTaskActivity(PvmActivity activity) throws Exception {
        //
        List<PvmTransition> transitions = activity.getOutgoingTransitions();
        // 处理选择流
        if (ActivityUtils.isExclusiveGateway(activity)) {
            // 默认流
            String def = (String) activity.getProperty("default");
            if (StringUtils.isEmpty(def)) {
                throw new Exception(String.format("Must set default flow on ExclusiveGateway[%s]", activity.toString()));
            }

            for (PvmTransition transition : transitions) {
                if (transition.getId().equals(def)) {
                    return _nextUserTask(transition.getDestination());
                }
            }
        } else {
            for (PvmTransition transition : transitions) {
                return _nextUserTask(transition.getDestination());
            }
        }

        return null;
    }

    // 如果传入的是UserTask则直接返回,否则返回最近的UserTask
    private static PvmActivity _nextUserTask(PvmActivity destination) throws Exception {
        if (ActivityUtils.isUserTask(destination)) {
            return destination;
        } else if (ActivityUtils.isExclusiveGateway(destination)) {
            return getNextUserTaskActivity(destination);
        } else if (ActivityUtils.isEndEvent(destination)) {
            return null;
        } else {
            throw new Exception(String.format("Not Supported Activity Type: %s", destination.getProperty("type")));
        }
    }


    /**
     * 获得下一个节点
     *
     * @param activity
     * @return
     * @throws Exception
     */
    public static PvmActivity getNextActivity(PvmActivity activity) throws Exception {
        //
        List<PvmTransition> transitions = activity.getOutgoingTransitions();
        // 处理选择流
        if (ActivityUtils.isExclusiveGateway(activity)) {
            // 默认流
            String def = (String) activity.getProperty("default");
            if (StringUtils.isEmpty(def)) {
                throw new Exception(String.format("Must set default flow on ExclusiveGateway[%s]", activity.toString()));
            }

            for (PvmTransition transition : transitions) {
                if (transition.getId().equals(def)) {
                    return transition.getDestination();
                }
            }
        } else {
            if (transitions.size() > 0) {
                return transitions.get(0).getDestination();
            }
        }

        return null;
    }
}
