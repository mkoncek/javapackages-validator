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
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.fedoraproject.javapackages.validator.XML_document;
import org.fedoraproject.javapackages.validator.XML_document.XML_node;
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
		try (var config = new XML_document(new FileInputStream("src/test/resources/config-1.xml")))
		{
			final var text = FileUtils.readFileToString(new File("src/test/resources/config-1.xml"), "UTF-8");
			
			final var myDiff = DiffBuilder.compare(text)
					.withTest(config.iterator().next().to_xml())
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
	
	@Test
	void test_2() throws Exception
	{
		try (var config = new XML_document(new FileInputStream("src/test/resources/config-1.xml")))
		{
			config.start("config");
			var it = config.iterator();
			
			var execution = it.next();
			Assertions.assertEquals("tag", execution.get().name());
			
			var rules = new ArrayList<XML_node>();
			
			rules.add(it.next());
			rules.add(it.next());
			rules.add(it.next());
			rules.add(it.next());
			
			Assertions.assertEquals("source", rules.get(0).get("name").content());
			
			Assertions.assertEquals("javapackages-tools", rules.get(1).get("name").content());
			Assertions.assertEquals("source", rules.get(1).get("match").get("rule").content());
			Assertions.assertEquals(".*", rules.get(1).get("files").get("file-rule").get("filename").get("any").get("regex").content());
			Assertions.assertFalse(rules.get(1).getop("nonexisting").isPresent());
			Assertions.assertFalse(rules.get(1).get("files").getop("nonexisting").isPresent());
			
			Assertions.assertEquals("/other_file", rules.get(2).get("files").gets("file-rule")
					.reduce((first, second) -> first).get().get("symlink").get("target").get("text").content());
			
			Assertions.assertNull(rules.get(3).get("match").content());
			Assertions.assertEquals(".*", rules.get(3).get("requires").get("any").get("all").get("none").get("regex").content());
			
			Assertions.assertFalse(it.hasNext());
		}
	}
}
