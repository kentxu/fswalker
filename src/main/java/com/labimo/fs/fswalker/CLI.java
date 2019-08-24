package com.labimo.fs.fswalker;



import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.labimo.fs.fswalker.FSEntryWriter.OPTION;


public class CLI {
	private static final Logger LOGGER = LogManager.getLogger(CLI.class.getName());

	public static void main(String[] args) {
		LOGGER.warn("startiing FSWalker CLI");
		CommandLine cmd = processOptions(args);
		if (cmd == null) {
			System.exit(1);
		}

		long startTime = System.currentTimeMillis();
		List<String> fileArgs = cmd.getArgList();
		List<Path> paths = processPathList(fileArgs);

		// configure visitor
		FSVisitor visitor = new FSVisitor();
		EnumSet<FSVisitorOption> visitorOptions = EnumSet.noneOf(FSVisitorOption.class);
		if (cmd.hasOption("hc")) {
			String hashType = "hash_" + cmd.getOptionValue("hc");
			if (StringUtils.equalsIgnoreCase(FSVisitorOption.HASH_SIZE20K.name(), hashType)) {
				visitorOptions.add(FSVisitorOption.HASH_SIZE20K);
			}
		}
		visitor.setOptions(visitorOptions);

		// configure writer
		FSEntryWriter fswriter = null;

		EnumSet<OPTION> writerOptions = EnumSet.noneOf(OPTION.class);

		if (cmd.hasOption("o")) {
			Path outFile = FileSystems.getDefault().getPath(cmd.getOptionValue("o"));
			try {
				Files.createFile(outFile);
				fswriter = new DefaultFSEntryWriter(outFile);
			} catch (IOException e) {
				System.out.println("unable to write to " + outFile);
				LOGGER.error(e);
				System.exit(2);
			}
		} else if (cmd.hasOption("q")) {
			fswriter = null;// no writer
		} else {
			fswriter = new DefaultFSEntryWriter(System.out);
		}
		if (cmd.hasOption('s')) {
			writerOptions.add(OPTION.INCLUDE_DIR);
		}
		if (cmd.hasOption('l')) {
			writerOptions.addAll(EnumSet.of(OPTION.SHOW_DATE, OPTION.SHOW_SIZE, OPTION.SHOW_TYPE));
		}

		if (cmd.hasOption("hc")) {
			writerOptions.add(OPTION.SHOW_HASH);
		}

		if (cmd.hasOption('h')) {
			writerOptions.add(OPTION.HUMAN_SHORTFORMAT);
		}
		if (fswriter != null)
			fswriter.setOptions(writerOptions);

		// configure walker
		FSWalker walker = new DefaultFSWalker(visitor);
		int maxDepth = Integer.MAX_VALUE;
		if (cmd.hasOption('d')) {
			try {
				String val = cmd.getOptionValue('d');
				maxDepth = Integer.parseInt(val);
			} catch (Exception e) {
				LOGGER.debug("invalid option value for \"d\"");
			}
		}

		maxDepth = maxDepth < 0 ? 0 : maxDepth;

		// walk
		visitor.setWriter(fswriter);

		try (FSEntryWriter thisWriter = fswriter) {
			for (Path p : paths) {
				walker.walk(p, maxDepth);
			}
		}
		long endTime = System.currentTimeMillis();
		if (cmd.hasOption('c')) {
			System.out.println("Summary: \n");
			if (cmd.hasOption('h')) {
				System.out.println("Total size: " + FileUtils.byteCountToDisplaySize(visitor.getSize()));
			} else {
				System.out.println("Total size: " + visitor.getSize());
			}
			System.out.println("Directories: " + visitor.getDirCount());
			System.out.println("Files: " + visitor.getFileCount());
			System.out.println("Errors: " + visitor.getFileErrorCount());
			System.out.println("Completed in " + (endTime - startTime)/1000F  + " seconds");
		}
	}

	private static List<Path> processPathList(List<String> fileArgs) {
		ArrayList<Path> paths = new ArrayList<Path>();
		for (String p : fileArgs) {
			try {
				Path path = FileSystems.getDefault().getPath(p);
				if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
					paths.add(path);
				} else {
					throw new Exception("path does not exist: " + path);
				}
			} catch (Exception e) {
				System.out.println("invalid path ignored: " + p);
			}
		}
		return paths;
	}

	private static CommandLine processOptions(String[] args) {
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();
		Option depthOption = Option.builder("d").longOpt("depth").required(false).argName("depth").hasArg()
				.desc("max scan depth. Default to 0 (MAX).").build();
		options.addOption(depthOption);
		options.addOption("q", "quiet", false, "no console output");
		options.addOption("c", "console", false,
				"display a summary to console output. This is not affected by the 'q' flag");
		options.addOption("h", "human", false, "human readable output");
		options.addOption("l", "detail", false, "include detailed information for each file");
		options.addOption("s", "directory", false, "include directories");

		Option hashOption = Option.builder("hc").longOpt("hash").required(false).argName("hash type").hasArg()
				.desc("generate a content hash code.\n size20k: size + first 20k data").build();
		options.addOption(hashOption);

		Option outfile = Option.builder("o").longOpt("output").required(false).argName("file").hasArg()
				.desc("output to a file").build();
		options.addOption(outfile);
		CommandLine cmdLine = null;
		try {
			cmdLine = parser.parse(options, args, false);
		} catch (ParseException exp) {
			System.out.println("Unexpected exception:" + exp.getMessage());

		}

		if (cmdLine == null || cmdLine.getArgList().size() == 0) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("FSWalker", options);
		}
		return cmdLine;

	}

}
