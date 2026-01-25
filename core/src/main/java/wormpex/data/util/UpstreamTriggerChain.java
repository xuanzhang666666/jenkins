package wormpex.data.util;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * 上游触发链路信息，记录触发者和上游构建简要信息。
 */
public class UpstreamTriggerChain {

    // 第一个任务的触发者
    private String triggerUser = "KNOWN";

    // 上游链路中的触发信息，只有任务名和构建 id
    private List<JobBuildSimpleInfo> jobBuildSimpleInfos = Lists.newArrayList();

    public String getTriggerUser() {
        return triggerUser;
    }

    public void setTriggerUser(String triggerUser) {
        this.triggerUser = triggerUser;
    }

    public List<JobBuildSimpleInfo> getJobBuildSimpleInfos() {
        return jobBuildSimpleInfos;
    }

    public void setJobBuildSimpleInfos(List<JobBuildSimpleInfo> jobBuildSimpleInfos) {
        this.jobBuildSimpleInfos = jobBuildSimpleInfos;
    }

    @Override
    public String toString() {
        return "UpstreamTriggerChain{" +
                "triggerUser='" + triggerUser + '\'' +
                ", jobBuildSimpleInfos=" + jobBuildSimpleInfos +
                '}';
    }

    public static class JobBuildSimpleInfo {
        private String jobName;
        private int buildId;

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public int getBuildId() {
            return buildId;
        }

        public void setBuildId(int buildId) {
            this.buildId = buildId;
        }

        @Override
        public String toString() {
            return "JobBuildSimpleInfo{" +
                    "jobName='" + jobName + '\'' +
                    ", buildId=" + buildId +
                    '}';
        }
    }
}
