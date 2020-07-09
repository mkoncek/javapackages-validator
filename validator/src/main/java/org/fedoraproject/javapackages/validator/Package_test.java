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
package org.fedoraproject.javapackages.validator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.text.MessageFormat;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmInfo;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.fedoraproject.javapackages.validator.Validator.Test_result;

/**
 * @author Marián Konček
 */
public class Package_test
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
		var jcommander = JCommander.newBuilder().addObject(arguments).build();
		jcommander.parse(args);
		
		if (arguments.help)
		{
			System.out.println("javapackage-validator");
			System.out.println("Options:");
			System.out.println("  -h, --help                  " +
					"Display help."
			);
			System.out.println();
			System.out.println("  -o, --output=FILE           " +
					"The file to write the output to. " +
					"If not provided then outputs to the standard output."
			);
			System.out.println();
			System.out.println("  -c, --config=FILE           " +
					"The file to read the configuration from."
			);
			System.out.println();
			System.out.println("  -f, --files=FILES...        " +
					"The list of .rpm files to validate."
			);
			System.out.println("  -i, --input=FILE            " +
					"The file to read the list of input files from."
			);
			System.out.println("                              " +
					"If neither -i nor -f is provided then the list of " +
					"validated files is read from the standard input."
			);
			
			return;
		}
		
		if (arguments.config_file == null)
		{
			System.err.println("error: Missing --config file");
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
						var path = Paths.get(filename);
						
						if (! path.isAbsolute())
						{
							filename = Paths.get(arguments.input_file).getParent().resolve(path).toString();
						}
						
						arguments.test_files.add(filename);
					});
				}
			}
			
			var config = new Config(new FileInputStream(arguments.config_file));
			
			/// The union of file paths present in all RPM files
			var files = new TreeSet<String>();
			
			/// The map of symbolic link names to their targets present in all RPM files
			var symlinks = new TreeMap<String, String>();
			var test_results = new ArrayList<Test_result>();
			
			for (final String filename : arguments.test_files)
			{
				final var rpm_path = Paths.get(filename).toAbsolutePath().normalize();
				final String rpm_name = rpm_path.getFileName().toString();
				final var rpm_info = new RpmInfo(rpm_path);
				final var applicable_rules = config.rules().stream()
						.filter((r) -> r.applies(rpm_info))
						.collect(Collectors.toCollection(ArrayList::new));
				
				/// Prefix every message with the RPM file name
				for (var rule : applicable_rules)
				{
					var results = rule.apply(rpm_path);
					
					for (var tr : results)
					{
						tr.prefix(rpm_name + ": ");
						test_results.add(tr);
					}
				}
				
				final var applicable_jar_validators = applicable_rules.stream()
						.map((r) -> r.jar_validator)
						.filter((jc) -> jc != null)
						.collect(Collectors.toCollection(ArrayList::new));
				
				try (final var rpm_is = new RpmArchiveInputStream(rpm_path))
				{
					CpioArchiveEntry rpm_entry;
					while ((rpm_entry = rpm_is.getNextEntry()) != null)
					{
						String rpm_entry_name = rpm_entry.getName();
						
						if (rpm_entry_name.startsWith("./"))
						{
							rpm_entry_name = rpm_entry_name.substring(1);
						}
						
						files.add(rpm_entry_name);
						
						var content = new byte[(int) rpm_entry.getSize()];
						rpm_is.read(content);
						
						if (rpm_entry.isSymbolicLink())
						{
							var target = new String(content, "UTF-8");
							target = Paths.get(rpm_entry_name).getParent().resolve(Paths.get(target)).normalize().toString();
							symlinks.put(rpm_entry_name, target);
						}
						else
						{
							if (rpm_entry.getName().endsWith(".jar"))
							{
								final String jar_name = rpm_entry_name;
								
								try (var jar_stream = new JarArchiveInputStream(
										new ByteArrayInputStream(content)))
								{
									for (var jv : applicable_jar_validators)
									{
										jv.accept(new Jar_validator.Visitor()
										{
											@Override
											public void visit(Test_result result, String entry)
											{
												result.message = entry + ": " + result.message;
												test_results.add(result);
											}
										}, jar_stream, rpm_name + ": " + jar_name);
									}
								}
							}
						}
					}
				}
			}
			
			for (var pair : symlinks.entrySet())
			{
				String message = "[Symlink]: ";
				final boolean result = files.contains(pair.getValue());
				
				if (result)
				{
					message += MessageFormat.format(
							"Symbolic link \"{0}\" points to \"{1}\" and the target file exists",
							pair.getKey(), pair.getValue());
				}
				else
				{
					message += MessageFormat.format(
							"Symbolic link \"{0}\" points to \"{1}\" but the target file does not exist",
							pair.getKey(), pair.getValue());
				}
				
				test_results.add(new Test_result(result, message));
			}
			
			int test_number = test_results.isEmpty() ? 0 : 1;
			
			output.println(MessageFormat.format("{0}..{1}", test_number, Integer.toString(test_results.size())));
			
			for (var tr : test_results)
			{
				output.println(MessageFormat.format("{0} {1} - {2}",
						(tr.result ? "ok" : "nok"), Integer.toString(test_number), tr.message));
				++test_number;
			}
		}
	}
}
