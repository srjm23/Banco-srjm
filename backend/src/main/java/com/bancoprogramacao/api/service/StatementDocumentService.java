package com.bancoprogramacao.api.service;

import com.bancoprogramacao.api.domain.Account;
import com.bancoprogramacao.api.domain.BankTransaction;
import com.bancoprogramacao.api.domain.TransactionDirection;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

@Service
public class StatementDocumentService {

    private static final int WIDTH = 1240;
    private static final int ROW_HEIGHT = 68;
    private static final int TRANSACTIONS_PER_PDF_PAGE = 12;
    private static final ZoneId BANK_TIME_ZONE = ZoneId.of("America/Recife");
    private static final DateTimeFormatter GENERATED_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy 'às' HH:mm:ss")
            .withZone(BANK_TIME_ZONE);
    private static final DateTimeFormatter TRANSACTION_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(BANK_TIME_ZONE);

    public Download generate(BankService.StatementData statement, String requestedFormat) {
        StatementFormat format = StatementFormat.parse(requestedFormat);
        Instant generatedAt = Instant.now();
        try {
            byte[] content = switch (format) {
                case PDF -> createPdf(statement, generatedAt);
                case PNG -> createImage(statement, generatedAt, "png");
                case JPEG -> createImage(statement, generatedAt, "jpeg");
            };
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .withZone(BANK_TIME_ZONE)
                    .format(generatedAt);
            return new Download(
                    content,
                    format.mediaType,
                    "extrato-banco-srjm-" + statement.account().getAccountReference() + "-" + timestamp + "." + format.extension
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o arquivo do extrato.", exception);
        }
    }

    private byte[] createImage(BankService.StatementData statement, Instant generatedAt, String format) throws IOException {
        BufferedImage image = renderPage(statement.account(), statement.transactions(), generatedAt, 1, 1);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return output.toByteArray();
        }
    }

    private byte[] createPdf(BankService.StatementData statement, Instant generatedAt) throws IOException {
        List<BankTransaction> transactions = statement.transactions();
        int pageCount = Math.max(1, (transactions.size() + TRANSACTIONS_PER_PDF_PAGE - 1) / TRANSACTIONS_PER_PDF_PAGE);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                int from = pageIndex * TRANSACTIONS_PER_PDF_PAGE;
                int to = Math.min(from + TRANSACTIONS_PER_PDF_PAGE, transactions.size());
                List<BankTransaction> pageTransactions = transactions.subList(from, to);
                BufferedImage pageImage = renderPage(statement.account(), pageTransactions, generatedAt, pageIndex + 1, pageCount);
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                PDImageXObject pdfImage = LosslessFactory.createFromImage(document, pageImage);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.drawImage(pdfImage, 0, 0, PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private BufferedImage renderPage(
            Account account,
            List<BankTransaction> transactions,
            Instant generatedAt,
            int pageNumber,
            int pageCount
    ) {
        int height = Math.max(900, 500 + Math.max(1, transactions.size()) * ROW_HEIGHT);
        BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 247, 251));
            graphics.fillRect(0, 0, WIDTH, height);

            graphics.setColor(new Color(17, 31, 65));
            graphics.fillRoundRect(48, 42, WIDTH - 96, 205, 28, 28);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
            graphics.drawString("BANCO SRJM", 88, 105);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            graphics.setColor(new Color(190, 204, 244));
            graphics.drawString("Extrato bancário", 88, 143);
            graphics.drawString("Emitido em " + GENERATED_FORMAT.format(generatedAt), 88, 203);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            graphics.drawString(account.getClient().getFullName(), 700, 105);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            graphics.setColor(new Color(190, 204, 244));
            graphics.drawString("Conta " + account.getAccountReference(), 700, 140);
            graphics.drawString("Saldo atual", 700, 178);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            graphics.setColor(Color.WHITE);
            graphics.drawString(formatMoney(account.getBalance()), 700, 213);

            int tableTop = 292;
            graphics.setColor(new Color(112, 128, 158));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            graphics.drawString("MOVIMENTAÇÃO", 76, tableTop);
            graphics.drawString("DATA E HORA", 520, tableTop);
            graphics.drawString("VALOR", 835, tableTop);
            graphics.drawString("SALDO", 1020, tableTop);

            int rowTop = tableTop + 28;
            if (transactions.isEmpty()) {
                graphics.setColor(Color.WHITE);
                graphics.fillRoundRect(48, rowTop, WIDTH - 96, ROW_HEIGHT, 18, 18);
                graphics.setColor(new Color(112, 128, 158));
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
                graphics.drawString("Nenhuma movimentação registrada.", 76, rowTop + 42);
            } else {
                for (int index = 0; index < transactions.size(); index++) {
                    drawTransaction(graphics, transactions.get(index), rowTop + index * ROW_HEIGHT);
                }
            }

            graphics.setColor(new Color(112, 128, 158));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            graphics.drawString("Documento gerado eletronicamente pelo Banco SRJM.", 48, height - 42);
            graphics.drawString("Página " + pageNumber + " de " + pageCount, WIDTH - 150, height - 42);
            return image;
        } finally {
            graphics.dispose();
        }
    }

    private void drawTransaction(Graphics2D graphics, BankTransaction transaction, int y) {
        boolean credit = transaction.getDirection() == TransactionDirection.C;
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(48, y, WIDTH - 96, ROW_HEIGHT - 8, 16, 16);
        graphics.setColor(credit ? new Color(30, 170, 120) : new Color(215, 78, 96));
        graphics.fillOval(72, y + 14, 32, 32);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        graphics.drawString(credit ? "+" : "−", 82, y + 37);

        String label = transaction.getTransactionType().name();
        if (transaction.getCounterpartyAccount() != null) {
            label += " · Conta " + transaction.getCounterpartyAccount();
        }
        graphics.setColor(new Color(21, 33, 58));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        graphics.drawString(label, 122, y + 29);
        graphics.setColor(new Color(112, 128, 158));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        graphics.drawString(transaction.getDescription(), 122, y + 50);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        graphics.drawString(TRANSACTION_FORMAT.format(transaction.getCreatedAt()), 520, y + 36);
        graphics.setColor(credit ? new Color(30, 150, 105) : new Color(200, 65, 84));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        graphics.drawString((credit ? "+ " : "− ") + formatMoney(transaction.getAmount()), 835, y + 36);
        graphics.setColor(new Color(21, 33, 58));
        graphics.drawString(formatMoney(transaction.getBalanceAfter()), 1020, y + 36);
    }

    private String formatMoney(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    public record Download(byte[] content, String mediaType, String filename) {
    }

    private enum StatementFormat {
        PDF("application/pdf", "pdf"),
        PNG("image/png", "png"),
        JPEG("image/jpeg", "jpeg");

        private final String mediaType;
        private final String extension;

        StatementFormat(String mediaType, String extension) {
            this.mediaType = mediaType;
            this.extension = extension;
        }

        private static StatementFormat parse(String value) {
            try {
                return StatementFormat.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new com.bancoprogramacao.api.exception.BankBusinessException(
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                        "O formato deve ser pdf, png ou jpeg."
                );
            }
        }
    }
}
