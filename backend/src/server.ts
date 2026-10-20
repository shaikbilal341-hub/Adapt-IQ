import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import dotenv from 'dotenv';
import bcrypt from 'bcrypt';
import prisma from './config/database';
import authRouter from './modules/auth/auth.controller';
import sessionsRouter from './modules/sessions/sessions.controller';
import { validateAndGetJwtSecret } from './middleware/auth.middleware';
import { corsOptions } from './config/cors';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 4000;

// Trust loopback reverse proxy (e.g. Caddy or Nginx on host 127.0.0.1)
app.set('trust proxy', 'loopback');

app.use(helmet());
app.use(cors(corsOptions));
app.use(express.json({ limit: '10mb' }));

// Optional: Bootstrap initial admin user strictly if environment variables are provided
async function bootstrapAdminFromEnv() {
    const email = process.env.BOOTSTRAP_ADMIN_EMAIL;
    const password = process.env.BOOTSTRAP_ADMIN_PASSWORD;
    const role = process.env.BOOTSTRAP_ADMIN_ROLE || 'RESEARCHER';

    if (!email || !password) {
        return;
    }

    try {
        const existing = await prisma.adminUser.findUnique({ where: { email } });
        if (!existing) {
            const passwordHash = await bcrypt.hash(password, 12);
            await prisma.adminUser.create({
                data: {
                    email,
                    passwordHash,
                    role
                }
            });
            console.log(`Provisioned initial administrator account: ${email}`);
        }
    } catch (e: any) {
        console.error('Failed to bootstrap administrator account from environment:', e.message);
    }
}

// Liveness probe (Express process responsiveness)
app.get('/health', (req: Request, res: Response) => {
    res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

// Readiness probe (Database connectivity check)
app.get('/health/ready', async (req: Request, res: Response) => {
    try {
        await prisma.$queryRaw`SELECT 1`;
        res.json({ status: 'ready', database: 'connected', timestamp: new Date().toISOString() });
    } catch (e: any) {
        console.error('Database readiness check failed');
        res.status(503).json({ status: 'unavailable', database: 'disconnected', error: 'Database service unavailable' });
    }
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
    try {
        validateAndGetJwtSecret();
    } catch (err: any) {
        console.error(err.message);
        process.exit(1);
    }

    app.listen(Number(PORT), '0.0.0.0', async () => {
        console.log(`Adapt-IQ Backend API running on port ${PORT} (bound to 0.0.0.0)`);
        await bootstrapAdminFromEnv();
    });
}

export default app;
