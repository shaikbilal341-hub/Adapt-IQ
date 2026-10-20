import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

const INSECURE_JWT_SECRETS = new Set([
    'adapt-iq-secure-research-secret-key',
    'adapt-iq-secure-research-secret-key-prod',
    'your-secure-jwt-secret-here',
    'secret',
    'changeme',
    'jwt_secret',
    'password'
]);

export function validateAndGetJwtSecret(): string {
    const secret = process.env.JWT_SECRET;
    if (!secret || secret.trim().length === 0) {
        throw new Error(
            'FATAL: JWT_SECRET environment variable is missing. The server cannot start without a securely configured JWT_SECRET.'
        );
    }
    const trimmed = secret.trim();
    if (INSECURE_JWT_SECRETS.has(trimmed.toLowerCase())) {
        throw new Error(
            'FATAL: JWT_SECRET is set to an insecure default/placeholder. You must supply a cryptographically secure, high-entropy secret.'
        );
    }
    if (process.env.NODE_ENV === 'production' && trimmed.length < 32) {
        throw new Error(
            'FATAL: JWT_SECRET must be at least 32 characters in production.'
        );
    }
    return trimmed;
}

const SESSION_TOKEN_EXPIRATION = process.env.SESSION_TOKEN_EXPIRATION || '4h';

export interface AuthenticatedRequest extends Request {
    user?: {
        id: string;
        role: string;
        testId?: string;
        sessionId?: string;
    };
}

export const authenticateToken = (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ error: 'Access token required' });
    }

    try {
        const secret = validateAndGetJwtSecret();
        jwt.verify(token, secret, (err: any, user: any) => {
            if (err) {
                return res.status(403).json({ error: 'Invalid or expired session credential' });
            }
            req.user = user;
            next();
        });
    } catch (e: any) {
        return res.status(500).json({ error: 'Internal security configuration error' });
    }
};

export const requireRole = (role: string) => {
    return (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
        if (!req.user || (req.user.role !== role && req.user.role !== 'SUPER_ADMIN')) {
            return res.status(403).json({ error: 'Insufficient permissions' });
        }
        next();
    };
};

export const verifySessionOwnership = (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    if (!req.user) {
        return res.status(401).json({ error: 'Authentication required' });
    }
    // Researchers retain full authorized access
    if (req.user.role === 'RESEARCHER' || req.user.role === 'SUPER_ADMIN') {
        return next();
    }
    // Participant session check: verify token role, expiration (JWT verify), and session ID match
    const routeSessionId = req.params.sessionId || req.body.sessionId;
    if (
        req.user.role === 'PARTICIPANT_SESSION' &&
        req.user.sessionId &&
        req.user.sessionId === routeSessionId
    ) {
        return next();
    }
    return res.status(403).json({ error: 'Forbidden: Invalid session scope or IDOR access denied' });
};

export const generateSessionToken = (sessionId: string, testId: string) => {
    const secret = validateAndGetJwtSecret();
    return jwt.sign(
        { id: testId, role: 'PARTICIPANT_SESSION', testId, sessionId },
        secret,
        { expiresIn: SESSION_TOKEN_EXPIRATION as any }
    );
};

export const generateAdminToken = (id: string, role: string) => {
    const secret = validateAndGetJwtSecret();
    return jwt.sign({ id, role }, secret, { expiresIn: '12h' });
};
