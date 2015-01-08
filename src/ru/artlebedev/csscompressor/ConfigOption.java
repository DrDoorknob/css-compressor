/**
 * Author: Alexander Samilyak (aleksam241@gmail.com)
 * Created: 2012.02.19
 * Copyright 2012 Art. Lebedev Studio. All Rights Reserved.
 */

package ru.artlebedev.csscompressor;


public enum ConfigOption {

  ROOT(
      "root", "string",
      new Updater(){
        @Override
        public void update(final String rootPath, final ConfigBuilder builder) {
          builder.setRootPath(rootPath);
        }
      },
      "."), // relative to location of config json file

  INPUT_PATH(
		  "inpath", "string",
		  new Updater() {
			  @Override
			public void update(final String value, final ConfigBuilder builder) {
				  builder.setInputPath(value);
			  }
		  }),

  OUTPUT_PATH(
      "outpath", "string",
      new Updater(){
        @Override
        public void update(final String outputPath, final ConfigBuilder builder){
          builder.setOutputPath(outputPath);
        }
      }),

  OUTPUT_WRAPPER(
      "outwrapper", "string or array",
      new Updater(){
        @Override
        public void update(final String outputWrapper, final ConfigBuilder builder){
          builder.setOutputWrapper(outputWrapper);
        }

        /**
         * output-wrapper can also be an array of strings that should be
         * concatenated together.
         */
        /* JSON dependency temporarily removed, will be changed over to Commons net.sf.json
         * gsondeprecate
        @Override
        public void update(
            final JsonArray outputWrapperParts, final ConfigBuilder builder) {

          StringBuilder outputWrapper = new StringBuilder();
          for (JsonElement item : outputWrapperParts) {
            String part = Utils.jsonElementToStringOrNull(item);
            if (part == null) {
              throw new RuntimeException(
                  String.format(
                      "Some parts of array '%s' are not string: %s",
                      ConfigOption.OUTPUT_WRAPPER.getName(), item));
            }

            outputWrapper.append(part);
          }
          update(outputWrapper.toString(), builder);
        }*/
      }),

  /*
   * gsondeprecate
   * MODULES(
      "modules", "object",
      new Updater(){
        @Override
        public void update(final JsonObject modules, final ConfigBuilder builder){
          builder.setModulesInfo(modules);
        }
      }),*/

  CHARSET(
      "charset", "string",
      new Updater(){
        @Override
        public void update(final String charset, final ConfigBuilder builder){
          builder.setCharset(charset);
        }
      },
      "UTF-8"),

  PREPROCESS(
      "preprocess", "string",
      new Updater(){
        @Override
        public void update(final String preprocessCommand, final ConfigBuilder builder){
          builder.setPreprocessCommand(preprocessCommand);
        }
      },
      "UTF-8")
  ;



  private final String name;

  private final String allowedTypes;

  private final Updater updater;

  private final String defaultValue;


  ConfigOption(final String name, final String allowedTypes, final Updater updater) {
    this(name, allowedTypes, updater, null);
  }

  ConfigOption(
      final String name, final String allowedTypes, final Updater updater, final String defaultValue) {

    this.name = name;
    this.allowedTypes = allowedTypes;
    this.updater = updater;
    this.defaultValue = defaultValue;

    this.updater.setOptionName(name);
    this.updater.setOptionAllowedTypes(allowedTypes);
  }

  public String getName() {
    return name;
  }

  public String getAllowedTypes() {
    return allowedTypes;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void update(final Object cfgValue, final ConfigBuilder builder) {
    updater.update(cfgValue, builder);
  }



  private static class Updater {

    private String optionName;

    private String optionAllowedTypes;


    public void update(final boolean value, final ConfigBuilder builder) {
      throwExceptionOnOptionWrongType(Boolean.toString(value));
    }

    public void update(final Number value, final ConfigBuilder builder) {
      throwExceptionOnOptionWrongType(value.toString());
    }

    public void update(final String value, final ConfigBuilder builder) {
      throwExceptionOnOptionWrongType(value);
    }

    /*gsondeprecate
     * public void update(final JsonArray value, final ConfigBuilder builder) {
      throwExceptionOnOptionWrongType(value.toString());
    }

    public void update(final JsonObject value, final ConfigBuilder builder) {
      throwExceptionOnOptionWrongType(value.toString());
    }*/

    private void update(final Object object, final ConfigBuilder builder) {

    	/*gsondeprecate
    	 * if (object instanceof JsonElement) {
    		JsonElement jsonElement = (JsonElement)object;
	      if (jsonElement.isJsonPrimitive()) {
	        JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();

	        if (primitive.isString()) {
	          update(primitive.getAsString(), builder);
	        } else if (primitive.isBoolean()) {
	          update(primitive.getAsBoolean(), builder);
	        } else if (primitive.isNumber()) {
	          update(primitive.getAsNumber(), builder);
	        }
	      } else if (jsonElement.isJsonArray()) {
	        update(jsonElement.getAsJsonArray(), builder);
	      } else if (jsonElement.isJsonObject()) {
	        update(jsonElement.getAsJsonObject(), builder);
	      }
    	}
    	else if (object instanceof String) {
    		update(object.toString(), builder);
    	}*/
    	if (object != null) {
    		update(object.toString(), builder);
    	}
    }

    public void setOptionName(final String name) {
      this.optionName = name;
    }

    public void setOptionAllowedTypes(final String types) {
      this.optionAllowedTypes = types;
    }

    private void throwExceptionOnOptionWrongType(final String jsonElementValue) {
      throw new IllegalArgumentException(
          String.format(
              "Option '%s' must be %s. Found: %s",
              this.optionName, this.optionAllowedTypes, jsonElementValue));
    }
  }

}
