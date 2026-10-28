package com.example.service

import java.util.Locale

/**
 * Official EMVCo QRIS Merchant Presented Mode (MPM) Generator for BDS Coffee Co.
 * Implements standard TLV parsing, dynamic amount injection (Tag 54),
 * and CRC16-CCITT (0x1021) recalculation.
 */
object QrisPayloadGenerator {

    const val DEFAULT_MERCHANT_NAME = "BDS COFFEE CO"
    const val DEFAULT_NMID = "ID1024365195063"
    const val DEFAULT_TERMINAL = "A01"
    const val DEFAULT_CITY = "JAKARTA"
    const val DEFAULT_POSTAL_CODE = "12190"

    /**
     * Exact Official Base Raw Static QRIS Payload string for BDS Coffee Co (NMID: ID1024365195063).
     * Includes acquirer Mandiri switch 93600008, merchant ID 71709175308, and terminal A01.
     */
    const val OFFICIAL_STATIC_QRIS_PAYLOAD =
        "00020101021126620014ID.CO.QRIS.WWW01189360000801709175300211717091753080303UMI51440014ID.CO.QRIS.WWW0115ID102436519506302030005204581253033605802ID5913BDS COFFEE CO6007JAKARTA61051219062070703A0163042590"

    /**
     * Parses an EMVCo QRIS string into a map of Tag to Value, preserving tag order.
     */
    fun parseTlv(raw: String): Pair<LinkedHashMap<String, String>, List<String>> {
        val tags = LinkedHashMap<String, String>()
        val order = mutableListOf<String>()
        val end = if (raw.contains("6304")) raw.lastIndexOf("6304") else raw.length
        var i = 0

        while (i + 4 <= end) {
            val tag = raw.substring(i, i + 2)
            val lenStr = raw.substring(i + 2, i + 4)
            val len = lenStr.toIntOrNull() ?: break
            if (i + 4 + len > end) break
            val value = raw.substring(i + 4, i + 4 + len)
            tags[tag] = value
            if (!order.contains(tag)) {
                order.add(tag)
            }
            i += 4 + len
        }

        return Pair(tags, order)
    }

    /**
     * Core conversion utility: Converts any static QRIS payload into a valid, scannable Dynamic QRIS payload.
     * 1. Parses TLV blocks from the static string.
     * 2. Sets Point of Initiation (Tag 01) to "12" (Dynamic).
     * 3. Injects Tag 54 with formatted nominal amount (e.g. for Rp 29.400 -> "540529400").
     * 4. Assembles tags and recalculates standard CRC16-CCITT (poly 0x1021, init 0xFFFF).
     */
    fun convertToDynamicQRIS(
        staticString: String = OFFICIAL_STATIC_QRIS_PAYLOAD,
        amount: Double,
        invoiceNumber: String = ""
    ): String {
        val (tags, order) = parseTlv(staticString)

        // 1. Set Point of Initiation to Dynamic (12)
        tags["01"] = "12"

        // 2. Format and inject Tag 54 (Amount without decimal points for IDR)
        val amountInt = amount.toLong().coerceAtLeast(1)
        val amountStr = amountInt.toString()
        tags["54"] = amountStr

        // Optional: If invoice number is provided, update Tag 62 with invoice reference sub-tag 01
        if (invoiceNumber.isNotBlank()) {
            val currentTag62 = tags["62"] ?: ""
            val terminalVal = if (currentTag62.contains("0703A01")) "A01" else DEFAULT_TERMINAL
            val newTag62 = formatTlv("01", invoiceNumber.take(20)) + formatTlv("07", terminalVal)
            tags["62"] = newTag62
        }

        // Build new tag ordering: Tag 54 placed right after Tag 53 (Currency) or before Tag 58 (Country)
        val newOrder = mutableListOf<String>()
        var inserted54 = false

        for (t in order) {
            if (t == "63" || t == "54") continue
            newOrder.add(t)
            if (t == "53") {
                newOrder.add("54")
                inserted54 = true
            }
        }

        if (!inserted54) {
            val idx58 = newOrder.indexOf("58")
            if (idx58 != -1) {
                newOrder.add(idx58, "54")
            } else {
                newOrder.add("54")
            }
        }

        // Assemble TLV string
        val sb = StringBuilder()
        for (t in newOrder) {
            val v = tags[t] ?: continue
            sb.append(formatTlv(t, v))
        }

        // Append 6304 and calculate CRC16-CCITT
        val payloadToCrc = sb.toString() + "6304"
        val crc = calculateCrc16Ccitt(payloadToCrc)
        return payloadToCrc + crc
    }

    /**
     * Backward-compatible helper method used throughout the app.
     */
    fun generateDynamicQris(
        invoiceNumber: String,
        amount: Double,
        merchantName: String = DEFAULT_MERCHANT_NAME,
        nmid: String = DEFAULT_NMID,
        terminalId: String = DEFAULT_TERMINAL,
        merchantCity: String = DEFAULT_CITY,
        postalCode: String = DEFAULT_POSTAL_CODE
    ): String {
        val staticBase = generateStaticQris(merchantName, nmid, terminalId, merchantCity, postalCode)
        return convertToDynamicQRIS(
            staticString = staticBase,
            amount = amount,
            invoiceNumber = invoiceNumber
        )
    }

    /**
     * Retrieves the official static QRIS payload.
     * Uses the exact hardcoded official raw payload for BDS Coffee Co, or constructs custom if parameters differ.
     */
    fun generateStaticQris(
        merchantName: String = DEFAULT_MERCHANT_NAME,
        nmid: String = DEFAULT_NMID,
        terminalId: String = DEFAULT_TERMINAL,
        merchantCity: String = DEFAULT_CITY,
        postalCode: String = DEFAULT_POSTAL_CODE
    ): String {
        if (merchantName == DEFAULT_MERCHANT_NAME && nmid == DEFAULT_NMID && terminalId == DEFAULT_TERMINAL) {
            return OFFICIAL_STATIC_QRIS_PAYLOAD
        }

        val tag26 = formatTlv("00", "ID.CO.QRIS.WWW") +
                formatTlv("01", "936000080170917530") +
                formatTlv("02", "71709175308") +
                formatTlv("03", "UMI")
        val tag51 = formatTlv("00", "ID.CO.QRIS.WWW") +
                formatTlv("01", nmid) +
                formatTlv("02", "000")
        val tag62 = formatTlv("07", terminalId)

        val builder = StringBuilder()
        builder.append(formatTlv("00", "01")) // Format Indicator: 01
        builder.append(formatTlv("01", "11")) // Point of Initiation: 11 (Static QR)
        builder.append(formatTlv("26", tag26))
        builder.append(formatTlv("51", tag51))
        builder.append(formatTlv("52", "5812")) // MCC: F&B
        builder.append(formatTlv("53", "360")) // Currency: IDR
        builder.append(formatTlv("58", "ID")) // Country: ID
        builder.append(formatTlv("59", merchantName.take(25)))
        builder.append(formatTlv("60", merchantCity.take(15)))
        builder.append(formatTlv("61", postalCode))
        builder.append(formatTlv("62", tag62))

        val payloadWithoutCrc = builder.toString() + "6304"
        val crc = calculateCrc16Ccitt(payloadWithoutCrc)
        return payloadWithoutCrc + crc
    }

    fun formatTlv(tag: String, value: String): String {
        val lengthStr = String.format(Locale.US, "%02d", value.length)
        return "$tag$lengthStr$value"
    }

    /**
     * CRC16-CCITT (False): Polynomial 0x1021, Initial value 0xFFFF.
     * Complies with ISO/IEC 13239 and EMVCo specifications.
     */
    fun calculateCrc16Ccitt(data: String): String {
        val bytes = data.toByteArray(Charsets.ISO_8859_1)
        var crc = 0xFFFF
        val polynomial = 0x1021

        for (b in bytes) {
            crc = crc xor (b.toInt() and 0xFF shl 8)
            for (i in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor polynomial
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return String.format(Locale.US, "%04X", crc)
    }

    /**
     * Validates if a QRIS string has a valid format and checksum.
     */
    fun isValidQrisPayload(payload: String): Boolean {
        if (payload.length < 20 || !payload.contains("6304")) return false
        val pos63 = payload.lastIndexOf("6304")
        if (pos63 + 8 != payload.length) return false
        val expectedCrc = payload.takeLast(4).uppercase(Locale.US)
        val dataToCrc = payload.substring(0, pos63 + 4)
        val calculatedCrc = calculateCrc16Ccitt(dataToCrc)
        return expectedCrc == calculatedCrc
    }
}
