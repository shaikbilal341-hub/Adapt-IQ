import { CorsOptions } from 'cors';

const parseAllowedOrigins = (): string[] => {
    const raw = process.env.ALLOWED_ORIGINS;
    if (!raw) return [];
    return raw.split(',').map(origin => origin.trim()).filter(Boolean);
};

export const corsOptions: CorsOptions = {
    origin: (origin, callback) => {
        // Allow requests with no Origin header (native Android apps, mobile Retrofit/OkHttp, curl, server-to-server)
        if (!origin) {
            return callback(null, true);
        }

        const allowedOrigins = parseAllowedOrigins();
        if (allowedOrigins.includes(origin)) {
            return callback(null, true);
        }

        // Allow localhost origins during non-production development testing
        if (process.env.NODE_ENV !== 'production' && /^https?:\/\/localhost(:\d+)?$/.test(origin)) {
            return callback(null, true);
        }

        // Reject untrusted web browser origins by withholding Access-Control-Allow-Origin
        return callback(null, false);
    },
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
};
