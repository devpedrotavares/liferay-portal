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

package com.liferay.object.internal.field.business.type;

import com.liferay.dynamic.data.mapping.form.field.type.constants.DDMFormFieldTypeConstants;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.constants.ObjectFieldSettingConstants;
import com.liferay.object.field.business.type.ObjectFieldBusinessType;
import com.liferay.object.field.setting.util.ObjectFieldSettingUtil;
import com.liferay.object.model.ObjectField;
import com.liferay.object.model.ObjectFieldSetting;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.vulcan.extension.PropertyDefinition;

import java.text.ParseException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcela Cunha
 */
@Component(
	property = "object.field.business.type.key=" + ObjectFieldConstants.BUSINESS_TYPE_DATE_TIME,
	service = ObjectFieldBusinessType.class
)
public class DateTimeObjectFieldBusinessType
	implements ObjectFieldBusinessType {

	@Override
	public Set<String> getAllowedObjectFieldSettingsNames() {
		return Collections.singleton(
			ObjectFieldSettingConstants.NAME_TIME_STORAGE);
	}

	@Override
	public String getDBType() {
		return "DateTime";
	}

	@Override
	public String getDDMFormFieldTypeName() {
		return DDMFormFieldTypeConstants.DATE_TIME;
	}

	@Override
	public String getDescription(Locale locale) {
		return _language.get(locale, "add-date-and-time-value");
	}

	@Override
	public String getLabel(Locale locale) {
		return _language.get(locale, "date-and-time");
	}

	@Override
	public String getName() {
		return ObjectFieldConstants.BUSINESS_TYPE_DATE_TIME;
	}

	@Override
	public PropertyDefinition.PropertyType getPropertyType() {
		return PropertyDefinition.PropertyType.DATE_TIME;
	}

	@Override
	public Set<String> getRequiredObjectFieldSettingsNames(
		ObjectField objectField) {

		return Collections.singleton(
			ObjectFieldSettingConstants.NAME_TIME_STORAGE);
	}

	public Set<String> getUnmodifiableObjectFieldSettingsNames() {
		return Collections.singleton(
			ObjectFieldSettingConstants.NAME_TIME_STORAGE);
	}

	@Override
	public Object getValue(
			ObjectField objectField, long userId, Map<String, Object> values)
		throws PortalException {

		if (!values.containsKey(objectField.getName())) {
			return null;
		}

		String objectFieldSettingValue = ObjectFieldSettingUtil.getValue(
			ObjectFieldSettingConstants.NAME_TIME_STORAGE, objectField);

		String value = String.valueOf(values.get(objectField.getName()));

		User user = _userLocalService.getUser(userId);

		if (StringUtil.equals(
				objectFieldSettingValue,
				ObjectFieldSettingConstants.VALUE_CONVERT_TO_UTC)) {

			try {
				return DateUtil.parseDate(
					"yyyy-MM-dd'T'HH:mm:ss'Z'", value, user.getLocale());
			}
			catch (ParseException parseException) {
				if (_log.isDebugEnabled()) {
					_log.debug(parseException);
				}

				/*	ZonedDateTime zonedDateTime = ZonedDateTime.of(
						LocalDateTime.parse(
							value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
						ZoneId.of(user.getTimeZoneId()));*/

				ZonedDateTime zonedDateTime = ZonedDateTime.of(
					LocalDateTime.parse(
						value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
					ZoneId.of(user.getTimeZoneId()));

				return LocalDateTime.ofInstant(
					zonedDateTime.toInstant(), ZoneOffset.UTC);
			}
		}
		else if (StringUtil.equals(
					objectFieldSettingValue,
					ObjectFieldSettingConstants.VALUE_USE_INPUT_AS_ENTERED)) {

			try {
				return DateUtil.parseDate(
					"yyyy-MM-dd'T'HH:mm:ss", value, user.getLocale());
			}
			catch (ParseException parseException1) {
				if (_log.isDebugEnabled()) {
					_log.debug(parseException1);
				}

				try {
					return DateUtil.parseDate(
						"yyyy-MM-dd HH:mm", value, user.getLocale());
				}
				catch (ParseException parseException2) {
					throw new BadRequestException(
						"Unable to parse date that does not conform to ISO-8601",
						parseException2);
				}
			}
		}

		return null;
	}

	@Override
	public void validateObjectFieldSettings(
			ObjectField objectField,
			List<ObjectFieldSetting> objectFieldSettings)
		throws PortalException {

		ObjectFieldBusinessType.super.validateObjectFieldSettings(
			objectField, objectFieldSettings);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DateTimeObjectFieldBusinessType.class);

	@Reference
	private Language _language;

	@Reference
	private UserLocalService _userLocalService;

}