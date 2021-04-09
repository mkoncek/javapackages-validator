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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.fedoraproject.javadeptools.rpm.RpmInfo;
import org.fedoraproject.javapackages.validator.Ansi_colors.Type;
import org.fedoraproject.javapackages.validator.Rule.File_validator_args;

/**
 * @author Marián Konček
 */
abstract public class Validator implements Xml_writable
{
	static private final Ansi_colors.Decorator decor()
	{
		return Package_test.color_decorator();
	}
	
	static private int debug_nesting = 0;
	static private final String single_indent = "  ";
	static private String indent()
	{
		return single_indent.repeat(debug_nesting);
	}
	
	public Validator()
	{
	}
	
	public Validator(Validator delegate)
	{
		this.rule = delegate.rule;
	}
	
	static final class Test_result
	{
		Validator validator;
		boolean result;
		private StringBuilder message = new StringBuilder();
		StringBuilder verbose_text;
		
		public Test_result(boolean result)
		{
			this.result = result;
		}
		
		public Test_result(boolean result, String message)
		{
			this(result);
			this.message.append(message);
		}
		
		final String message()
		{
			return message.toString();
		}
	}
	
	Rule rule;
	private String message_prefix = "";
	
	protected abstract Test_result do_validate(Object value, RpmInfo rpm_info);
	
	public Validator prefix(String message_prefix)
	{
		this.message_prefix = message_prefix;
		return this;
	}
	
	public final Test_result validate(Object value, String prefix, RpmInfo rpm_info)
	{
		final var result = do_validate(value, rpm_info);
		result.message.insert(0, prefix + message_prefix);
		result.validator = this;
		
		if (result.result)
		{
			result.message.append(decor().decorate("passed", Type.green, Type.bold));
			result.message.append(" with value \"");
			result.message.append(decor().decorate(value, Type.yellow));
			result.message.append("\"");
		}
		else
		{
			result.message.append(decor().decorate("failed", Type.red, Type.bold));
			result.message.append(" with value \"");
			result.message.append(decor().decorate(value, Type.yellow));
			result.message.append("\"");
		}
		
		return result;
	}
	
	static protected String substitute(String string, RpmInfo rpm_info)
	{
		string = string.replace("#{NAME}", rpm_info.getName());
		return string;
	}
	
	static class Pass_validator extends Validator
	{
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var result = new Test_result(true);
			result.verbose_text = new StringBuilder(indent());
			result.verbose_text.append("validator <pass>");
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<pass/>");
		}
	}
	
	static class Fail_validator extends Validator
	{
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var result = new Test_result(false);
			result.verbose_text = new StringBuilder(indent());
			result.verbose_text.append("validator <fail>");
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<fail/>");
		}
	}
	
	static class Text_validator extends Validator
	{
		String string;
		
		public Text_validator(String string)
		{
			super();
			this.string = string;
		}
		
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var string = substitute(this.string, rpm_info);
			
			final var result = new Test_result(string.equals(value));
			result.verbose_text = new StringBuilder(indent());
			
			result.verbose_text.append("text \"");
			result.verbose_text.append(decor().decorate(string, Type.cyan));
			result.verbose_text.append("\" ");
			
			if (result.result)
			{
				result.verbose_text.append(decor().decorate("matches", Type.green, Type.bold));
			}
			else
			{
				result.verbose_text.append(decor().decorate("does not match", Type.red, Type.bold));
			}
			
			result.verbose_text.append(" value \"");
			result.verbose_text.append(decor().decorate(value, Type.yellow));
			result.verbose_text.append("\"");
			
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<text>");
			result.append(string);
			result.append("</text>");
		}
	}
	
	static class Regex_validator extends Validator
	{
		String pattern;
		
		public Regex_validator(String pattern)
		{
			super();
			this.pattern = pattern;
		}
		
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var pattern = substitute(this.pattern, rpm_info);
			
			final var result = new Test_result(value.toString().matches(pattern));
			result.verbose_text = new StringBuilder(indent());
			
			result.verbose_text.append("regex \"");
			result.verbose_text.append(decor().decorate(pattern, Type.cyan));
			result.verbose_text.append("\" ");
			
			if (result.result)
			{
				result.verbose_text.append(decor().decorate("matches", Type.green, Type.bold));
			}
			else
			{
				result.verbose_text.append(decor().decorate("does not match", Type.red, Type.bold));
			}
			
			result.verbose_text.append(" value \"");
			result.verbose_text.append(decor().decorate(value, Type.yellow));
			result.verbose_text.append("\"");
					
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<regex>");
			result.append(pattern.toString());
			result.append("</regex>");
		}
	}
	
	static class Int_range_validator extends Validator
	{
		long min;
		long max;
		
		Int_range_validator(long min, long max)
		{
			super();
			this.min = min;
			this.max = max;
		}
		
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var numeric = Long.parseLong(value.toString());
			
			final var result = new Test_result(min <= numeric && numeric <= max);
			result.verbose_text = new StringBuilder(indent());
			
			result.verbose_text.append("int-range [");
			result.verbose_text.append(decor().decorate(min == Long.MIN_VALUE ? "" : Long.toString(min)));
			result.verbose_text.append(decor().decorate(" - "));
			result.verbose_text.append(decor().decorate((max == Long.MAX_VALUE ? "" : Long.toString(max)), Type.cyan));
			result.verbose_text.append("] ");
			
			if (result.result)
			{
				result.verbose_text.append(decor().decorate("contains", Type.green, Type.bold));
			}
			else
			{
				result.verbose_text.append(decor().decorate("does not contain", Type.red, Type.bold));
			}
			
			result.verbose_text.append(" value \"");
			result.verbose_text.append(decor().decorate(value, Type.yellow));
			result.verbose_text.append("\"");
					
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<int-range>");
			result.append(min == Long.MIN_VALUE ? "" : Long.toString(min));
			result.append("-");
			result.append(max == Long.MAX_VALUE ? "" : Long.toString(max));
			result.append("</int-range>");
		}
	}
	
	static abstract class List_validator extends Validator
	{
		public ArrayList<Validator> list;
		
		protected List_validator(List<Validator> list)
		{
			super();
			this.list = new ArrayList<Validator>(list);
		}
		
		protected abstract void do_list_validate(Object value, RpmInfo rpm_info, Test_result result);
		
		protected final void partial_validate(Object value, RpmInfo rpm_info, Test_result result)
		{
			int offset = result.verbose_text.length();
			
			++debug_nesting;
			do_list_validate(value, rpm_info, result);
			--debug_nesting;
			
			var inserted = new StringBuilder();
			if (result.result)
			{
				inserted.append(decor().decorate("accepted", Type.green, Type.bold));
			}
			else
			{
				inserted.append(decor().decorate("rejected", Type.red, Type.bold));
			}
			inserted.append(" value \"");
			inserted.append(decor().decorate(value, Type.yellow));
			inserted.append("\": {");
			inserted.append(System.lineSeparator());
			result.verbose_text.insert(offset, inserted);
			result.verbose_text.append(indent());
			result.verbose_text.append("}");
		}
		
		protected final void partial_to_xml(StringBuilder result)
		{
			for (final var validator : list)
			{
				validator.to_xml(result);
			}
		}
	}
	
	static class All_validator extends List_validator
	{
		public All_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		protected void do_list_validate(Object value, RpmInfo rpm_info, Test_result result)
		{
			for (final var val : list)
			{
				final var test_result = val.do_validate(value, rpm_info);
				
				if (test_result.result == false)
				{
					result.result = false;
				}
				
				result.verbose_text.append(test_result.verbose_text);
				result.verbose_text.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var result = new Test_result(true);
			result.verbose_text = new StringBuilder(indent());
			result.verbose_text.append("validator <all> ");
			
			partial_validate(value, rpm_info, result);
			
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<all>");
			partial_to_xml(result);
			result.append("</all>");
		}
	}
	
	static class Any_validator extends List_validator
	{
		public Any_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		protected void do_list_validate(Object value, RpmInfo rpm_info, Test_result result)
		{
			for (final var val : list)
			{
				final var test_result = val.do_validate(value, rpm_info);
				
				if (test_result.result)
				{
					result.result = true;
				}
				
				result.verbose_text.append(test_result.verbose_text);
				result.verbose_text.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var result = new Test_result(false);
			result.verbose_text = new StringBuilder(indent());
			result.verbose_text.append("validator <any> ");
			
			partial_validate(value, rpm_info, result);
			
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<any>");
			partial_to_xml(result);
			result.append("</any>");
		}
	}
	
	static class None_validator extends List_validator
	{
		public None_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		protected void do_list_validate(Object value, RpmInfo rpm_info, Test_result result)
		{
			for (final var val : list)
			{
				final var test_result = val.do_validate(value, rpm_info);
				
				if (test_result.result)
				{
					result.result = false;
				}
				
				result.verbose_text.append(test_result.verbose_text);
				result.verbose_text.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var result = new Test_result(true);
			result.verbose_text = new StringBuilder(indent());
			result.verbose_text.append("validator <none> ");
			
			partial_validate(value, rpm_info, result);
			
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<none>");
			partial_to_xml(result);
			result.append("</none>");
		}
	}
	
	static class File_multivalidator extends Validator
	{
		ArrayList<File_validator> list;
		
		public File_multivalidator(List<File_validator> list)
		{
			this.list = new ArrayList<>(list);
		}
		
		@Override
		protected Test_result do_validate(Object value, RpmInfo rpm_info)
		{
			final var result = new Test_result(true);
			result.verbose_text = new StringBuilder();
			result.verbose_text.append("union validator of <files> ");
			++debug_nesting;
			
			for (var fv : list)
			{
				var tr = fv.do_validate_file((File_validator_args) value, rpm_info);
				
				if (! tr.result)
				{
					result.result = false;
					
					result.verbose_text.append(System.lineSeparator());
					result.verbose_text.append(single_indent);
					result.verbose_text.append("<file-rule> ");
					result.verbose_text.append(tr.verbose_text);
				}
			}
			
			--debug_nesting;
			
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			list.forEach((v) -> v.to_xml(result));
		}
	}
	
	static class File_validator implements Xml_writable
	{
		Match match;
		Validator name_validator = null;
		boolean want_directory = false;
		Optional<Validator> symlink_target = null;
		
		private static interface Match extends Predicate<CpioArchiveEntry>, Xml_writable
		{
		}
		
		public File_validator(String name_match)
		{
			if (name_match != null && ! name_match.isEmpty())
			{
				this.match = new Match()
				{
					@Override
					public void to_xml(StringBuilder result)
					{
						result.append(name_match);
					}
					
					@Override
					public boolean test(CpioArchiveEntry value)
					{
						return value.getName().matches(name_match);
					}
				};
			}
			else
			{
				this.match = new Match()
				{
					@Override
					public void to_xml(StringBuilder result)
					{
						/// Empty
					}
					
					@Override
					public boolean test(CpioArchiveEntry value)
					{
						return true;
					}
				};
			}
		}
		
		private Test_result do_validate_file(File_validator_args args, RpmInfo rpm_info)
		{
			var result = new Test_result(true);
			result.verbose_text = new StringBuilder();
			var suffix = new StringBuilder();
			
			if (match.test(args.entry))
			{
				if (name_validator != null)
				{
					/// Remove the leading dot
					var entry_name = args.entry.getName().substring(1);
					
					suffix.append("matching file name \"");
					suffix.append(decor().decorate(entry_name, Type.yellow));
					suffix.append("\"");
					suffix.append(System.lineSeparator());
					
					++debug_nesting;
					var test_result = name_validator.do_validate(entry_name, rpm_info);
					--debug_nesting;
					
					if (! test_result.result)
					{
						result.result = false;
					}
					
					suffix.append(test_result.verbose_text);
				}
				
				if (want_directory)
				{
					suffix.append("want ");
					suffix.append(decor().decorate("directory", Type.cyan));
					
					if (args.entry.isDirectory())
					{
						suffix.append(" and the archive entry ");
						suffix.append(decor().decorate("is", Type.green, Type.bold));
					}
					else
					{
						result.result = false;
						suffix.append(" but the archive entry ");
						suffix.append(decor().decorate("is not", Type.red, Type.bold));
					}
					
					suffix.append(" a ");
					suffix.append(decor().decorate("directory", Type.yellow));
				}
				
				if (symlink_target != null)
				{
					suffix.append("want ");
					suffix.append(decor().decorate("symbolic link", Type.cyan));
					
					if (args.entry.isSymbolicLink())
					{
						suffix.append(" and the archive entry ");
						suffix.append(decor().decorate("is", Type.green, Type.bold));
						
						suffix.append(" a ");
						suffix.append(decor().decorate("symbolic link", Type.yellow));
						
						if (symlink_target.isPresent())
						{
							try
							{
								var target = new String(args.content, "UTF-8");
								++debug_nesting;
								var test_result = symlink_target.get().do_validate(target, rpm_info);
								--debug_nesting;
								
								if (test_result.result)
								{
								}
								else
								{
									result.result = false;
								}
								
								suffix.append(System.lineSeparator());
								suffix.append(single_indent);
								suffix.append("matching symbolic link target \"");
								suffix.append(decor().decorate(target, Type.yellow));
								suffix.append("\"");
								suffix.append(System.lineSeparator());
								suffix.append(test_result.verbose_text);
							}
							catch (UnsupportedEncodingException ex)
							{
								throw new RuntimeException(ex);
							}
						}
					}
					else
					{
						result.result = false;
						suffix.append(" but the archive entry ");
						suffix.append(decor().decorate("is not", Type.red, Type.bold));
						
						suffix.append(" a ");
						suffix.append(decor().decorate("symbolic link", Type.yellow));
					}
				}
			}
			
			result.verbose_text.append(suffix);
			
			return result;
		}
		
		@Override
		public void to_xml(StringBuilder result)
		{
			result.append("<file-rule>");
			result.append("<match>");
			match.to_xml(result);
			result.append("</match>");
			
			if (name_validator != null)
			{
				result.append("<name>");
				name_validator.to_xml(result);
				result.append("</name>");
			}
			
			if (want_directory)
			{
				result.append("<directory/>");
			}
			
			if (symlink_target != null)
			{
				if (symlink_target.isEmpty())
				{
					result.append("<symlink/>");
				}
				else
				{
					result.append("<symlink><target>");
					symlink_target.get().to_xml(result);
					result.append("</target></symlink>");
				}
			}
			result.append("</file-rule>");
		}
	}
}
