/**
 * Author: Alexander Samilyak (aleksam241@gmail.com)
 * Created: 2012.02.18
 * Copyright 2012 Art. Lebedev Studio. All Rights Reserved.
 */

package ru.artlebedev.csscompressor;

import java.util.Date;


public final class Main {


  public static void main(final String args[]) throws Exception {

    ConfigBuilder builder = new CliConfigBuilder(args);
    //ConfigBuilder builder = new JsonConfigBuilder(cmdLine);
    Config config = builder.build();
    Date now = new Date();
    System.out.println("Building CSS from input directory " + builder.getFullInputPath() + " ...");
    new CssCompressor(config).compress();
    Date finish = new Date();
    long finishTime = finish.getTime() - now.getTime();
    System.out.println("Finished building CSS in " + finishTime + "ms");
  }

}
