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
import org.fedoraproject.javapackages.validator.XML_document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

/**
 * @author Marián Konček
 */
public class Test_xml_document
{
	@Test
	void test_1() throws Exception
	{
		final var config = new XML_document(new FileInputStream("src/test/resources/config-1.xml"));
		
		final var text = FileUtils.readFileToString(new File("src/test/resources/config-1.xml"), "UTF-8");
		
		final var myDiff = DiffBuilder.compare(text)
				.withTest(config.dump())
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
	
	@Test
	void test_2() throws Exception
	{
		final var config = new XML_document(new FileInputStream("src/test/resources/config-1.xml"));
		
		Assertions.assertEquals(4, config.get("config").gets("rule").count());
		
		var first_rule = config.get("config").gets("rule").findFirst().get();
		Assertions.assertEquals("source", first_rule.get("name").content());
		
		var second_rule = config.get("config").gets("rule").skip(1).findFirst().get();
		Assertions.assertEquals("javapackages-tools", second_rule.get("name").content());
		Assertions.assertEquals("source", second_rule.get("match").get("rule").content());
		Assertions.assertEquals(".*", second_rule.get("files").get("regex").content());
		
		var fourth_rule = config.get("config").gets("rule").skip(3).findFirst().get();
		Assertions.assertNull(fourth_rule.get("match").content());
		Assertions.assertEquals(".*", fourth_rule.get("requires").get("any").get("all").get("none").get("regex").content());
	}
}
