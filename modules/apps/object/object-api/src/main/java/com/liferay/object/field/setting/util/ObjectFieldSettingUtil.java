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

package com.liferay.object.field.setting.util;

import com.liferay.dynamic.data.mapping.expression.CreateExpressionRequest;
import com.liferay.dynamic.data.mapping.expression.DDMExpression;
import com.liferay.dynamic.data.mapping.expression.DDMExpressionException;
import com.liferay.dynamic.data.mapping.expression.DDMExpressionFactory;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.constants.ObjectFieldSettingConstants;
import com.liferay.object.model.ObjectField;
import com.liferay.object.model.ObjectFieldSetting;
import com.liferay.object.service.ObjectFieldSettingLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Carolina Barbosa
 */
public class ObjectFieldSettingUtil {

	public static String getDefaultValueAsString(
		DDMExpressionFactory ddmExpressionFactory, long objectFieldId,
		ObjectFieldSettingLocalService objectFieldSettingLocalService,
		Map<String, Object> values) {

		ObjectFieldSetting defaultValueObjectFieldSetting =
			objectFieldSettingLocalService.fetchObjectFieldSetting(
				objectFieldId, ObjectFieldSettingConstants.NAME_DEFAULT_VALUE);

		if (defaultValueObjectFieldSetting == null) {
			return null;
		}

		ObjectFieldSetting defaultValueTypeObjectFieldSetting =
			objectFieldSettingLocalService.fetchObjectFieldSetting(
				objectFieldId,
				ObjectFieldSettingConstants.NAME_DEFAULT_VALUE_TYPE);

		if ((defaultValueTypeObjectFieldSetting == null) ||
			StringUtil.equals(
				defaultValueTypeObjectFieldSetting.getValue(),
				ObjectFieldSettingConstants.VALUE_INPUT_AS_VALUE)) {

			return defaultValueObjectFieldSetting.getValue();
		}

		if (ddmExpressionFactory == null) {
			return StringPool.BLANK;
		}

		try {
			DDMExpression<String> ddmExpression =
				ddmExpressionFactory.createExpression(
					CreateExpressionRequest.Builder.newBuilder(
						defaultValueObjectFieldSetting.getValue()
					).build());

			ddmExpression.setVariables(values);

			return ddmExpression.evaluate();
		}
		catch (DDMExpressionException ddmExpressionException) {
			if (_log.isDebugEnabled()) {
				_log.debug(ddmExpressionException);
			}

			return StringPool.BLANK;
		}
	}

	public static String getValue(String name, ObjectField objectField) {
		for (ObjectFieldSetting objectFieldSetting :
				objectField.getObjectFieldSettings()) {

			if (Objects.equals(objectFieldSetting.getName(), name)) {
				return objectFieldSetting.getValue();
			}
		}

		return null;
	}

	public static String getTimeZoneAsString(
		List<ObjectFieldSetting> objectFieldSettings, User user) {

		if(user == null || ListUtil.isNull(objectFieldSettings)){
			return null;
		}

		for (ObjectFieldSetting objectFieldSetting : objectFieldSettings) {

			if (!Objects.equals(objectFieldSetting.getName(),
				ObjectFieldSettingConstants.NAME_TIME_STORAGE)) {
				continue;
			}

			if(StringUtil.equals(objectFieldSetting.getValue(),
				ObjectFieldSettingConstants.VALUE_CONVERT_TO_UTC)){

				return StringPool.UTC;
			}
			else if(StringUtil.equals(objectFieldSetting.getValue(),
				ObjectFieldSettingConstants.VALUE_USE_INPUT_AS_ENTERED)){

				return user.getTimeZoneId();
			}
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ObjectFieldSettingUtil.class);

}