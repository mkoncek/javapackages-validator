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
	private static final Ansi_colors.Decorator decor = Package_test.color_decorator();
	
	static final class Test_result
	{
		boolean result;
		private StringBuilder message = new StringBuilder();
		StringBuilder debug;
		
		public Test_result(boolean result)
		{
			this.result = result;
		}
		
		public Test_result(boolean result, String message)
		{
			this.result = result;
			this.message.append(message);
		}
		
		final Test_result prefix(String prefix)
		{
			message.insert(0, prefix);
			return this;
		}
		
		final String message()
		{
			return message.toString();
		}
	}
	
	protected abstract Test_result do_validate(String value);
	
	public final Test_result validate(String value)
	{
		var result = do_validate(value);
		
		if (result.result)
		{
			result.message.append(decor.decorate("passed", Type.green, Type.bold));
			result.message.append(" with value \"");
			result.message.append(decor.decorate(value, Type.yellow));
			result.message.append("\"");
		}
		else
		{
			result.message.append(decor.decorate("failed", Type.red, Type.bold));
			result.message.append(" with value \"");
			result.message.append(decor.decorate(value, Type.yellow));
			result.message.append("\"");
		}
		
		return result;
	}
	
	static abstract class Delegating_validator extends Validator
	{
		Validator delegate;
		
		public Delegating_validator(Validator delegate)
		{
			super();
			this.delegate = delegate;
		}
	}
	
	static class Regex_validator extends Validator
	{
		Pattern pattern;
		
		public Regex_validator(Pattern pattern)
		{
			this.pattern = pattern;
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			Test_result result = new Test_result(pattern.matcher(value).matches());
			result.debug = new StringBuilder("\t".repeat(Package_test.debug_nesting));
			
			result.debug.append("regex \"");
			result.debug.append(decor.decorate(pattern.toString(), Type.cyan));
			result.debug.append("\" ");
			
			result.debug.append(result.result ?
				decor.decorate("matches", Type.green, Type.bold) :
				decor.decorate("does not match", Type.red, Type.bold));
			
			result.debug.append(" value \"");
			result.debug.append(decor.decorate(value, Type.yellow));
			result.debug.append("\"");
					
			return result;
		}
	}
	
	static abstract class Transforming_validator extends Delegating_validator
	{
		public Transforming_validator(Validator delegate)
		{
			super(delegate);
		}
		
		protected abstract String transform(String value);
		
		@Override
		protected Test_result do_validate(String value)
		{
			final String transformed = transform(value);
			++Package_test.debug_nesting;
			Test_result result = delegate.do_validate(transformed);
			--Package_test.debug_nesting;
			
			var inserted = new StringBuilder("\t".repeat(Package_test.debug_nesting));
			inserted.append("transforming validator transforms \"");
			inserted.append(decor.decorate(value, Type.yellow));
			inserted.append("\" -> \"");
			inserted.append(decor.decorate(transformed, Type.yellow));
			inserted.append("\" and evaluates");
			inserted.append(System.lineSeparator());
			
			result.debug.insert(0, inserted);
			
			return result;
		}
	}
	
	static class Int_range_validator extends Validator
	{
		long min;
		long max;
		
		Int_range_validator(long min, long max)
		{
			this.min = min;
			this.max = max;
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			final var numeric = Long.parseLong(value);
			
			Test_result result = new Test_result(min <= numeric && numeric <= max);
			result.debug = new StringBuilder("\t".repeat(Package_test.debug_nesting));
			
			result.debug.append("int-range <");
			result.debug.append(decor.decorate(MessageFormat.format("{0} - {1}",
					min == Long.MIN_VALUE ? "" : Long.toString(min),
					max == Long.MAX_VALUE ? "" : Long.toString(max)), Type.cyan));
			result.debug.append("> ");
			result.debug.append(result.result ?
					decor.decorate("contains", Type.green, Type.bold) :
					decor.decorate("does not contain", Type.red, Type.bold));
			result.debug.append(" value \"");
			result.debug.append(decor.decorate(value, Type.yellow));
			result.debug.append("\"");
					
			return result;
		}
	}
	
	static abstract class List_validator extends Validator
	{
		protected ArrayList<Validator> list;
		
		protected List_validator(List<Validator> list)
		{
			this.list = new ArrayList<Validator>(list);
		}
		
		protected abstract void do_list_validate(String value, Test_result result);
		
		protected final void partial_validate(String value, Test_result result)
		{
			int offset = result.debug.length();
			
			++Package_test.debug_nesting;
			do_list_validate(value, result);
			--Package_test.debug_nesting;
			
			var inserted = new StringBuilder();
			if (result.result)
			{
				inserted.append(decor.decorate("accepted", Type.green, Type.bold));
			}
			else
			{
				inserted.append(decor.decorate("rejected", Type.red, Type.bold));
			}
			inserted.append(" value \"");
			inserted.append(decor.decorate(value, Type.yellow));
			inserted.append("\": {");
			inserted.append(System.lineSeparator());
			result.debug.insert(offset, inserted);
			result.debug.append("}");
		}
	}
	
	@Deprecated
	static class Whitelist_validator extends Any_validator
	{
		public Whitelist_validator(List<Validator> list)
		{
			super(list);
		}
	}
	
	@Deprecated
	static class Blacklist_validator extends None_validator
	{
		public Blacklist_validator(List<Validator> list)
		{
			super(list);
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
				final var test_result = val.validate(value);
				
				if (test_result.result == false)
				{
					result.result = false;
				}
				
				result.debug.append(test_result.debug);
				result.debug.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			var result = new Test_result(true);
			result.debug = new StringBuilder("\t".repeat(Package_test.debug_nesting));
			result.debug.append("validator <all> ");
			
			partial_validate(value, result);
			
			return result;
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
				final var test_result = val.validate(value);
				
				if (test_result.result)
				{
					result.result = true;
				}
				
				result.debug.append(test_result.debug);
				result.debug.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			var result = new Test_result(false);
			result.debug = new StringBuilder("\t".repeat(Package_test.debug_nesting));
			result.debug.append("validator <any> ");
			
			partial_validate(value, result);
			
			return result;
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
				final var test_result = val.validate(value);
				
				if (test_result.result)
				{
					result.result = false;
				}
				
				result.debug.append(test_result.debug);
				result.debug.append(System.lineSeparator());
			}
		}
		
		@Override
		protected Test_result do_validate(String value)
		{
			var result = new Test_result(true);
			result.debug = new StringBuilder("\t".repeat(Package_test.debug_nesting));
			result.debug.append("validator <none> ");
			
			partial_validate(value, result);
			
			return result;
		}
	}
}
