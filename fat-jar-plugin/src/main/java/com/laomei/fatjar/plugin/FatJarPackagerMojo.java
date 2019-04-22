package com.laomei.fatjar.plugin;

import com.laomei.fatjar.common.boot.tool.ArtifactsLibraries;
import com.laomei.fatjar.common.boot.tool.Libraries;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * @author laomei on 2019/1/7 14:24
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class FatJarPackagerMojo extends AbstractMojo {

    @Component
    private MavenProject      project;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File              outputDirectory;

    @Parameter
    private String            classifier;

    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String            finalName;

    private Log               logger = getLog();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logger.info("============> begin to repackage jar as fat jar");
        repackage();
        logger.info("============> create fat jar successfully");
    }

    private void repackage() throws MojoExecutionException, MojoFailureException {

        File sourceFile = project.getArtifact().getFile();
        Repackager repackager = new Repackager(sourceFile);
        File target = getTargetFile();
        Set<Artifact> artifacts = project.getArtifacts();
        Libraries libraries = new ArtifactsLibraries(artifacts, Collections.emptyList(), getLog());
        try {
            repackager.repackage(target, libraries);
        }
        catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private File getTargetFile() {
        String classifier = (this.classifier != null ? this.classifier.trim() : "");
        if (classifier.length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
        String name = this.finalName + classifier + "-fat." + this.project.getArtifact().getArtifactHandler().getExtension();
        return new File(this.outputDirectory, name);
    }
}
