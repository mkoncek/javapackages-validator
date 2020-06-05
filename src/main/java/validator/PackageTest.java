/*-
 * Copyright (c) 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package validator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.text.MessageFormat;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmInfo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author Marián Konček
 */
public class PackageTest
{
	static final String error_separator = ";";
	
	static class Arguments
	{
		@Parameter(names = {"-h", "--help"}, help = true)
		private boolean help = false;
		
		@Parameter(names = {"-o", "--output"})
		String output_file = null;
		
		@Parameter(names = {"-c", "--config"})
		String config_file = null;
		
		@Parameter(names = {"-f", "--files"}, variableArity = true)
		ArrayList<String> test_files = new ArrayList<>();
		
		@Parameter(names = {"-i", "--input"})
		String input_file = null;
	}
	
	public static void main(String[] args) throws Exception
	{
		var arguments = new Arguments();
		JCommander.newBuilder().addObject(arguments).build().parse(args);
		
		if (arguments.help)
		{
			System.out.println("javapackage-validator version 0.0.1");
			System.out.println("Options:");
			System.out.println("  -h, --help                  " +
					"Display help."
			);
			System.out.println("  -o, --output=FILE           " +
					"The file to write the output to. " +
					"If not provided outputs to standard output."
			);
			System.out.println("  -c, --config=FILE           " +
					"The file to read the configuration from."
			);
			System.out.println();
			System.out.println("  -f, --files=FILES           " +
					"The list of .rpm files to validate."
			);
			System.out.println("  -i, --input=FILE            " +
					"The file to read the list of input files from."
			);
			System.out.println("                              " +
					"If neither -i nor -f is provided read the list of " +
					"validated files is read from the standard input."
			);
			
			return;
		}
		
		try (PrintStream output = arguments.output_file != null ?
				new PrintStream(arguments.output_file) : System.out)
		{
			if (arguments.test_files.isEmpty())
			{
				InputStream is = arguments.input_file != null ?
						new FileInputStream(arguments.input_file) : System.in;
				
				try (var br = new BufferedReader(new InputStreamReader(is)))
				{
					br.lines().forEach((filename) ->
					{
						arguments.test_files.add(filename);
					});
				}
			}
			
			var config = new Config(new FileInputStream(arguments.config_file));
			
			output.println("1.." + Integer.toString(arguments.test_files.size() + 1));
			
			/// The union of file paths present in all rpm files
			var files = new TreeSet<String>();
			
			/// The union of targets of all symbolic links present in rpm files
			var symlinks = new TreeSet<String>();
			
			int error_number = 1;
			for (; error_number < arguments.test_files.size() + 1; ++error_number)
			{
				final String filename = arguments.test_files.get(error_number - 1);
				final var rpm_path = Paths.get(filename);
				final var rpm_info = new RpmInfo(rpm_path);
				var errors = new ArrayList<String>();
				final var applicable_rules = config.rules().stream()
						.filter((r) -> r.applies(rpm_info))
						.collect(Collectors.toCollection(ArrayList::new));
				
				applicable_rules.stream()
						.map((r) -> r.apply(rpm_path))
						.filter(Predicate.not(List::isEmpty))
						.forEachOrdered(errors::addAll);
				
				try (final var rpm_is = new RpmArchiveInputStream(rpm_path))
				{
					CpioArchiveEntry rpm_entry;
					while ((rpm_entry = rpm_is.getNextEntry()) != null)
					{
						/// Remove the leading "."
						files.add(rpm_entry.getName().substring(1));
						
						var content = new byte[(int) rpm_entry.getSize()];
						rpm_is.read(content);
						
						if (rpm_entry.isSymbolicLink())
						{
							final var target = new String(content, "UTF-8");
							symlinks.add(target);
						}
						else
						{
							if (rpm_entry.getName().endsWith(".jar"))
							{
								try (var jar_stream = new JarArchiveInputStream(
										new ByteArrayInputStream(content)))
								{
									var jar_message = applicable_rules.stream()
											.map((r) -> r.jar_validator)
											.filter((jc) -> jc != null && ! jc.validate(jar_stream))
											.map((jc) -> jc.info())
											.collect(Collectors.joining(", "));
									
									if (! jar_message.isEmpty())
									{
										errors.add(jar_message);
									}
								}
							}
						}
					}
				}
				
				if (errors.isEmpty())
				{
					output.println(MessageFormat.format("ok {0} - {1}",
							Integer.toString(error_number), rpm_path.getFileName()));
				}
				else
				{
					output.print(MessageFormat.format("nok {0} - {1} - ",
							Integer.toString(error_number), rpm_path.getFileName()));
					output.println(errors.stream().collect(Collectors.joining(error_separator + " ")));
				}
			}
			
			{
				final var symlink_errors = symlinks.stream()
						.filter(Predicate.not(files::contains))
						.map((str) -> MessageFormat.format("File {0} is a dangling symbolic link", str))
						.collect(Collectors.joining(error_separator + " "));
				
				if (symlink_errors.isEmpty())
				{
					output.println(MessageFormat.format(
							"ok {0} - All symbolic links have been resolved", error_number));
				}
				else
				{
					output.print(MessageFormat.format(
							"nok {0} - dangling symbolic links - ", error_number));
					output.println(symlink_errors);
				}
				
				++error_number;
			}
		}
	}
}
