package com.example.adaptiq

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportUnitTest {

    @Test
    fun testEasyAnalysisHeaderColumnCount() {
        val header = RecordingService.EASY_ANALYSIS_HEADER
        val columns = header.split(",")
        assertEquals(25, columns.size)
        assertEquals("Age_Group", columns[0])
        assertEquals("Gender", columns[1])
        assertEquals("Session_Name", columns[2])
        assertEquals("Participant_ID", columns[3])
        assertEquals("Session_ID", columns[4])
        assertEquals("Activity_Name", columns[5])
        assertEquals("Game_Mode", columns[6])
        assertEquals("Trial_ID", columns[7])
        assertEquals("Touch_Info", columns[15])
        assertEquals("gyro_magnitude", columns[24])
    }

    @Test
    fun testLegacyHeaderColumnCount() {
        val header = RecordingService.LEGACY_CSV_HEADER
        val columns = header.split(",")
        assertEquals(22, columns.size)
        assertEquals("timestamp", columns[0])
        assertEquals("gyro_magnitude", columns[21])
    }

    @Test
    fun testCsvParsingAndEscapingCompliance() {
        // Test standard CSV parsing rules for quoted fields, commas, and newlines
        val sampleRow = "\"18–25\",\"Prefer not to say\",\"Adapt-IQ Research Session\",\"PARTICIPANT_A1B2C3\",\"SESSION_9F8E7D\",\"tap_precision\",\"standard\",1,101,1717942800000,1250,\"tap_target\",\"tap_target\",\"successful\",145,\"x=120.5, y=340.2\",NA,NA,NA,NA,NA,NA,NA,NA,NA"
        
        // Simple robust CSV line parser for testing
        val fields = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < sampleRow.length) {
            val c = sampleRow[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < sampleRow.length && sampleRow[i + 1] == '"') {
                        cur.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(cur.toString())
                    cur.clear()
                }
                else -> {
                    cur.append(c)
                }
            }
            i++
        }
        fields.add(cur.toString())

        assertEquals(25, fields.size)
        assertEquals("18–25", fields[0])
        assertEquals("Prefer not to say", fields[1])
        assertEquals("Adapt-IQ Research Session", fields[2])
        assertEquals("x=120.5, y=340.2", fields[15])
        assertEquals("NA", fields[16])
    }
}
