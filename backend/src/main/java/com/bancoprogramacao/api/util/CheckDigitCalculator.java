package com.bancoprogramacao.api.util;

import com.bancoprogramacao.api.exception.BankBusinessException;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class CheckDigitCalculator {

    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^[0-9]{3,7}$");

    private CheckDigitCalculator() {
    }

    public static String calculate(String accountNumber) {
        if (accountNumber == null || !ACCOUNT_NUMBER.matcher(accountNumber).matches()) {
            throw new BankBusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O número da conta deve possuir de 3 a 7 dígitos."
            );
        }

        int sum = 0;
        int multiplier = 2;
        for (int index = accountNumber.length() - 1; index >= 0; index--) {
            sum += Character.digit(accountNumber.charAt(index), 10) * multiplier;
            multiplier++;
        }

        int remainder = (sum * 10) % 11;
        return Integer.toString(remainder % 10);
    }
}

