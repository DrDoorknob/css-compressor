/**
 * Author: Alexander Samilyak (aleksam241@gmail.com)
 * Created: 2012.02.19
 * Copyright 2012 Art. Lebedev Studio. All Rights Reserved.
 */

package ru.artlebedev.csscompressor;

import java.util.List;


class Config {

  public static final String OUTPUT_WRAPPER_MARKER = "%output%";


  private final String rootPath;

  private final String charset;

  private final String outputWrapper;

  private final List<Module> modules;

  private final List<Replace> replaces;

  private final String preprocessCommand;

  private final boolean quiet;


  Config(
      final String rootPath,
      final String charset,
      final String outputWrapper,
      final List<Module> modules,
      final List<Replace> replaces,
      final String preprocessCommand,
      final boolean quiet){

    this.rootPath = rootPath;
    this.charset = charset;
    this.outputWrapper = outputWrapper;
    this.modules = modules;
    this.replaces = replaces;
    this.preprocessCommand = preprocessCommand;
    this.quiet = quiet;
  }

  public String getRootPath() {
    return rootPath;
  }

  public String getCharset() {
    return charset;
  }

  public String getOutputWrapper() {
    return outputWrapper;
  }

  public List<Module> getModules() {
    return modules;
  }

  public List<Replace> getReplaces() {
    return replaces;
  }

  public String getPreprocessCommand() {
    return preprocessCommand;
  }

  public boolean isQuiet() {
    return quiet;
  }


  final static class Module {

    final String name;
    final String input;
    final String outputPath;

    Module(final String name, final String input, final String outputPath) {
      this.name = name;
      this.input = input;
      this.outputPath = outputPath;
    }

  }


  final static class Replace {

    final String search;
    final String replacement;

    Replace(final String search, final String replacement) {
      this.search = search;
      this.replacement = replacement;
    }

  }

}
