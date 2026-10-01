import { z } from 'zod';

export const sessionRegisterSchema = z.object({
    sessionId: z.string().min(1),
    testId: z.string().min(1),
    startTime: z.string().datetime(),
    orientation: z.string().default('portrait'),
    deviceModel: z.string().optional(),
    availableSensors: z.string().optional()
});

export const sessionFinalizeSchema = z.object({
    endTime: z.string().datetime()
});

export const actionEventSchema = z.object({
    eventId: z.number().or(z.string()).transform(val => BigInt(val)),
    timestamp: z.number().or(z.string()).transform(val => BigInt(val)),
    elapsedMs: z.number().or(z.string()).transform(val => BigInt(val)),
    activity: z.string().min(1),
    orientation: z.string().default('portrait'),
    expectedAction: z.string(),
    detectedAction: z.string(),
    result: z.string(),
    attemptId: z.number().int().default(1),
    durationMs: z.number().or(z.string()).transform(val => BigInt(val)).default(0),
    touchInfo: z.string().optional().default('')
});

export const sensorSampleSchema = z.object({
    timestamp: z.number().or(z.string()).transform(val => BigInt(val)),
    elapsedMs: z.number().or(z.string()).transform(val => BigInt(val)),
    activity: z.string().min(1),
    accX: z.number(),
    accY: z.number(),
    accZ: z.number(),
    gyroX: z.number(),
    gyroY: z.number(),
    gyroZ: z.number(),
    proximity: z.number(),
    accMagnitude: z.number(),
    gyroMagnitude: z.number()
});

export const sessionUploadSchema = z.object({
    batchIndex: z.number().int().nonnegative(),
    events: z.array(actionEventSchema).optional().default([]),
    sensorSamples: z.array(sensorSampleSchema).optional().default([])
});
