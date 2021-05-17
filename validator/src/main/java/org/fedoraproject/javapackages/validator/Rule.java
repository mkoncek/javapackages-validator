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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmArchiveInputStream;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Ansi_colors.Type;
import org.fedoraproject.javapackages.validator.Validator.Test_result;

/**
 * @author Marián Konček
 */
public class Rule implements XML_writable
{
	public static abstract class Structured implements XML_writable
	{
		protected abstract boolean do_apply(List<Test_result> results, Path rpm_path, String prefix);
		
		public List<Test_result> apply(Path rpm_path, String prefix)
		{
			var result = new ArrayList<Test_result>();
			do_apply(result, rpm_path, prefix);
			return result;
		}
		
		public static class Name extends Structured
		{
			Rule rule;
			
			Name(Rule rule)
			{
				this.rule = rule;
			}
			
			@Override
			protected boolean do_apply(List<Test_result> results, Path rpm_path, String prefix)
			{
				try
				{
					if (rule.is_applicable(new RpmInfo(rpm_path)))
					{
						results.addAll(rule.apply(rpm_path, prefix));
						return true;
					}
				}
				catch (IOException ex)
				{
					throw new UncheckedIOException(ex);
				}
				
				return false;
			}
			
			@Override
			public void to_xml(StringBuilder result)
			{
				result.append("<rule>");
				result.append(rule.name);
				result.append("</rule>");
			}
		}
		
		public static class Tag extends Structured
		{
			String tag_name;
			ArrayList<Rule> rules;
			
			Tag(String tag_name, List<Rule> rules)
			{
				this.tag_name = tag_name;
				this.rules = new ArrayList<Rule>(rules.size());
				this.rules.addAll(rules);
			}
			
			@Override
			protected boolean do_apply(List<Test_result> results, Path rpm_path, String prefix)
			{
				boolean result = false;
				
				try
				{
					var rpm_info = new RpmInfo(rpm_path);
					
					for (var rule : rules)
					{
						if (rule.is_applicable(rpm_info))
						{
							results.addAll(rule.apply(rpm_path, prefix));
							result = true;
						}
					}
				}
				catch (IOException ex)
				{
					throw new UncheckedIOException(ex);
				}
				
				return result;
			}
			
			@Override
			public void to_xml(StringBuilder result)
			{
				result.append("<tag>");
				result.append(tag_name);
				result.append("</tag>");
			}
		}
		
		static abstract class Structured_list extends Structured
		{
			protected String node_tag;
			public ArrayList<Structured> rules = new ArrayList<>();
			
			Structured_list(String node_tag)
			{
				this.node_tag = node_tag;
			}
			
			@Override
			public void to_xml(StringBuilder result)
			{
				result.append("<" + node_tag + ">");
				rules.stream().forEach(r -> r.to_xml(result));
				result.append("</" + node_tag + ">");
			}
		}
		
		public static class All extends Structured_list
		{
			public All()
			{
				super("all");
			}
			
			@Override
			protected boolean do_apply(List<Test_result> results, Path rpm_path, String prefix)
			{
				boolean result = false;
				
				for (var structured : rules)
				{
					if (structured.do_apply(results, rpm_path, prefix))
					{
						result = true;
					}
				}
				
				return result;
			}
		}
		
		public static class Any extends Structured_list
		{
			public Any()
			{
				super("any");
			}
			
			@Override
			protected boolean do_apply(List<Test_result> results, Path rpm_path, String prefix)
			{
				boolean result = false;
				var stage_results = new ArrayList<Test_result>();
				var accumulated_results = new ArrayList<Test_result>();
				
				for (var structured : rules)
				{
					if (structured.do_apply(stage_results, rpm_path, prefix))
					{
						result = true;
						
						if (stage_results.stream().allMatch(tr -> tr.result))
						{
							results.addAll(stage_results);
							return result;
						}
						
						accumulated_results.addAll(stage_results);
						stage_results.clear();
					}
				}
				
				results.addAll(accumulated_results);
				
				return result;
			}
		}
	}
	
	public static interface Match extends Predicate<RpmInfo>, XML_writable
	{
	}
	
	public static class Rule_match implements Match
	{
		private Rule rule;
		
		public Rule_match(Rule rule)
		{
			super();
			this.rule = rule;
		}
		
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			return rule.match.test(rpm_info);
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<rule>");
			result.append(rule.name);
			result.append("</rule>");
		}
	}
	
	public static class All_match implements Match
	{
		@Override
		public boolean test(RpmInfo rpm_info)
		{
			return true;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			/// Empty
		}
	}
	
	public static class Not_match implements Match
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
		public void to_xml(StringBuilder result)
		{
			result.append("<not>");
			match.to_xml(result);
			result.append("</not>");
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
		
		protected final void partial_to_xml(StringBuilder result)
		{
			for (final var match : list)
			{
				match.to_xml(result);
			}
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
		public void to_xml(StringBuilder result)
		{
			result.append("<rule>");
			partial_to_xml(result);
			result.append("</rule>");
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
		public void to_xml(StringBuilder result)
		{
			result.append("<or>");
			partial_to_xml(result);
			result.append("</or>");
		}
	}
	
	public static class Method_match implements Match
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
		public void to_xml(StringBuilder result)
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
				
			case "getRelease":
				name = "release";
				break;
				
			default:
				throw new RuntimeException("Invalid " + this.getClass().getSimpleName() + ": " + getter.getName());
			}
			
			result.append("<" + name + ">");
			result.append(pattern.toString());
			result.append("</" + name + ">");
		}
	}
	
	String name = null;
	Match match = null;
	Map<String, Validator> validators = new LinkedHashMap<>();
	
	final boolean is_applicable(RpmInfo rpm_info)
	{
		return match.test(rpm_info);
	}
	
	public static class File_validator_args
	{
		CpioArchiveEntry entry;
		byte[] content;
		
		@Override
		public String toString()
		{
			return entry.getName();
		}
	}
	private void validate_files(Validator validator, Path rpm_path, String prefix,
			RpmInfo rpm_info, List<Test_result> result)
	{
		try (final var rpm_is = new RpmArchiveInputStream(rpm_path))
		{
			CpioArchiveEntry rpm_entry;
			while ((rpm_entry = rpm_is.getNextEntry()) != null)
			{
				var content = new byte[(int) rpm_entry.getSize()];
				rpm_is.read(content);
				
				var args = new File_validator_args();
				args.entry = rpm_entry;
				args.content = content;
				
				result.add(validator.validate(args, prefix, rpm_info));
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
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			return delegate.do_validate(value, rpm_info);
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<java-bytecode>");
			delegate.to_xml(result);
			result.append("</java-bytecode>");
		}
	}
	
	private void validate_java_bytecode(Validator validator, Path rpm_path,
			String prefix, RpmInfo rpm_info, List<Test_result> result)
	{
		final String jb_prefix = Config.message_map.get("java-bytecode");
		
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
									decor.decorate(class_name, Ansi_colors.Type.cyan) + ": " + jb_prefix);
							
							final var version = Short.toString(version_buffer.getShort());
							
							result.add(bc_validator.validate(version, prefix, rpm_info));
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
	
	private void validate_trait(RpmInfo rpm_info, Stream<String> stream, String name, String prefix, List<Test_result> result)
	{
		final Validator validator = validators.get(name);
		var full_prefix = prefix + Package_test.color_decorator().decorate(
		        Config.message_map.get(name), Type.bright_white);
		
		if (validator != null)
		{
			stream.map((s) -> validator.validate(s, full_prefix, rpm_info)).forEachOrdered(result::add);
		}
	}
	
	public final List<Test_result> apply(Path rpm_path, String prefix)
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
				validate_files(files, rpm_path, prefix, rpm_info, result);
			}
		}
		
		validate_trait(rpm_info, rpm_info.getProvides().stream(), "provides", prefix, result);
		validate_trait(rpm_info, rpm_info.getRequires().stream(), "requires", prefix, result);
		validate_trait(rpm_info, rpm_info.getConflicts().stream(), "conflicts", prefix, result);
		validate_trait(rpm_info, rpm_info.getObsoletes().stream(), "obsoletes", prefix, result);
		validate_trait(rpm_info, rpm_info.getRecommends().stream(), "recommends", prefix, result);
		validate_trait(rpm_info, rpm_info.getSuggests().stream(), "suggests", prefix, result);
		validate_trait(rpm_info, rpm_info.getSupplements().stream(), "supplements", prefix, result);
		validate_trait(rpm_info, rpm_info.getEnhances().stream(), "enhances", prefix, result);
		validate_trait(rpm_info, rpm_info.getOrderWithRequires().stream(), "order-with-requires", prefix, result);
		
		{
			final Validator rpm_file_size = validators.get("rpm-file-size-bytes");
			
			if (rpm_file_size != null)
			{
				result.add(rpm_file_size.
						validate(Long.toString(rpm_path.toFile().length()), prefix, rpm_info));
			}
		}
		{
			final Validator java_bytecode = validators.get("java-bytecode");
			
			if (java_bytecode != null)
			{
				validate_java_bytecode(java_bytecode, rpm_path, prefix, rpm_info, result);
			}
		}
		
		return result;
	}
	
	public String description()
	{
		return "rule \"" + name + "\"";
	}
	
	@Override
	public void to_xml(StringBuilder result)
	{
		result.append("<rule>");
		
		result.append("<name>" + name + "</name>");
		result.append("<match>" + match.to_xml() + "</match>");
		
		for (final var pair : validators.entrySet())
		{
			final String key = pair.getKey();
			
			result.append("<" + key + ">" + pair.getValue().to_xml() + "</" + key + ">");
		}
		
		result.append("</rule>");
	}
}
