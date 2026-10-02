"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
const supertest_1 = __importDefault(require("supertest"));
const bcrypt_1 = __importDefault(require("bcrypt"));
const server_1 = __importDefault(require("../src/server"));
const database_1 = __importDefault(require("../src/config/database"));
const auth_middleware_1 = require("../src/middleware/auth.middleware");
describe('Adapt-IQ Backend API Integration Tests', () => {
    let participantToken;
    let adminToken;
    const testSessionId = 'SESSION_TEST_1234';
    beforeAll(async () => {
        // Clean test DB
        await database_1.default.sensorSample.deleteMany();
        await database_1.default.actionEvent.deleteMany();
        await database_1.default.uploadBatch.deleteMany();
        await database_1.default.session.deleteMany();
        await database_1.default.participant.deleteMany();
        await database_1.default.adminUser.deleteMany();
        // Create admin user
        const passwordHash = await bcrypt_1.default.hash('adminpassword123', 10);
        await database_1.default.adminUser.create({
            data: {
                email: 'researcher@adaptiq.org',
                passwordHash,
                role: 'RESEARCHER'
            }
        });
        adminToken = (0, auth_middleware_1.generateAdminToken)('admin-uuid-1', 'RESEARCHER');
        participantToken = (0, auth_middleware_1.generateParticipantToken)('TEST_001');
    });
    afterAll(async () => {
        await database_1.default.$disconnect();
    });
    test('1. Participant Authentication & Token Generation', async () => {
        const res = await (0, supertest_1.default)(server_1.default)
            .post('/api/v1/auth/participant')
            .send({ testId: 'TEST_001' });
        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('token');
        expect(res.body.testId).toBe('TEST_001');
    });
    test('2. Register Research Session', async () => {
        const res = await (0, supertest_1.default)(server_1.default)
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
        const res1 = await (0, supertest_1.default)(server_1.default)
            .post(`/api/v1/sessions/${testSessionId}/upload`)
            .set('Authorization', `Bearer ${participantToken}`)
            .send(uploadPayload);
        expect(res1.status).toBe(200);
        expect(res1.body.eventsProcessed).toBe(1);
        // Second upload (Duplicate / Retry with same eventId - Idempotency test)
        const res2 = await (0, supertest_1.default)(server_1.default)
            .post(`/api/v1/sessions/${testSessionId}/upload`)
            .set('Authorization', `Bearer ${participantToken}`)
            .send(uploadPayload);
        expect(res2.status).toBe(200);
    });
    test('4. Finalize Session', async () => {
        const res = await (0, supertest_1.default)(server_1.default)
            .post(`/api/v1/sessions/${testSessionId}/finalize`)
            .set('Authorization', `Bearer ${participantToken}`)
            .send({ endTime: new Date().toISOString() });
        expect(res.status).toBe(200);
        expect(res.body.status).toBe('COMPLETED');
    });
    test('5. Admin Access to Sessions List and Detail', async () => {
        const listRes = await (0, supertest_1.default)(server_1.default)
            .get('/api/v1/sessions')
            .set('Authorization', `Bearer ${adminToken}`);
        expect(listRes.status).toBe(200);
        expect(Array.isArray(listRes.body)).toBe(true);
        expect(listRes.body.length).toBeGreaterThan(0);
        const detailRes = await (0, supertest_1.default)(server_1.default)
            .get(`/api/v1/sessions/${testSessionId}`)
            .set('Authorization', `Bearer ${adminToken}`);
        expect(detailRes.status).toBe(200);
        expect(detailRes.body.sessionId).toBe(testSessionId);
        expect(detailRes.body.events.length).toBe(1);
    });
});
