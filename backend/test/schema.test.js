import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const sql = await readFile(new URL('../db/002_marketplace.sql', import.meta.url), 'utf8');

test('esquema protege datos privados mediante RLS forzado', () => {
  for (const table of ['users','businesses','business_members','subscriptions','business_modes','listings','business_hours','reservations','payments','favorites']) {
    assert.match(sql, new RegExp(`ALTER TABLE ${table} ENABLE ROW LEVEL SECURITY;\\s*ALTER TABLE ${table} FORCE ROW LEVEL SECURITY;`), `${table} debe tener RLS habilitado y forzado`);
  }
});

test('esquema previene reservas de cita superpuestas a nivel PostgreSQL', () => {
  assert.match(sql, /EXCLUDE USING gist \(business_id WITH =, tstzrange\(starts_at, ends_at, '\[\)'\) WITH &&\)/);
  assert.match(sql, /WHERE \(mode = 'CITA' AND status NOT IN \('CANCELADA','NO_SHOW'\)\)/);
});

test('esquema incluye trial de 14 días y tarifas aún no fijadas', () => {
  assert.match(sql, /\('TRIAL','Prueba'.*?,0,14\)/);
  assert.match(sql, /\('BASIC'.*?NULL,0\)/);
  assert.match(sql, /\('PRO'.*?NULL,0\)/);
});

test('inventario se descuenta y devuelve con triggers transaccionales', () => {
  assert.match(sql, /CREATE TRIGGER reservations_decrement_stock\s+BEFORE INSERT ON reservations/);
  assert.match(sql, /CREATE TRIGGER reservations_restore_stock\s+AFTER UPDATE OF status ON reservations/);
  assert.match(sql, /RAISE EXCEPTION 'STOCK_UNAVAILABLE'/);
});

