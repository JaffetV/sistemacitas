import { createHmac, randomBytes, scrypt as scryptCallback, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';
import { HttpError } from './domain.js';

const scrypt = promisify(scryptCallback);
const SCRYPT_OPTIONS = { N: 16384, r: 8, p: 1, maxmem: 64 * 1024 * 1024 };

export async function hashPassword(password) {
  const salt = randomBytes(16);
  const derived = await scrypt(password, salt, 64, SCRYPT_OPTIONS);
  return `scrypt$16384$8$1$${salt.toString('base64url')}$${derived.toString('base64url')}`;
}

export async function verifyPassword(password, storedHash) {
  try {
    const [algorithm, n, r, p, saltText, hashText] = storedHash.split('$');
    if (algorithm !== 'scrypt' || Number(n) !== 16384 || Number(r) !== 8 || Number(p) !== 1) return false;
    const salt = Buffer.from(saltText, 'base64url');
    const expected = Buffer.from(hashText, 'base64url');
    const actual = await scrypt(password, salt, expected.length, SCRYPT_OPTIONS);
    return expected.length === actual.length && timingSafeEqual(expected, actual);
  } catch {
    return false;
  }
}

function encode(value) {
  return Buffer.from(JSON.stringify(value)).toString('base64url');
}

export function createAccessToken(user, secret, lifetimeSeconds = 60 * 60 * 12) {
  const now = Math.floor(Date.now() / 1000);
  const header = encode({ alg: 'HS256', typ: 'JWT' });
  const payload = encode({ iss: 'unamad-citas-api', sub: user.id, email: user.email, iat: now, exp: now + lifetimeSeconds });
  const data = `${header}.${payload}`;
  const signature = createHmac('sha256', secret).update(data).digest('base64url');
  return `${data}.${signature}`;
}

export function verifyAccessToken(token, secret) {
  const parts = typeof token === 'string' ? token.split('.') : [];
  if (parts.length !== 3) throw new HttpError(401, 'La sesión no es válida o ha vencido.', 'UNAUTHENTICATED');
  const data = `${parts[0]}.${parts[1]}`;
  const expected = createHmac('sha256', secret).update(data).digest();
  let provided;
  let payload;
  try {
    provided = Buffer.from(parts[2], 'base64url');
    payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
  } catch {
    throw new HttpError(401, 'La sesión no es válida o ha vencido.', 'UNAUTHENTICATED');
  }
  if (provided.length !== expected.length || !timingSafeEqual(provided, expected)) {
    throw new HttpError(401, 'La sesión no es válida o ha vencido.', 'UNAUTHENTICATED');
  }
  if (payload.iss !== 'unamad-citas-api' || !payload.sub || !Number.isFinite(payload.exp) || payload.exp <= Math.floor(Date.now() / 1000)) {
    throw new HttpError(401, 'La sesión no es válida o ha vencido.', 'UNAUTHENTICATED');
  }
  return { id: payload.sub, email: payload.email };
}

export function bearerToken(request, secret) {
  const header = request.headers.authorization || '';
  const match = /^Bearer\s+(.+)$/i.exec(header);
  if (!match) throw new HttpError(401, 'Inicia sesión para continuar.', 'UNAUTHENTICATED');
  return verifyAccessToken(match[1], secret);
}
