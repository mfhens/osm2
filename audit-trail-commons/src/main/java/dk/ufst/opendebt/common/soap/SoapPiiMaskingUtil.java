package dk.ufst.opendebt.common.soap;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SoapPiiMaskingUtil {

  public String mask(String soapEnvelopeXml) {
    if (soapEnvelopeXml == null || soapEnvelopeXml.isBlank()) {
      return soapEnvelopeXml;
    }
    String result = soapEnvelopeXml;
    result = maskElement(result, "CPRNummer");
    result = maskElement(result, "cprNummer");
    result = maskElement(result, "SkyldnerCPR");
    result = maskElement(result, "BankKontoNummer");
    result = maskElement(result, "KontoNummer");
    result = maskElement(result, "Navn");
    result = maskElement(result, "SkyldnerNavn");
    return result;
  }

  private String maskElement(String xml, String elementName) {
    Pattern p =
        Pattern.compile(
            "<([^:>]+:)?"
                + Pattern.quote(elementName)
                + ">([^<]*)</([^:>]+:)?"
                + Pattern.quote(elementName)
                + ">",
            Pattern.CASE_INSENSITIVE);
    return p.matcher(xml)
        .replaceAll(
            m -> {
              String prefix1 = m.group(1) != null ? m.group(1) : "";
              String prefix2 = m.group(3) != null ? m.group(3) : "";
              return "<" + prefix1 + elementName + ">[MASKED]</" + prefix2 + elementName + ">";
            });
  }
}
