package com.example.adaptiq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CsvExportUnitTest {

    @Test
    fun testEasyAnalysisHeaderColumnCount() {
        val header = RecordingService.EASY_ANALYSIS_HEADER
        val columns = header.split(",")
        assertEquals(25, columns.size)
        assertEquals("Age_Years", columns[0])
        assertEquals("Gender", columns[1])
        assertEquals("Session_Name", columns[2])
        assertEquals("Participant_ID", columns[3])
        assertEquals("Session_ID", columns[4])
        assertEquals("Activity_Name", columns[5])
        assertEquals("Game_Mode", columns[6])
        assertEquals("Trial_ID", columns[7])
        assertEquals("Event_ID", columns[8])
        assertEquals("Timestamp_UTC", columns[9])
        assertEquals("Elapsed_Time_ms", columns[10])
        assertEquals("Expected_Action", columns[11])
        assertEquals("Detected_Action", columns[12])
        assertEquals("Outcome", columns[13])
        assertEquals("Response_Latency_ms", columns[14])
        assertEquals("Touch_Info", columns[15])
        assertEquals("acc_x", columns[16])
        assertEquals("acc_y", columns[17])
        assertEquals("acc_z", columns[18])
        assertEquals("gyro_x", columns[19])
        assertEquals("gyro_y", columns[20])
        assertEquals("gyro_z", columns[21])
        assertEquals("proximity", columns[22])
        assertEquals("acc_magnitude", columns[23])
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
        val sampleRow = "25,\"Prefer not to say\",\"Adapt-IQ Research Session\",\"PARTICIPANT_A1B2C3\",\"SESSION_9F8E7D\",\"tap_precision\",\"standard\",1,101,1717942800000,1250,\"tap_target\",\"tap_target\",\"successful\",145,\"x=120.5, y=340.2\",NA,NA,NA,NA,NA,NA,NA,NA,NA"
        val fields = parseCsvRow(sampleRow)

        assertEquals(25, fields.size)
        assertEquals("25", fields[0])
        assertEquals("Prefer not to say", fields[1])
        assertEquals("Adapt-IQ Research Session", fields[2])
        assertEquals("PARTICIPANT_A1B2C3", fields[3])
        assertEquals("SESSION_9F8E7D", fields[4])
        assertEquals("tap_precision", fields[5])
        assertEquals("standard", fields[6])
        assertEquals("1", fields[7])
        assertEquals("tap_target", fields[11])
        assertEquals("successful", fields[13])
        assertEquals("145", fields[14])
        assertEquals("x=120.5, y=340.2", fields[15])
        assertEquals("NA", fields[16])
        assertEquals("NA", fields[24])
    }

    @Test
    fun testHoldingPhoneActionEventRowFormatting() {
        val ageYears = 28
        val gender = "Female"
        val sessionName = "Adapt-IQ Research Session"
        val participantId = "PARTICIPANT_H1P2"
        val sessionId = "SESSION_TEST01"
        val activity = "holding_phone"
        val gameMode = "standard"
        val attemptId = 1
        val eventId = 1001L
        val timestamp = 1717943000000L
        val elapsedMs = 15000L
        val expectedAction = "hold_phone_steady"
        val detectedAction = "held_steady"
        val result = "successful"
        val durationMs = 15000L
        val touchInfo = "duration_s=15,stability_pct=94.5"

        val row = String.format(
            Locale.US,
            "%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%s,%s,%s,%d,\"%s\",NA,NA,NA,NA,NA,NA,NA,NA,NA",
            ageYears,
            gender,
            sessionName,
            participantId,
            sessionId,
            activity,
            gameMode,
            attemptId,
            eventId,
            timestamp,
            elapsedMs,
            expectedAction,
            detectedAction,
            result,
            durationMs,
            touchInfo
        )

        val fields = parseCsvRow(row)
        assertEquals(25, fields.size)
        assertEquals("holding_phone", fields[5])
        assertEquals("hold_phone_steady", fields[11])
        assertEquals("held_steady", fields[12])
        assertEquals("successful", fields[13])
        assertEquals("15000", fields[14])
        assertEquals("duration_s=15,stability_pct=94.5", fields[15])
    }

    @Test
    fun testRunnerAndDrivingGameActionEventRowFormatting() {
        // Runner swipe dodge test
        val runnerRow = String.format(
            Locale.US,
            "%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%s,%s,%s,%d,\"%s\",NA,NA,NA,NA,NA,NA,NA,NA,NA",
            22,
            "Male",
            "Adapt-IQ Research Session",
            "PARTICIPANT_RUN01",
            "SESSION_GAME01",
            "gaming",
            "runner",
            3,
            201L,
            1717943100000L,
            12400L,
            "swipe_dodge",
            "swipe_left",
            "successful",
            280L,
            "lane=0,playerLane=1,type=DODGE"
        )

        val runnerFields = parseCsvRow(runnerRow)
        assertEquals(25, runnerFields.size)
        assertEquals("gaming", runnerFields[5])
        assertEquals("runner", runnerFields[6])
        assertEquals("swipe_dodge", runnerFields[11])
        assertEquals("280", runnerFields[14])

        // Driving steer left test
        val drivingRow = String.format(
            Locale.US,
            "%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%s,%s,%s,%d,\"%s\",NA,NA,NA,NA,NA,NA,NA,NA,NA",
            30,
            "Non-binary",
            "Adapt-IQ Research Session",
            "PARTICIPANT_DRV01",
            "SESSION_GAME02",
            "gaming",
            "driving_lab",
            5,
            302L,
            1717943200000L,
            8500L,
            "steer_left",
            "steer_left",
            "successful",
            120L,
            "offset=-25.0,dev=0.25"
        )

        val drivingFields = parseCsvRow(drivingRow)
        assertEquals(25, drivingFields.size)
        assertEquals("gaming", drivingFields[5])
        assertEquals("driving_lab", drivingFields[6])
        assertEquals("steer_left", drivingFields[11])
        assertEquals("120", drivingFields[14])
    }

    @Test
    fun testPeriodicSensorRowFormatting() {
        val row = String.format(
            Locale.US,
            "%d,%s,%s,%s,%s,%s,%s,0,%d,%d,%d,none,sensor_sample,none,NA,\"\",%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f",
            25,
            "Prefer not to say",
            "Adapt-IQ Research Session",
            "PARTICIPANT_101",
            "SESSION_202",
            "holding_phone",
            "standard",
            501L,
            1717943300000L,
            3500L,
            0.12345f,
            9.78910f,
            0.45678f,
            0.01234f,
            0.00567f,
            0.00891f,
            5.0f,
            9.80054f,
            0.01630f
        )

        val fields = parseCsvRow(row)
        assertEquals(25, fields.size)
        assertEquals("holding_phone", fields[5])
        assertEquals("0", fields[7])
        assertEquals("none", fields[11])
        assertEquals("sensor_sample", fields[12])
        assertEquals("NA", fields[14])
        assertEquals("", fields[15])
        assertEquals("0.12345", fields[16])
        assertEquals("9.78910", fields[17])
    }

    private fun parseCsvRow(row: String): List<String> {
        val fields = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val c = row[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < row.length && row[i + 1] == '"') {
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
        return fields
    }
}
