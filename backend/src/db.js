import pg from 'pg';

const { Pool } = pg;

export function createPool(connectionString) {
  return new Pool({
    connectionString,
    max: 15,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 5_000,
    application_name: 'unamad-citas-marketplace-api',
    ssl: process.env.PGSSL === 'true' ? { rejectUnauthorized: true } : undefined
  });
}

export async function withTransaction(pool, userId, work) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query("SELECT set_config('app.user_id', $1, true)", [userId || '']);
    const result = await work(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  } finally {
    client.release();
  }
}
