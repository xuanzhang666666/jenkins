package hudson.model;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.Objects;

@ExportedBean
public class ProjectSimpleInfo {

    private String projectName;
    private String owner;

    public static ProjectSimpleInfo of(String projectName, String owner) {
        return new ProjectSimpleInfo(projectName, owner);
    }

    private ProjectSimpleInfo(String projectName, String owner) {
        this.projectName = projectName;
        this.owner = owner;
    }

    @Exported(visibility = 3)
    public String getProjectName() {
        return projectName;
    }

    @Exported(visibility = 3)
    public String getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (o instanceof ProjectSimpleInfo) {
            ProjectSimpleInfo project = (ProjectSimpleInfo) o;
            return Objects.equals(projectName, project.projectName);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName);
    }

    @Override
    public String toString() {
        return "ProjectSimpleInfo{" +
                "projectName='" + projectName + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
