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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Ansi_colors.Type;
import org.fedoraproject.javapackages.validator.Validator.Test_result;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author Marián Konček
 */
public class Package_test
{
	static private Ansi_colors.Decorator color_decorator = new Ansi_colors.No_decorator();
	static final Ansi_colors.Decorator color_decorator()
	{
		return color_decorator;
	}
	
	/// TODO integrate this into the normal rule - validator - test-result workflow
	static final Rule symlink_rule = new Rule();
	static final Validator symlink_validator = new Validator()
	{
		@Override
		public String to_xml()
		{
			return null;
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			return null;
		}
	};
	
	static
	{
		symlink_rule.name = "symbolic link resolver";
		symlink_validator.rule = symlink_rule;
	};
	
	static class Arguments
	{
		@Parameter(names = {"-h", "--help"}, help = true, description =
				"Display help.")
		boolean help = false;
		
		@Parameter(names = {"-o", "--output"}, description =
				"The file to write the output to. " +
				"If not provided then outputs to the standard output.")
		String output_file = null;
		
		@Parameter(names = {"-c", "--config"}, description =
				"The file to read the configuration from.")
		String config_file = null;
		
		@Parameter(names = {"-f", "--files"}, variableArity = true, description =
				"The list of .rpm files to validate.")
		ArrayList<String> test_files = new ArrayList<>();
		
		@Parameter(names = {"-i", "--input"}, description =
				"The file to read the list of input files from.")
		String input_file = null;
		
		@Parameter(names = {"-r", "--color"}, description =
				"Print colored output.")
		boolean color = false;
		
		@Parameter(names = {"-v", "--verbose"}, description =
				"Print more detailed output (affected by --color as well).")
		boolean verbose = false;
		
		@Parameter(names = {"-n", "--only-failed"}, description =
				"Print only failed test cases.")
		boolean only_failed = false;
		
		@Parameter(names = {"-d", "--dump-config"}, description =
				"Print the XML configuration.")
		boolean dump_config = false;
	}
	
	private static void print_usage(JCommander jcommander)
	{
		jcommander.usage();
		System.out.println("    " +
				"If neither -i nor -f is provided then the list of " +
				"validated files is read from the standard input.");
	}
	
	public static void main(String[] args) throws Exception
	{
		var arguments = new Arguments();
		var jcommander = JCommander.newBuilder().addObject(arguments).build();
		jcommander.parse(args);
		
		if (arguments.help)
		{
			print_usage(jcommander);
			return;
		}
		
		if (arguments.config_file == null)
		{
			System.err.println("error: Configuration file not specified, see usage with -h");
			print_usage(jcommander);
			return;
		}
		
		if (arguments.color)
		{
			Package_test.color_decorator = new Ansi_colors.Default_decorator();
		}
		
		final var config = new Config(new FileInputStream(arguments.config_file));
		
		if (arguments.dump_config)
		{
			System.out.println(config.to_xml());
			return;
		}
		
		try (PrintStream output = arguments.output_file != null ?
				new PrintStream(arguments.output_file) : System.out)
		{
			if (arguments.test_files.isEmpty())
			{
				InputStream is;
				
				if (arguments.input_file != null)
				{
					is = new FileInputStream(arguments.input_file);
				}
				else
				{
					is = System.in;
					System.err.println("Reading list of validated files from the standard input");
				}
				
				try (var br = new BufferedReader(new InputStreamReader(is)))
				{
					br.lines().forEachOrdered((filename) ->
					{
						var path = Paths.get(filename);
						
						if (! path.isAbsolute())
						{
							if (arguments.input_file != null)
							{
								filename = Paths.get(arguments.input_file).resolveSibling(path).toString();
							}
							else
							{
								filename = Paths.get(System.getProperty("user.dir")).resolve(path).toString();
							}
						}
						
						arguments.test_files.add(filename);
					});
				}
			}
			
			class Rpm_file implements Comparable<Rpm_file>
			{
				final String rpm_name;
				final String file_name;
				
				public Rpm_file(String rpm_name, String file_name)
				{
					super();
					this.rpm_name = rpm_name;
					this.file_name = file_name;
				}
				
				public int compareTo(Rpm_file other)
				{
					return file_name.compareTo(other.file_name);
				};
			}
			
			/// The union of file paths present in all RPM files mapped to the RPM
			/// file names they are present in
			var files = new TreeMap<String, String>();
			
			/// The map of symbolic link names to their targets present in all RPM files
			var symlinks = new TreeMap<Rpm_file, String>();
			
			var test_results = new ArrayList<Test_result>();
			
			for (final String filename : arguments.test_files)
			{
				final var rpm_path = Paths.get(filename).toAbsolutePath().normalize();
				final String rpm_name = rpm_path.getFileName().toString();
				final var rpm_info = new RpmInfo(rpm_path);
				
				/// Prefix every message with the RPM file name
				test_results.addAll(Rule.union(config.rules().stream()
						.filter(r -> r.is_applicable(rpm_info))).apply(rpm_path,
								color_decorator.decorate(rpm_name, Type.bright_cyan) + ": "));
				
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
						
						files.put(rpm_entry_name, rpm_name);
						
						var content = new byte[(int) rpm_entry.getSize()];
						rpm_is.read(content);
						
						if (rpm_entry.isSymbolicLink())
						{
							var target = new String(content, "UTF-8");
							target = Paths.get(rpm_entry_name).getParent().resolve(Paths.get(target)).normalize().toString();
							symlinks.put(new Rpm_file(rpm_name, rpm_entry_name), target);
						}
					}
				}
				catch (Exception ex)
				{
					throw new RuntimeException(MessageFormat.format("When reading file \"{0}\"", filename), ex);
				}
			}
			
			for (var pair : symlinks.entrySet())
			{
				var message = new StringBuilder();
				message.append(color_decorator().decorate("[Symlink]", Ansi_colors.Type.bold));
				final var rpm_file = files.get(pair.getValue());
				
				if (arguments.verbose)
				{
					message.append(MessageFormat.format(" (from \"{0}\"): ",
							color_decorator.decorate(pair.getKey().rpm_name, Type.bright_cyan)));
				}
				
				message.append(MessageFormat.format(" Symbolic link \"{0}\" ",
						color_decorator.decorate(pair.getKey().file_name, Ansi_colors.Type.cyan)));
				
				message.append(MessageFormat.format("points to \"{0}\" ",
						color_decorator.decorate(pair.getValue(), Ansi_colors.Type.yellow)));
				
				if (rpm_file != null)
				{
					message.append("and the target file ");
					message.append(color_decorator.decorate("exists", Ansi_colors.Type.green, Ansi_colors.Type.bold));
					
					if (arguments.verbose)
					{
						message.append(MessageFormat.format(" (provided by \"{0}\")",
								color_decorator.decorate(rpm_file, Type.bright_yellow)));
					}
				}
				else
				{
					message.append("but the target file ");
					message.append(color_decorator.decorate("does not exist", Ansi_colors.Type.red, Ansi_colors.Type.bold));
				}
				
				final var symlink_result = new Test_result((rpm_file != null), message.toString());
				symlink_result.validator = symlink_validator;
				test_results.add(symlink_result);
			}
			
			int test_number = test_results.isEmpty() ? 0 : 1;
			
			output.println(MessageFormat.format("{0}..{1}", test_number, Integer.toString(test_results.size())));
			
			for (var tr : test_results)
			{
				if (tr.result && arguments.only_failed)
				{
					continue;
				}
				
				output.println(MessageFormat.format("{0} {1} - {2}",
						(tr.result ? "ok" : "nok"), Integer.toString(test_number), tr.message()));
				
				if (arguments.verbose)
				{
					output.print(MessageFormat.format("[VERBOSE] from {0}", tr.validator.rule.description()));
					
					if (tr.verbose_text != null)
					{
						output.print(" ");
						output.print(tr.verbose_text.toString());
					}
					
					output.println();
				}
				
				++test_number;
			}
		}
	}
}
