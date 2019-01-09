package com.laomei.plugin;

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
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.AbstractZipArchiver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * @author laomei on 2019/1/7 14:24
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class FatJarPackagerMojo extends AbstractMojo {

    private static final String FAT_JAR_TOOL = "Fat-Jar-Build-Tool";
    private static final String FAT_JAR_TOOL_VALUE = "laomei-Fat-Jar-Plugin";

    @Component
    private MavenProject project;

    @Component
    protected ArchiverManager archiverManager;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.artifactId}")
    private String artifactName;

    @Parameter(defaultValue = "${project.version}")
    private String version;

    private Log logger = getLog();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logger.info("============> begin to repackage jar as fat jar");
        repackage();
        logger.info("============> create fat jar successfully");
    }

    private void repackage() throws MojoExecutionException, MojoFailureException {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        Archiver archiver;
        try {
            archiver = getArchiver();
        } catch (NoSuchArchiverException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        String fileName = artifactName + "-" + version + "-fat.jar";
        File destination = new File(outputDirectory, fileName);
        if (destination.exists()) {
            destination.delete();
        }
        archiver.setDestFile(destination);

        addLibs(archiver);

        File manifestTempFile = null;

        try {
            manifestTempFile = new File(outputDirectory, "MANIFEST.MF");
            addManifest(archiver, manifestTempFile);

            try {
                createArchive(archiver, destination);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage());
            }
        } finally {
            if (manifestTempFile != null) {
                manifestTempFile.delete();
            }
        }
    }

    private void addManifest(Archiver archiver, File manifestFile) throws MojoExecutionException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue(FAT_JAR_TOOL, FAT_JAR_TOOL_VALUE);
        try (PrintStream printStream = new PrintStream(manifestFile, "UTF-8")) {
            manifest.write(printStream);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        archiver.addFile(manifestFile, "META-INF/MANIFEST.MF");
    }

    private void addLibs(Archiver archiver) {
        Set<Artifact> artifacts = project.getArtifacts();
        for (Artifact artifact : artifacts) {
            String dest = artifact.getFile().getName();
            dest = "lib/" + dest;
            archiver.addFile(artifact.getFile(), dest);
        }
        File sourceFile = project.getArtifact().getFile();
        String dest = sourceFile.getAbsoluteFile().getName();
        archiver.addFile(sourceFile, "lib/" + dest);
    }

    private void createArchive(Archiver archiver, File destination) throws IOException {
        archiver.createArchive();
        Artifact artifact = project.getArtifact();
        artifact.setFile(destination);
        project.setArtifact(artifact);
    }

    protected Archiver getArchiver() throws NoSuchArchiverException {
        Archiver archiver = archiverManager.getArchiver("zip");
        ((AbstractZipArchiver) archiver).setCompress(false);
        return archiver;
    }
}
