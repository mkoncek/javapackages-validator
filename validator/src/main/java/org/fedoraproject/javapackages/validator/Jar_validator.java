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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.fedoraproject.javapackages.validator.Validator.Test_result;

/**
 * @author Marián Konček
 */
public interface Jar_validator
{
	static interface Visitor
	{
		void visit(Validator.Test_result result, String entry);
	}
	
	void accept(Visitor visitor, JarArchiveInputStream jar, String entry);
	
	/**
	 * Applies its member validator on all of the contained .class files
	 * accumulating error messages
	 */
	static class Jar_class_validator implements Jar_validator
	{
		Jar_validator class_validator;
		
		ArrayList<String> messages = new ArrayList<>(0);
		
		public Jar_class_validator(Jar_validator class_validator)
		{
			super();
			this.class_validator = class_validator;
		}
		
		@Override
		public void accept(Visitor visitor, JarArchiveInputStream jar, String entry)
		{
			try
			{
				JarArchiveEntry jar_entry;
				while ((jar_entry = jar.getNextJarEntry()) != null)
				{
					final String class_name = jar_entry.getName();
					
					if (class_name.endsWith(".class"))
					{
						class_validator.accept(visitor, jar, entry + ": " + class_name);
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
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
		public void accept(Visitor visitor, JarArchiveInputStream jar, String entry)
		{
			/// Read 6-th and 7-th bytes which indicate the
			/// .class bytecode version
			try
			{
				jar.skip(6);
				var version_buffer = ByteBuffer.allocate(2);
				jar.read(version_buffer.array());
				
				final var bc_validator = new Validator.Delegating_validator(bytecode_validator)
				{
					@Override
					protected Test_result do_validate(String value)
					{
						return delegate.do_validate(value).prefix(Package_test.color_decorator()
								.decorate("[Bytecode version]", Ansi_colors.Type.bold) + ": ");
					}
				};
				
				final var version = Short.toString(version_buffer.getShort());
				
				visitor.visit(bc_validator.validate(version), entry);
			}
			catch (IOException e)
			{
				throw new UncheckedIOException(e);
			}
		}
	}
}
