/**
 * Copyright 2015 American Well Systems
 * All rights reserved.
 *
 * It is illegal to use, reproduce or distribute
 * any part of this Intellectual Property without
 * prior written authorization from American Well.
 */
package ru.artlebedev.csscompressor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ru.artlebedev.csscompressor.Config.Replace;

/**
 * @author david.keyworth
 *
 */
public abstract class ConfigBuilder {

	private final String[] cssExtensions = new String[] { "css" };
	private String inputPath;
	protected String rootPath;
	private String charset;
	private String outputPath;
	//gsondeprecate
	//private JsonObject modulesInfo;
	protected String outputWrapper;
	private String preprocessCommand;
	protected List<Replace> replaces;
	private boolean quiet;


	protected Config build() throws Exception {
		return new Config(
		        getRootFullPath(),
		        getCharset(),
		        outputWrapper,
		        getModules(),
		        replaces,
		        preprocessCommand,
		        isQuiet());
	}

	public void setRootPath(final String rootPath) {
	    this.rootPath = rootPath;
	  }

	public void setCharset(final String charset) {
	    this.charset = charset;
	  }

	public void setOutputPath(final String outputPath) {
	    this.outputPath = outputPath;
	  }

	/*gsondeprecate
	 * public void setModulesInfo(final JsonObject modulesInfo) {
	    this.modulesInfo = modulesInfo;
	  }*/

	public void setOutputWrapper(final String outputWrapper) {
	    this.outputWrapper = outputWrapper;
	  }

	public void setPreprocessCommand(final String command) {
	    this.preprocessCommand = command;
	  }

	public void setInputPath(final String inputPath) {
		this.inputPath = inputPath;
	}

	protected String getRootFullPath() {
	    return new File(rootPath).getPath();
	  }

	protected String getCharset() {
	    if (charset != null) {
	      return charset;
	    } else {
	      return ConfigOption.CHARSET.getDefaultValue();
	    }
	  }

	protected List<Config.Module> getModules() {
	    if (true) {// gsondeprecate modulesInfo == null) {
	      if (inputPath == null) {
	    	  throw new RuntimeException(
	    	          "One option [inputPath / modules] is required.");
	      }
	      return getModulesDirectory();
	    }
	    else {
	    	return getModulesSpecified();
	    }
	  }

	@SuppressWarnings("unchecked")
	private List<Config.Module> getModulesDirectory() {
		  Path rootPathObj = getFullInputPath();
		  File inDir = rootPathObj.toFile();
		  if (!inDir.isDirectory()) {
			  throw new RuntimeException("Option inputPath must be a valid directory "
			  		+ "relative to the \"root\" option.");
		  }
		  Collection<File> fileCol = FileUtils.listFiles(inDir, cssExtensions, true);
		  List<Config.Module> modules = new ArrayList<Config.Module>();
		  for (File cssFile : fileCol) {
			  String cssPath = cssFile.getPath();
			  Path filePath = Paths.get(cssPath).normalize();
			  String fileOutName = rootPathObj.relativize(filePath).toString();
			  modules.add(new Config.Module(null, cssPath, getModuleOutputPath(fileOutName)));
		  }
		  return modules;
	  }

	public Path getFullInputPath() {
		return Paths.get(getRootFullPath(), inputPath).toAbsolutePath().normalize();
	}

	private List<Config.Module> getModulesSpecified() {


	    List<Config.Module> modules = new ArrayList<Config.Module>();

	    /*gsondeprecate
	     * for (Map.Entry<String, JsonElement> moduleData :
	        modulesInfo.entrySet()) {

	      String name = moduleData.getKey();
	      String inPath = calculateFullPath(moduleData.getValue().getAsString());

	      modules.add(new Config.Module(name, inPath, getModuleOutputPath(name)));
	    }*/

	    return modules;
	  }

	protected String getModuleOutputPath(final String moduleName) {
		return Paths.get(rootPath).resolve(outputPath).resolve(moduleName).toString();
	}

	public boolean isQuiet() {
	    return quiet;
	  }

	protected String calculateFullPath(final String path) {
	    return new File(getRootFullPath(), path).getPath();
	  }

}