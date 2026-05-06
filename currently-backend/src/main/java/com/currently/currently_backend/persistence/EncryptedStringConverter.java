package com.currently.currently_backend.persistence;

import com.currently.currently_backend.util.DataProtectionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    // Database converter: encrypts sensitive entity strings before JPA writes them to PostgreSQL.
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return DataProtectionUtil.encrypt(attribute);
    }

    // Database converter: decrypts sensitive entity strings after JPA reads them from PostgreSQL.
    @Override
    public String convertToEntityAttribute(String dbData) {
        return DataProtectionUtil.decrypt(dbData);
    }
}
