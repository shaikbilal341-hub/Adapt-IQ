import dotenv from 'dotenv';
dotenv.config();

import request from 'supertest';
import bcrypt from 'bcrypt';
import app from '../src/server';
import prisma from '../src/config/database';
import { generateSessionToken, generateAdminToken } from '../src/middleware/auth.middleware';

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
        participantToken = generateSessionToken(testSessionId, 'PARTICIPANT_TEST_001');
    });

    afterAll(async () => {
        await prisma.$disconnect();
    });

    test('1. Anonymous Participant Session Start & Token Generation', async () => {
        const res = await request(app)
            .post('/api/v1/auth/participant/session/start')
            .send({ participantType: 'ADULT', condition: 'SITTING', ageGroup: '18-25' });

        expect(res.status).toBe(201);
        expect(res.body).toHaveProperty('token');
        expect(res.body).toHaveProperty('sessionId');
        expect(res.body).toHaveProperty('testId');
    });

    test('2. Register Research Session', async () => {
        const res = await request(app)
            .post('/api/v1/sessions')
            .set('Authorization', `Bearer ${participantToken}`)
            .send({
                sessionId: testSessionId,
                testId: 'PARTICIPANT_TEST_001',
                startTime: new Date().toISOString(),
                orientation: 'portrait',
                deviceModel: 'Normalized Device'
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

    test('6. Gaming Activity (Runner & Driving) Production Upload, Idempotency & Admin Export Verification', async () => {
        const gamingSessionId = 'SESSION_GAMING_RESEARCH_01';
        const gamingToken = generateSessionToken(gamingSessionId, 'PARTICIPANT_GAME_007');

        // 1. Register gaming session
        await request(app)
            .post('/api/v1/sessions')
            .set('Authorization', `Bearer ${gamingToken}`)
            .send({
                sessionId: gamingSessionId,
                testId: 'PARTICIPANT_GAME_007',
                startTime: new Date().toISOString(),
                orientation: 'portrait',
                deviceModel: 'Normalized Device'
            });

        // 2. Upload Runner & Driving events + sensor samples
        const gamingPayload = {
            batchIndex: 0,
            events: [
                {
                    eventId: 5001,
                    timestamp: 1717944000000,
                    elapsedMs: 2500,
                    activity: 'gaming:runner',
                    orientation: 'portrait',
                    expectedAction: 'swipe_dodge',
                    detectedAction: 'swipe_left',
                    result: 'successful',
                    attemptId: 1,
                    durationMs: 280,
                    touchInfo: 'lane=0,playerLane=1,type=DODGE_ROADBLOCK'
                },
                {
                    eventId: 5002,
                    timestamp: 1717944005000,
                    elapsedMs: 7500,
                    activity: 'gaming:runner',
                    orientation: 'portrait',
                    expectedAction: 'jump',
                    detectedAction: 'jump',
                    result: 'successful',
                    attemptId: 1,
                    durationMs: 310,
                    touchInfo: 'lane=1,playerLane=1,type=JUMP_HURDLE'
                },
                {
                    eventId: 5003,
                    timestamp: 1717944015000,
                    elapsedMs: 17500,
                    activity: 'gaming:driving',
                    orientation: 'portrait',
                    expectedAction: 'avoid_traffic',
                    detectedAction: 'steer_around',
                    result: 'successful',
                    attemptId: 2,
                    durationMs: 350,
                    touchInfo: 'trafficLane=2,playerLane=1,speed=85'
                },
                {
                    eventId: 5004,
                    timestamp: 1717944030000,
                    elapsedMs: 30000,
                    activity: 'gaming:driving',
                    orientation: 'portrait',
                    expectedAction: 'complete_trial',
                    detectedAction: 'completed_30s',
                    result: 'successful',
                    attemptId: 2,
                    durationMs: 30000,
                    touchInfo: 'score=380,avoided=6,collisions=0,distance=420'
                }
            ],
            sensorSamples: [
                {
                    timestamp: 1717944002000,
                    elapsedMs: 4500,
                    activity: 'gaming:runner',
                    accX: 0.15,
                    accY: 9.78,
                    accZ: 0.35,
                    gyroX: 0.02,
                    gyroY: -0.01,
                    gyroZ: 0.04,
                    proximity: 5.0,
                    accMagnitude: 9.79,
                    gyroMagnitude: 0.05
                },
                {
                    timestamp: 1717944018000,
                    elapsedMs: 20500,
                    activity: 'gaming:driving',
                    accX: -0.25,
                    accY: 9.65,
                    accZ: 0.85,
                    gyroX: 0.12,
                    gyroY: 0.05,
                    gyroZ: -0.08,
                    proximity: 5.0,
                    accMagnitude: 9.69,
                    gyroMagnitude: 0.15
                }
            ]
        };

        const uploadRes1 = await request(app)
            .post(`/api/v1/sessions/${gamingSessionId}/upload`)
            .set('Authorization', `Bearer ${gamingToken}`)
            .send(gamingPayload);

        expect(uploadRes1.status).toBe(200);
        expect(uploadRes1.body.eventsProcessed).toBe(4);
        expect(uploadRes1.body.sensorsProcessed).toBe(2);

        // Retry duplicate upload (Idempotency verification)
        const uploadRes2 = await request(app)
            .post(`/api/v1/sessions/${gamingSessionId}/upload`)
            .set('Authorization', `Bearer ${gamingToken}`)
            .send(gamingPayload);
        expect(uploadRes2.status).toBe(200);

        // Finalize session
        await request(app)
            .post(`/api/v1/sessions/${gamingSessionId}/finalize`)
            .set('Authorization', `Bearer ${gamingToken}`)
            .send({ endTime: new Date().toISOString() });

        // Verify retrieval via Admin API
        const adminDetailRes = await request(app)
            .get(`/api/v1/sessions/${gamingSessionId}`)
            .set('Authorization', `Bearer ${adminToken}`);

        expect(adminDetailRes.status).toBe(200);
        expect(adminDetailRes.body.sessionId).toBe(gamingSessionId);
        expect(adminDetailRes.body.events.length).toBe(4);
        expect(adminDetailRes.body.sensorSamples.length).toBe(2);

        const runnerEv = adminDetailRes.body.events.find((e: any) => e.detectedAction === 'swipe_left');
        expect(runnerEv).toBeDefined();
        expect(runnerEv.activity).toBe('gaming:runner');

        const drivingEv = adminDetailRes.body.events.find((e: any) => e.detectedAction === 'steer_around');
        expect(drivingEv).toBeDefined();
        expect(drivingEv.activity).toBe('gaming:driving');

        // Verify Admin Complete Dataset Export
        const exportRes = await request(app)
            .get('/api/v1/sessions/admin/export')
            .set('Authorization', `Bearer ${adminToken}`);

        expect(exportRes.status).toBe(200);
        expect(exportRes.body.sessions.length).toBeGreaterThanOrEqual(2);
        const exportedGaming = exportRes.body.sessions.find((s: any) => s.sessionId === gamingSessionId);
        expect(exportedGaming).toBeDefined();
        expect(exportedGaming.events.length).toBe(4);
    });
});

