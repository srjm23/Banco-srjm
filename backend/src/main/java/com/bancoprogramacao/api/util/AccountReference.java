package com.bancoprogramacao.api.util;

import com.bancoprogramacao.api.exception.BankBusinessException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public record AccountReference(String number, String checkDigit) {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^([0-9]{3,7})-([0-9])$");

    public static AccountReference parse(String value) {
        Matcher matcher = REFERENCE_PATTERN.matcher(value == null ? "" : value.trim());
        if (!matcher.matches()) {
            throw new BankBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Informe a conta no formato numero-dv, por exemplo 261533-9."
            );
        }

        String number = matcher.group(1);
        String suppliedDigit = matcher.group(2);
        String calculatedDigit = CheckDigitCalculator.calculate(number);
        if (!suppliedDigit.equals(calculatedDigit)) {
            throw new BankBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Dígito verificador inválido para a conta informada."
            );
        }
        return new AccountReference(number, suppliedDigit);
    }

    public String value() {
        return number + "-" + checkDigit;
    }
}

