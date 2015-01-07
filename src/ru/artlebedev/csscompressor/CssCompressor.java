/**
 * Author: Alexander Samilyak (aleksam241@gmail.com)
 * Created: 2012.02.11
 * Copyright 2012 Art. Lebedev Studio. All Rights Reserved.
 *
 * Fork by David Keyworth (david.keyworth@americanwell.com)
 * Copyright 2015 American Well.
 */

package ru.artlebedev.csscompressor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;


public class CssCompressor {

	/*
		CSS imports are allowed in 2 syntaxes:
			1. @import url("style.css")
			2. @import "style.css"
		So this regex is expecting a valid input CSS.

		TODO(samilyak): Consider using more bulletproof regex -
		it tracks a paring of quotes and parenthesis
		@import\s+(?:url\(\s*(?=[^;$]+?\)))?(["']?)([\w\\\/\-\_\.]+?\.css)\1(?!["'])[^;$]*?(;|$)

		TODO(samilyak): Prevent from matching @import inside CSS comments
	*/
	private static final Pattern cssImportPattern = Pattern.compile(
			"@import\\s+" +

			// optional 'url(' part (non capturing subpattern) with optional quote
			"(?:url\\(\\s*)?" + "[\"']?" +

			// file path ending with '.css' in capturing subpattern 1
			// word characters, slashes, dash, underscore, dot,
			// colon and question mark (possible for absolute urls) are allowed
			"([\\w\\\\/\\-_.:?]+?\\.css)" +

			// the rest of the line until semicolon or line break
			"[^;$]*?(;|$)",
			Pattern.MULTILINE);

	// Pattern without escaping slashes: /\*([\s\S]*)\*/
	private static final Pattern cssCommentPattern = Pattern.compile("/\\*([\\s\\S]*)\\*/");

	// Pattern without escaping slashes: url\((['"])?((?:[\w\.]+/)*\w+\.([a-zA-Z]{2,4}))(['"])?\)
	// Matches the url() section of: "background-image: url("../thing.png");"
	// Group 1: First quotation mark. (optional)
	// Group 2: The URL of the image/resource
	// Group 3: The file's extension
	// Group 4: The closing quotation mark. (optional)
	private static final Pattern cssUrlPattern = Pattern.compile("url\\((['\"])?((?:[\\w\\.]+/)*\\w+\\.([a-zA-Z]{2,4}))(['\"])?\\)");

	private static final int MAX_COMMENT_SURROUND_LENGTH = 500;

	private final Config config;



	public CssCompressor(final Config config) {
		this.config = config;
	}


	public void compress() throws IOException {
		for (Config.Module module : config.getModules()) {
			prepareModuleOutputCatalog(module);

			String css = processCssRootFile(module.input, true);

			com.yahoo.platform.yui.compressor.CssCompressor compressor =
					new com.yahoo.platform.yui.compressor.CssCompressor(
							new StringReader(css));

			StringWriter stringWriter = new StringWriter();
			compressor.compress(stringWriter, -1);
			css = stringWriter.toString();

			css = applyReplaces(css);
			css = wrapCssWithOutputWrapper(css);

			Utils.writeToFile(module.outputPath, css, config.getCharset());
		}
	}

	private void prepareModuleOutputCatalog(final Config.Module module) {
		File outputCatalog = new File(module.outputPath).getParentFile();
		if (outputCatalog != null) {
			// null means outputPath doesn't contain catalog part, just filename -
			// it's OK, we'll write to current catalog

			outputCatalog.mkdirs();
			if (!outputCatalog.exists()) {
				throw new RuntimeException(
						"Unable to write to catalog " + outputCatalog.getPath());
			}
		}
	}

	private String processCssRootFile(final String path, final boolean tryPreprocess)
			throws IOException {

		List<String> processedFiles = new ArrayList<String>(0);

		// The directory containing the CSS file representing the compression. ie, "css/layers/"
		Path rootPath = Paths.get(path).getParent();

		CssProcessingResult pathProcessingResult =
				processCssFile(rootPath, path, processedFiles, tryPreprocess);

		return pathProcessingResult.content;
	}


	private String applyReplaces(String css) {
		List<Config.Replace> replaces = config.getReplaces();

		if (replaces != null) {
			for (Config.Replace replace : replaces) {
				css = css.replaceAll(replace.search, replace.replacement);
			}
		}

		return css;
	}


	private String wrapCssWithOutputWrapper(String css) {
		if (config.getOutputWrapper() != null) {
			if (config.getOutputWrapper().contains(Config.OUTPUT_WRAPPER_MARKER)) {
				css = config.getOutputWrapper().replace(
						Config.OUTPUT_WRAPPER_MARKER, css);
			} else {
				throw new RuntimeException(
						String.format(
								"Option '%s' did not contain placeholder %s",
								ConfigOption.OUTPUT_WRAPPER.getName(),
								Config.OUTPUT_WRAPPER_MARKER));
			}
		}

		return css;
	}


	private CssProcessingResult processCssFile(final Path rootFilePath,
			final String path, final List<String> processedFiles, final boolean tryPreprocess)
			throws IOException {

		/*
			We need to prevent from processing same files more than once,
			to minify result build file and more importantly to avoid cyclic imports.
			That's why we need 2nd argument
			containing paths of already processed files.
		*/

		File fileAtPath = new File(path);
		Path fileDir = Paths.get(path).getParent();
		String fileCanonicalPath = fileAtPath.getCanonicalPath();
		String fileCatalog = fileAtPath.getParent();

		if (processedFiles.contains(fileCanonicalPath)) {
			return new CssProcessingResult("", processedFiles);
		}

		processedFiles.add(fileCanonicalPath);

		String inputContent;
		if (tryPreprocess && config.getPreprocessCommand() != null) {
			inputContent =
					preprocessAndGetOutput(config.getPreprocessCommand(), path);
		} else {
			inputContent = Utils.readFile(path, config.getCharset());
		}

		if (!fileDir.equals(rootFilePath)) {
			Path relPath = rootFilePath.relativize(fileDir).normalize();
			inputContent = rewriteRelativePaths(inputContent, relPath);
		}

		Matcher matcher = cssImportPattern.matcher(inputContent);

		StringBuffer stringResult = new StringBuffer();
		while(matcher.find()){

			// It's likely we've just found an import statement. HOWEVER, here we do a check to ensure that
			// it's not actually the inside of a comment. Keep in mind, CSS does not support single-line comments.

			int startIdx = matcher.start(0);
			int commentFindStartIndex = startIdx - MAX_COMMENT_SURROUND_LENGTH;
			Matcher commentFind = cssCommentPattern.matcher(inputContent.substring(
					Math.max(commentFindStartIndex, 0),
					Math.min(startIdx + MAX_COMMENT_SURROUND_LENGTH, inputContent.length())));
			boolean inComment = false;
			while (commentFind.find()) {
				if (commentFindStartIndex + commentFind.start() < startIdx &&
					commentFindStartIndex + commentFind.end() > startIdx) {
					inComment = true;
					break;
				}
			}
			if (inComment) {
				continue;
			}


			String importPath = matcher.group(1);

			String importFileContent = "";
			if (!isCssImportAbsolute(importPath)) {
				File importFile = new File(fileCatalog, importPath);
				CssProcessingResult importProcessingResult =
						processCssFile(rootFilePath, importFile.getPath(), processedFiles, false);

				importFileContent = importProcessingResult.content;
			}

			/**
			 * Do it like that (rather than simply
			 * matcher.appendReplacement(stringResult, importFileContent))
			 * because appendReplacement() is treating symbols \ and $ in its
			 * 2nd argument in a special regex specific way.
			 * So we need to avoid problem when source css
			 * content:'\2014\a0' is converted to content:'2014a0'
			 */
			matcher.appendReplacement(stringResult, "");
			stringResult.append(importFileContent);
		}
		matcher.appendTail(stringResult);


		return new CssProcessingResult(stringResult.toString(), processedFiles);
	}


	/**
	 * @param inputContent
	 * @param relPath The path that this CSS file will be retrieved from
	 * @return
	 */
	private String rewriteRelativePaths(final String inputContent, final Path relPath) {
		StringBuffer sb = new StringBuffer();

		Matcher matcher = cssUrlPattern.matcher(inputContent);

		while (matcher.find()) {

			/* Note: It would be possible to refactor the @import comment-watching so we don't
			replace commented URLs, but that's not really going to be an issue here. It's bad
			if we accidentally import a file, but it's not harmful to fix a URL inside of a comment.*/

			String quot1 = matcher.group(1);
			String quot2 = matcher.group(4);
			if (!((quot1 == null && quot2 == null) ||
					quot1.equals(quot2))) {
				System.out.println("Found likely url() candidate, but was not able to match quotes: " + matcher.group());
				matcher.appendReplacement(sb, "");
				sb.append(matcher.group());
				continue;
			}

			// URL will be a relative path like "../../images/icon.png"
			String url = matcher.group(2);
			Path urlPath = Paths.get(url);
			// Combine the relative path that moves us from "css/layers/renderingFile.css" to "css/requireFile.css"
			// WITH the relative path that moves us from "css/requireFile.css" to "images/icon.png"
			String newRelPath = relPath.resolve(urlPath).normalize().toString();

			String patternReplace = String.format("url(%s)", newRelPath);

			matcher.appendReplacement(sb, "");
			sb.append(patternReplace);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}


	private String preprocessAndGetOutput(final String command, final String path)
			throws IOException {

		// replace %s with a file path
		String expandedCommand = String.format(command, path);

		CommandLine commandLine = CommandLine.parse(expandedCommand);

		DefaultExecutor executor = new DefaultExecutor();
		executor.setWatchdog(new ExecuteWatchdog(30 * 1000));
		final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		executor.setStreamHandler(new PumpStreamHandler(stdout, stderr));

		if (!config.isQuiet()) {
			System.out.println(
					String.format(
							"INFO: executing preprocess command `%s`", expandedCommand));
		}

		try {
			executor.execute(commandLine);

			String innerErrors = stderr.toString(config.getCharset());
			if (innerErrors != null && !innerErrors.equals("")) {
				System.out.println(innerErrors);
			}

		} catch (IOException e) {
			throw new RuntimeException(
					String.format("Preprocessing file %s failed.", path) +
					"\n" + stderr.toString(config.getCharset()) +
					"\n" + e.getMessage());
		}

		return stdout.toString(config.getCharset());
	}


	private static boolean isCssImportAbsolute(final String path) {
		boolean isAbsoluteUri;
		try{
			URI uri = new URI(path);
			isAbsoluteUri = uri.isAbsolute();
		} catch (URISyntaxException e) {
			isAbsoluteUri = false;
		}

		return isAbsoluteUri || new File(path).isAbsolute();
	}



	private final static class CssProcessingResult {

		final String content;
		final List<String> processedFiles;

		public CssProcessingResult(
				final String content, final List<String> processedFiles){

			this.content = content;
			this.processedFiles = processedFiles;
		}

	}

}
