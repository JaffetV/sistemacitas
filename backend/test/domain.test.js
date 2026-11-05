import test from 'node:test';
import assert from 'node:assert/strict';
import {
  HttpError, canTransitionReservation, normalizeEmail,
  validateBusinessPayload, validateCustomerPayload, validateListingPayload,
  validateReservationPayload
} from '../src/domain.js';

const validBusiness = {
  name: 'Barbería Centro', categoryId: 'a8c43d28-0e84-4b57-954c-0e81e3b38a6c',
  phone: '999888777', department: 'Madre de Dios', province: 'Tambopata', district: 'Puerto Maldonado',
  address: 'Av. Principal 123', modes: ['CITA','RECOJO']
};

test('normaliza correo y rechaza formato inválido', () => {
  assert.equal(normalizeEmail('  CLIENTE@EXAMPLE.COM '), 'cliente@example.com');
  assert.throws(() => normalizeEmail('correo-sin-dominio'), HttpError);
});

test('cliente necesita datos mínimos y contraseña robusta', () => {
  const payload = validateCustomerPayload({ fullName: 'Ana Pérez', email: 'ana@example.com', password: 'clave-segura-123' });
  assert.equal(payload.fullName, 'Ana Pérez');
  assert.equal(payload.email, 'ana@example.com');
  assert.throws(() => validateCustomerPayload({ fullName: 'Ana', email: 'ana@example.com', password: '123' }), HttpError);
});

test('negocio admite una o varias modalidades conocidas', () => {
  assert.deepEqual(validateBusinessPayload(validBusiness).modes, ['CITA','RECOJO']);
  assert.throws(() => validateBusinessPayload({ ...validBusiness, modes: ['DELIVERY'] }), HttpError);
});

test('publicación valida tipo, modalidad, precio y stock', () => {
  const service = validateListingPayload({ title: 'Corte clásico', kind: 'SERVICIO', mode: 'CITA', price: 25, durationMinutes: 30 });
  assert.equal(service.durationMinutes, 30);
  assert.throws(() => validateListingPayload({ title: 'Sándwich', kind: 'PRODUCTO', mode: 'AMBOS', price: 10 }), HttpError);
  assert.throws(() => validateListingPayload({ title: 'Producto', kind: 'PRODUCTO', mode: 'RECOJO', price: 10, stock: -1 }), HttpError);
});

test('reserva requiere fecha futura, cantidad y canal válidos', () => {
  const payload = validateReservationPayload({ mode: 'CITA', quantity: 1, paymentMethod: 'EN_LOCAL', dateTime: '2099-01-01T10:00:00-05:00' });
  assert.equal(payload.mode, 'CITA');
  assert.throws(() => validateReservationPayload({ mode: 'CITA', quantity: 0, paymentMethod: 'EN_LOCAL', dateTime: '2099-01-01T10:00:00-05:00' }), HttpError);
  assert.throws(() => validateReservationPayload({ mode: 'CITA', quantity: 1, paymentMethod: 'EN_LOCAL', dateTime: '2099-01-01T10:00:00' }), HttpError);
});

test('transiciones de reserva siguen el ciclo permitido', () => {
  assert.equal(canTransitionReservation('SOLICITADA','CONFIRMADA'), true);
  assert.equal(canTransitionReservation('CONFIRMADA','LISTA'), true);
  assert.equal(canTransitionReservation('COMPLETADA','CANCELADA'), false);
  assert.equal(canTransitionReservation('SOLICITADA','COMPLETADA'), false);
});
