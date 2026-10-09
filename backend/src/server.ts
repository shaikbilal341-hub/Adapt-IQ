import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import bcrypt from 'bcrypt';
import prisma from './config/database';
import authRouter from './modules/auth/auth.controller';
import sessionsRouter from './modules/sessions/sessions.controller';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 4000;

app.use(cors());
app.use(express.json({ limit: '10mb' }));

// Seed authorized student researcher accounts on startup
async function seedResearchers() {
    const researchers = [
        { email: 'nandakishore.christ@gmail.com', pass: 'Nandu*2809' },
        { email: 'zinudu1437@gmail.com', pass: 'Zinudu*28' },
        { email: 'shaikbilal341@gmail.com', pass: 'Biala*341' }
    ];

    for (const r of researchers) {
        try {
            const existing = await prisma.adminUser.findUnique({ where: { email: r.email } });
            if (!existing) {
                const passwordHash = await bcrypt.hash(r.pass, 10);
                await prisma.adminUser.create({
                    data: {
                        email: r.email,
                        passwordHash,
                        role: 'RESEARCHER'
                    }
                });
                console.log(`Seeded authorized researcher account: ${r.email}`);
            }
        } catch (e) {
            console.error(`Failed to seed researcher account ${r.email}:`, e);
        }
    }
}

// Health check
app.get('/health', (req: Request, res: Response) => {
    res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

// API Routes
app.use('/api/v1/auth', authRouter);
app.use('/api/v1/sessions', sessionsRouter);

// Global Error Handler
app.use((err: any, req: Request, res: Response, next: NextFunction) => {
    console.error('Unhandled error:', err);
    res.status(500).json({ error: 'Internal Server Error' });
});

if (process.env.NODE_ENV !== 'test') {
    app.listen(PORT, async () => {
        console.log(`Adapt-IQ Backend API running on port ${PORT}`);
        await seedResearchers();
    });
}

export default app;
