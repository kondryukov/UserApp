CREATE TABLE IF NOT EXISTS public.users (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100)  NOT NULL,
  email       VARCHAR(254)  NOT NULL,
  age         INTEGER       NOT NULL,
  created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS users_email_ci_uidx ON public.users (lower(email));
