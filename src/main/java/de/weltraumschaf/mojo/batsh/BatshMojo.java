/*
 *  LICENSE
 *
 * "THE BEER-WARE LICENSE" (Revision 43):
 * "Sven Strittmatter" <weltraumschaf@googlemail.com> wrote this file.
 * As long as you retain this notice you can do whatever you want with
 * this stuff. If we meet some day, and you think this stuff is worth it,
 * you can buy me a non alcohol-free beer in return.
 *
 * Copyright (C) 2012 "Sven Strittmatter" <weltraumschaf@googlemail.com>
 */
package de.weltraumschaf.mojo.batsh;

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
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sven Strittmatter <weltraumschaf@googlemail.com>
 */
@Mojo(
        name = "batsh",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresProject = true)
public class BatshMojo extends AbstractMojo {

    // First, let's deal with the options that the ANTLR tool itself
    // can be configured by.
    //
    /**
     * If set to true then the ANTLR tool will generate a description of the ATN for each rule in <a
     * href="http://www.graphviz.org">Dot format</a>.
     */
    @Parameter(property = "antlr4.atn", defaultValue = "false")
    protected boolean atn;

    /**
     * specify grammar file encoding; e.g., euc-jp
     */
    @Parameter(property = "project.build.sourceEncoding")
    protected String encoding;

    /**
     * Generate parse tree listener interface and base class.
     */
    @Parameter(property = "antlr4.listener", defaultValue = "true")
    protected boolean listener;

    /**
     * Generate parse tree visitor interface and base class.
     */
    @Parameter(property = "antlr4.visitor", defaultValue = "false")
    protected boolean visitor;

    /**
     * Treat warnings as errors.
     */
    @Parameter(property = "antlr4.treatWarningsAsErrors", defaultValue = "false")
    protected boolean treatWarningsAsErrors;

    /**
     * Use the ATN simulator for all predictions.
     */
    @Parameter(property = "antlr4.forceATN", defaultValue = "false")
    protected boolean forceATN;

    /**
     * A list of grammar options to explicitly specify to the tool. These options are passed to the tool using the
     * <code>-D&lt;option&gt;=&lt;value&gt;</code> syntax.
     */
    @Parameter
    protected Map<String, String> options;

    /**
     * A list of additional command line arguments to pass to the ANTLR tool.
     */
    @Parameter
    protected List<String> arguments;

    /* --------------------------------------------------------------------
     * The following are Maven specific parameters, rather than specific
     * options that the ANTLR tool can use.
     */
    /**
     * Provides an explicit list of all the grammars that should be included in the generate phase of the plugin. Note
     * that the plugin is smart enough to realize that imported grammars should be included but not acted upon directly
     * by the ANTLR Tool.
     * <p/>
     * A set of Ant-like inclusion patterns used to select files from the source directory for processing. By default,
     * the pattern <code>**&#47;*.g4</code> is used to select grammar files.
     */
    @Parameter
    protected Set<String> includes = new HashSet<String>();
    /**
     * A set of Ant-like exclusion patterns used to prevent certain files from being processed. By default, this set is
     * empty such that no files are excluded.
     */
    @Parameter
    protected Set<String> excludes = new HashSet<String>();
    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The directory where the ANTLR grammar files ({@code *.g4}) are located.
     */
    @Parameter(defaultValue = "${basedir}/src/main/antlr4")
    private File sourceDirectory;

    /**
     * Specify output directory where the Java files are generated.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/antlr4")
    private File outputDirectory;

    /**
     * Specify location of imported grammars and tokens files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/antlr4/imports")
    private File libDirectory;

    @Component
    private BuildContext buildContext;

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public File getLibDirectory() {
        return libDirectory;
    }

    void addSourceRoot(File outputDir) {
        project.addCompileSourceRoot(outputDir.getPath());
    }

    /**
     * The main entry point for this Mojo, it is responsible for converting ANTLR 4.x grammars into the target language
     * specified by the grammar.
     *
     * @exception MojoExecutionException if a configuration or grammar error causes the code generation process to fail
     * @exception MojoFailureException if an instance of the ANTLR 4 {@link Tool} cannot be created
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Log log = getLog();

        if (log.isDebugEnabled()) {
            for (String e : excludes) {
                log.debug("ANTLR: Exclude: " + e);
            }

            for (String e : includes) {
                log.debug("ANTLR: Include: " + e);
            }

            log.debug("ANTLR: Output: " + outputDirectory);
            log.debug("ANTLR: Library: " + libDirectory);
        }

        if (!sourceDirectory.isDirectory()) {
            log.info("No ANTLR 4 grammars to compile in " + sourceDirectory.getAbsolutePath());
            return;
        }

        // Ensure that the output directory path is all in tact so that
        // ANTLR can just write into it.
        //
        File outputDir = getOutputDirectory();

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Now pick up all the files and process them with the Tool
        List<List<String>> argumentSets = null;
//        try {
//            List<String> args = getCommandArguments();
//            argumentSets = processGrammarFiles(args, sourceDirectory);
//        } catch (InclusionScanException ie) {
//            log.error(ie);
//            throw new MojoExecutionException("Fatal error occured while evaluating the names of the grammar files to analyze", ie);
//        }

        log.debug("Output directory base will be " + outputDirectory.getAbsolutePath());
        log.info("ANTLR 4: Processing source directory " + sourceDirectory.getAbsolutePath());
        for (List<String> args : argumentSets) {
            try {
                // Create an instance of the ANTLR 4 build tool
//                tool = new CustomTool(args.toArray(new String[args.size()]));
            } catch (Exception e) {
                log.error("The attempt to create the ANTLR 4 build tool failed, see exception report for details", e);
                throw new MojoFailureException("Error creating an instanceof the ANTLR tool.", e);
            }

            // Set working directory for ANTLR to be the base source directory
//            tool.inputDirectory = sourceDirectory;
//            tool.processGrammarsOnCommandLine();
            // If any of the grammar files caused errors but did nto throw exceptions
            // then we should have accumulated errors in the counts
//            if (tool.getNumErrors() > 0) {
//                throw new MojoExecutionException("ANTLR 4 caught " + tool.getNumErrors() + " build errors.");
//            }
        }

        if (project != null) {
            // Tell Maven that there are some new source files underneath the output directory.
            addSourceRoot(this.getOutputDirectory());
        }
    }

    private List<String> getCommandArguments() {
        List<String> args = new ArrayList<String>();

        if (getOutputDirectory() != null) {
            args.add("-o");
            args.add(outputDirectory.getAbsolutePath());
        }

        // Where do we want ANTLR to look for .tokens and import grammars?
        if (getLibDirectory() != null && getLibDirectory().isDirectory()) {
            args.add("-lib");
            args.add(libDirectory.getAbsolutePath());
        }

        // Next we need to set the options given to us in the pom into the
        // tool instance we have created.
        if (atn) {
            args.add("-atn");
        }

        if (encoding != null && !encoding.isEmpty()) {
            args.add("-encoding");
            args.add(encoding);
        }

        if (listener) {
            args.add("-listener");
        } else {
            args.add("-no-listener");
        }

        if (visitor) {
            args.add("-visitor");
        } else {
            args.add("-no-visitor");
        }

        if (treatWarningsAsErrors) {
            args.add("-Werror");
        }

        if (forceATN) {
            args.add("-Xforce-atn");
        }

        if (options != null) {
            for (Map.Entry<String, String> option : options.entrySet()) {
                args.add(String.format("-D%s=%s", option.getKey(), option.getValue()));
            }
        }

        if (arguments != null) {
            args.addAll(arguments);
        }

        return args;
    }

    public Set<String> getIncludesPatterns() {
        if (includes == null || includes.isEmpty()) {
            return Collections.singleton("**/*.g4");
        }
        return includes;
    }

    /**
     * Given the source directory File object and the full PATH to a grammar, produce the path to the named grammar file
     * in relative terms to the {@code sourceDirectory}. This will then allow ANTLR to produce output relative to the
     * base of the output directory and reflect the input organization of the grammar files.
     *
     * @param sourceDirectory The source directory {@link File} object
     * @param grammarFileName The full path to the input grammar file
     * @return The path to the grammar file relative to the source directory
     */
    private String findSourceSubdir(File sourceDirectory, String grammarFileName) {
        String srcPath = sourceDirectory.getPath() + File.separator;

        if (!grammarFileName.startsWith(srcPath)) {
            throw new IllegalArgumentException("expected " + grammarFileName + " to be prefixed with " + sourceDirectory);
        }

        File unprefixedGrammarFileName = new File(grammarFileName.substring(srcPath.length()));
        if (unprefixedGrammarFileName.getParent() == null) {
            return "";
        }

        return unprefixedGrammarFileName.getParent() + File.separator;
    }

}
