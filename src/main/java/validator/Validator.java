package validator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

abstract public class Validator
{
	private String last_failed_value;
	
	final boolean validate(String value)
	{
		if (do_validate(value))
		{
			return true;
		}
		else
		{
			last_failed_value = value;
			return false;
		}
	}
	
	final String last_failed_value()
	{
		return last_failed_value;
	}
	
	abstract protected boolean do_validate(String value);
	abstract String info();
	
	static class Regex_validator extends Validator
	{
		Pattern pattern;
		
		public Regex_validator(Pattern pattern)
		{
			this.pattern = pattern;
		}
		
		@Override
		public boolean do_validate(String value)
		{
			return pattern.matcher(value).matches();
		}
		
		@Override
		public String info()
		{
			return "regex \"" + pattern.toString() + "\"";
		}
	}
	
	static abstract class Transforming_validator extends Validator
	{
		Validator delegate;
		
		public Transforming_validator(Validator delegate)
		{
			super();
			this.delegate = delegate;
		}
		
		protected abstract String transform(String value);
		
		@Override
		public boolean do_validate(String value)
		{
			return delegate.validate(transform(value));
		}
		
		@Override
		public String info()
		{
			return delegate.info();
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
		public boolean do_validate(String value)
		{
			var numeric = Long.parseLong(value);
			return min <= numeric && numeric <= max;
		}
		
		@Override
		public String info()
		{
			return MessageFormat.format("int-range [{0} - {1}]",
					min == Long.MIN_VALUE ? "" : Long.toString(min),
					max == Long.MAX_VALUE ? "" : Long.toString(max));
		}
	}
	
	static abstract class List_validator extends Validator
	{
		protected ArrayList<Validator> list;
		
		protected List_validator(List<Validator> list)
		{
			this.list = new ArrayList<Validator>(list);
		}
		
		final protected String listinfo()
		{
			return list.stream().map(Validator::info).collect(Collectors.joining(", "));
		}
	}
	
	static class Whitelist_validator extends List_validator
	{
		public Whitelist_validator(List<Validator> list)
		{
			super(list);
		}

		@Override
		public boolean do_validate(String value)
		{
			return list.stream().anyMatch((validator) -> validator.validate(value));
		}
		
		@Override
		public String info()
		{
			return "whitelist: [" + listinfo() + "]";
		}
	}
	
	static class Blacklist_validator extends List_validator
	{
		public Blacklist_validator(List<Validator> list)
		{
			super(list);
		}

		@Override
		public boolean do_validate(String value)
		{
			return ! list.stream().anyMatch((validator) -> validator.validate(value));
		}
		
		@Override
		public String info()
		{
			return "blacklist: [" + listinfo() + "]";
		}
	}
}
