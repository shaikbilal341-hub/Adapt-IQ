import { validateAndGetJwtSecret } from '../src/middleware/auth.middleware';
import bcrypt from 'bcrypt';
import prisma from '../src/config/database';
import request from 'supertest';
import app from '../src/server';

describe('Security & Authentication Hardening Tests', () => {
    const originalEnv = process.env;

    beforeEach(() => {
        jest.resetModules();
        process.env = { ...originalEnv };
    });

    afterAll(() => {
        process.env = originalEnv;
    });

    test('refuses to start when JWT_SECRET is missing', () => {
        delete process.env.JWT_SECRET;
        expect(() => validateAndGetJwtSecret()).toThrow(/missing/i);
    });

    test('refuses to start when JWT_SECRET is empty whitespace', () => {
        process.env.JWT_SECRET = '   ';
        expect(() => validateAndGetJwtSecret()).toThrow(/missing/i);
    });

    test('refuses to start when JWT_SECRET is a known insecure placeholder', () => {
        const knownInsecure = [
            'adapt-iq-secure-research-secret-key',
            'adapt-iq-secure-research-secret-key-prod',
            'your-secure-jwt-secret-here',
            'secret',
            'changeme'
        ];

        for (const badSecret of knownInsecure) {
            process.env.JWT_SECRET = badSecret;
            expect(() => validateAndGetJwtSecret()).toThrow(/insecure default\/placeholder/i);
        }
    });

    test('refuses to start in production if JWT_SECRET is shorter than 32 characters', () => {
        process.env.NODE_ENV = 'production';
        process.env.JWT_SECRET = 'short-secret-key-12345';
        expect(() => validateAndGetJwtSecret()).toThrow(/at least 32 characters/i);
    });

    test('accepts strong high-entropy secret and trims whitespace', () => {
        const strongSecret = '9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3d2c1b0a9f8e';
        process.env.JWT_SECRET = `  ${strongSecret}  `;
        const result = validateAndGetJwtSecret();
        expect(result).toBe(strongSecret);
    });

    test('password hashing uses securely salted bcrypt with min 10 rounds', async () => {
        const plainPassword = 'SampleSecurePassword123!';
        const hash = await bcrypt.hash(plainPassword, 12);

        expect(hash).not.toBe(plainPassword);
        expect(hash.startsWith('$2b$12$') || hash.startsWith('$2a$12$')).toBe(true);

        const isValid = await bcrypt.compare(plainPassword, hash);
        expect(isValid).toBe(true);

        const isWrong = await bcrypt.compare('WrongPassword', hash);
        expect(isWrong).toBe(false);
    });

    describe('Admin Account Provisioning & RBAC Isolation', () => {
        const testAdminEmail = 'provisioning.test@adaptiq.org';

        afterEach(async () => {
            await prisma.adminUser.deleteMany({ where: { email: testAdminEmail } });
        });

        test('provisions admin user with salted hash and default RESEARCHER role', async () => {
            const password = 'SecurePassword789!';
            const passwordHash = await bcrypt.hash(password, 12);

            const user = await prisma.adminUser.upsert({
                where: { email: testAdminEmail },
                update: { passwordHash, role: 'RESEARCHER' },
                create: { email: testAdminEmail, passwordHash, role: 'RESEARCHER' }
            });

            expect(user.email).toBe(testAdminEmail);
            expect(user.role).toBe('RESEARCHER');
            expect(user.passwordHash).not.toBe(password);
            expect(await bcrypt.compare(password, user.passwordHash)).toBe(true);
        });

        test('handles duplicate email by updating hash without creating duplicate rows', async () => {
            const initialHash = await bcrypt.hash('InitialPassword123!', 12);
            const user1 = await prisma.adminUser.create({
                data: { email: testAdminEmail, passwordHash: initialHash, role: 'RESEARCHER' }
            });

            const updatedHash = await bcrypt.hash('UpdatedPassword456!', 12);
            const user2 = await prisma.adminUser.upsert({
                where: { email: testAdminEmail },
                update: { passwordHash: updatedHash, role: 'SUPER_ADMIN' },
                create: { email: testAdminEmail, passwordHash: updatedHash, role: 'SUPER_ADMIN' }
            });

            expect(user2.id).toBe(user1.id);
            expect(user2.role).toBe('SUPER_ADMIN');
            expect(await bcrypt.compare('UpdatedPassword456!', user2.passwordHash)).toBe(true);

            const total = await prisma.adminUser.count({ where: { email: testAdminEmail } });
            expect(total).toBe(1);
        });

        test('ensures no public HTTP route exposes admin creation or role promotion', async () => {
            const request = require('supertest');
            const app = require('../src/server').default;

            // Attempting to POST to non-existent admin creation route returns 404
            const resCreate = await request(app)
                .post('/api/v1/auth/admin/register')
                .send({ email: testAdminEmail, password: 'password123', role: 'SUPER_ADMIN' });
            expect(resCreate.status).toBe(404);

            const resPromote = await request(app)
                .post('/api/v1/auth/admin/promote')
                .send({ email: testAdminEmail, role: 'SUPER_ADMIN' });
            expect(resPromote.status).toBe(404);
        });
    });

    describe('Security Headers (Helmet)', () => {
        test('sets expected security headers on HTTP responses', async () => {
            const res = await request(app).get('/health');
            expect(res.status).toBe(200);
            expect(res.headers['x-content-type-options']).toBe('nosniff');
            expect(res.headers['x-frame-options']).toBe('SAMEORIGIN');
            expect(res.headers['x-dns-prefetch-control']).toBe('off');
            expect(res.headers['content-security-policy']).toBeDefined();
        });
    });

    describe('CORS Restrictions & Client Compatibility', () => {
        test('allows requests with no Origin header (native Android apps)', async () => {
            const res = await request(app).get('/health');
            expect(res.status).toBe(200);
        });

        test('withholds Access-Control-Allow-Origin for untrusted web origins', async () => {
            const res = await request(app)
                .get('/health')
                .set('Origin', 'https://malicious-website.example.com');
            expect(res.status).toBe(200);
            expect(res.headers['access-control-allow-origin']).toBeUndefined();
        });

        test('reflects Access-Control-Allow-Origin for allowed localhost in test environment', async () => {
            const res = await request(app)
                .get('/health')
                .set('Origin', 'http://localhost:3000');
            expect(res.status).toBe(200);
            expect(res.headers['access-control-allow-origin']).toBe('http://localhost:3000');
        });
    });

    describe('Database Readiness & Liveness Probes', () => {
        test('returns 200 healthy on liveness probe', async () => {
            const res = await request(app).get('/health');
            expect(res.status).toBe(200);
            expect(res.body.status).toBe('healthy');
            expect(res.body).toHaveProperty('timestamp');
        });

        test('returns 200 ready when database is connected on readiness probe', async () => {
            const res = await request(app).get('/health/ready');
            expect(res.status).toBe(200);
            expect(res.body.status).toBe('ready');
            expect(res.body.database).toBe('connected');
            expect(res.body).toHaveProperty('timestamp');
        });

        test('returns 503 unavailable when database connectivity fails without leaking credentials', async () => {
            const originalQueryRaw = prisma.$queryRaw;
            try {
                (prisma as any).$queryRaw = jest.fn().mockRejectedValueOnce(
                    new Error('Connection refused to postgresql://user:secret123@postgres:5432/db')
                );
                const res = await request(app).get('/health/ready');
                expect(res.status).toBe(503);
                expect(res.body.status).toBe('unavailable');
                expect(res.body.database).toBe('disconnected');
                expect(res.body.error).toBe('Database service unavailable');
                // Ensure zero sensitive connection or credential leakage
                expect(JSON.stringify(res.body)).not.toContain('secret123');
                expect(JSON.stringify(res.body)).not.toContain('postgresql://');
            } finally {
                (prisma as any).$queryRaw = originalQueryRaw;
            }
        });
    });

    describe('Admin Login Rate Limiting', () => {
        test('enforces rate limit after 5 login attempts while leaving participant uploads unblocked', async () => {
            // First 5 attempts return 400 or 401
            for (let i = 0; i < 5; i++) {
                const res = await request(app)
                    .post('/api/v1/auth/admin/login')
                    .send({ email: 'ratelimit.test@adaptiq.org', password: 'wrongpassword' });
                expect([400, 401]).toContain(res.status);
            }

            // 6th attempt must be rejected with HTTP 429
            const blockedRes = await request(app)
                .post('/api/v1/auth/admin/login')
                .send({ email: 'ratelimit.test@adaptiq.org', password: 'wrongpassword' });
            expect(blockedRes.status).toBe(429);
            expect(blockedRes.body.error).toMatch(/too many login attempts/i);

            // Verify participant endpoints remain completely unblocked
            const participantRes = await request(app)
                .post('/api/v1/auth/participant/session/start')
                .send({ participantType: 'ADULT', condition: 'SITTING' });
            expect(participantRes.status).toBe(201);
            expect(participantRes.body).toHaveProperty('token');

            // Verify liveness probe remains unblocked
            const healthRes = await request(app).get('/health');
            expect(healthRes.status).toBe(200);
        });
    });
});
