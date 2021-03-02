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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Validator.Test_result;

/**
 * @author Marián Konček
 */
public class Rule
{
	static public interface Match extends Predicate<RpmInfo>
	{
		public String to_xml();
	}
	
	static public class Rule_match implements Match
	{
		protected Rule rule;
		public Boolean result;
		
		public Rule_match(Rule rule)
		{
			super();
			this.rule = rule;
		}
		
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			return result;
		}

		@Override
		public String to_xml()
		{
			return MessageFormat.format("<{0}>{1}</{0}>", "rule", rule.name);
		}
	}
	
	static public class All_match implements Match
	{
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			return true;
		}

		@Override
		public String to_xml()
		{
			return "";
		}
	}
	
	static public class Not_match implements Match
	{
		protected Match match;
		
		public Not_match(Match match)
		{
			super();
			this.match = match;
		}
		
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			return ! match.test(rpm_info);
		}
		
		@Override
		public String to_xml()
		{
			return MessageFormat.format("<{0}>{1}</{0}>", "not", match.to_xml());
		}
	}
	
	static protected abstract class List_match implements Match
	{
		ArrayList<Match> list;
		
		List_match(List<Match> list)
		{
			super();
			
			if (list.isEmpty())
			{
				throw new RuntimeException("Constructing a list match with no content");
			}
			
			this.list = new ArrayList<>(list);
		}
		
		protected final String partial_to_xml()
		{
			var result = new StringBuilder();
			
			for (final var match : list)
			{
				result.append(match.to_xml());
			}
			
			return result.toString();
		}
	}
	
	static class And_match extends List_match
	{
		public And_match(List<Match> list)
		{
			super(list);
		}
		
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			return list.stream().allMatch((m) -> m.test(rpm_info));
		}
		
		@Override
		public String to_xml()
		{
			return MessageFormat.format("<{0}>{1}</{0}>", "and", partial_to_xml());
		}
	}
	
	static class Or_match extends List_match
	{
		public Or_match(List<Match> list)
		{
			super(list);
		}
		
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			return list.stream().anyMatch((m) -> m.test(rpm_info));
		}
		
		@Override
		public String to_xml()
		{
			return MessageFormat.format("<{0}>{1}</{0}>", "or", partial_to_xml());
		}
	}
	
	static public class Method_match implements Match
	{
		Method getter;
		Pattern pattern;
		
		public Method_match(Method getter, Pattern pattern)
		{
			super();
			this.getter = getter;
			this.pattern = pattern;
		}
		
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			try
			{
				return pattern.matcher((String) getter.invoke(rpm_info)).matches();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public String to_xml()
		{
			String name = null;
			
			switch (getter.getName())
			{
			case "getName":
				name = "name";
				break;
				
			case "getArch":
				name = "arch";
				break;
				
			default:
				throw new RuntimeException("Invalid " + this.getClass().getSimpleName());
			}
			
			return MessageFormat.format("<{0}>{1}</{0}>", name, pattern.toString());
		}
	}
	
	String name = null;
	Match match = null;
	Map<String, Validator> validators = new LinkedHashMap<>();
	
	final boolean is_applicable(RpmInfo rpm_info)
	{
		return match.test(rpm_info);
	}
	
	private void validate_files(Validator validator, Path rpm_path, String prefix, List<Test_result> result)
	{
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
				
				result.add(validator.validate(rpm_entry_name, prefix));
			}
		}
		catch (IOException ex)
		{
			throw new UncheckedIOException(ex);
		}
	}
	
	static private class Java_bytecode_validator extends Validator
	{
		Validator delegate;
		
		Java_bytecode_validator(Validator delegate)
		{
			super(delegate);
			this.delegate = delegate;
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			return delegate.do_validate(value);
		}

		@Override
		public String to_xml()
		{
			return MessageFormat.format("<{0}>{1}</{0}>", "java-bytecode", delegate.to_xml());
		}
		
	}
	
	private void validate_java_bytecode(Validator validator, Path rpm_path, final String prefix, List<Test_result> result)
	{
		try (final var rpm_is = new RpmArchiveInputStream(rpm_path))
		{
			CpioArchiveEntry rpm_entry;
			while ((rpm_entry = rpm_is.getNextEntry()) != null)
			{
				var content = new byte[(int) rpm_entry.getSize()];
				rpm_is.read(content);
				
				if (! rpm_entry.isSymbolicLink() && rpm_entry.getName().endsWith(".jar"))
				{
					final String jar_name = rpm_entry.getName().substring(1);
					
					var jar_stream = new JarArchiveInputStream(new ByteArrayInputStream(content));
					
					JarArchiveEntry jar_entry;
					while ((jar_entry = jar_stream.getNextJarEntry()) != null)
					{
						final String class_name = jar_entry.getName();
						
						if (class_name.endsWith(".class"))
						{
							/// Read 6-th and 7-th bytes which indicate the
							/// .class bytecode version
							jar_stream.skip(6);
							var version_buffer = ByteBuffer.allocate(2);
							jar_stream.read(version_buffer.array());
							
							final var bc_validator = new Java_bytecode_validator(validator);
							
							final var decor = Package_test.color_decorator();
							bc_validator.prefix(
									decor.decorate(jar_name, Ansi_colors.Type.bright_magenta) + ": " +
									decor.decorate(class_name, Ansi_colors.Type.cyan) + ": ");
							
							final var version = Short.toString(version_buffer.getShort());
							
							result.add(bc_validator.validate(version, prefix));
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public final List<Test_result> apply(final Path rpm_path, final String prefix)
	{
		RpmInfo rpm_info;
		
		try
		{
			rpm_info = new RpmInfo(rpm_path);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		var result = new ArrayList<Test_result>();
		
		{
			final Validator files = validators.get("files");
			
			if (files != null)
			{
				validate_files(files, rpm_path, prefix, result);
			}
		}
		{
			final Validator provides = validators.get("provides");
			
			if (provides != null)
			{
				rpm_info.getProvides().stream().map((s) -> provides.validate(s, prefix)).forEachOrdered(result::add);
			}
		}
		{
			final Validator requires = validators.get("requires");
			
			if (requires != null)
			{
				rpm_info.getRequires().stream().map((s) -> requires.validate(s, prefix)).forEachOrdered(result::add);
			}
		}
		{
			final Validator obsoletes = validators.get("obsoletes");
			
			if (obsoletes != null)
			{
				throw new RuntimeException("Obsoletes not implemented");
			}
		}
		{
			final Validator rpm_file_size = validators.get("rpm-file-size-bytes");
			
			if (rpm_file_size != null)
			{
				result.add(rpm_file_size.validate(Long.toString(rpm_path.toFile().length()), prefix));
			}
		}
		{
			final Validator java_bytecode = validators.get("java-bytecode");
			
			if (java_bytecode != null)
			{
				validate_java_bytecode(java_bytecode, rpm_path, prefix, result);
			}
		}
		
		return result;
	}
	
	public static Rule union(Stream<Rule> rules)
	{
		final var result = new Rule()
		{
			List<String> rule_names = new ArrayList<>();
			
			@Override
			public String description()
			{
				return "a union rule of " + rule_names.stream()
						.map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));
			}
		};
		
		rules.forEach(rule ->
		{
			result.rule_names.add(rule.name);
			
			for (final var pair : rule.validators.entrySet())
			{
				final String key = pair.getKey();
				var result_validator = (Validator.Any_validator) result.validators.get(key);
				
				if (result_validator == null)
				{
					result_validator = new Validator.Any_validator(new ArrayList<Validator>());
					result_validator.rule = result;
					result.validators.put(key, result_validator);
				}
				
				Validator validator = pair.getValue();
				
				/// JDK 14
				// if (validator instanceof Validator.Any_validator any_validator)
				// {
				// 	result_validator.list.addAll(any_validator.list);
				// }
				
				if (validator instanceof Validator.Any_validator)
				{
					result_validator.list.addAll(((Validator.Any_validator) validator).list);
				}
				else
				{
					result_validator.list.add(validator);
				}
			}
		});
		
		return result;
	}
	
	public String description()
	{
		return "rule \"" + name + "\"";
	}
	
	public String to_xml()
	{
		final var result = new StringBuilder();
		
		result.append("<rule>");
		
		result.append("<name>" + name + "</name>");
		result.append("<match>" + match.to_xml() + "</match>");
		
		for (final var pair : validators.entrySet())
		{
			final String key = pair.getKey();
			
			result.append("<" + key + ">" + pair.getValue().to_xml() + "</" + key + ">");
		}
		
		result.append("</rule>");
		
		return result.toString();
	}
}
