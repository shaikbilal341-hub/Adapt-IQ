import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || 'adapt-iq-secure-research-secret-key';
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

    jwt.verify(token, JWT_SECRET, (err: any, user: any) => {
        if (err) {
            return res.status(403).json({ error: 'Invalid or expired session credential' });
        }
        req.user = user;
        next();
    });
};

export const requireRole = (role: string) => {
    return (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
        if (!req.user || req.user.role !== role) {
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
    return jwt.sign(
        { id: testId, role: 'PARTICIPANT_SESSION', testId, sessionId },
        JWT_SECRET,
        { expiresIn: SESSION_TOKEN_EXPIRATION as any }
    );
};

export const generateAdminToken = (id: string, role: string) => {
    return jwt.sign({ id, role }, JWT_SECRET, { expiresIn: '12h' });
};
