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

package com.liferay.dynamic.data.mapping.internal.exportimport.data.handler;

import com.liferay.dynamic.data.mapping.io.DDMFormValuesDeserializer;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesDeserializerDeserializeRequest;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesDeserializerDeserializeResponse;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormInstance;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceSettings;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceLocalService;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.DDMFormFactory;
import com.liferay.exportimport.data.handler.base.BaseStagedModelDataHandler;
import com.liferay.exportimport.kernel.lar.ExportImportPathUtil;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.exportimport.kernel.lar.StagedModelDataHandler;
import com.liferay.exportimport.kernel.lar.StagedModelDataHandlerUtil;
import com.liferay.exportimport.staged.model.repository.StagedModelRepository;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSON;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Brian Wing Shun Chan
 */
@Component(service = StagedModelDataHandler.class)
public class DDMFormInstanceStagedModelDataHandler
	extends BaseStagedModelDataHandler<DDMFormInstance> {

	public static final String[] CLASS_NAMES = {
		DDMFormInstance.class.getName()
	};

	@Override
	public String[] getClassNames() {
		return CLASS_NAMES;
	}

	@Override
	public String getDisplayName(DDMFormInstance formInstance) {
		return formInstance.getNameCurrentValue();
	}

	protected DDMFormValues deserialize(String content, DDMForm ddmForm) {
		DDMFormValuesDeserializerDeserializeRequest.Builder builder =
			DDMFormValuesDeserializerDeserializeRequest.Builder.newBuilder(
				content, ddmForm);

		DDMFormValuesDeserializerDeserializeResponse
			ddmFormValuesDeserializerDeserializeResponse =
				_jsonDDMFormValuesDeserializer.deserialize(builder.build());

		return ddmFormValuesDeserializerDeserializeResponse.getDDMFormValues();
	}

	@Override
	protected void doExportStagedModel(
			PortletDataContext portletDataContext, DDMFormInstance formInstance)
		throws Exception {

		DDMStructure ddmStructure = formInstance.getStructure();

		StagedModelDataHandlerUtil.exportReferenceStagedModel(
			portletDataContext, formInstance, ddmStructure,
			PortletDataContext.REFERENCE_TYPE_STRONG);

		List<DDMTemplate> ddmTemplates = ddmStructure.getTemplates();

		Element formInstanceElement = portletDataContext.getExportDataElement(
			formInstance);

		for (DDMTemplate ddmTemplate : ddmTemplates) {
			StagedModelDataHandlerUtil.exportReferenceStagedModel(
				portletDataContext, formInstance, ddmTemplate,
				PortletDataContext.REFERENCE_TYPE_STRONG);
		}

		_exportFormInstanceSettings(
			portletDataContext, formInstance, formInstanceElement);

		portletDataContext.addClassedModel(
			formInstanceElement,
			ExportImportPathUtil.getModelPath(formInstance), formInstance);
	}

	@Override
	protected void doImportMissingReference(
			PortletDataContext portletDataContext, String uuid, long groupId,
			long formInstanceId)
		throws Exception {

		DDMFormInstance existingFormInstance = fetchMissingReference(
			uuid, groupId);

		if (existingFormInstance == null) {
			return;
		}

		Map<Long, Long> formInstanceIds =
			(Map<Long, Long>)portletDataContext.getNewPrimaryKeysMap(
				DDMFormInstance.class);

		formInstanceIds.put(
			formInstanceId, existingFormInstance.getFormInstanceId());
	}

	@Override
	protected void doImportStagedModel(
			PortletDataContext portletDataContext, DDMFormInstance formInstance)
		throws Exception {

		Map<Long, Long> ddmStructureIds =
			(Map<Long, Long>)portletDataContext.getNewPrimaryKeysMap(
				DDMStructure.class);

		long ddmStructureId = MapUtil.getLong(
			ddmStructureIds, formInstance.getStructureId(),
			formInstance.getStructureId());

		DDMFormInstance importedFormInstance =
			(DDMFormInstance)formInstance.clone();

		importedFormInstance.setGroupId(portletDataContext.getScopeGroupId());
		importedFormInstance.setStructureId(ddmStructureId);

		DDMFormInstance existingFormInstance =
			_stagedModelRepository.fetchStagedModelByUuidAndGroupId(
				formInstance.getUuid(), portletDataContext.getScopeGroupId());

		if ((existingFormInstance == null) ||
			!portletDataContext.isDataStrategyMirror()) {

			importedFormInstance = _stagedModelRepository.addStagedModel(
				portletDataContext, importedFormInstance);
		}
		else {
			importedFormInstance.setMvccVersion(
				existingFormInstance.getMvccVersion());
			importedFormInstance.setFormInstanceId(
				existingFormInstance.getFormInstanceId());

			importedFormInstance = _stagedModelRepository.updateStagedModel(
				portletDataContext, importedFormInstance);
		}

		Element formInstanceElement = portletDataContext.getImportDataElement(
			formInstance);

		DDMFormValues settingsDDMFormValues = _getImportFormInstanceSettings(
			portletDataContext, formInstanceElement);

		_ddmFormInstanceLocalService.updateFormInstance(
			importedFormInstance.getFormInstanceId(),
			importedFormInstance.getStructureId(),
			importedFormInstance.getNameMap(),
			importedFormInstance.getDescriptionMap(), settingsDDMFormValues,
			portletDataContext.createServiceContext(importedFormInstance));

		portletDataContext.importClassedModel(
			formInstance, importedFormInstance);
	}

	@Override
	protected StagedModelRepository<DDMFormInstance>
		getStagedModelRepository() {

		return _stagedModelRepository;
	}

	private void _exportFormInstanceSettings(
			PortletDataContext portletDataContext, DDMFormInstance formInstance,
			Element formInstanceElement)
		throws Exception {

		String settingsDDMFormValuesPath = ExportImportPathUtil.getModelPath(
			formInstance, "settings-ddm-form-values.json");

		formInstanceElement.addAttribute(
			"settings-ddm-form-values-path", settingsDDMFormValuesPath);

		portletDataContext.addZipEntry(
			settingsDDMFormValuesPath,
			_includeObjectSettings(formInstance.getSettings()));
	}

	private String _addSquareBrackets(String string) {
		return String.format("[\"%s\"]", string);
	}

	private String _removeSquareBrackets(String string) {
		if(string.length() < 4) {
			return StringPool.BLANK;
		}

		if(StringUtil.startsWith(string, "[\"") && StringUtil.endsWith(string, "\"]")) {
			return string.substring(2, string.length() - 2);
		}

		return string;
	}

	private DDMFormValues _getImportFormInstanceSettings(
			PortletDataContext portletDataContext, Element formInstanceElement)
		throws Exception {

		DDMForm ddmForm = DDMFormFactory.create(DDMFormInstanceSettings.class);

		String settingsDDMFormValuesPath = formInstanceElement.attributeValue(
			"settings-ddm-form-values-path");

		String serializedSettingsDDMFormValues =
			_updateObjectDefinitionIdInDDMFormValues(
				portletDataContext.getZipEntryAsString(
					settingsDDMFormValuesPath));

		return deserialize(serializedSettingsDDMFormValues, ddmForm);
	}

	private String _includeObjectSettings(String settings) throws Exception {
		JSONObject settingsJSONObject = _jsonFactory.createJSONObject(settings);

		JSONArray fieldValuesJSONArray = settingsJSONObject.getJSONArray(
			"fieldValues");

		long objectDefinitionId = 0L;

		for (Object fieldValue : fieldValuesJSONArray) {
			JSONObject fieldValueJSONObject = (JSONObject) fieldValue;

			if (StringUtil.equals(fieldValueJSONObject.getString("name"), "objectDefinitionId")) {

				objectDefinitionId = GetterUtil.getLong(
					_removeSquareBrackets(fieldValueJSONObject.getString("value")));
			}
		}

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.fetchObjectDefinition(
				objectDefinitionId);

		if (objectDefinition == null) {
			return settings;
		}

		JSONObject companyIdJSONObject = _jsonFactory.createJSONObject();

		companyIdJSONObject.put(
			"name", "companyId"
		).put(
			"value", objectDefinition.getCompanyId()
		);

		JSONObject externalReferenceJSONObject =
			_jsonFactory.createJSONObject();

		externalReferenceJSONObject.put(
			"name", "externalReferenceCode"
		).put(
			"value", objectDefinition.getExternalReferenceCode()
		);

		JSONArray updatedJSONArray = _jsonFactory.createJSONArray();
		boolean hasCompanyId = false;
		boolean hasExternalReferenceCode = false;

		for (int i = 0; i < fieldValuesJSONArray.length(); i++) {
			JSONObject fieldValueJSONObject = fieldValuesJSONArray.getJSONObject(i);

			if (StringUtil.equals(
				fieldValueJSONObject.getString(
					"name"
				),
				"companyId")) {

				hasCompanyId = true;
				updatedJSONArray.put(companyIdJSONObject);
			}
			else if (StringUtil.equals(
				fieldValueJSONObject.getString(
					"name"
				),
				"externalReferenceCode")) {

				hasExternalReferenceCode = true;
				updatedJSONArray.put(externalReferenceJSONObject);
			}
			else {
				updatedJSONArray.put(fieldValueJSONObject);
			}
		}

		if(!hasCompanyId) {
			updatedJSONArray.put(companyIdJSONObject);
		}

		if(!hasExternalReferenceCode) {
			updatedJSONArray.put(externalReferenceJSONObject);
		}

		settingsJSONObject.put("fieldValues", updatedJSONArray);

		return settingsJSONObject.toString();
	}

	private String _updateObjectDefinitionIdInDDMFormValues(
			String serializedSettingsDDMFormValues)
		throws Exception {

		JSONObject settingsJSONObject = _jsonFactory.createJSONObject(
			serializedSettingsDDMFormValues);

		JSONArray fieldValuesJSONArray = settingsJSONObject.getJSONArray(
			"fieldValues");

		long companyId = 0;
		String externalReferenceCode = null;

		for (Object fieldValue : fieldValuesJSONArray) {
			JSONObject fieldValueJSONObject = _jsonFactory.createJSONObject(
				fieldValue.toString());

			if (StringUtil.equals(
					fieldValueJSONObject.get(
						"name"
					).toString(),
					"companyId")) {

				companyId = GetterUtil.getLong(_removeSquareBrackets(fieldValueJSONObject.getString("value")), 0);
			}
			else if (StringUtil.equals(
						fieldValueJSONObject.getString(
							"name"
						),
						"externalReferenceCode")) {

				externalReferenceCode = _removeSquareBrackets(fieldValueJSONObject.getString("value"));
			}
		}

		if ((companyId == 0) || Validator.isNull(externalReferenceCode)) {
			return serializedSettingsDDMFormValues;
		}

		ObjectDefinition objectDefinitionByExternalReferenceCode =
			_objectDefinitionLocalService.
				fetchObjectDefinitionByExternalReferenceCode(
					externalReferenceCode, companyId);

		if (objectDefinitionByExternalReferenceCode == null) {
			return serializedSettingsDDMFormValues;
		}

		JSONArray updatedJSONArray = _jsonFactory.createJSONArray();

		for (Object fieldValue : fieldValuesJSONArray) {
			JSONObject fieldValueJSONObject = (JSONObject) fieldValue;

			if (StringUtil.equals(
					fieldValueJSONObject.getString(
						"name"
					),
					"objectDefinitionId")) {

				long objectDefinitionId =
					objectDefinitionByExternalReferenceCode.
						getObjectDefinitionId();

				if (objectDefinitionId > 0) {
					updatedJSONArray.put(fieldValueJSONObject.put(
						"value", _addSquareBrackets(String.valueOf(objectDefinitionId))));
				}

				continue;
			}

			updatedJSONArray.put(fieldValueJSONObject);
		}

		settingsJSONObject.put("fieldValues", updatedJSONArray);

		return settingsJSONObject.toString();
	}

	@Reference
	private DDMFormInstanceLocalService _ddmFormInstanceLocalService;

	@Reference(target = "(ddm.form.values.deserializer.type=json)")
	private DDMFormValuesDeserializer _jsonDDMFormValuesDeserializer;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference(
		target = "(model.class.name=com.liferay.dynamic.data.mapping.model.DDMFormInstance)"
	)
	private StagedModelRepository<DDMFormInstance> _stagedModelRepository;

}