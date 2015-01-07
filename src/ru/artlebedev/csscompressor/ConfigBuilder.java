/**
 * Author: Alexander Samilyak (aleksam241@gmail.com)
 * Created: 2012.02.19
 * Copyright 2012 Art. Lebedev Studio. All Rights Reserved.
 */

package ru.artlebedev.csscompressor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


class ConfigBuilder {

  private final String REPLACE_SPLITTER = "::";

  private final String[] cssExtensions = new String[] { "css" };


  private final CommandLine cmdLine;

  private final String configFilePath;

  private String inputPath;

  private String rootPath;

  private String charset;

  private String outputPath;

  private JsonObject modulesInfo;

  private String outputWrapper;

  private String preprocessCommand;


  ConfigBuilder(final CommandLine cmdLine) {
    this.cmdLine = cmdLine;
    configFilePath = cmdLine.getArgs()[0];
  }


  public Config build() throws IOException {
    parseConfigFile();

    return new Config(
        getRootFullPath(),
        getCharset(),
        outputWrapper,
        getModules(),
        getReplaces(),
        preprocessCommand,
        isQuiet());
  }


  private void parseConfigFile() throws IOException {
    JsonElement root = new JsonParser().parse(Utils.readFile(configFilePath));

    if (!root.isJsonObject()) {
      throw new RuntimeException(String.format(
          "Config file %s contains not a JSON object as its root",
          configFilePath));
    }

    JsonObject jsonConfig = root.getAsJsonObject();

    // Keep track of the keys in the 'options' object so that we can warn
    // about unused options in the config file.
    Set<String> options = new HashSet<String>();
    for (Map.Entry<String, JsonElement> entry : jsonConfig.entrySet()) {
      options.add(entry.getKey());
    }

    for (ConfigOption configOption : ConfigOption.values()) {
      String optionName = configOption.getName();

      if (jsonConfig.has(optionName)) {
        configOption.update(jsonConfig.get(optionName), this);
        options.remove(optionName);
      }
    }

    for (String unusedOption : options) {
      System.err.printf(
          "WARNING: Unused option \"%s\" in %s\n",
          unusedOption,
          configFilePath);
    }
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

  public void setModulesInfo(final JsonObject modulesInfo) {
    this.modulesInfo = modulesInfo;
  }

  public void setOutputWrapper(final String outputWrapper) {
    this.outputWrapper = outputWrapper;
  }

  public void setPreprocessCommand(final String command) {
    this.preprocessCommand = command;
  }

	public void setInputPath(final String inputPath) {
		this.inputPath = inputPath;
	}


private String getRootFullPath() {
    String configCatalog = new File(configFilePath).getParent();

    String rootPath;
    if (this.rootPath != null) {
      rootPath = this.rootPath;
    } else {
      rootPath = ConfigOption.ROOT.getDefaultValue();
    }

    return new File(configCatalog, rootPath).getPath();
  }

  private String getCharset() {
    if (charset != null) {
      return charset;
    } else {
      return ConfigOption.CHARSET.getDefaultValue();
    }
  }

  private List<Config.Module> getModules() {
    if (modulesInfo == null) {
      if (inputPath == null) {
    	  throw new RuntimeException(
    	          "Option '" + ConfigOption.MODULES.getName() + "' " +
    	              "is required.");
      }
      return getModulesDirectory();
    }
    else {
    	return getModulesSpecified();
    }
  }

  @SuppressWarnings("unchecked")
  private List<Config.Module> getModulesDirectory() {
	  Path rootPathObj = Paths.get(getRootFullPath(), inputPath).toAbsolutePath().normalize();
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
		  modules.add(new Config.Module(null, cssPath, getModuleOutputPath(fileOutName, null)));
	  }
	  return modules;
  }

  private List<Config.Module> getModulesSpecified() {


    List<Config.Module> modules = new ArrayList<Config.Module>();

    for (Map.Entry<String, JsonElement> moduleData :
        modulesInfo.entrySet()) {

      String name = moduleData.getKey();
      String inPath = calculateFullPath(moduleData.getValue().getAsString());

      modules.add(new Config.Module(name, inPath, getModuleOutputPath(name, null)));
    }

    return modules;
  }


  private String getModuleOutputPath(final String moduleName, String moduleOutput) {
    if (moduleOutput == null && outputPath == null) {
      throw new IllegalArgumentException(
          String.format(
              "Module '%s' didn't have output path. " +
                  "You must specify output path either by global key '%s' or " +
                  "by module own object key 'output' containing string value.",
              moduleName,
              ConfigOption.OUTPUT_PATH.getName())
      );
    }

    if (moduleOutput == null) {
      moduleOutput = outputPath;
    }

    return calculateFullPath(
        // replace %s with a module name
        String.format(moduleOutput, moduleName));
  }


  private List<Config.Replace> getReplaces() {
    String[] replaces = cmdLine.getOptionValues("replace");
    List<Config.Replace> processedReplaces = new ArrayList<Config.Replace>();

    if (replaces != null) {
      for (String replaceStr : replaces) {
        if (replaceStr.contains(REPLACE_SPLITTER)) {
          String[] split = replaceStr.split(REPLACE_SPLITTER, 2);
          processedReplaces.add(
              new Config.Replace(split[0], split[1]));

          if (!isQuiet()) {
            System.out.println("Replace: " + split[0] + " => " +  split[1]);
          }

        } else {
          throw new RuntimeException(
              String.format(
                  "Replace '%s' did not contain splitter '%s'",
                  replaceStr, REPLACE_SPLITTER));
        }
      }
    }

    return processedReplaces;
  }


  private boolean isQuiet() {
    return cmdLine.hasOption("quiet");
  }


  private String calculateFullPath(final String path) {
    return new File(getRootFullPath(), path).getPath();
  }

}
