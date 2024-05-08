/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.web.internal.portlet.action.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.object.admin.rest.dto.v1_0.ObjectDefinition;
import com.liferay.object.admin.rest.resource.v1_0.ObjectDefinitionResource;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.FeatureFlags;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Guilherme Sá
 */
@FeatureFlags("LPS-187142")
@RunWith(Arquillian.class)
public class BoundObjectDefinitionsExportImportTest
	extends BaseExportImportTestCase {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		user = TestPropsValues.getUser();

		ObjectDefinitionResource.Builder builder =
			_objectDefinitionResourceFactory.create();

		_objectDefinitionResource = builder.user(
			user
		).build();

		Class<?> clazz = getClazz();

		_baseObjectDefinitionJSONString = StringUtil.read(
			clazz.getResourceAsStream(
				"dependencies/test-base-object-definition.json"));

		_baseObjectRelatioshipJSONString = StringUtil.read(
			clazz.getResourceAsStream(
				"dependencies/test-base-object-relationship.json"));
	}

	@Test
	public void testExportImportBoundObjectDefinitions() throws Exception {
		//		testExportImport(
		//			"test-bound-object-definitions.draft.json",
		//			"test-bound-object-definitions.draft.json", null,
		//			"TestObjectDefinition1");

		// import bound object definitions as draft status

		JSONArray boundObjectDefinitionsJSONArray = JSONUtil.putAll(
			_jsonFactory.createJSONObject(
				_baseObjectDefinitionJSONString
			).put(
				"externalReferenceCode", "TESTOBJECTDEFINITION1"
			).put(
				"name", "TestObjectDefinition1"
			).put(
				"objectRelationships",
				JSONUtil.put(
					_createOneToManyObjectRelationship(
						"TESTOBJECTDEFINITION1", "TESTOBJECTDEFINITION2"))
			).put(
				"rootObjectDefinitionExternalReferenceCode",
				"TESTOBJECTDEFINITION1"
			),
			_jsonFactory.createJSONObject(
				_baseObjectDefinitionJSONString
			).put(
				"externalReferenceCode", "TESTOBJECTDEFINITION2"
			).put(
				"name", "TestObjectDefinition2"
			).put(
				"objectRelationships",
				JSONUtil.put(
					_createOneToManyObjectRelationship(
						"TESTOBJECTDEFINITION2", "TESTOBJECTDEFINITION3"))
			).put(
				"rootObjectDefinitionExternalReferenceCode",
				"TESTOBJECTDEFINITION1"
			),
			_jsonFactory.createJSONObject(
				_baseObjectDefinitionJSONString
			).put(
				"externalReferenceCode", "TESTOBJECTDEFINITION3"
			).put(
				"name", "TestObjectDefinition3"
			).put(
				"rootObjectDefinitionExternalReferenceCode",
				"TESTOBJECTDEFINITION1"
			));

		testExportImportJSONString(
			boundObjectDefinitionsJSONArray.toString(),
			boundObjectDefinitionsJSONArray.toString(), null,
			"TestObjectDefinition1");

		//		testExportImport(
		//			"test-bound-object-definitions.published.json",
		//			"test-bound-object-definitions.draft.json", null,
		//			"TestObjectDefinition1");

		//		testFailedImport(
		//			"test-invalid-bound-object-definitions.json",
		//			"test-invalid-bound-object-definitions.error-message.json", null,
		//			null);
	}

	@Override
	protected ClassLoader getClassLoader() {
		return BoundObjectDefinitionsExportImportTest.class.getClassLoader();
	}

	@Override
	protected Class<?> getClazz() {
		return getClass();
	}

	@Override
	protected long getId(String name) throws Exception {
		ObjectDefinition objectDefinition = _getObjectDefinition(name);

		return objectDefinition.getId();
	}

	@Override
	protected String getIdentifierName() {
		return "objectDefinitionId";
	}

	@Override
	protected String getJSONName() {
		return "objectDefinitionJSON";
	}

	@Override
	protected MVCActionCommand getMVCActionCommand() {
		return _mvcActionCommand;
	}

	@Override
	protected MVCResourceCommand getMVCResourceCommand() {
		return _mvcResourceCommand;
	}

	private JSONObject _createOneToManyObjectRelationship(
			String objectDefinitionExternalReferenceCode1,
			String objectDefinitionExternalReferenceCode2)
		throws Exception {

		return _jsonFactory.createJSONObject(
			_baseObjectRelatioshipJSONString
		).put(
			"edge", true
		).put(
			"objectDefinitionExternalReferenceCode1",
			objectDefinitionExternalReferenceCode1
		).put(
			"objectDefinitionExternalReferenceCode2",
			objectDefinitionExternalReferenceCode2
		).put(
			"objectDefinitionName2", objectDefinitionExternalReferenceCode2
		).put(
			"type", "oneToMany"
		);
	}

	private ObjectDefinition _getObjectDefinition(String name)
		throws Exception {

		Page<ObjectDefinition> page =
			_objectDefinitionResource.getObjectDefinitionsPage(
				name, null, null, Pagination.of(1, 1), null);

		List<ObjectDefinition> items = (List<ObjectDefinition>)page.getItems();

		return items.get(0);
	}

	private String _baseObjectDefinitionJSONString;
	private String _baseObjectRelatioshipJSONString;

	@Inject
	private JSONFactory _jsonFactory;

	@Inject(
		filter = "mvc.command.name=/object_definitions/import_object_definition"
	)
	private MVCActionCommand _mvcActionCommand;

	@Inject(
		filter = "mvc.command.name=/object_definitions/export_bound_object_definitions"
	)
	private MVCResourceCommand _mvcResourceCommand;

	private ObjectDefinitionResource _objectDefinitionResource;

	@Inject
	private ObjectDefinitionResource.Factory _objectDefinitionResourceFactory;

}