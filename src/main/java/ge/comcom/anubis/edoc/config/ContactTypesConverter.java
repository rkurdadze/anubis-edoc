package ge.comcom.anubis.edoc.config;

import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ContactTypes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ContactTypesConverter implements Converter<String, ContactTypes> {

    @Override
    public ContactTypes convert(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        try {
            return ContactTypes.fromValue(source);
        } catch (IllegalArgumentException ex) {
            return ContactTypes.valueOf(source.trim().toUpperCase());
        }
    }
}
