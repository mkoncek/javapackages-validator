package validator;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.fedoraproject.javadeptools.rpm.RpmInfo;

public class Rule
{
	public interface Match
	{
		public boolean matches(RpmInfo rpm_info);
	}
	
	public class Method_match implements Match
	{
		Method getter;
		Pattern match;
		
		public Method_match(Method getter, Pattern match)
		{
			super();
			this.getter = getter;
			this.match = match;
		}
		
		public boolean matches(RpmInfo rpm_info)
		{
			try
			{
				return match.matcher((String) getter.invoke(rpm_info)).matches();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
	
	Method_match rpm_name_match(Pattern match) throws Exception
	{
		return new Method_match(RpmInfo.class.getMethod("getName"), match);
	}
	
	Match match;
	
	Validator provides;
	Validator requires;
	Validator obsoletes;
	Validator rpm_file_size;
	Jar_validator jar_validator;
	
	boolean applies(RpmInfo rpm_info)
	{
		return match != null && match.matches(rpm_info);
	}
	
	public List<String> apply(Path rpm_path)
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
		
		var result = new ArrayList<String>();
		
		if (! applies(rpm_info))
		{
			return result;
		}
		
		if (provides != null && ! rpm_info.getProvides().stream().allMatch((str) -> provides.validate(str)))
		{
			result.add("Provides does not match: " + provides.info());
		}
		
		if (requires != null && ! rpm_info.getRequires().stream().allMatch((str) -> requires.validate(str)))
		{
			result.add("Requires does not match: " + requires.info());
		}
		
		if (rpm_file_size != null && ! rpm_file_size.validate(Long.toString(rpm_path.toFile().length())))
		{
			result.add("File size does not match: " + rpm_file_size.info());
		}
		
		return result;
	}
}
