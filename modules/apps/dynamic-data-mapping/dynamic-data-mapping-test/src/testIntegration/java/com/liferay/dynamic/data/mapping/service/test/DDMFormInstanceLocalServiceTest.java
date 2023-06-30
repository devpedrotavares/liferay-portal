/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.dynamic.data.mapping.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormInstance;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceVersion;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.UnlocalizedValue;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceLocalServiceUtil;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.test.util.DDMFormTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMFormValuesTestUtil;
import com.liferay.object.constants.ObjectDefinitionConstants;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.field.util.ObjectFieldUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import com.liferay.portal.vulcan.util.LocalizedMapUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Rafael Praxedes
 */
@RunWith(Arquillian.class)
public class DDMFormInstanceLocalServiceTest extends BaseDDMServiceTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() {
		Class<?> formInstanceClass = DDMFormInstance.class;

		_classNameId = PortalUtil.getClassNameId(formInstanceClass.getName());
	}

	@Test
	public void testAddFormInstanceShouldCreateApprovedFormInstanceVersion()
		throws Exception {

		DDMStructure structure = addStructure(_classNameId, "Test Structure");

		DDMForm settingsDDMForm = DDMFormTestUtil.createDDMForm();

		DDMFormValues settingsDDMFormValues =
			DDMFormValuesTestUtil.createDDMFormValues(settingsDDMForm);

		DDMFormInstance formInstance =
			DDMFormInstanceLocalServiceUtil.addFormInstance(
				structure.getUserId(), structure.getGroupId(),
				structure.getStructureId(), structure.getNameMap(),
				structure.getNameMap(), settingsDDMFormValues,
				ServiceContextTestUtil.getServiceContext(
					group, TestPropsValues.getUserId()));

		DDMFormInstanceVersion latestFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		Assert.assertEquals(
			WorkflowConstants.STATUS_APPROVED,
			latestFormInstanceVersion.getStatus());
	}

	@Test
	public void testAddFormInstanceShouldCreateDraftFormInstanceVersion()
		throws Exception {

		DDMStructure structure = addStructure(_classNameId, "Test Structure");

		DDMForm settingsDDMForm = DDMFormTestUtil.createDDMForm();

		DDMFormValues settingsDDMFormValues =
			DDMFormValuesTestUtil.createDDMFormValues(settingsDDMForm);

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				group, TestPropsValues.getUserId());

		serviceContext.setAttribute("status", WorkflowConstants.STATUS_DRAFT);

		DDMFormInstance formInstance =
			DDMFormInstanceLocalServiceUtil.addFormInstance(
				structure.getUserId(), structure.getGroupId(),
				structure.getStructureId(), structure.getNameMap(),
				structure.getNameMap(), settingsDDMFormValues, serviceContext);

		DDMFormInstanceVersion latestFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		Assert.assertEquals(
			WorkflowConstants.STATUS_DRAFT,
			latestFormInstanceVersion.getStatus());
	}

	@Test
	public void testUpdateFormInstanceShouldCreateNewFormInstanceVersion1()
		throws Exception {

		DDMStructure structure = addStructure(_classNameId, "Test Structure");

		DDMForm settingsDDMForm = DDMFormTestUtil.createDDMForm();

		DDMFormValues settingsDDMFormValues =
			DDMFormValuesTestUtil.createDDMFormValues(settingsDDMForm);

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				group, TestPropsValues.getUserId());

		serviceContext.setAttribute("status", WorkflowConstants.STATUS_DRAFT);

		DDMFormInstance formInstance =
			DDMFormInstanceLocalServiceUtil.addFormInstance(
				structure.getUserId(), structure.getGroupId(),
				structure.getStructureId(), structure.getNameMap(),
				structure.getNameMap(), settingsDDMFormValues, serviceContext);

		DDMFormInstanceVersion firstFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		serviceContext.setAttribute(
			"status", WorkflowConstants.STATUS_APPROVED);

		formInstance = DDMFormInstanceLocalServiceUtil.updateFormInstance(
			formInstance.getFormInstanceId(), formInstance.getStructureId(),
			formInstance.getNameMap(), formInstance.getDescriptionMap(),
			settingsDDMFormValues, serviceContext);

		DDMFormInstanceVersion secondFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		Assert.assertNotEquals(
			firstFormInstanceVersion, secondFormInstanceVersion);
		Assert.assertEquals(
			WorkflowConstants.STATUS_APPROVED,
			secondFormInstanceVersion.getStatus());
	}

	@Test
	public void testUpdateFormInstanceShouldCreateNewFormInstanceVersion2()
		throws Exception {

		DDMStructure structure = addStructure(_classNameId, "Test Structure");

		DDMForm settingsDDMForm = DDMFormTestUtil.createDDMForm();

		DDMFormValues settingsDDMFormValues =
			DDMFormValuesTestUtil.createDDMFormValues(settingsDDMForm);

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				group, TestPropsValues.getUserId());

		DDMFormInstance formInstance =
			DDMFormInstanceLocalServiceUtil.addFormInstance(
				structure.getUserId(), structure.getGroupId(),
				structure.getStructureId(), structure.getNameMap(),
				structure.getNameMap(), settingsDDMFormValues, serviceContext);

		DDMFormInstanceVersion firstFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		formInstance = DDMFormInstanceLocalServiceUtil.updateFormInstance(
			formInstance.getFormInstanceId(), formInstance.getStructureId(),
			formInstance.getNameMap(), formInstance.getDescriptionMap(),
			settingsDDMFormValues, serviceContext);

		DDMFormInstanceVersion secondFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		Assert.assertNotEquals(
			firstFormInstanceVersion, secondFormInstanceVersion);

		Assert.assertEquals(
			WorkflowConstants.STATUS_APPROVED,
			firstFormInstanceVersion.getStatus());

		Assert.assertEquals(
			WorkflowConstants.STATUS_APPROVED,
			secondFormInstanceVersion.getStatus());
	}

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Test
	public void testAddDDMFormInstanceWithObjectStorageType()
		throws Exception {

		ObjectDefinition objectDefinition = _objectDefinitionLocalService.addCustomObjectDefinition(
			TestPropsValues.getUserId(), false, false,
			LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
			"Test", null, null,
			LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
			true, ObjectDefinitionConstants.SCOPE_COMPANY,
			ObjectDefinitionConstants.STORAGE_TYPE_DEFAULT,
			Collections.singletonList(
				ObjectFieldUtil.createObjectField(
					ObjectFieldConstants.BUSINESS_TYPE_TEXT,
					ObjectFieldConstants.DB_TYPE_STRING,
					RandomTestUtil.randomString(), "textField")));

		DDMForm ddmForm = DDMFormTestUtil.createDDMForm("textField");

		ddmForm.getDDMFormFields().get(0).setProperty("objectFieldName", "[\"textField\"]");

		DDMStructure structure = ddmStructureTestHelper.addStructure(
			ddmForm, "object");

		DDMFormValues settingsDDMFormValues =
			DDMFormValuesTestUtil.createDDMFormValues(ddmForm);

		DDMFormFieldValue objectDefinitionIdFieldValue =
			DDMFormValuesTestUtil.createDDMFormFieldValue("objectDefinitionId",
				new UnlocalizedValue(String.valueOf(objectDefinition.getObjectDefinitionId())));

		settingsDDMFormValues.addDDMFormFieldValue(objectDefinitionIdFieldValue);

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				group, TestPropsValues.getUserId());

		DDMFormInstance formInstance =
			DDMFormInstanceLocalServiceUtil.addFormInstance(
				structure.getUserId(), structure.getGroupId(),
				structure.getStructureId(), structure.getNameMap(),
				structure.getNameMap(), settingsDDMFormValues, serviceContext);

		Assert.assertEquals("object", formInstance.getStorageType());
	}

	@Test
	public void testUpdateFormInstanceShouldKeepFormInstanceVersion()
		throws Exception {

		DDMStructure structure = addStructure(_classNameId, "Test Structure");

		DDMForm settingsDDMForm = DDMFormTestUtil.createDDMForm();

		DDMFormValues settingsDDMFormValues =
			DDMFormValuesTestUtil.createDDMFormValues(settingsDDMForm);

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				group, TestPropsValues.getUserId());

		serviceContext.setAttribute("status", WorkflowConstants.STATUS_DRAFT);

		DDMFormInstance formInstance =
			DDMFormInstanceLocalServiceUtil.addFormInstance(
				structure.getUserId(), structure.getGroupId(),
				structure.getStructureId(), structure.getNameMap(),
				structure.getNameMap(), settingsDDMFormValues, serviceContext);

		DDMFormInstanceVersion firstFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		formInstance = DDMFormInstanceLocalServiceUtil.updateFormInstance(
			formInstance.getFormInstanceId(), formInstance.getStructureId(),
			formInstance.getNameMap(), formInstance.getDescriptionMap(),
			settingsDDMFormValues, serviceContext);

		DDMFormInstanceVersion secondFormInstanceVersion =
			formInstance.getFormInstanceVersion(formInstance.getVersion());

		Assert.assertEquals(
			firstFormInstanceVersion, secondFormInstanceVersion);

		Assert.assertEquals(
			WorkflowConstants.STATUS_DRAFT,
			firstFormInstanceVersion.getStatus());

		Assert.assertEquals(
			WorkflowConstants.STATUS_DRAFT,
			secondFormInstanceVersion.getStatus());
	}

	private static long _classNameId;

}