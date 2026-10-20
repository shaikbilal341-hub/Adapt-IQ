import rateLimit from 'express-rate-limit';

export const adminLoginLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 5, // Limit each IP to 5 login requests per 15-minute window
    standardHeaders: true, // Return standard RateLimit-* headers
    legacyHeaders: false, // Disable X-RateLimit-* headers
    message: {
        error: 'Too many login attempts. Please try again later.'
    },
    statusCode: 429
});
