-- Esquema PostgreSQL multiempresa. Se ejecuta una sola vez al inicializar el volumen.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  full_name varchar(140) NOT NULL,
  email citext NOT NULL UNIQUE,
  phone varchar(30),
  password_hash text NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BLOCKED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  last_login_at timestamptz
);
CREATE UNIQUE INDEX users_phone_unique ON users(phone) WHERE phone IS NOT NULL AND phone <> '';

CREATE TABLE categories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name varchar(100) NOT NULL UNIQUE,
  slug varchar(110) NOT NULL UNIQUE,
  emoji varchar(12) NOT NULL DEFAULT '🏪',
  active boolean NOT NULL DEFAULT true
);

CREATE TABLE businesses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  category_id uuid NOT NULL REFERENCES categories(id),
  name varchar(140) NOT NULL,
  description text NOT NULL DEFAULT '',
  phone varchar(30) NOT NULL,
  contact_email citext,
  country_code char(2) NOT NULL DEFAULT 'PE',
  time_zone varchar(64) NOT NULL DEFAULT 'America/Lima',
  department varchar(100) NOT NULL,
  province varchar(100) NOT NULL,
  district varchar(100) NOT NULL,
  address varchar(220) NOT NULL,
  reference text,
  latitude numeric(10,7),
  longitude numeric(10,7),
  coverage varchar(16) NOT NULL DEFAULT 'LOCAL' CHECK (coverage IN ('LOCAL')),
  state varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (state IN ('ACTIVE','PAUSED','SUSPENDED')),
  visible_marketplace boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))
);
CREATE INDEX businesses_category_state_idx ON businesses(category_id, state, visible_marketplace);
CREATE INDEX businesses_locality_idx ON businesses(department, province, district);
CREATE INDEX businesses_name_search_idx ON businesses USING gin (to_tsvector('spanish', name || ' ' || description));

CREATE TABLE business_members (
  business_id uuid NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role varchar(16) NOT NULL CHECK (role IN ('OWNER','MANAGER','STAFF')),
  status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (business_id, user_id)
);
CREATE INDEX business_members_user_idx ON business_members(user_id, status);

CREATE TABLE plans (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(20) NOT NULL UNIQUE,
  name varchar(80) NOT NULL,
  description text NOT NULL DEFAULT '',
  monthly_price numeric(12,2),
  trial_days integer NOT NULL DEFAULT 0 CHECK (trial_days >= 0),
  active boolean NOT NULL DEFAULT true
);

CREATE TABLE subscriptions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  business_id uuid NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  plan_id uuid NOT NULL REFERENCES plans(id),
  status varchar(16) NOT NULL CHECK (status IN ('TRIAL','ACTIVE','PAST_DUE','EXPIRED','CANCELLED')),
  started_at timestamptz NOT NULL,
  ends_at timestamptz,
  auto_renew boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK (ends_at IS NULL OR ends_at > started_at)
);
CREATE INDEX subscriptions_business_state_idx ON subscriptions(business_id, status, ends_at DESC);

CREATE TABLE business_modes (
  business_id uuid NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  mode varchar(12) NOT NULL CHECK (mode IN ('CITA','RECOJO')),
  PRIMARY KEY (business_id, mode)
);

CREATE TABLE listings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  business_id uuid NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  title varchar(140) NOT NULL,
  description text NOT NULL DEFAULT '',
  kind varchar(12) NOT NULL CHECK (kind IN ('SERVICIO','PRODUCTO')),
  mode varchar(8) NOT NULL CHECK (mode IN ('CITA','RECOJO','AMBOS')),
  price numeric(12,2) NOT NULL CHECK (price >= 0),
  compare_at_price numeric(12,2) CHECK (compare_at_price IS NULL OR compare_at_price >= price),
  duration_minutes integer CHECK (duration_minutes IS NULL OR duration_minutes BETWEEN 5 AND 1440),
  stock integer CHECK (stock IS NULL OR stock >= 0),
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX listings_business_active_idx ON listings(business_id, active, created_at DESC);

CREATE TABLE business_hours (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  business_id uuid NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  day_of_week smallint NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  opens_at time NOT NULL,
  closes_at time NOT NULL,
  active boolean NOT NULL DEFAULT true,
  CHECK (closes_at > opens_at),
  UNIQUE (business_id, day_of_week, opens_at, closes_at)
);
CREATE INDEX business_hours_business_day_idx ON business_hours(business_id, day_of_week, active);

CREATE TABLE reservations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(24) NOT NULL UNIQUE,
  business_id uuid NOT NULL REFERENCES businesses(id) ON DELETE RESTRICT,
  listing_id uuid NOT NULL REFERENCES listings(id) ON DELETE RESTRICT,
  customer_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  mode varchar(8) NOT NULL CHECK (mode IN ('CITA','RECOJO')),
  starts_at timestamptz,
  ends_at timestamptz,
  pickup_at timestamptz,
  quantity integer NOT NULL DEFAULT 1 CHECK (quantity > 0),
  unit_price numeric(12,2) NOT NULL CHECK (unit_price >= 0),
  total numeric(12,2) NOT NULL CHECK (total >= 0),
  status varchar(16) NOT NULL DEFAULT 'SOLICITADA'
    CHECK (status IN ('SOLICITADA','CONFIRMADA','LISTA','COMPLETADA','CANCELADA','NO_SHOW')),
  payment_method varchar(12) NOT NULL CHECK (payment_method IN ('EN_LINEA','EN_LOCAL')),
  payment_state varchar(12) NOT NULL DEFAULT 'PENDIENTE'
    CHECK (payment_state IN ('PENDIENTE','PAGADO','FALLIDO','REEMBOLSADO')),
  note text NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK (
    (mode = 'CITA' AND starts_at IS NOT NULL AND ends_at IS NOT NULL AND ends_at > starts_at AND pickup_at IS NULL)
    OR (mode = 'RECOJO' AND pickup_at IS NOT NULL AND starts_at IS NULL AND ends_at IS NULL)
  )
);
CREATE INDEX reservations_customer_date_idx ON reservations(customer_user_id, created_at DESC);
CREATE INDEX reservations_business_date_idx ON reservations(business_id, starts_at, pickup_at);
-- La base de datos impide carreras entre solicitudes simultáneas para el mismo negocio.
ALTER TABLE reservations ADD CONSTRAINT reservations_no_appointment_overlap
  EXCLUDE USING gist (business_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&)
  WHERE (mode = 'CITA' AND status NOT IN ('CANCELADA','NO_SHOW'));

CREATE TABLE payments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  reservation_id uuid REFERENCES reservations(id) ON DELETE RESTRICT,
  subscription_id uuid REFERENCES subscriptions(id) ON DELETE RESTRICT,
  concept varchar(16) NOT NULL CHECK (concept IN ('RESERVA','SUSCRIPCION')),
  amount numeric(12,2) NOT NULL CHECK (amount >= 0),
  currency char(3) NOT NULL DEFAULT 'PEN',
  channel varchar(12) NOT NULL CHECK (channel IN ('EN_LINEA','EN_LOCAL')),
  state varchar(12) NOT NULL DEFAULT 'PENDIENTE'
    CHECK (state IN ('PENDIENTE','PAGADO','FALLIDO','REEMBOLSADO')),
  provider varchar(50),
  external_reference varchar(150),
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK ((reservation_id IS NOT NULL AND subscription_id IS NULL AND concept = 'RESERVA') OR
         (reservation_id IS NULL AND subscription_id IS NOT NULL AND concept = 'SUSCRIPCION'))
);
CREATE INDEX payments_reservation_idx ON payments(reservation_id);

CREATE TABLE favorites (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  business_id uuid NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, business_id)
);

-- Funciones internas con privilegios del propietario de la migración. No se exponen como endpoints.
CREATE OR REPLACE FUNCTION app_current_user_id() RETURNS uuid
LANGUAGE sql STABLE AS $$ SELECT NULLIF(current_setting('app.user_id', true), '')::uuid $$;

CREATE OR REPLACE FUNCTION is_business_member(p_business_id uuid, p_roles text[] DEFAULT NULL) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM business_members bm
    WHERE bm.business_id = p_business_id
      AND bm.user_id = app_current_user_id()
      AND bm.status = 'ACTIVE'
      AND (p_roles IS NULL OR bm.role = ANY(p_roles))
  )
$$;

CREATE OR REPLACE FUNCTION is_business_owner(p_business_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM businesses b WHERE b.id = p_business_id AND b.owner_user_id = app_current_user_id())
$$;

CREATE OR REPLACE FUNCTION is_public_business(p_business_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM businesses b
    WHERE b.id = p_business_id AND b.state = 'ACTIVE' AND b.visible_marketplace = true
      AND EXISTS (
        SELECT 1 FROM subscriptions s
        WHERE s.business_id = b.id AND s.status IN ('TRIAL','ACTIVE')
          AND (s.ends_at IS NULL OR s.ends_at >= now())
      )
  )
$$;

CREATE OR REPLACE FUNCTION is_customer_visible_to_business(p_customer_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM reservations r
    JOIN business_members bm ON bm.business_id = r.business_id
    WHERE r.customer_user_id = p_customer_id
      AND bm.user_id = app_current_user_id() AND bm.status = 'ACTIVE'
  )
$$;

CREATE OR REPLACE FUNCTION has_customer_reservation(p_business_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM reservations r
    WHERE r.business_id = p_business_id AND r.customer_user_id = app_current_user_id()
  )
$$;

CREATE OR REPLACE FUNCTION has_listing_reservation(p_listing_id uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM reservations r
    WHERE r.listing_id = p_listing_id AND r.customer_user_id = app_current_user_id()
  )
$$;

CREATE OR REPLACE FUNCTION auth_lookup_user(p_email citext)
RETURNS TABLE(id uuid, full_name varchar, email citext, phone varchar, password_hash text, status varchar)
LANGUAGE sql SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT u.id,u.full_name,u.email,u.phone,u.password_hash,u.status
  FROM users u WHERE u.email = p_email LIMIT 1
$$;

-- El inventario se ajusta dentro de la misma transacción que crea/cancela la reserva.
CREATE OR REPLACE FUNCTION reserve_listing_stock_before_insert() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
  IF NEW.mode = 'RECOJO' THEN
    UPDATE listings
      SET stock = CASE WHEN stock IS NULL THEN NULL ELSE stock - NEW.quantity END,
          updated_at = now()
      WHERE id = NEW.listing_id AND business_id = NEW.business_id AND active = true
        AND (stock IS NULL OR stock >= NEW.quantity);
    IF NOT FOUND THEN
      RAISE EXCEPTION 'STOCK_UNAVAILABLE' USING ERRCODE = 'P0001';
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION restore_listing_stock_after_cancel() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
BEGIN
  IF OLD.status <> 'CANCELADA' AND NEW.status = 'CANCELADA' AND OLD.mode = 'RECOJO' THEN
    UPDATE listings SET stock = stock + OLD.quantity, updated_at = now()
      WHERE id = OLD.listing_id AND business_id = OLD.business_id AND stock IS NOT NULL;
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER reservations_decrement_stock
  BEFORE INSERT ON reservations FOR EACH ROW
  EXECUTE FUNCTION reserve_listing_stock_before_insert();
CREATE TRIGGER reservations_restore_stock
  AFTER UPDATE OF status ON reservations FOR EACH ROW
  EXECUTE FUNCTION restore_listing_stock_after_cancel();

-- Los usuarios solo pueden consultar su perfil; el negocio ve los datos de sus clientes con reservas.
ALTER TABLE users ENABLE ROW LEVEL SECURITY; ALTER TABLE users FORCE ROW LEVEL SECURITY;
CREATE POLICY users_read_self_or_customer ON users FOR SELECT USING (
  id = app_current_user_id() OR is_customer_visible_to_business(id)
);
CREATE POLICY users_create_self ON users FOR INSERT WITH CHECK (id = app_current_user_id());
CREATE POLICY users_update_self ON users FOR UPDATE
  USING (id = app_current_user_id()) WITH CHECK (id = app_current_user_id());

-- RLS: todas las tablas privadas se filtran por el usuario autenticado.
ALTER TABLE businesses ENABLE ROW LEVEL SECURITY; ALTER TABLE businesses FORCE ROW LEVEL SECURITY;
CREATE POLICY businesses_read ON businesses FOR SELECT USING (
  owner_user_id = app_current_user_id() OR is_business_member(id) OR is_public_business(id) OR has_customer_reservation(id)
);
CREATE POLICY businesses_create ON businesses FOR INSERT WITH CHECK (owner_user_id = app_current_user_id());
CREATE POLICY businesses_manage ON businesses FOR UPDATE
  USING (is_business_member(id, ARRAY['OWNER','MANAGER']))
  WITH CHECK (is_business_member(id, ARRAY['OWNER','MANAGER']));

ALTER TABLE business_members ENABLE ROW LEVEL SECURITY; ALTER TABLE business_members FORCE ROW LEVEL SECURITY;
CREATE POLICY business_members_read ON business_members FOR SELECT USING (
  user_id = app_current_user_id() OR is_business_member(business_id)
);
CREATE POLICY business_members_create ON business_members FOR INSERT WITH CHECK (
  (user_id = app_current_user_id() AND is_business_owner(business_id))
  OR is_business_member(business_id, ARRAY['OWNER','MANAGER'])
);
CREATE POLICY business_members_manage ON business_members FOR UPDATE
  USING (is_business_member(business_id, ARRAY['OWNER','MANAGER']))
  WITH CHECK (is_business_member(business_id, ARRAY['OWNER','MANAGER']));

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY; ALTER TABLE subscriptions FORCE ROW LEVEL SECURITY;
CREATE POLICY subscriptions_read ON subscriptions FOR SELECT USING (is_business_member(business_id) OR is_public_business(business_id));
CREATE POLICY subscriptions_create ON subscriptions FOR INSERT WITH CHECK (is_business_member(business_id, ARRAY['OWNER','MANAGER']));
CREATE POLICY subscriptions_manage ON subscriptions FOR UPDATE
  USING (is_business_member(business_id, ARRAY['OWNER','MANAGER']))
  WITH CHECK (is_business_member(business_id, ARRAY['OWNER','MANAGER']));

ALTER TABLE business_modes ENABLE ROW LEVEL SECURITY; ALTER TABLE business_modes FORCE ROW LEVEL SECURITY;
CREATE POLICY business_modes_read ON business_modes FOR SELECT USING (is_business_member(business_id) OR is_public_business(business_id));
CREATE POLICY business_modes_manage ON business_modes FOR ALL
  USING (is_business_member(business_id, ARRAY['OWNER','MANAGER']))
  WITH CHECK (is_business_member(business_id, ARRAY['OWNER','MANAGER']));

ALTER TABLE listings ENABLE ROW LEVEL SECURITY; ALTER TABLE listings FORCE ROW LEVEL SECURITY;
CREATE POLICY listings_read ON listings FOR SELECT USING (
  (active = true AND is_public_business(business_id)) OR is_business_member(business_id) OR has_listing_reservation(id)
);
CREATE POLICY listings_create ON listings FOR INSERT WITH CHECK (is_business_member(business_id, ARRAY['OWNER','MANAGER']));
CREATE POLICY listings_manage ON listings FOR UPDATE
  USING (is_business_member(business_id, ARRAY['OWNER','MANAGER']))
  WITH CHECK (is_business_member(business_id, ARRAY['OWNER','MANAGER']));
CREATE POLICY listings_delete ON listings FOR DELETE USING (is_business_member(business_id, ARRAY['OWNER','MANAGER']));

ALTER TABLE business_hours ENABLE ROW LEVEL SECURITY; ALTER TABLE business_hours FORCE ROW LEVEL SECURITY;
CREATE POLICY business_hours_read ON business_hours FOR SELECT USING (is_business_member(business_id) OR is_public_business(business_id));
CREATE POLICY business_hours_manage ON business_hours FOR ALL
  USING (is_business_member(business_id, ARRAY['OWNER','MANAGER']))
  WITH CHECK (is_business_member(business_id, ARRAY['OWNER','MANAGER']));

ALTER TABLE reservations ENABLE ROW LEVEL SECURITY; ALTER TABLE reservations FORCE ROW LEVEL SECURITY;
CREATE POLICY reservations_read ON reservations FOR SELECT USING (
  customer_user_id = app_current_user_id() OR is_business_member(business_id)
);
CREATE POLICY reservations_create ON reservations FOR INSERT WITH CHECK (
  customer_user_id = app_current_user_id() AND is_public_business(business_id)
);
CREATE POLICY reservations_manage ON reservations FOR UPDATE
  USING (customer_user_id = app_current_user_id() OR is_business_member(business_id, ARRAY['OWNER','MANAGER','STAFF']))
  WITH CHECK (customer_user_id = app_current_user_id() OR is_business_member(business_id, ARRAY['OWNER','MANAGER','STAFF']));

ALTER TABLE payments ENABLE ROW LEVEL SECURITY; ALTER TABLE payments FORCE ROW LEVEL SECURITY;
CREATE POLICY payments_read ON payments FOR SELECT USING (
  customer_user_id = app_current_user_id()
  OR (reservation_id IS NOT NULL AND EXISTS (
    SELECT 1 FROM reservations r WHERE r.id = reservation_id AND is_business_member(r.business_id)
  ))
  OR (subscription_id IS NOT NULL AND EXISTS (
    SELECT 1 FROM subscriptions s WHERE s.id = subscription_id AND is_business_member(s.business_id)
  ))
);
CREATE POLICY payments_create ON payments FOR INSERT WITH CHECK (customer_user_id = app_current_user_id());

ALTER TABLE favorites ENABLE ROW LEVEL SECURITY; ALTER TABLE favorites FORCE ROW LEVEL SECURITY;
CREATE POLICY favorites_owner_all ON favorites FOR ALL USING (user_id = app_current_user_id())
  WITH CHECK (user_id = app_current_user_id());

INSERT INTO categories(name, slug, emoji) VALUES
  ('Barbería y peluquería','barberia-peluqueria','💈'),
  ('Belleza y uñas','belleza-unas','💅'),
  ('Salud y bienestar','salud-bienestar','🩺'),
  ('Comida y restaurantes','comida-restaurantes','🍽️'),
  ('Talleres y mantenimiento','talleres-mantenimiento','🛠️'),
  ('Tiendas y productos','tiendas-productos','🛍️'),
  ('Educación y asesorías','educacion-asesorias','📚'),
  ('Otros negocios','otros','🏪')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO plans(code, name, description, monthly_price, trial_days) VALUES
  ('TRIAL','Prueba','Acceso inicial de 14 días',0,14),
  ('BASIC','Básico','Plan comercial básico; tarifa por definir',NULL,0),
  ('PRO','Profesional','Plan comercial ampliado; tarifa por definir',NULL,0)
ON CONFLICT (code) DO NOTHING;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO marketplace_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO marketplace_app;
REVOKE ALL ON FUNCTION auth_lookup_user(citext) FROM PUBLIC;
REVOKE ALL ON FUNCTION is_business_member(uuid,text[]) FROM PUBLIC;
REVOKE ALL ON FUNCTION is_business_owner(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION is_public_business(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION is_customer_visible_to_business(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION has_customer_reservation(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION has_listing_reservation(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION app_current_user_id() FROM PUBLIC;
REVOKE ALL ON FUNCTION reserve_listing_stock_before_insert() FROM PUBLIC;
REVOKE ALL ON FUNCTION restore_listing_stock_after_cancel() FROM PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO marketplace_app;
