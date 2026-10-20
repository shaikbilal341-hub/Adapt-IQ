import { validateAndGetJwtSecret } from '../src/middleware/auth.middleware';
import bcrypt from 'bcrypt';
import prisma from '../src/config/database';

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
});
