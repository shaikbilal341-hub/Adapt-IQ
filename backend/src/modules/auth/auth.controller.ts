import { Router, Request, Response } from 'express';
import bcrypt from 'bcrypt';
import crypto from 'crypto';
import prisma from '../../config/database';
import { generateSessionToken, generateAdminToken } from '../../middleware/auth.middleware';
import { adminLoginLimiter } from '../../middleware/rate-limiter.middleware';

const router = Router();

// Anonymous participant session start (App 1)
router.post('/participant/session/start', async (req: Request, res: Response) => {
    try {
        const { participantType, condition, ageGroup } = req.body;

        // Use cryptographically secure random opaque UUIDs (no timestamps, sequential IDs, or device identifiers)
        const testId = `PARTICIPANT_${crypto.randomUUID()}`;
        const sessionId = `SESSION_${crypto.randomUUID()}`;

        // Upsert participant record
        let participant = await prisma.participant.findUnique({
            where: { pseudonymId: testId }
        });

        if (!participant) {
            participant = await prisma.participant.create({
                data: { pseudonymId: testId }
            });
        }

        // Create pending session in database
        await prisma.session.create({
            data: {
                sessionId,
                testId,
                participantId: participant.id,
                startTime: new Date(),
                orientation: 'portrait',
                deviceModel: 'Normalized Device',
                availableSensors: JSON.stringify({ participantType: participantType || 'UNKNOWN', condition: condition || 'SITTING', ageGroup: ageGroup || '' }),
                status: 'PENDING'
            }
        });

        const token = generateSessionToken(sessionId, testId);
        return res.status(201).json({
            sessionId,
            testId,
            token
        });
    } catch (error: any) {
        console.error("Error starting participant session:", error);
        return res.status(500).json({ error: error.message });
    }
});

// Admin login (Protected with rate limiting)
router.post('/admin/login', adminLoginLimiter, async (req: Request, res: Response) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ error: 'Email and password required' });
        }

        const admin = await prisma.adminUser.findUnique({ where: { email } });
        if (!admin) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        const validPassword = await bcrypt.compare(password, admin.passwordHash);
        if (!validPassword) {
            return res.status(401).json({ error: 'Invalid credentials' });
        }

        const token = generateAdminToken(admin.id, admin.role);
        return res.json({ token, role: admin.role, email: admin.email });
    } catch (error: any) {
        return res.status(500).json({ error: error.message });
    }
});

export default router;
