import dotenv from 'dotenv';
dotenv.config();

import bcrypt from 'bcrypt';
import prisma from '../config/database';

function parseArgs(): { email?: string; password?: string; role?: string } {
    const args = process.argv.slice(2);
    const parsed: { email?: string; password?: string; role?: string } = {};

    for (let i = 0; i < args.length; i++) {
        if (args[i] === '--email' && args[i + 1]) {
            parsed.email = args[i + 1].trim();
            i++;
        } else if (args[i] === '--password' && args[i + 1]) {
            parsed.password = args[i + 1];
            i++;
        } else if (args[i] === '--role' && args[i + 1]) {
            parsed.role = args[i + 1].trim().toUpperCase();
            i++;
        }
    }

    if (!parsed.email && process.env.ADMIN_EMAIL) {
        parsed.email = process.env.ADMIN_EMAIL.trim();
    }
    if (!parsed.password && process.env.ADMIN_PASSWORD) {
        parsed.password = process.env.ADMIN_PASSWORD;
    }
    if (!parsed.role && process.env.ADMIN_ROLE) {
        parsed.role = process.env.ADMIN_ROLE.trim().toUpperCase();
    }

    return parsed;
}

async function main() {
    const { email, password, role = 'RESEARCHER' } = parseArgs();

    if (!email || !password) {
        console.error('Error: Both --email and --password (or ADMIN_EMAIL and ADMIN_PASSWORD env vars) are required.');
        console.error('Usage: npm run admin:create -- --email <email> --password <password> [--role RESEARCHER|SUPER_ADMIN]');
        process.exit(1);
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        console.error('Error: Invalid email format.');
        process.exit(1);
    }

    if (password.length < 8) {
        console.error('Error: Password must be at least 8 characters in length.');
        process.exit(1);
    }

    const validRoles = ['RESEARCHER', 'SUPER_ADMIN'];
    if (!validRoles.includes(role)) {
        console.error(`Error: Invalid role '${role}'. Must be one of: ${validRoles.join(', ')}`);
        process.exit(1);
    }

    try {
        const passwordHash = await bcrypt.hash(password, 12);

        const user = await prisma.adminUser.upsert({
            where: { email },
            update: {
                passwordHash,
                role
            },
            create: {
                email,
                passwordHash,
                role
            }
        });

        console.log(`Success: Administrator account [${user.email}] provisioned with role [${user.role}].`);
    } catch (error: any) {
        console.error('Error provisioning administrator account:', error.message);
        process.exit(1);
    } finally {
        await prisma.$disconnect();
    }
}

main();
