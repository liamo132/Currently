package com.currently.currently_backend.persistence;

import com.currently.currently_backend.util.DataProtectionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return DataProtectionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return DataProtectionUtil.decrypt(dbData);
    }
}
