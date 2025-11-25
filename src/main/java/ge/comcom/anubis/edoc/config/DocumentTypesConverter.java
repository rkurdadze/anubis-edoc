package ge.comcom.anubis.edoc.config;

import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DocumentTypesConverter implements Converter<String, DocumentTypes> {

    @Override
    public DocumentTypes convert(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        try {
            return DocumentTypes.fromValue(source);
        } catch (IllegalArgumentException ex) {
            return DocumentTypes.valueOf(source.trim().toUpperCase());
        }
    }
}
