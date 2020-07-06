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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Marián Konček
 */
abstract public class Validator
{
	static final class Test_result
	{
		boolean result;
		String message;
		
		public Test_result(boolean result, String message)
		{
			this.result = result;
			this.message = message;
		}
		
		final Test_result prefix(String prefix)
		{
			message = prefix + message;
			return this;
		}
	}
	
	abstract Test_result validate(String value);
	
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
		Test_result validate(String value)
		{
			final boolean result = pattern.matcher(value).matches();
			
			String message = "regex \"" + pattern.toString();
			message += result ?
					"\" matches value \"" + value + "\"" :
					"\" does not match value \"" + value + "\"";
					
			return new Test_result(result, message);
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
		Test_result validate(String value)
		{
			final String transformed = transform(value);
			Test_result result = delegate.validate(transformed);
			result.message = "(transforming " + "\"" + value + "\" -> \"" + transformed + "\"): " + result.message;
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
		Test_result validate(String value)
		{
			final var numeric = Long.parseLong(value);
			final boolean result = min <= numeric && numeric <= max;
			
			String message = MessageFormat.format("int-range [{0} - {1}]",
					min == Long.MIN_VALUE ? "" : Long.toString(min),
					max == Long.MAX_VALUE ? "" : Long.toString(max));
			message += result ?
					" contains value \"" + value + "\"" :
					" does not contain value \"" + value + "\"";
					
			return new Test_result(result, message);
		}
	}
	
	static abstract class List_validator extends Validator
	{
		protected ArrayList<Validator> list;
		
		protected List_validator(List<Validator> list)
		{
			this.list = new ArrayList<Validator>(list);
		}
	}
	
	static class Whitelist_validator extends List_validator
	{
		public Whitelist_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		Test_result validate(String value)
		{
			boolean result = false;
			StringBuilder sb = new StringBuilder();
			sb.append("whitelist ");
			int offset = sb.length();
			
			for (final var val : list)
			{
				final var test_result = val.validate(value);
				
				if (test_result.result)
				{
					result = true;
				}
				
				sb.append(test_result.message + "; ");
			}
			
			if (result)
			{
				sb.insert(offset, "accepted value \"" + value + "\": {");
			}
			else
			{
				sb.insert(offset, "rejected value \"" + value + "\": {");
			}
			
			/// Remove the last "; ", list must contain at least one element
			sb.delete(sb.length() - 2, sb.length());
			sb.append("}");
			
			return new Test_result(result, sb.toString());
		}
	}
	
	static class Blacklist_validator extends List_validator
	{
		public Blacklist_validator(List<Validator> list)
		{
			super(list);
		}
		
		@Override
		Test_result validate(String value)
		{
			boolean result = true;
			StringBuilder sb = new StringBuilder();
			sb.append("blacklist ");
			int offset = sb.length();
			
			for (final var val : list)
			{
				final var test_result = val.validate(value);
				
				if (! test_result.result)
				{
					result = false;
				}
				
				sb.append(test_result.message + "; ");
			}
			
			if (result)
			{
				sb.insert(offset, "accepted value \"" + value + "\": {");
			}
			else
			{
				sb.insert(offset, "rejected value \"" + value + "\": {");
			}
			
			/// Remove the last "; ", list must contain at least one element
			sb.delete(sb.length() - 2, sb.length());
			sb.append("}");
			
			return new Test_result(result, sb.toString());
		}
	}
}
