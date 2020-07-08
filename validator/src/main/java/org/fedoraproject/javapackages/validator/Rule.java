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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.fedoraproject.javadeptools.rpm.RpmInfo;

import org.fedoraproject.javapackages.validator.Validator.Test_result;

/**
 * @author Marián Konček
 */
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
	
	public List<Test_result> apply(Path rpm_path)
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
		
		if (! applies(rpm_info))
		{
			return result;
		}
		
		if (provides != null)
		{
			rpm_info.getProvides().stream().map((s) -> provides.validate(s)).forEachOrdered(result::add);
		}
		
		if (requires != null)
		{
			rpm_info.getRequires().stream().map((s) -> requires.validate(s)).forEachOrdered(result::add);
		}
		
		if (obsoletes != null)
		{
			throw new RuntimeException("Obsoletes not implemented");
		}
		
		if (rpm_file_size != null)
		{
			result.add(rpm_file_size.validate(Long.toString(rpm_path.toFile().length())));
		}
		
		return result;
	}
}
