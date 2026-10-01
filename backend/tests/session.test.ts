import dotenv from 'dotenv';
dotenv.config();

import request from 'supertest';
import bcrypt from 'bcrypt';
import app from '../src/server';
import prisma from '../src/config/database';
import { generateParticipantToken, generateAdminToken } from '../src/middleware/auth.middleware';

describe('Adapt-IQ Backend API Integration Tests', () => {
    let participantToken: string;
    let adminToken: string;
    const testSessionId = 'SESSION_TEST_1234';

    beforeAll(async () => {
        // Clean test DB
        await prisma.sensorSample.deleteMany();
        await prisma.actionEvent.deleteMany();
        await prisma.uploadBatch.deleteMany();
        await prisma.session.deleteMany();
        await prisma.participant.deleteMany();
        await prisma.adminUser.deleteMany();

        // Create admin user
        const passwordHash = await bcrypt.hash('adminpassword123', 10);
        await prisma.adminUser.create({
            data: {
                email: 'researcher@adaptiq.org',
                passwordHash,
                role: 'RESEARCHER'
            }
        });

        adminToken = generateAdminToken('admin-uuid-1', 'RESEARCHER');
        participantToken = generateParticipantToken('TEST_001');
    });

    afterAll(async () => {
        await prisma.$disconnect();
    });

    test('1. Participant Authentication & Token Generation', async () => {
        const res = await request(app)
            .post('/api/v1/auth/participant')
            .send({ testId: 'TEST_001' });

        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('token');
        expect(res.body.testId).toBe('TEST_001');
    });

    test('2. Register Research Session', async () => {
        const res = await request(app)
            .post('/api/v1/sessions')
            .set('Authorization', `Bearer ${participantToken}`)
            .send({
                sessionId: testSessionId,
                testId: 'TEST_001',
                startTime: new Date().toISOString(),
                orientation: 'portrait',
                deviceModel: 'Test Device'
            });

        expect(res.status).toBe(201);
        expect(res.body.sessionId).toBe(testSessionId);
    });

    test('3. Idempotent Batch Upload of Events and Sensors', async () => {
        const uploadPayload = {
            batchIndex: 0,
            events: [
                {
                    eventId: 1001,
                    timestamp: Date.now(),
                    elapsedMs: 500,
                    activity: 'typing',
                    orientation: 'portrait',
                    expectedAction: 'key_press',
                    detectedAction: 'key_press',
                    result: 'successful',
                    attemptId: 1,
                    durationMs: 40,
                    touchInfo: 'pos=1'
                }
            ],
            sensorSamples: [
                {
                    timestamp: Date.now(),
                    elapsedMs: 500,
                    activity: 'typing',
                    accX: 0.1,
                    accY: 9.8,
                    accZ: 0.2,
                    gyroX: 0.0,
                    gyroY: 0.0,
                    gyroZ: 0.0,
                    proximity: 5.0,
                    accMagnitude: 9.81,
                    gyroMagnitude: 0.0
                }
            ]
        };

        // First upload
        const res1 = await request(app)
            .post(`/api/v1/sessions/${testSessionId}/upload`)
            .set('Authorization', `Bearer ${participantToken}`)
            .send(uploadPayload);

        expect(res1.status).toBe(200);
        expect(res1.body.eventsProcessed).toBe(1);

        // Second upload (Duplicate / Retry with same eventId - Idempotency test)
        const res2 = await request(app)
            .post(`/api/v1/sessions/${testSessionId}/upload`)
            .set('Authorization', `Bearer ${participantToken}`)
            .send(uploadPayload);

        expect(res2.status).toBe(200);
    });

    test('4. Finalize Session', async () => {
        const res = await request(app)
            .post(`/api/v1/sessions/${testSessionId}/finalize`)
            .set('Authorization', `Bearer ${participantToken}`)
            .send({ endTime: new Date().toISOString() });

        expect(res.status).toBe(200);
        expect(res.body.status).toBe('COMPLETED');
    });

    test('5. Admin Access to Sessions List and Detail', async () => {
        const listRes = await request(app)
            .get('/api/v1/sessions')
            .set('Authorization', `Bearer ${adminToken}`);

        expect(listRes.status).toBe(200);
        expect(Array.isArray(listRes.body)).toBe(true);
        expect(listRes.body.length).toBeGreaterThan(0);

        const detailRes = await request(app)
            .get(`/api/v1/sessions/${testSessionId}`)
            .set('Authorization', `Bearer ${adminToken}`);

        expect(detailRes.status).toBe(200);
        expect(detailRes.body.sessionId).toBe(testSessionId);
        expect(detailRes.body.events.length).toBe(1);
    });
});
