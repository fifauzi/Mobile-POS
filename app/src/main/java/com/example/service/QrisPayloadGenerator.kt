package com.example.service

import java.util.Locale

object QrisPayloadGenerator {

    /**
     * Generates a fully compliant EMVCo Dynamic QRIS payload string for Indonesian merchants.
     * Contains exact transaction amount and invoice reference with CRC16-CCITT checksum.
     */
    fun generateDynamicQris(
        invoiceNumber: String,
        amount: Double,
        merchantName: String = "CAFFEINERY COFFEE",
        merchantCity: String = "JAKARTA",
        postalCode: String = "12190"
    ): String {
        val amountInt = amount.toLong()
        val amountStr = amountInt.toString()

        // Merchant Account Info (Tag 26) Sub-tags
        val nmid = "ID1024356789012"
        val tag26Sub00 = formatTlv("00", "ID.CO.QRIS.WWW")
        val tag26Sub01 = formatTlv("01", nmid)
        val tag26Sub02 = formatTlv("02", "0000000001")
        val tag26Sub03 = formatTlv("03", "UMI") // Usaha Mikro
        val tag26Content = tag26Sub00 + tag26Sub01 + tag26Sub02 + tag26Sub03

        // Additional Data Field (Tag 62) Sub-tags
        val tag62Sub01 = formatTlv("01", invoiceNumber) // Bill Number / Invoice
        val tag62Sub07 = formatTlv("07", "TERM01") // Terminal ID

        val builder = StringBuilder()
        builder.append(formatTlv("00", "01")) // Payload Format Indicator
        builder.append(formatTlv("01", "12")) // Point of Initiation: 12 (Dynamic QR)
        builder.append(formatTlv("26", tag26Content))
        builder.append(formatTlv("51", formatTlv("00", "ID.CO.QRIS.WWW") + formatTlv("02", "0000000001")))
        builder.append(formatTlv("52", "5812")) // Eating Places / F&B
        builder.append(formatTlv("53", "360")) // IDR
        builder.append(formatTlv("54", amountStr)) // Exact Transaction Amount
        builder.append(formatTlv("58", "ID")) // Country
        builder.append(formatTlv("59", merchantName.take(25))) // Merchant Name
        builder.append(formatTlv("60", merchantCity.take(15))) // Merchant City
        builder.append(formatTlv("61", postalCode))
        builder.append(formatTlv("62", tag62Sub01 + tag62Sub07))

        // Append Tag 63 with length 04 for CRC
        val payloadWithoutCrc = builder.toString() + "6304"
        val crc = calculateCrc16Ccitt(payloadWithoutCrc)
        return payloadWithoutCrc + crc
    }

    private fun formatTlv(tag: String, value: String): String {
        val lengthStr = String.format(Locale.US, "%02d", value.length)
        return "$tag$lengthStr$value"
    }

    /**
     * CRC16-CCITT (False): Polynomial 0x1021, Initial value 0xFFFF.
     * Required by ISO/IEC 13239 and EMVCo specifications.
     */
    fun calculateCrc16Ccitt(data: String): String {
        val bytes = data.toByteArray(Charsets.ISO_8859_1)
        var crc = 0xFFFF
        val polynomial = 0x1021

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = ((b.toInt() shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.US, "%04X", crc)
    }
}
