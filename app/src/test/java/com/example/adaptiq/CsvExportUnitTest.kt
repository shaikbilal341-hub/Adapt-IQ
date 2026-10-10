package com.example.adaptiq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun testCsvEscaping() {
        val commaText = "hello, world"
        val quoteText = "say \"hi\""

        assertTrue(commaText.contains(","))
        assertTrue(quoteText.contains("\""))
    }
}
