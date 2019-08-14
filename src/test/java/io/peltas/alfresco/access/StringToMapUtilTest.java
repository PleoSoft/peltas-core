/**
 * Copyright 2019 Pleo Soft d.o.o. (pleosoft.com)

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.peltas.alfresco.access;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.peltas.core.alfresco.StringToMapUtil;

public class StringToMapUtilTest {

	@Test
	public void testAlfrescoNamespaceString() {
		String alfrescoNamespacetring = "{{http://www.gradecak.com/model/flow/1.0}caseType=0, {http://www.gradecak.com/model/flow/1.0}received=Thu May 07 17:19:14 CEST 2015, "
				+ "{http://www.gradecak.com/model/flow/1.0}personId=[1121966, 442015, 556914], {http://www.alfresco.org/model/content/1.0}name=Platni nalog - isplata place  4-070515171914.pdf, "
				+ "{http://www.alfresco.org/model/system/1.0}node-dbid=26560, {http://www.alfresco.org/model/system/1.0}store-identifier=SpacesStore, {http://www.gradecak.com/model/flow/1.0}jobRef=10021A, "
				+ "{http://www.gradecak.com/model/flow/1.0}propertyNo=[191732, 242232], {http://www.gradecak.com/model/flow/1.0}documentReference=AAACCLT, {http://www.alfresco.org/model/system/1.0}locale=hr_HR, "
				+ "{http://www.alfresco.org/model/content/1.0}content=contentUrl=store://2015/5/7/17/19/1282d227-8c03-47c6-8087-4ecad7b18c7c.bin|mimetype=application/pdf|size=51846|encoding=UTF-8|locale=hr_HR_|id=2181, "
				+ "{http://www.alfresco.org/model/content/1.0}modified=Thu May 07 17:19:14 CEST 2015, {http://www.alfresco.org/model/system/1.0}node-uuid=1da246e3-be4a-487e-b155-68bfcdc5f1fd, "
				+ "{http://www.alfresco.org/model/content/1.0}created=Thu May 07 17:19:14 CEST 2015, {http://www.gradecak.com/model/flow/1.0}companyId=1, "
				+ "{http://www.gradecak.com/model/flow/1.0}type={http://www.gradecak.com/model/flow/1.0}repairJobPropertyType, {http://www.alfresco.org/model/system/1.0}store-protocol=workspace, "
				+ "{http://www.alfresco.org/model/content/1.0}creator=admin, {http://www.alfresco.org/model/content/1.0}modifier=admin, "
				+ "{http://www.alfresco.org/model/content/1.0}someProperty=null, {http://www.alfresco.org/model/content/1.0}title=[{locale=en_US_, value=Start Pooled Review and Approve Workflow}]}";

		Map<String, Object> map = StringToMapUtil.stringToMap(alfrescoNamespacetring, ',');

		String caseType = (String) map.get("{http://www.gradecak.com/model/flow/1.0}caseType");
		Assert.assertNotNull(caseType);

		List<String> list = (List<String>) map.get("{http://www.gradecak.com/model/flow/1.0}propertyNo");
		// List<Object> list = StringToMapUtil.valueAsList(string);
		Assert.assertEquals(2, list.size());

		String someProperty = (String) map.get("{http://www.alfresco.org/model/content/1.0}someProperty");
		Assert.assertNull(someProperty);

	}

	@Test
	public void testAlfrescoPrefixString() {
		String alfrescoPrefixtring = "{cm:title=[{locale=en_US_, value=Start Pooled Review and Approve Workflow}], cm:creator=System, "
				+ "cm:modifier=System, cm:created=2018-05-10T11:49:17.018+02:00, app:editInline=true, "
				+ "custom:propertyNo=[191732, 242232], sys:store-protocol=workspace, sys:store-identifier=SpacesStore, "
				+ "cm:content={contentId=234, encoding=UTF-8, locale=en_US_, mimetype=application/x-javascript, size=1490}, "
				+ "cm:description=[{locale=en_US_, value=Starts the Pooled Review and Approve workflow for all members of the site the document belongs to}], "
				+ "sys:node-uuid=18c4b8a1-79bf-4fbe-9667-abb6c1849057, cm:name=start-pooled-review-workflow.js, "
				+ "cm:author=Alfresco, sys:node-dbid=711, sys:locale=en_US_, cm:modified=2018-05-10T11:49:17.018+02:00, "
				+ "custom:someProperty=null}";

		Map<String, Object> map = StringToMapUtil.stringToMap(alfrescoPrefixtring, ',');

		String editInline = (String) map.get("app:editInline");
		Assert.assertNotNull(editInline);

		List<String> list = (List<String>) map.get("custom:propertyNo");
		// List<Object> list = StringToMapUtil.valueAsList(string);
		Assert.assertEquals(2, list.size());

		String someProperty = (String) map.get("{http://www.alfresco.org/model/content/1.0}someProperty");
		Assert.assertNull(someProperty);
	}

	@Test
	public void testValueAsMap() {
		String alfrescoPrefixtring = "{a= {b=1, c=1}}";

		Map<String, Object> map = StringToMapUtil.stringToMap(alfrescoPrefixtring, ',');

		String a = (String) map.get("a");
		Assert.assertNotNull(a);

		List<String> list = (List<String>) map.get("custom:propertyNo");
		// List<Object> list = StringToMapUtil.valueAsList(string);
		Assert.assertEquals(2, list.size());

		String someProperty = (String) map.get("{http://www.alfresco.org/model/content/1.0}someProperty");
		Assert.assertNull(someProperty);
	}
}