package com.bancoprogramacao.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.domain.Client;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatementDocumentServiceTest {

    private final StatementDocumentService service = new StatementDocumentService();

    @ParameterizedTest
    @CsvSource({
            "pdf,application/pdf,%PDF",
            "png,image/png,PNG",
            "jpeg,image/jpeg,JPEG"
    })
    void generatesStatementDownloads(String format, String mediaType, String expectedSignature) {
        Account account = new Account("261533", "9", new Client("Cliente SRJM"), "hash");
        StatementDocumentService.Download download = service.generate(
                new BankService.StatementData(account, List.of()),
                format
        );

        assertThat(download.mediaType()).isEqualTo(mediaType);
        assertThat(download.filename()).endsWith("." + format);
        assertThat(download.content()).isNotEmpty();
        if ("%PDF".equals(expectedSignature)) {
            assertThat(new String(download.content(), 0, 4)).isEqualTo(expectedSignature);
        } else if ("PNG".equals(expectedSignature)) {
            assertThat(download.content()[1]).isEqualTo((byte) 'P');
            assertThat(download.content()[2]).isEqualTo((byte) 'N');
            assertThat(download.content()[3]).isEqualTo((byte) 'G');
        } else {
            assertThat(download.content()[0]).isEqualTo((byte) 0xFF);
            assertThat(download.content()[1]).isEqualTo((byte) 0xD8);
        }
    }
}
