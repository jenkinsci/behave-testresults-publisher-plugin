package net.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import net.python.behave.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BehaveReportPublisher extends Recorder {

    public final String jsonReportDirectory;
    public final String pluginUrlPath;
    public final boolean skippedFails;
    public final boolean undefinedFails;
    public final boolean noFlashCharts;
	public final boolean ignoreFailedTests;
    public final boolean parallelTesting;

    @DataBoundConstructor
    public BehaveReportPublisher(String jsonReportDirectory, String pluginUrlPath, boolean skippedFails, boolean undefinedFails, boolean noFlashCharts, boolean ignoreFailedTests, boolean parallelTesting) {
        this.jsonReportDirectory = jsonReportDirectory;
        this.pluginUrlPath = pluginUrlPath;
        this.skippedFails = skippedFails;
        this.undefinedFails = undefinedFails;
        this.noFlashCharts = noFlashCharts;
		this.ignoreFailedTests = ignoreFailedTests;
        this.parallelTesting = parallelTesting;
    }

    private String[] findJsonFiles(File targetDirectory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{"**/*.json"});
        scanner.setBasedir(targetDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {

        listener.getLogger().println("[BehaveReportPublisher] Compiling Behave Html Reports ...");

        // source directory (possibly on slave)
        FilePath workspaceJsonReportDirectory;
        if (jsonReportDirectory != null && !jsonReportDirectory.isEmpty()) {
            workspaceJsonReportDirectory = new FilePath(build.getWorkspace(), jsonReportDirectory);
        } else {
            workspaceJsonReportDirectory = build.getWorkspace();
        }

        // target directory (always on master)
        File targetBuildDirectory = new File(build.getRootDir(), "behave-html-reports");
        if (!targetBuildDirectory.exists()) {
            targetBuildDirectory.mkdirs();
        }

        String buildNumber = Integer.toString(build.getNumber());
        String buildProject = build.getProject().getName();

        if (Computer.currentComputer() instanceof SlaveComputer) {
            listener.getLogger().println("[BehaveReportPublisher] copying all json files from slave: " + workspaceJsonReportDirectory.getRemote() + " to master reports directory: " + targetBuildDirectory);
        } else {
            listener.getLogger().println("[BehaveReportPublisher] copying all json files from: " + workspaceJsonReportDirectory.getRemote() + " to reports directory: " + targetBuildDirectory);
        }
        workspaceJsonReportDirectory.copyRecursiveTo("**/*.json", new FilePath(targetBuildDirectory));

        // generate the reports from the targetBuildDirectory
		Result result = Result.NOT_BUILT;
        String[] jsonReportFiles = findJsonFiles(targetBuildDirectory);
        if (jsonReportFiles.length != 0) {

            listener.getLogger().println("[BehaveReportPublisher] Found the following number of json files: " + jsonReportFiles.length);
            int jsonIndex = 0;
            for (String jsonReportFile : jsonReportFiles) {
                listener.getLogger().println("[BehaveReportPublisher] " + jsonIndex + ". Found a json file: " + jsonReportFile);
                jsonIndex++;
            }
            listener.getLogger().println("[BehaveReportPublisher] Generating HTML reports");

            try {                
                ReportBuilder reportBuilder = new ReportBuilder(
                        fullPathToJsonFiles(jsonReportFiles, targetBuildDirectory),
                        targetBuildDirectory,
                        pluginUrlPath,
                        buildNumber,
                        buildProject,
                        skippedFails,
                        undefinedFails,
                        !noFlashCharts,
                        true,
                        false,
                        "",
                        false);
                reportBuilder.generateReports();

				boolean buildSuccess = reportBuilder.getBuildStatus();

				if (buildSuccess)
				{
					result = Result.SUCCESS;
				}
				else
				{
					result = ignoreFailedTests ? Result.UNSTABLE : Result.FAILURE;
				}
				
            } catch (Exception e) {
                e.printStackTrace();
				result = Result.FAILURE;
                listener.getLogger().println("[BehaveReportPublisher] there was an error generating the reports: " + e);
                for(StackTraceElement error : e.getStackTrace()){
                   listener.getLogger().println(error);
                }
            }
        } else {
			result = Result.SUCCESS;
            listener.getLogger().println("[BehaveReportPublisher] there were no json results found in: " + targetBuildDirectory);
        }

        build.addAction(new BehaveReportBuildAction(build));
		build.setResult(result);
		
        return true;
    }

    private List<String> fullPathToJsonFiles(String[] jsonFiles, File targetBuildDirectory) {
        List<String> fullPathList = new ArrayList<String>();
        for (String file : jsonFiles) {
            fullPathList.add(new File(targetBuildDirectory, file).getAbsolutePath());
        }
        return fullPathList;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new BehaveReportProjectAction(project);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public String getDisplayName() {
            return "Publish behave results as a report";
        }


        // Performs on-the-fly validation on the file mask wildcard.
        public FormValidation doCheck(@AncestorInPath AbstractProject project,
                                      @QueryParameter String value) throws IOException, ServletException {
            FilePath ws = project.getSomeWorkspace();
            return ws != null ? ws.validateRelativeDirectory(value) : FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
