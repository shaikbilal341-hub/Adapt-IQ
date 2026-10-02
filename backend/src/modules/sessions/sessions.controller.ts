import { Router, Response } from 'express';
import prisma from '../../config/database';
import { authenticateToken, requireRole, verifySessionOwnership, AuthenticatedRequest } from '../../middleware/auth.middleware';
import { sessionRegisterSchema, sessionFinalizeSchema, sessionUploadSchema } from '../../validators/session.validator';

const router = Router();

// Register session metadata (App 1)
router.post('/', authenticateToken, verifySessionOwnership, async (req: AuthenticatedRequest, res: Response) => {
    try {
        const data = sessionRegisterSchema.parse(req.body);

        let participant = await prisma.participant.findUnique({
            where: { pseudonymId: data.testId }
        });

        if (!participant) {
            participant = await prisma.participant.create({
                data: { pseudonymId: data.testId }
            });
        }

        const session = await prisma.session.upsert({
            where: { sessionId: data.sessionId },
            update: {
                testId: data.testId,
                orientation: data.orientation,
                deviceModel: data.deviceModel,
                availableSensors: data.availableSensors
            },
            create: {
                sessionId: data.sessionId,
                testId: data.testId,
                participantId: participant.id,
                startTime: new Date(data.startTime),
                orientation: data.orientation,
                deviceModel: data.deviceModel,
                availableSensors: data.availableSensors,
                status: 'PENDING'
            }
        });

        return res.status(201).json({ message: 'Session registered', sessionId: session.sessionId });
    } catch (error: any) {
        console.error("Error in session register:", error);
        return res.status(400).json({ error: error.message });
    }
});

// Finalize session (App 1)
router.post('/:sessionId/finalize', authenticateToken, verifySessionOwnership, async (req: AuthenticatedRequest, res: Response) => {
    try {
        const { sessionId } = req.params;
        const data = sessionFinalizeSchema.parse(req.body);

        const session = await prisma.session.update({
            where: { sessionId },
            data: {
                endTime: new Date(data.endTime),
                status: 'COMPLETED'
            }
        });

        return res.json({ message: 'Session finalized', sessionId: session.sessionId, status: session.status });
    } catch (error: any) {
        console.error("Error in session finalize:", error);
        return res.status(400).json({ error: error.message });
    }
});

// Idempotent batch upload (App 1)
router.post('/:sessionId/upload', authenticateToken, verifySessionOwnership, async (req: AuthenticatedRequest, res: Response) => {
    try {
        const { sessionId } = req.params;
        const uploadData = sessionUploadSchema.parse(req.body);

        const session = await prisma.session.findUnique({ where: { sessionId } });
        if (!session) {
            return res.status(404).json({ error: 'Session not found' });
        }

        // Record upload batch
        await prisma.uploadBatch.create({
            data: {
                sessionId,
                batchIndex: uploadData.batchIndex,
                status: 'RECEIVED'
            }
        });

        // Insert Action Events idempotently (skip duplicates on eventId)
        for (const event of uploadData.events) {
            try {
                await prisma.actionEvent.upsert({
                    where: { eventId: event.eventId },
                    update: {},
                    create: {
                        eventId: event.eventId,
                        sessionId: session.id,
                        timestamp: event.timestamp,
                        elapsedMs: event.elapsedMs,
                        activity: event.activity,
                        orientation: event.orientation,
                        expectedAction: event.expectedAction,
                        detectedAction: event.detectedAction,
                        result: event.result,
                        attemptId: event.attemptId,
                        durationMs: event.durationMs,
                        touchInfo: event.touchInfo
                    }
                });
            } catch (e) {
                // Ignore unique constraint violation on eventId for idempotency
            }
        }

        // Insert Sensor Samples
        if (uploadData.sensorSamples.length > 0) {
            await prisma.sensorSample.createMany({
                data: uploadData.sensorSamples.map(sample => ({
                    sessionId: session.id,
                    timestamp: sample.timestamp,
                    elapsedMs: sample.elapsedMs,
                    activity: sample.activity,
                    accX: sample.accX,
                    accY: sample.accY,
                    accZ: sample.accZ,
                    gyroX: sample.gyroX,
                    gyroY: sample.gyroY,
                    gyroZ: sample.gyroZ,
                    proximity: sample.proximity,
                    accMagnitude: sample.accMagnitude,
                    gyroMagnitude: sample.gyroMagnitude
                }))
            });
        }

        return res.json({
            message: 'Batch uploaded successfully',
            batchIndex: uploadData.batchIndex,
            eventsProcessed: uploadData.events.length,
            sensorsProcessed: uploadData.sensorSamples.length
        });
    } catch (error: any) {
        console.error("Error in session upload:", error);
        return res.status(400).json({ error: error.message });
    }
});

// Admin list sessions (App 2)
router.get('/', authenticateToken, requireRole('RESEARCHER'), async (req: AuthenticatedRequest, res: Response) => {
    try {
        const sessions = await prisma.session.findMany({
            include: {
                _count: {
                    select: { events: true, sensorSamples: true }
                }
            },
            orderBy: { createdAt: 'desc' }
        });
        return res.json(sessions);
    } catch (error: any) {
        console.error("Error in admin list sessions:", error);
        return res.status(500).json({ error: error.message });
    }
});

// Admin export dataset (App 2)
router.get('/admin/export', authenticateToken, requireRole('RESEARCHER'), async (req: AuthenticatedRequest, res: Response) => {
    try {
        const sessions = await prisma.session.findMany({
            include: {
                events: true,
                sensorSamples: true
            },
            orderBy: { createdAt: 'desc' }
        });
        return res.json({ exportedAt: new Date().toISOString(), totalSessions: sessions.length, sessions });
    } catch (error: any) {
        console.error("Error in admin export:", error);
        return res.status(500).json({ error: error.message });
    }
});

// Admin session detail (App 2)
router.get('/:sessionId', authenticateToken, requireRole('RESEARCHER'), async (req: AuthenticatedRequest, res: Response) => {
    try {
        const { sessionId } = req.params;
        const session = await prisma.session.findUnique({
            where: { sessionId },
            include: {
                events: { orderBy: { timestamp: 'asc' } },
                sensorSamples: { take: 100, orderBy: { timestamp: 'asc' } }
            }
        });

        if (!session) {
            return res.status(404).json({ error: 'Session not found' });
        }

        // Serialize BigInt to string for JSON serialization compatibility
        const serialized = {
            ...session,
            events: session.events.map(ev => ({
                ...ev,
                eventId: ev.eventId.toString(),
                timestamp: ev.timestamp.toString(),
                elapsedMs: ev.elapsedMs.toString(),
                durationMs: ev.durationMs.toString()
            })),
            sensorSamples: session.sensorSamples.map(s => ({
                ...s,
                timestamp: s.timestamp.toString(),
                elapsedMs: s.elapsedMs.toString()
            }))
        };

        return res.json(serialized);
    } catch (error: any) {
        console.error("Error in admin session detail:", error);
        return res.status(500).json({ error: error.message });
    }
});

export default router;
