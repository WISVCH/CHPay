package ch.wisv.chpay.qr.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class QRCodeUtil {

  /**
   * Generates a Base64-encoded QR code image for the given text with the specified width and
   * height.
   *
   * @param text the content to encode into the QR code
   * @param width the desired width of the QR code image in pixels
   * @param height the desired height of the QR code image in pixels
   * @return a Base64-encoded string representing the QR code image
   * @throws WriterException if an error occurs while encoding the text into a QR code
   * @throws IOException if an error occurs during the creation of the QR code image
   */
  public static String generateQRCodeBase64(String text, int width, int height)
      throws WriterException, IOException {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
    ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
    byte[] pngData = pngOutputStream.toByteArray();
    return Base64.getEncoder().encodeToString(pngData);
  }
}
