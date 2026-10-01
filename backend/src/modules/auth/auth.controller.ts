import { Router, Request, Response } from 'express';
import bcrypt from 'bcrypt';
import prisma from '../../config/database';
import { generateParticipantToken, generateAdminToken } from '../../middleware/auth.middleware';

const router = Router();

// Participant token registration
router.post('/participant', async (req: Request, res: Response) => {
    try {
        const { testId } = req.body;
        if (!testId || typeof testId !== 'string') {
            return res.status(400).json({ error: 'Valid testId is required' });
        }

        // Upsert participant
        let participant = await prisma.participant.findUnique({
            where: { pseudonymId: testId }
        });

        if (!participant) {
            participant = await prisma.participant.create({
                data: { pseudonymId: testId }
            });
        }

        const token = generateParticipantToken(testId);
        return res.json({ token, testId });
    } catch (error: any) {
        return res.status(500).json({ error: error.message });
    }
});

// Admin login
router.post('/admin/login', async (req: Request, res: Response) => {
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
