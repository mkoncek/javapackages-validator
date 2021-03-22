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

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.FileUtils;
import org.fedoraproject.javapackages.validator.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

/**
 * @author Marián Konček
 */
class Test_config_read
{
	@Test
	void config_1() throws Exception
	{
		final var config = new Config(new FileInputStream("src/test/resources/config-1.xml"));
		
		final var text = FileUtils.readFileToString(new File("src/test/resources/config-1.xml"), "UTF-8");
		
		final var myDiff = DiffBuilder.compare(text)
				.withTest(config.to_xml())
				.ignoreElementContentWhitespace()
				.ignoreComments()
				
				/// Order-independent nodes
				.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
				.checkForSimilar()
				
				.build();
		
		for (var diff : myDiff.getDifferences())
		{
			System.out.println(diff.toString());
		}
		
		Assertions.assertFalse(myDiff.hasDifferences());
	}
}
