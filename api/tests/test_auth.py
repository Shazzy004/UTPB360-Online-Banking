from tests.conftest import token_de


def test_login_exitoso(client, usuarios):
    r = client.post("/api/v1/auth/login",
                    json={"email": "ana@utpb.com", "password": "123456"})
    assert r.status_code == 200
    body = r.json()
    assert "access_token" in body and "refresh_token" in body


def test_login_password_incorrecta(client, usuarios):
    r = client.post("/api/v1/auth/login",
                    json={"email": "ana@utpb.com", "password": "mala"})
    assert r.status_code == 401
    assert r.json()["codigo"] == "CREDENCIALES_INVALIDAS"


def test_login_email_inexistente_mismo_error(client, usuarios):
    # Mismo código de error que password mala: no se revela qué emails existen
    r = client.post("/api/v1/auth/login",
                    json={"email": "nadie@utpb.com", "password": "123456"})
    assert r.status_code == 401
    assert r.json()["codigo"] == "CREDENCIALES_INVALIDAS"


def test_ruta_protegida_sin_token(client, usuarios):
    r = client.get("/api/v1/cuentas")
    assert r.status_code == 401
    assert r.json()["codigo"] == "NO_AUTORIZADO"


def test_refresh_no_acepta_access_token(client, usuarios):
    login = client.post("/api/v1/auth/login",
                        json={"email": "ana@utpb.com", "password": "123456"}).json()
    # Intentar refrescar usando el ACCESS token debe fallar (type mismatch)
    r = client.post("/api/v1/auth/refresh",
                    json={"refresh_token": login["access_token"]})
    assert r.status_code == 401


def test_perfil_y_cuentas_del_usuario_autenticado(client, usuarios):
    headers = token_de(client, "ana@utpb.com")

    perfil = client.get("/api/v1/clientes/me", headers=headers).json()
    assert perfil["nombre"] == "Ana"

    cuentas = client.get("/api/v1/cuentas", headers=headers).json()
    assert len(cuentas) == 1
    assert cuentas[0]["numero_cuenta"] == "UTPB-0003-1290"
