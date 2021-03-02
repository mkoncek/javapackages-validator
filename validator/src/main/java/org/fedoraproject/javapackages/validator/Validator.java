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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.fedoraproject.javapackages.validator.Ansi_colors.Type;

/**
 * @author Marián Konček
 */
abstract public class Validator
{
	static private final Ansi_colors.Decorator decor()
	{
		return Package_test.color_decorator();
	}
	
	static private int debug_nesting = 0;
	
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
	
	protected abstract Test_result do_validate(String value);
	public abstract String to_xml();
	
	public Validator prefix(String message_prefix)
	{
		this.message_prefix = message_prefix;
		return this;
	}
	
	public final Test_result validate(String value, String prefix)
	{
		final var result = do_validate(value);
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
	
	static class Pass_validator extends Validator
	{
		@Override
		protected Test_result do_validate(String value)
		{
			final var result = new Test_result(true);
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			result.verbose_text.append("validator <pass>");
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<pass/>";
		}
	}
	
	static class Fail_validator extends Validator
	{
		@Override
		protected Test_result do_validate(String value)
		{
			final var result = new Test_result(false);
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			result.verbose_text.append("validator <fail>");
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<fail/>";
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
		protected Test_result do_validate(String value)
		{
			final var result = new Test_result(string.equals(value));
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			
			result.verbose_text.append("text \"");
			result.verbose_text.append(decor().decorate(string, Type.cyan));
			result.verbose_text.append("\" ");
			
			result.verbose_text.append(result.result ?
				decor().decorate("matches", Type.green, Type.bold) :
				decor().decorate("does not match", Type.red, Type.bold));
			
			result.verbose_text.append(" value \"");
			result.verbose_text.append(decor().decorate(value, Type.yellow));
			result.verbose_text.append("\"");
			
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<text>" + string + "</text>";
		}
	}
	
	static class Regex_validator extends Validator
	{
		Pattern pattern;
		
		public Regex_validator(Pattern pattern)
		{
			super();
			this.pattern = pattern;
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			final var result = new Test_result(pattern.matcher(value).matches());
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			
			result.verbose_text.append("regex \"");
			result.verbose_text.append(decor().decorate(pattern.toString(), Type.cyan));
			result.verbose_text.append("\" ");
			
			result.verbose_text.append(result.result ?
				decor().decorate("matches", Type.green, Type.bold) :
				decor().decorate("does not match", Type.red, Type.bold));
			
			result.verbose_text.append(" value \"");
			result.verbose_text.append(decor().decorate(value, Type.yellow));
			result.verbose_text.append("\"");
					
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<regex>" + pattern.toString() + "</regex>";
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
		protected Test_result do_validate(String value)
		{
			final var numeric = Long.parseLong(value);
			
			final var result = new Test_result(min <= numeric && numeric <= max);
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			
			result.verbose_text.append("int-range <");
			result.verbose_text.append(decor().decorate(MessageFormat.format("{0} - {1}",
					min == Long.MIN_VALUE ? "" : Long.toString(min),
					max == Long.MAX_VALUE ? "" : Long.toString(max)), Type.cyan));
			result.verbose_text.append("> ");
			result.verbose_text.append(result.result ?
					decor().decorate("contains", Type.green, Type.bold) :
					decor().decorate("does not contain", Type.red, Type.bold));
			result.verbose_text.append(" value \"");
			result.verbose_text.append(decor().decorate(value, Type.yellow));
			result.verbose_text.append("\"");
					
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<int-range>" +
					(min == Long.MIN_VALUE ? "" : Long.toString(min)) +
					"-" +
					(max == Long.MAX_VALUE ? "" : Long.toString(max)) +
					"</int-range>";
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
		
		protected abstract void do_list_validate(String value, Test_result result);
		
		protected final void partial_validate(String value, Test_result result)
		{
			int offset = result.verbose_text.length();
			
			++debug_nesting;
			do_list_validate(value, result);
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
			result.verbose_text.append("\t".repeat(debug_nesting));
			result.verbose_text.append("}");
		}
		
		protected final String partial_to_xml()
		{
			var result = new StringBuilder();
			
			for (final var validator : list)
			{
				result.append(validator.to_xml());
			}
			
			return result.toString();
		}
	}
	
	static class All_validator extends List_validator
	{
		public All_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		protected void do_list_validate(String value, Test_result result)
		{
			for (final var val : list)
			{
				final var test_result = val.do_validate(value);
				
				if (test_result.result == false)
				{
					result.result = false;
				}
				
				result.verbose_text.append(test_result.verbose_text);
				result.verbose_text.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			final var result = new Test_result(true);
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			result.verbose_text.append("validator <all> ");
			
			partial_validate(value, result);
			
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<all>" + partial_to_xml() + "</all>";
		}
	}
	
	static class Any_validator extends List_validator
	{
		public Any_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		protected void do_list_validate(String value, Test_result result)
		{
			for (final var val : list)
			{
				final var test_result = val.do_validate(value);
				
				if (test_result.result)
				{
					result.result = true;
				}
				
				result.verbose_text.append(test_result.verbose_text);
				result.verbose_text.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			final var result = new Test_result(false);
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			result.verbose_text.append("validator <any> ");
			
			partial_validate(value, result);
			
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<any>" + partial_to_xml() + "</any>";
		}
	}
	
	static class None_validator extends List_validator
	{
		public None_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		protected void do_list_validate(String value, Test_result result)
		{
			for (final var val : list)
			{
				final var test_result = val.do_validate(value);
				
				if (test_result.result)
				{
					result.result = false;
				}
				
				result.verbose_text.append(test_result.verbose_text);
				result.verbose_text.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			final var result = new Test_result(true);
			result.verbose_text = new StringBuilder("\t".repeat(debug_nesting));
			result.verbose_text.append("validator <none> ");
			
			partial_validate(value, result);
			
			return result;
		}
		
		@Override
		public String to_xml()
		{
			return "<none>" + partial_to_xml() + "</none>";
		}
	}
}
