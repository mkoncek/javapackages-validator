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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;

/**
 * @author Marián Konček
 */
public interface Jar_validator
{
	boolean validate(JarArchiveInputStream jar);
	String info();
	
	static class Jar_class_validator implements Jar_validator
	{
		Jar_validator class_validator;
		String message;
		
		public Jar_class_validator(Jar_validator class_validator)
		{
			super();
			this.class_validator = class_validator;
		}

		@Override
		public boolean validate(JarArchiveInputStream jar)
		{
			try
			{
				JarArchiveEntry jar_entry;
				while ((jar_entry = jar.getNextJarEntry()) != null)
				{
					final String class_name = jar_entry.getName();
					if (class_name.endsWith(".class") && ! class_validator.validate(jar))
					{
						message = class_name + ": " + class_validator.info();
						return false;
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			
			return true;
		}
		
		@Override
		public String info()
		{
			return message;
		}
	}
	
	static class Class_bytecode_validator implements Jar_validator
	{
		Validator bytecode_validator;
		
		public Class_bytecode_validator(Validator bytecode_validator)
		{
			super();
			this.bytecode_validator = bytecode_validator;
		}
		
		@Override
		public boolean validate(JarArchiveInputStream jar)
		{
			/// Read 6-th and 7-th bytes which indicate the
			/// .class bytecode version
			try
			{
				jar.skip(6);
				var versionBuffer = ByteBuffer.allocate(2);
				jar.read(versionBuffer.array());
				
				final var version = Short.toString(versionBuffer.getShort());
				
				return bytecode_validator.validate(version);
			}
			catch (IOException e)
			{
				throw new UncheckedIOException(e);
			}
		}
		
		@Override
		public String info()
		{
			return "bytecode version validate failed: " + bytecode_validator.info() +
					" but bytecode version is " + bytecode_validator.last_failed_value();
		}
	}
}
