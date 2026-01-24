package wormpex.data.util;

import com.google.common.collect.Lists;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.List;

public class UpstreamTriggerChain {

    private String triggerUser = "KNOWN";
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
        String jobName;
        int buildId;

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
package wormpex.data.util;

import com.google.common.collect.Lists;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.List;

/**
 * <br>
 * <b>功能:</b><br>
 * <b>作者:</b> yashiro <br>
 * <b>日期:</b> 2018/10/26 <br>
 */
public class UpstreamTriggerChain {

    //第一个任务的触发者
    private String triggerUser = "KNOWN";

    //上游链路中的触发信息，只有任务名和构建id
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
        String jobName;
        int buildId;

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
